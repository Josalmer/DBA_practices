package Drone;

import com.eclipsesource.json.*;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;

public class Seeker extends Drone {
    String sensorTicket;
    int targetPositionX;
    int targetPositionY;
    boolean targetPositionVisited;
    
    //JsonObjects de las posiciones en el mapa que va a tener que visitar el seeker
   ArrayList<JsonObject> targetPositions = new ArrayList<>();

    @Override
    public void plainExecute() {
        while (!_exitRequested) {
            if (this.printMessages) {
                Info("\n\n\033[36m " + this.getLocalName() + " - Current Status: " + this.status);
            }
            switch (this.status) {

                case SUBSCRIBED_TO_PLATFORM:
                    this.requestSessionIdAndMap();
                    if (this.status == DroneStatus.SUBSCRIBED_TO_PLATFORM) {
                        this.checkingRadio("seeker");
                    }
                    break;
                case SUBSCRIBED_TO_WORLD:
                    this.sendCashToAPB();
                    if (this._communications.getDronesNumber().equals(2) && this.getLocalName().equals("Buscador Domingo")) {
                        this._communications.waitIddle();
                        this.status = DroneStatus.FINISHED;
                    } else {
                        this.status = DroneStatus.WAITING_INIT_DATA;
                    }
                    break;
                case WAITING_INIT_DATA:
                    this.receiveLoginData();
                    if (this.status == DroneStatus.WAITING_INIT_DATA) {
                        this.loginWorld(this.knowledge.currentPositionX, this.knowledge.currentPositionY);
                    }
                    this.initialRecharge();
                    this.status = DroneStatus.EXPLORING;
                    break;
                case EXPLORING:
                    this.reactiveBehaviour();
                    break;
                case NEED_RECHARGE:
                    this.claimRecharge();
                    if (this.rechargeTicket == null) {
                        this.status = DroneStatus.RECHARGING;
                    } else {
                        this.status = DroneStatus.FINISHED;
                    }
                    break;
                case NEED_SENSOR:
                    this.readSensor();
                    break;
                case RECHARGING:
                    this.recharge();
                case FINISHED:
                    this.logout();
                    break;
            }
        }
    }

    @Override
    void requestLoginData() {
        JsonObject content = new JsonObject();
        content.add("request", "login");
        JsonObject response = this._communications.sendAndReceiveToAPB(ACLMessage.QUERY_REF, content, "login");
        if (response != null) {
            this.knowledge.currentPositionX = response.get("content").asObject().get("x").asInt();
            this.knowledge.currentPositionY = response.get("content").asObject().get("y").asInt();
            this.knowledge.currentHeight = this.knowledge.map.get(this.knowledge.currentPositionX).get(this.knowledge.currentPositionY);
            this.rechargeTicket = response.get("content").asObject().get("rechargeTicket").asString();
            this.sensorTicket = response.get("content").asObject().get("sensorTicket").asString();
        } else {
            this.status = DroneStatus.FINISHED;
        }

    }

    @Override
    void receiveLoginData() {
        JsonObject response = this._communications.receiveFromAPB("login");
        if (response != null) {
            this.knowledge.initializeKnowledge(response);
            this.rechargeTicket = response.get("content").asObject().get("rechargeTicket").asString();
            this.sensorTicket = response.get("content").asObject().get("sensorTicket").asString();
            //Calculamos las 4 esquinas del mapa por las que pasará el seeker
            this.calculateCorners();
            //Iniciamos como target principal del Seeker la primera esquina
            this.targetPositionX = this.targetPositions.get(0).asObject().get("x").asInt();
            this.targetPositionY = this.targetPositions.get(0).asObject().get("y").asInt();
            this.targetPositionVisited = this.targetPositions.get(0).asObject().get("visited").asBoolean();
        } else {
            this.status = DroneStatus.FINISHED;
        }
    }

    @Override
    void loginWorld(int x, int y) {

        ArrayList<String> sensors = new ArrayList<>();
        sensors.add(this.sensorTicket);

        String result = this._communications.loginWorld("seeker", x, y, sensors);
        if (result.equals("error")) {
            System.out.print("\nError en el login del Seeker");
            this.status = DroneStatus.FINISHED;
        } else {
            System.out.print("\nSeeker logueado correctamente en el mundo. Enviado el ticket del sensor al server y visto bueno\n");
        }
    }

    @Override
    public void recharge() {
        if (this.toLand()) {
            String result = this._communications.requestRecharge(this.rechargeTicket);
            if (result.equals("ok")) {
                this.knowledge.energy = 1000;
                this.rechargeTicket = null;
                if (this.plan.isEmpty() || this.plan == null) {
                    this.status = DroneStatus.FINISHED;
                } else {
                    this.status = DroneStatus.EXPLORING;
                }
            } else {
                this.status = DroneStatus.FINISHED;
            }
            Info("\n\033[36m " + "Changed status to: " + this.status);
        }
    }

    /**
     * Comportamiento Reactivo para la P3
     *
     * @author Domingo López, Jose Saldaña
     * @return
     *
     */
    void reactiveBehaviour() {

        if (this.targetPositions.get(0) == null) {
            Info("\n\033[36m " + "He explorado todas las esquinas....Saliendo del mundo\n");
            this.status = DroneStatus.FINISHED;
            Info("\n\033[36m " + "Changed status to: " + this.status);
        } else if (this.knowledge.amIAboveTarget(this.targetPositionX, this.targetPositionY)) {
            Info("\n\033[36m " + "He llegado a la esquina " + this.targetPositions.get(0).asObject() + "\n");
            this.targetPositions.remove(0);
            this.targetPositionX = this.targetPositions.get(0).asObject().get("x").asInt();
            this.targetPositionY = this.targetPositions.get(0).asObject().get("y").asInt();
            this.targetPositionVisited = this.targetPositions.get(0).asObject().get("visited").asBoolean();
            this.knowledge.nActionsExecutedToGetCorner = 0;
            this.plan = null;
        } else if (this.knowledge.maxLimitActionPermited()) {
            Info("\n\033[36m " + "He llegado al máximo de acciones permitidas....Saliendo del mundo\n");
            this.status = DroneStatus.FINISHED;
            Info("\n\033[36m " + "Changed status to: " + this.status);

        } else if (this.knowledge.cantReachTarget()) {
            Info("\n\033[36m " + "He llegado al máximo de acciones permitidas para llegar al destino/Corner....Cambiando a la siguiente esquina\n");

            this.targetPositions.remove(0);
            this.targetPositionX = this.targetPositions.get(0).asObject().get("x").asInt();
            this.targetPositionY = this.targetPositions.get(0).asObject().get("y").asInt();
            this.targetPositionVisited = this.targetPositions.get(0).asObject().get("visited").asBoolean();
            this.knowledge.nActionsExecutedToGetCorner = 0;
            this.plan = null;

        } else if(this.knowledge.alemanes == 10) {
            Info("\n\033[36m " + "He encontrado todos los alemanes\n");
            this.status = DroneStatus.FINISHED;
            Info("\n\033[36m " + "Changed status to: " + this.status);
        } else {
            if (this.knowledge.needRecharge()) {
                this.status = DroneStatus.NEED_RECHARGE;
                Info("\n\033[36m " + "Changed status to: " + this.status);
            } else {
                if (this.plan != null) {
                    this.executePlan();
                } else {
                    this.thinkPlan();
                }
            }
        }
    }

    void thinkPlan() {
        ArrayList<DroneOption> options = this.generateOptions();
        ArrayList<DroneOption> noVisitedOptions = new ArrayList<>();
        if (options != null) {
            for (DroneOption o : options) {
                if (o.visitedAt == -1) {
                    noVisitedOptions.add(o);
                }
            }
            if (noVisitedOptions.size() > 0) {
                options = noVisitedOptions;
            }
            DroneOption winner;
            winner = chooseFromNoVisitedOptions(options);
            if (winner != null) {
                this.plan = winner.plan;
                if (this.knowledge.shouldIRechargueFirst(winner)) {
                    this.status = DroneStatus.NEED_RECHARGE;
                }
            } else {
                throw new RuntimeException("No hay un plan ganador");
            }
        }
    }

    ArrayList<DroneOption> generateOptions() {
        ArrayList<DroneOption> options = new ArrayList<>();
        int[] orientations = {-45, 0, 45, -90, 0, 90, -135, 180, 135};
        for (int i = 0; i < 9; i++) {
            if (i != 4) { // Not check current position
                int xPosition = this.knowledge.currentPositionX - 1 + (i % 3);
                int yPosition = this.knowledge.currentPositionY - 1 + (i / 3);
                int orientation = orientations[i];
                if (this.knowledge.insideMap(xPosition, yPosition)) {
                    int targetHeight = this.knowledge.map.get(xPosition).get(yPosition);
                    double thermalValue = this.getThermalValue(xPosition, yPosition);
                    if (thermalValue == -1.0) {
                        this.status = DroneStatus.NEED_SENSOR;
                        Info("\n\033[36m " + "Changed status to: " + this.status);
                        return null;
                    }
                    if (targetHeight < this.knowledge.maxFlight) {
                        if ((this.knowledge.currentHeight + 5 < this.knowledge.maxFlight && this.knowledge.currentHeight < targetHeight) || targetHeight <= this.knowledge.currentHeight) {
                            options.add(this.generateOption(xPosition, yPosition, targetHeight, orientation, thermalValue));
                        }
                    }
                }
            }
        }
        return options;
    }

    DroneOption generateOption(int xPosition, int yPosition, int height, int orientation, Double thermalValue) {
        DroneOption option = new DroneOption(xPosition, yPosition, height, this.knowledge.visitedAtMap.get(xPosition).get(yPosition), thermalValue);
        ArrayList<DroneAction> plan = new ArrayList<>();
        int cost = 0;
        boolean onWantedBox = false;
        int provisionalOrientation = this.knowledge.orientation;
        int provisionalHeight = this.knowledge.currentHeight;

        while (!onWantedBox) {
            DroneAction nextAction;
            if (orientation != provisionalOrientation) {
                int turns = this.knowledge.howManyTurns(orientation);
                if (this.knowledge.shouldTurnRight(turns)) {
                    nextAction = DroneAction.rotateR;
                    provisionalOrientation = this.knowledge.getNextOrientation(provisionalOrientation, true);
                } else { // shouldTurnLeft
                    nextAction = DroneAction.rotateL;
                    provisionalOrientation = this.knowledge.getNextOrientation(provisionalOrientation, false);
                }
            } else if (provisionalHeight < height) {
                nextAction = DroneAction.moveUP;
                provisionalHeight += 5;
            } else {
                nextAction = DroneAction.moveF;
                onWantedBox = true;
            }
            plan.add(nextAction);
            cost += this.knowledge.energyCost(nextAction);
        }
        option.plan = plan;
        option.cost = cost;

        //Esto es lo que hay que tocar
        //option.calculateDistanceToTarget(this.targetPositionX, this.targetPositionY);
        /**
         * Lo que vamos a hacer es que recorra cuatro puntos del mapa. Tiene que
         * ir de su current position a la esquina izquierda superior - 7
         * posiciones, esquina derecha superior -7 posiciones esquina izquierda
         * inferior menos 7 posiciones, etc.
         *
         * * * * * * *
         * x * * * x *
         * * * * * * *
         * * * * * * *
         * x * * * x *
         * * * * * * *
         *
         * Algo así. por lo que vamos a tener 4 puntos en el mapa definidos, e
         * intentará llegar a ellos. La otra opción es que empiece a recorrerlo
         * todo como pollo sin HEAD.
         */
        //Ojo, debe cambiar una vez alcancemos o hagamos un límite de pasos para llegar a la primera esquina, etc.
        option.calculateDistanceToTarget(this.targetPositionX, this.targetPositionY);
        return option;
    }

    DroneOption chooseFromAlreadyVisitedOptions(ArrayList<DroneOption> options) {
        double lastVisited = options.get(0).visitedAt;
        DroneOption bestOption = options.get(0);
        for (DroneOption o : options) {
            if (o.visitedAt < lastVisited) {
                bestOption = o;
                lastVisited = o.visitedAt;
            }
        }
        return bestOption;
    }

    DroneOption chooseFromNoVisitedOptions(ArrayList<DroneOption> options) {
        double min = options.get(0).puntuation;
        DroneOption bestOption = options.get(0);
        for (DroneOption o : options) {
            if (o.puntuation < min) {
                min = o.puntuation;
                bestOption = o;
            }
        }
        return bestOption;
    }

    @Override
    void executePlan() {
        this.doAction(this.plan.get(0));
        this.plan.remove(0);
        if (this.plan.size() == 0) {
            this.plan = null;
        }

    }

    /**
     * author: Domingo
     *
     * @return
     */
    void readSensor() {

        JsonObject response = this._communications.readSensor();

        //Parecido a la P2. Leemos y si es OK, actualizamos valores de los sensores.
        if (response.get("result").asString().equals("ok")) {
            Info("\n\033[36m " + "Valores de los sensores leídos...");
            this.useEnergy(DroneAction.LECTURA_SENSORES);
            this.perception.update(response.get("details").asObject().get("perceptions").asArray());
            Info("\n\033[36m " + this.perception.toString());
            this.knowledge.updateThermalMap(this.perception.thermal);
            Info("\n\033[36m " + "Mapa termal actualizado");

            this.status = DroneStatus.EXPLORING;
            Info("\n\033[36m " + "Changed status to: " + this.status + "after reading sensors");
        } else {
            Info("\n\033[36m " + "ERROR EN LA LECTURA DE SENSORES\n");
            this.status = DroneStatus.FINISHED;
        }

    }

    /**
     * author: Domingo
     *
     * @return
     */
    public void calculateCorners() {

        int width = this.knowledge.mapWidth;
        int height = this.knowledge.mapHeight;

        //(15,15) sería la casila central en la que el thermal Deluxe cubriría la esquina completa, consiguiendo poblar el mapa completo.
        JsonObject corner1 = new JsonObject();
        corner1.add("x", 15);
        corner1.add("y", 15);

        JsonObject corner2 = new JsonObject();
        corner2.add("x", width - 15);
        corner2.add("y", 15);

        JsonObject corner3 = new JsonObject();
        corner3.add("x", 15);
        corner3.add("y", height - 15);

        JsonObject corner4 = new JsonObject();
        corner4.add("x", width - 15);
        corner4.add("y", height - 15);

        JsonObject centro1 = new JsonObject();
        centro1.add("x", width / 2);
        centro1.add("y", height / 2);

        JsonObject centro2 = new JsonObject();
        centro2.add("x", width / 2);
        centro2.add("y", height / 2);

        this.targetPositions.add(corner3);
        this.targetPositions.add(centro1);
        this.targetPositions.add(corner4);
        this.targetPositions.add(corner2);
        this.targetPositions.add(centro2);
        this.targetPositions.add(corner1);

    }

    /**
     * author: Domingo ThermalDELUXE = 31x31. Po lo que la casilla central está
     * en la posición del array (15,15). Usando el juego de i%3 y i/3 podemos
     * recorrer las casillas adyacentes a la central del mapa termal conseguido
     * y así interpolar las coordenadas que estamos analizando con las que
     * tenemos del thermal.
     *
     * @return
     */
    public double getThermalValue(int xPosition, int yPosition) {
        return this.knowledge.thermalMap.get(xPosition).get(yPosition);
    }
}
