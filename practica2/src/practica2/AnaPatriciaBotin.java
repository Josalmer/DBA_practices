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
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @author Domingo Lopez
     * @author Miguel García
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
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @author Domingo Lopez
     * @author Miguel García
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
                case ABOVE_LUDWIG:
                    this.getLudwig();
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
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @author Domingo Lopez
     * @author Miguel García
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
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @author Domingo Lopez
     * @author Miguel García
     * @param answer
     */
    void reactiveBehaviour() {
        if (this.knowledge.distanceToObjective == 0) {
            Info("Estoy encima de Ludwig");
            this.status = AgentStatus.ABOVE_LUDWIG;
            Info("Changed status to: " + this.status);
        } else if (this.knowledge.nActionsExecuted > 1000) {
            Info("1000 acciones y no encuentro el objetivo");
            this.status = AgentStatus.FINISHED;
            Info("Changed status to: " + this.status);
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
     * @author Jose Saldaña
     * @author Manuel Pancorbo
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
     * @author Jose Saldaña
     * @author Manuel Pancorbo
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
        boolean onWantedBox = false;
        int provisionalOrientation = this.knowledge.orientation;
        int provisionalHeight = this.knowledge.currentHeight;

        while (!onWantedBox) {
            AgentAction nextAction;
            if (orientation != provisionalOrientation) {
                int turns = this.knowledge.howManyTurns(orientation);
                if (this.knowledge.shouldTurnRight(turns)) {
                    nextAction = AgentAction.rotateR;
                    provisionalOrientation = this.knowledge.getNextOrientation(orientation, true);
                } else { // shouldTurnLeft
                    nextAction = AgentAction.rotateL;
                    provisionalOrientation = this.knowledge.getNextOrientation(orientation, false);
                }
            } else if (provisionalHeight <= targetHeight) {
                nextAction = AgentAction.moveUp;
                provisionalHeight += 5;
            } else {
                nextAction = AgentAction.moveF;
                onWantedBox = true;
            }
            this.plan.add(nextAction);
            cost += this.knowledge.energyCost(nextAction, 0);
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
     * @author Jose Saldaña
     * @return 
     */
    boolean toLand() {
        if (this.knowledge.currentHeight - this.knowledge.getFloorHeight() >= 5) {
            this.doAction(AgentAction.moveD);
            return false;
        } else {
            this.doAction(AgentAction.touchD);
            return true;
        }
    }
    
    /**
     * @author Jose Saldaña
     */
    void getLudwig() {
        if (this.toLand()) {
            Info("Rescatado Ludwig");
            this.status = AgentStatus.FINISHED;
            Info("Changed status to: " + this.status);
        }
    }

    /**
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     */
    void rechargeBattery() {
        if (this.toLand()) {
            this.doAction(AgentAction.recharge);
            this.status = AgentStatus.READY;
            Info("Changed status to: " + this.status);
        }
    }

    // ------------------------------------------------------------------
    // Read sensors and update perception--------------------------------
    /**
     * @author Jose Saldaña
     * @author Manuel Pancorbo
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
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @param newPerception
     */
    void updatePerception(JsonArray newPerception) {
        this.perception.asignValues(newPerception);
        Info(this.perception.toString());
    }

    /**
     * @author Jose Saldaña
     * @author Manuel Pancorbo
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
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @param action
     */
    void executeAction(AgentAction action) {
        switch (action) {
            case recharge:
                this.plan = null;
                this.knowledge.energy = 1000;
                break;
            default:
                this.knowledge.manageMovement(action);
                this.knowledge.nActionsExecuted += 1;
                this.useEnergy(action);
                break;
        }
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
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @author Domingo Lopez
     * @author Miguel García
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
            this.myControlPanel.feedData(in, this.knowledge.mapWidth, this.knowledge.mapHeight, this.knowledge.maxFlight);
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
