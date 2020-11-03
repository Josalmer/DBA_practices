package practica2;

import ControlPanel.TTYControlPanel;
import IntegratedAgent.IntegratedAgent;
import com.eclipsesource.json.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Arrays;

public class AnaPatriciaBotin extends IntegratedAgent {
    
    // AGENT CONFIGURATION  -------------------------------------------
    String world = "World9";   // Select World
    boolean showPanel = true;      // True to show SensorControlPanel
    // Select sensors
    ArrayList<String> requestedSensors = new ArrayList<String>(Arrays.asList("gps", "compass", "distance", "angular", "visual"));
    // END CONFIGURATION  ---------------------------------------------
    
    ACLMessage outChannel = new ACLMessage();
    AgentAction lastAction;
    ArrayList<AgentAction> plan;
    String receiver;
    String sessionKey;
    AgentStatus status;
    Knowledge knowledge = new Knowledge();
    Perception perception = new Perception();

    // Authorized sensors
    ArrayList<String> authorizedSensors = new ArrayList();

    // Control Panel
    TTYControlPanel myControlPanel;

    /**
     * Inizializa y confugira el agente en la plataforma de agentes
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @author Domingo Lopez
     * @author Miguel García
     * 
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
     * Es el método principal de ejecución del agente.
     * Dependiendo del estado en que se encuentre el agente se realizará una opción u otra
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
     * Logea el agente en la plataforma e inicializa el agente,
     * estableciendo los canales de comunicación
     * @author Jose Saldaña
     */
    void login() {
        JsonObject params = new JsonObject();
        params.add("command", "login");
        params.add("world", this.world);
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
     * Manda la petición de login y crea el outChannel para posteriores comunicaciones
     * @author Jose Saldaña
     * @param params El JsonObject: command, world, attach
     * @return La respuesta del login como JsonObject
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
     * Inicializa el agente
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @author Domingo Lopez
     * @author Miguel García
     * @param answer Objeto json  enviado por el agente de LARVA
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
     * Contiene el comportamiento reactivo del agente. 
     * En este método el agente valora cual es su situación en el mapa 
     * y en función de esta actualiza su estado
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @author Domingo Lopez
     * @author Miguel García
     * 
     */
    void reactiveBehaviour() {
        if (this.knowledge.amIAboveLudwig()) {
            Info("Estoy encima de Ludwig");
            this.status = AgentStatus.ABOVE_LUDWIG;
            Info("Changed status to: " + this.status);
        } else if (this.knowledge.cantReachTarget()) {
            Info("No encuentro el objetivo");
            this.status = AgentStatus.FINISHED;
            Info("Changed status to: " + this.status);
        } else {
            if (this.knowledge.needRecharge()) {
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
     * Método que piensa el plan que va a seguir el agente.
     * En este método se valora las distintas opciones posibles y se escoge la mejor 
     * que será la que constituirá el plan que ejecutará el agente
     * @author Miguel García
     * 
     */
    void thinkPlan() {
        ArrayList<AgentOption> options = this.generateOptions();
        ArrayList<AgentOption> noVisitedOptions = new ArrayList<>();
        if (options != null) {
            for (AgentOption o : options) {
                if (o.visitedAt == -1) {
                    noVisitedOptions.add(o);
                }
            }
            if (noVisitedOptions.size() > 0) {
                options = noVisitedOptions;
            } else {
                AgentOption bestOption;
                bestOption = chooseFromAlreadyVisitedOptions(options);
                noVisitedOptions.add(bestOption);
                options = noVisitedOptions;
            }
            AgentOption winner;
            winner = chooseFromNoVisitedOptions(options);
            if (winner != null) {
                this.plan = winner.plan;
                if (this.knowledge.shouldIRechargueFirst(winner))
                    this.status = AgentStatus.RECHARGING;
            } else {
                throw new RuntimeException("No hay un plan ganador");
            }
        }
    }
    
    /**
     * Elige la mejor casilla de entre las ya visitadas que contenga la mejor valoración según el momento
     * en el que la visitó.
     * Escoge aquella que visitó antes, es decir, la menos reciente.
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @author Miguel García
     * @param options, array de AgentOption que contiene las casillas disponibles que ya estan visitadas
     * @return devuelve la mejor casilla a la que moverse
     */
    AgentOption chooseFromAlreadyVisitedOptions(ArrayList<AgentOption> options){
         double lastVisited = options.get(0).visitedAt;
         AgentOption bestOption = options.get(0);
         for (AgentOption o : options) {
            if (o.visitedAt < lastVisited) {
                  bestOption = o;
                  lastVisited = o.visitedAt;
             }
         }    
         return bestOption;
    }
    
   /**
     * Elige la mejor casilla de entre las no visitadas que contenga la mejor valoración según la distancia al objetivo.
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @author Miguel García
     * @param options, array de AgentOption que contiene las casillas disponibles que no estan visitadas
     * @return devuelve la mejor casilla a la que moverse
     */
    AgentOption chooseFromNoVisitedOptions(ArrayList<AgentOption> options){
        double min = options.get(0).distanceToLudwig;
        AgentOption bestOption = options.get(0);
        for (AgentOption o : options) {
            if (o.distanceToLudwig < min) {
                min = o.distanceToLudwig;
                bestOption = o;
            }
        }
         return bestOption;
    }

    /**
     * Genera opciones a partir de las casillas contiguas al agente, 
     * descarta aquellas no viables y devuelve un array de AgentAction con las posibles.
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @return array que contiene todas las AgentOption viables.
     */
    ArrayList<AgentOption> generateOptions() {
        ArrayList<AgentOption> options = new ArrayList<>();
        int[] orientations = {-45, 0, 45, -90, 0, 90, -135, 180, 135};
        for (int i = 0; i < 9; i++) {
            if (i != 4) { // Not check current position
                int xPosition = this.knowledge.currentPositionX - 1 + (i % 3);
                int yPosition = this.knowledge.currentPositionY - 1 + (i / 3);
                int orientation = orientations[i];
                if (this.knowledge.insideMap(xPosition, yPosition)) {
                    int targetHeight = this.knowledge.map.get(xPosition).get(yPosition);
                    if (targetHeight == -1) {
                        this.status = AgentStatus.NEED_SENSOR;
                        Info("Changed status to: " + this.status);
                        return null;
                    }
                    if (targetHeight < this.knowledge.maxFlight) { // TODO poner esto bonito
                        if ((this.knowledge.currentHeight + 5 < this.knowledge.maxFlight && this.knowledge.currentHeight < targetHeight) || targetHeight <= this.knowledge.currentHeight) {
                            options.add(this.generateOption(xPosition, yPosition, targetHeight, orientation));
                        }
                    }
                }
            }
        }
        return options;
    }

    /**
     * Genera la opción para una determinada casilla, 
     * genera un plan para llegar hasta ella y su coste y,
     * finalmente, calcula una heurística para asociarle un valor.
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @param xPosition, posición x de la casilla en el mundo
     * @param yPosition, posición y de la casilla en el mundo
     * @param height, altura del suelo de la casilla
     * @param orientation, orientación a la que debe apuntar el agente para avanzar a esa casilla
     * @return AgentOption
     */
    AgentOption generateOption(int xPosition, int yPosition, int height, int orientation) {
        AgentOption option = new AgentOption(xPosition, yPosition, height, this.knowledge.visitedAtMap.get(xPosition).get(yPosition));
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
                    provisionalOrientation = this.knowledge.getNextOrientation(provisionalOrientation, true);
                } else { // shouldTurnLeft
                    nextAction = AgentAction.rotateL;
                    provisionalOrientation = this.knowledge.getNextOrientation(provisionalOrientation, false);
                }
            } else if (provisionalHeight < height) {
                nextAction = AgentAction.moveUP;
                provisionalHeight += 5;
            } else {
                nextAction = AgentAction.moveF;
                onWantedBox = true;
            }
            plan.add(nextAction);
            cost += this.knowledge.energyCost(nextAction, 0);
        }
        option.plan = plan;
        option.cost = cost;
        option.calculateDistanceToLudig(this.knowledge.ludwigPositionX, this.knowledge.ludwigPositionY);
        return option;
    }

    /**
     * Método que ejecuta el mejor plan pensado por el agente.
     * @author Miguel García
     */
    void executePlan() {
        this.doAction(this.plan.get(0));
        this.plan.remove(0);
        if (this.plan.size() == 0)  {
            this.plan = null;
        }
    }
    
    /**
     * Intenta aterrizar al agente en el suelo, si no puede baja 5 niveles en su lugar
     * @author Jose Saldaña
     * @return booleano que indica si ha conseguido aterrizar
     */
    boolean toLand() {
        if (this.knowledge.canTouchDown()) {
            this.doAction(AgentAction.touchD);
            return true;
        } else {
            this.doAction(AgentAction.moveD);
            return false;
        }
    }
    
    /**
     * Rescata a Ludwig y cambia estado a Finished para finalizar Agente
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
     * Asegura que el agente esté en tierra y realiza la recarga de batería.
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
     * Solicita el estado de los sensores autorizados y se encarga de actualizar Perception y Knowledge.
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
            this.perception.update(answer.get("details").asObject().get("perceptions").asArray());
            Info(this.perception.toString());
            this.knowledge.update(this.perception);
            this.status = AgentStatus.READY;
            Info("Changed status to: " + this.status);
        } else {
            this.logout();
        }
    }

    // ------------------------------------------------------------------
    // Agent low level functions ----------------------------------------
    /**
     * Ejecuta la acción y actualiza el estado interno
     * @author Domingo Lopez
     * @param action Acción a realizar
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
     * Maneja los correspondientes cambios que generará la acción en el knowledge del agente.
     * @author Domingo Lopez
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     * @param action, siguiente accion que realizara el agente
     */
    void executeAction(AgentAction action) {
        switch (action) {
            case recharge:
                this.plan = null;
                this.knowledge.fullRecharge();
                break;
            default:
                this.knowledge.manageMovement(action);
                this.useEnergy(action);
                break;
        }
    }

    /**
     * Realiza el consumo de energia tras realizar una acción
     * @author Domingo Lopez
     * @param action Acción a realizar
     */
    void useEnergy(AgentAction action) {
        this.knowledge.energy -= this.knowledge.energyCost(action, this.authorizedSensors.size());
    }

    // ------------------------------------------------------------------
    // Logout and takeDown-----------------------------------------------
    /**
     * Envía mensaje de logout y comienza fin (_exitRequested = true)
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
     * Método para desconectarse de la plataforma de agentes
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
     * Envío bloqueante de un mensaje, el agente se bloquea hasta que recibe respuesta
     * @author Jose Saldaña
     * @param params JsonObject con la estructura de lo que se quiere mandar
     * @return parsedAnswer JsonObject con la estructura según respuesta
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
     * Envío no bloqueante de un mensaje
     * @author Jose Saldaña
     * @param params JsonObject con la estructura de lo que se quiere mandar
     */
    void sendMessage(JsonObject params) {
        String parsedParams = params.toString();
        Info("Request: " + parsedParams);
        this.outChannel.setContent(parsedParams);
        this.sendServer(this.outChannel);
    }
}
