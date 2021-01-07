package Drone;

import MapOption.Coordinates;
import com.eclipsesource.json.*;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;

/**
 * Clase seeker que hereda de Drone
 *
 * @author Domingo López
 */
public class Seeker extends Drone {

    String sensorTicket;
    int targetPositionX;
    int targetPositionY;
    boolean targetPositionVisited;
    boolean searching = true;

    //JsonObjects de las posiciones en el mapa que va a tener que visitar el seeker
    ArrayList<JsonObject> targetPositions = new ArrayList<>();

    @Override
    public void plainExecute() {

        this.printMessages = true;
        this.color = "";
        this._communications.setPrintMessages(this.printMessages);

        while (!_exitRequested) {

            print("Current Status: " + this.status);

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
                        this._communications.waitForFinish();
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
                    break;
                case RECHARGING:
                    this.recharge();
                    break;
                case NEED_SENSOR:
                    this.readSensor();
                    break;
                case WAITING_FOR_FINISH:
                    if (this.knowledge.currentHeight < 250) {
                        this.doAction(DroneAction.moveUP);
                    }
                    this._communications.waitForFinish();
                    this.status = DroneStatus.FINISHED;
                    break;
                case FINISHED:
                    this.logout();
                    break;
            }
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
            this.targetPositions.remove(0);
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
            print("Error en el login del Seeker");
            this.status = DroneStatus.FINISHED;
        } else {
            print("Seeker logueado correctamente en el mundo. Enviado el ticket del sensor al server y visto bueno");
        }
    }

    @Override
    public void recharge() {
        if (this.toLand()) {
            String result = this._communications.requestRecharge(this.rechargeTicket);
            if (result.equals("ok")) {
                this.knowledge.fullRecharge();
                this.rechargeTicket = null;
                this.status = DroneStatus.EXPLORING;
            } else {
                this.status = DroneStatus.WAITING_FOR_FINISH;
            }
            print("Changed status to: " + this.status);
        }
    }

    /**
     * Comportamiento Reactivo para la P3 del Seeker
     *
     * @author Domingo López, Jose Saldaña
     *
     */
    void reactiveBehaviour() {

        if (this.knowledge.amIAboveTarget(this.targetPositionX, this.targetPositionY)) {
            if (this.targetPositions.isEmpty()) {
                print("He explorado todas las esquinas....Saliendo del mundo");
                this.status = DroneStatus.WAITING_FOR_FINISH;
                print("Changed status to: " + this.status);
            } else {
                print("He llegado a la esquina " + this.targetPositions.get(0).asObject());
                this.targetPositionX = this.targetPositions.get(0).asObject().get("x").asInt();
                this.targetPositionY = this.targetPositions.get(0).asObject().get("y").asInt();
                this.targetPositions.remove(0);
                this.knowledge.nActionsExecutedToGetCorner = 0;
                this.plan = null;
            }
        } else if (this.knowledge.maxLimitActionPermited()) {
            print("He llegado al máximo de acciones permitidas....Saliendo del mundo");
            this.status = DroneStatus.WAITING_FOR_FINISH;
            print("Changed status to: " + this.status);

        } else if (this.knowledge.cantReachTarget()) {
            print("He llegado al máximo de acciones permitidas para llegar al destino/Corner....Cambiando a la siguiente esquina");
            this.targetPositions.remove(0);
            this.targetPositionX = this.targetPositions.get(0).asObject().get("x").asInt();
            this.targetPositionY = this.targetPositions.get(0).asObject().get("y").asInt();
            this.knowledge.nActionsExecutedToGetCorner = 0;
            this.plan = null;
//        } else if (!this.searching) {
        } else if (this.knowledge.alemanes >= 10) {
            print("He encontrado todos los alemanes");
            this.status = DroneStatus.WAITING_FOR_FINISH;
            print("Changed status to: " + this.status);
        } else {
            if (this.knowledge.needRecharge()) {
                this.status = DroneStatus.NEED_RECHARGE;
                this.plan = null;
                print("Changed status to: " + this.status);
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
     * Piensa un plan reactivo a la mejor casilla cercana
     *
     * @author Domingo López
     *
     */
    void thinkPlan() {
        ArrayList<SeekerOption> options = this.generateOptions();
        if (options != null) {
            SeekerOption winner;
            winner = chooseBestOption(options);
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

    /**
     * Genera las opciones circundantes a la posición en la que nos encontramos
     *
     * @author Domingo López
     *
     * @return options generadas
     */
    ArrayList<SeekerOption> generateOptions() {
        ArrayList<SeekerOption> options = new ArrayList<>();
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
                        print("Changed status to: " + this.status);
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

    /**
     * Genera una SeekerOption con al mejor opción posible
     *
     * @param xPosition posición x de la opción
     * @param yPosition posición y de la opción
     * @param height altura de la opción
     * @param orientation orientación de la opción
     * @param thermalValue valor del thermal
     * @author Domingo López, Jose Saldaña
     * 
     * @return option SeekerOption
     *
     */
    SeekerOption generateOption(int xPosition, int yPosition, int height, int orientation, Double thermalValue) {
        SeekerOption option = new SeekerOption(xPosition, yPosition, height, this.knowledge.visitedAtMap.get(xPosition).get(yPosition), thermalValue);
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
        option.calculateDistanceToTarget(this.targetPositionX, this.targetPositionY);
        return option;
    }

    /**
     * Elige la mejor opción de las generadas
     *
     * @param options array de opciones
     * @return bestOption
     * @author Jose Saldaña
     *
     */
    SeekerOption chooseBestOption(ArrayList<SeekerOption> options) {
        double min = options.get(0).puntuation;
        SeekerOption bestOption = options.get(0);
        for (SeekerOption o : options) {
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
     * Lee los sensores mandando petición al servidor
     *
     * @author Domingo
     *
     */
    void readSensor() {
        JsonObject response = this._communications.readSensorMessage();
        if (response.get("result").asString().equals("ok")) {
            print("Valores de los sensores leídos...");
            this.useEnergy(DroneAction.LECTURA_SENSORES);
            this.perception.update(response.get("details").asObject().get("perceptions").asArray());
            print(this.perception.toString());

            this.knowledge.updateThermalMap(this.perception.thermal);

            this.sendGermans();

            print("Mapa termal actualizado");

            this.status = DroneStatus.EXPLORING;
            print("Changed status to: " + this.status + "after reading sensors");
        } else {
            print("ERROR EN LA LECTURA DE SENSORES\n");
            this.status = DroneStatus.FINISHED;
        }
    }

    /**
     * Manda las coordeadas de los alemanes encontrados
     *
     * @author Miguel García Tenorio, Jose Saldaña, Domingo López, Manuel
     * Pancorbo
     *
     */
    public void sendGermans() {
        Coordinates german = this.knowledge.getGerman();
        while (german != null) {
            this.searching = this._communications.informGermanFound(german.x, german.y);
            german = this.knowledge.getGerman();
        }
    }

    /**
     * Calcula las esquinas iniciales de donde parten los seekers
     *
     * @author Domingo
     *
     */
    public void calculateCorners() {

        int width = this.knowledge.mapWidth;
        int height = this.knowledge.mapHeight;

        int halfWidth = width/2;

        int half = halfWidth;
        if (!this.getLocalName().equals("Buscador Saldaña"))
            half = 0;

        //Esquinas de arriba
        for(int i=0; i<halfWidth/30 + 1;i++){

            JsonObject tUp = new JsonObject();
            tUp.add("x", 15 + i * 30 + half);
            tUp.add("y", 15);

            JsonObject tBottom = new JsonObject();
            tBottom.add("x", 15 + i*30 + half);
            tBottom.add("y", height - 15);

            if(i%2==0) {
                this.targetPositions.add(tUp);
                this.targetPositions.add(tBottom);
            } else {
                this.targetPositions.add(tBottom);
                this.targetPositions.add(tUp);
            }
        }

        JsonObject firstTarget = targetPositions.get(0);
        targetPositions.remove(firstTarget);
        targetPositions.add(firstTarget);

    }

    /**
     * Obtiene el valor del thermal de una posición concreta
     *
     * @param xPosition posición x del mapa Thermal
     * @param yPosition posición y del mapa Thermal
     * @author Domingo López
     * @return double valor del thermal
     */
    public double getThermalValue(int xPosition, int yPosition) {
        return this.knowledge.thermalMap.get(xPosition).get(yPosition);
    }

}
