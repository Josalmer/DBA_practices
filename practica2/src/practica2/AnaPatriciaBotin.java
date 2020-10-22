package practica2;

import ControlPanel.TTYControlPanel;
import IntegratedAgent.IntegratedAgent;
import com.eclipsesource.json.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Arrays;

public class AnaPatriciaBotin extends IntegratedAgent {

    ACLMessage outChannel = new ACLMessage();
    AgentAction lastAction;
    ArrayList<AgentAction> plan;
    String receiver;
    String sessionKey;
    AgentStatus status;
    Knowledge knowledge = new Knowledge();
    Perception perception = new Perception();

    // Selected sensors
    ArrayList<String> requestedSensors = new ArrayList<String>(Arrays.asList("gps", "compass", "distance", "angular", "visual"));

    // Authorized sensors
    ArrayList<String> authorizedSensors = new ArrayList();

    // Control Panel
    boolean showPanel = false; // True to show SensorControlPanel
    TTYControlPanel myControlPanel;

    /**
     * @author Jose Saldaña, Manuel Pancorbo, Domingo Lopez, Miguel García
     */
    @Override
    public void setup() {
        super.setup();
        doCheckinPlatform();
        doCheckinLARVA();
        this.receiver = this.whoLarvaAgent();
        this.status = AgentStatus.INITIALIZING;
        if (this.showPanel) {
            this.myControlPanel = new TTYControlPanel(getAID());
        }
        _exitRequested = false;
    }

    /**
     * @author Jose Saldaña, Manuel Pancorbo, Domingo Lopez, Miguel García
     */
    @Override
    public void plainExecute() {
        while (!_exitRequested) {
            Info("Current Status: " + this.status);
            switch (this.status) {
                case INITIALIZING:
                    this.login();
                    break;
                case NEED_SENSOR:
                    this.readSensors();
                    break;
                case RECHARGING:
                    this.rechargeBattery();
                    break;
                case READY:
                    this.reactiveBehaviour();
                    break;
                case FINISHED:
                    this.logout();
                    break;
            }
        }
    }

    // Login and Initializate -------------------------------------------
    /**
     * @author Jose Saldaña
     * @param params
     */
    void login() {
        JsonObject params = new JsonObject();
        params.add("command", "login");
        params.add("world", "BasePlayground");
        JsonArray sensors = new JsonArray();
        for (String sensor : this.requestedSensors) {
            sensors.add(sensor);
        }
        params.add("attach", sensors);

        JsonObject answer = this.sendAndReceiveLogin(params);

        if (answer.get("result").asString().equals("ok")) {
            this.initializeAgent(answer);
            this.status = AgentStatus.NEED_SENSOR;
            Info("Changed status to: " + this.status);
        } else {
            this.logout();
        }
    }

    /**
     * @author Jose Saldaña
     * @param params
     * @return
     */
    JsonObject sendAndReceiveLogin(JsonObject params) {
        ACLMessage out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(this.receiver, AID.ISLOCALNAME));
        String parsedParams = params.toString();
        Info("Request: " + parsedParams);
        out.setContent(parsedParams);
        this.sendServer(out);
        ACLMessage in = this.blockingReceive();
        this.outChannel = in.createReply();
        String answer = in.getContent();
        Info("Answer: " + answer);
        JsonObject parsedAnswer = Json.parse(answer).asObject();
        return parsedAnswer;
    }

    /**
     * @author Jose Saldaña, Manuel Pancorbo, Domingo Lopez, Miguel García
     * @param answer
     */
    void initializeAgent(JsonObject answer) {
        this.sessionKey = answer.get("key").asString();
        for (JsonValue j : answer.get("capabilities").asArray()) {
            authorizedSensors.add(j.asString());
        }
        this.knowledge.initializeKnowledge(answer);
    }

    // ------------------------------------------------------------------
    // Execution --------------------------------------------------------
    /**
     * @author Jose Saldaña, Manuel Pancorbo, Domingo Lopez, Miguel García
     * @param answer
     */
    void reactiveBehaviour() {
        if (this.knowledge.distanceToObjective == 0) {
            Info("Estoy en el objetivo");
            this.status = AgentStatus.FINISHED;
        } else if (this.knowledge.nActionsExecuted > 200) {
            Info("200 acciones y no encuentro el objetivo");
            this.status = AgentStatus.FINISHED;
        } else {
            if (this.knowledge.energy < ((2 * (this.knowledge.currentHeight - this.knowledge.getFloorHeight())) + 30)) {
                this.status = AgentStatus.RECHARGING;
                Info("Changed status to: " + this.status);
            } else {
                if (this.plan != null) {
                    this.executePlan();
                } else {
                    this.thinkPlan();
                }
            }
        }
    }

    /**
     * @author Miguel García
     * @return nextAction
     */
    void thinkPlan() {
        ArrayList<AgentOption> options = this.generateOptions();
        if (options != null) {
            double max = 0;
            AgentOption winner = null;
            for (AgentOption o : options) {
                if (o.puntuationCostRelation > max) {
                    max = o.puntuationCostRelation;
                    winner = o;
                }
            }
            if (winner != null) {
                this.plan = winner.plan;
            } else {
                throw new RuntimeException("No hay un plan ganador");
            }
        }
    }

    /**
     * @author Jose Saldaña, Manuel Pancorbo
     * @return ArrayList(AgentOption)
     */
    ArrayList<AgentOption> generateOptions() {
        ArrayList<AgentOption> options = new ArrayList<>();
        int[] orientations = {-45, 0, 45, -90, 0, 90, -135, 180, 135};
        for (int i = 0; i < 9; i++) {
            if (i != 4) { // Not check current position
                int xPosition = this.knowledge.currentPositionX - 1 + (i / 3);
                int yPosition = this.knowledge.currentPositionY - 1 + (i % 3);
                int orientation = orientations[i];
                if (xPosition >= 0 && xPosition < this.knowledge.mapWidth && yPosition >= 0 && yPosition < this.knowledge.mapHeight) {
                    int targetHeight = this.knowledge.map.get(xPosition).get(yPosition);
                    if (targetHeight == -1) {
                        this.status = AgentStatus.NEED_SENSOR;
                        Info("Changed status to: " + this.status);
                        return null;
                    }
                    if (targetHeight <= this.knowledge.maxFlight) {
                        options.add(this.generateOption(xPosition, yPosition, targetHeight, orientation));
                    }
                }
            }
        }
        return options;
    }

    /**
     * @author Jose Saldaña, Manuel Pancorbo
     * @param xPosition
     * @param yPosition
     * @param targetHeight
     * @param orientation
     * @return AgentOption
     */
    AgentOption generateOption(int xPosition, int yPosition, int targetHeight, int orientation) {
        AgentOption option = new AgentOption(xPosition, yPosition, targetHeight);
        ArrayList<AgentAction> plan = new ArrayList<>();
        int cost = 0;
        boolean onTarget = false;
        int provisionalOrientation = this.knowledge.orientation;
        int provisionalHeight = this.knowledge.currentHeight;

        while (!onTarget) {
            if (orientation != provisionalOrientation) {
                plan.add(AgentAction.rotateL);
                if (provisionalOrientation != -135) {
                    provisionalOrientation -= 45;
                } else {
                    provisionalOrientation = 180;
                }
                cost += this.knowledge.energyCost(AgentAction.rotateL, 0);
            } else if (provisionalHeight <= targetHeight) {
                plan.add(AgentAction.moveUp);
                provisionalHeight += 5;
                cost += this.knowledge.energyCost(AgentAction.moveUp, 0);
            } else {
                plan.add(AgentAction.moveF);
                cost += this.knowledge.energyCost(AgentAction.moveF, 0);
                onTarget = true;
            }
        }
        option.plan = plan;
        option.cost = cost;
        option.calculatePuntuation(orientation, this.knowledge.angular);
        return option;
    }

    /**
     * @author Miguel García
     */
    void executePlan() {
        this.doAction(this.plan.get(0));
        this.plan.remove(0);
        if (this.plan.size() == 0)  {
            this.plan = null;
            if (this.knowledge.distanceToObjective < 3) {
                this.status = AgentStatus.NEED_SENSOR;
                Info("Changed status to: " + this.status);
            }
        }
    }

    /**
     * @author Jose Saldaña, Manuel Pancorbo
     */
    void rechargeBattery() {
        if (this.knowledge.currentHeight - this.knowledge.getFloorHeight() >= 5) {
            this.doAction(AgentAction.moveD);
        } else {
            this.doAction(AgentAction.touchD);
//            this.doAction(AgentAction.RECARGAR);
            this.status = AgentStatus.READY;
            Info("Changed status to: " + this.status);
        }
    }

    // ------------------------------------------------------------------
    // Read sensors and update perception--------------------------------
    /**
     * @author Jose Saldaña, Manuel Pancorbo
     */
    void readSensors() {
        JsonObject params = new JsonObject();
        params.add("command", "read");
        params.add("key", this.sessionKey);

        JsonObject answer = this.sendAndReceiveMessage(params);

        if (answer.get("result").asString().equals("ok")) {
            Info("Valores de los sensores leídos...");
            this.useEnergy(AgentAction.LECTURA_SENSORES);
            this.updatePerception(answer.get("details").asObject().get("perceptions").asArray());
            this.updateKnowledge();
            this.status = AgentStatus.READY;
            Info("Changed status to: " + this.status);
        } else {
            this.logout();
        }
    }

    /**
     * @author Jose Saldaña, Manuel Pancorbo
     * @param newPerception
     */
    void updatePerception(JsonArray newPerception) {
        this.perception.asignValues(newPerception);
        Info(this.perception.toString());
    }

    /**
     * @author Jose Saldaña, Manuel Pancorbo
     */
    void updateKnowledge() {
        this.knowledge.currentPositionX = this.perception.gps.get(0);
        this.knowledge.currentPositionY = this.perception.gps.get(1);
        this.knowledge.currentHeight = this.perception.gps.get(2);
        this.knowledge.orientation = this.perception.compass;
        this.knowledge.angular = this.perception.angular;
        this.knowledge.distanceToObjective = this.perception.distance;
        for (int i = 0; i < this.perception.visual.size(); i++) {
            for (int j = 0; j < this.perception.visual.get(i).size(); j++) {
                int xPosition = this.knowledge.currentPositionX - 3 + i;
                int yPosition = this.knowledge.currentPositionY - 3 + j;
                if (xPosition >= 0 && yPosition >= 0 && xPosition < this.knowledge.mapWidth && yPosition < this.knowledge.mapHeight) {
                    this.knowledge.map.get(xPosition).set(yPosition, this.perception.visual.get(i).get(j));
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Agent low level functions ----------------------------------------
    //TODO
    //Gestión de errores. En vez de void, que devuelva un Integer y si es código -1 por ejemplo ya sabemos que algo ha ido mal
    /**
     * @author Domingo Lopez
     * @param action
     */
    void doAction(AgentAction action) { // Recibe enumerado, hace 6 acciones, actualiza estado del mundo

        JsonObject params = new JsonObject();
        params.add("command", "execute");
        params.add("action", action.toString());
        params.add("key", this.sessionKey);

        JsonObject answer = this.sendAndReceiveMessage(params);

        if (answer.get("result").asString().equals("ok")) {
            this.executeAction(action);
            Info("Acción realizada:" + action.toString());
            this.lastAction = action;
        } else {
            this.logout();
        }

    }

    /**
     * @author Domingo Lopez
     * @param action
     */
    void executeAction(AgentAction action) {
        switch (action) {
            case moveF:
                this.knowledge.moveForward();
                break;
            case moveUp:
                this.knowledge.currentHeight += 5;
                break;
            case moveD:
                this.knowledge.currentHeight -= 5;
                break;
            case rotateL:
                if (this.knowledge.orientation != -135) {
                    this.knowledge.orientation -= 45;
                } else {
                    this.knowledge.orientation = 180;
                }
                break;
            case rotateR:
                if (this.knowledge.orientation != 180) {
                    this.knowledge.orientation += 45;
                } else {
                    this.knowledge.orientation = -135;
                }
                break;
            case touchD:
                this.knowledge.currentHeight = this.knowledge.getFloorHeight();
                break;
            case RECHARGE:
                this.knowledge.energy = 1000;
                this.plan = null;
                break;
        }
        this.knowledge.nActionsExecuted += 1;
        this.useEnergy(action); // Cambiar función useEnergy por estructura de datos que almacena la energia de cada acción, y usarla aqui y en el thinkplan
    }

    /**
     * @author Domingo Lopez
     * @param action
     */
    void useEnergy(AgentAction action) {
        this.knowledge.energy -= this.knowledge.energyCost(action, this.authorizedSensors.size());
    }

    // ------------------------------------------------------------------
    // Logout and takeDown-----------------------------------------------
    /**
     * @author Jose Saldaña
     */
    void logout() {
        JsonObject params = new JsonObject();
        params.add("command", "logout");

        this.sendMessage(params);

        if (this.showPanel) {
            this.myControlPanel.close();
        }

        _exitRequested = true;
    }

    /**
     * @author Jose Saldaña, Manuel Pancorbo, Domingo Lopez, Miguel García
     */
    @Override
    public void takeDown() {
        this.doCheckoutLARVA();
        this.doCheckoutPlatform();
        super.takeDown();
    }

    // ------------------------------------------------------------------
    // Communications----------------------------------------------------
    /**
     * @author Jose Saldaña
     * @param params
     * @return
     */
    JsonObject sendAndReceiveMessage(JsonObject params) {
        String parsedParams = params.toString();
        Info("Request: " + parsedParams);
        this.outChannel.setContent(parsedParams);
        this.sendServer(this.outChannel);
        ACLMessage in = this.blockingReceive();
        if (this.showPanel && params.get("command").asString().equals("read")) {
            this.myControlPanel.feedData(in, this.knowledge.mapWidth, this.knowledge.mapHeight);
            this.myControlPanel.fancyShow();
        }
        String answer = in.getContent();
        Info("Answer: " + answer);
        JsonObject parsedAnswer = Json.parse(answer).asObject();
        return parsedAnswer;
    }

    /**
     * @author Jose Saldaña
     * @param params
     */
    void sendMessage(JsonObject params) {
        String parsedParams = params.toString();
        Info("Request: " + parsedParams);
        this.outChannel.setContent(parsedParams);
        this.sendServer(this.outChannel);
    }

    // ------------------------------------------------------------------
    // Misc--------------------------------------------------------------
}
