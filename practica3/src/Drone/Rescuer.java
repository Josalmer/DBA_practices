package Drone;

import com.eclipsesource.json.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Clase Rescuer que hereda de Drone
 * @author Miguel García 
 */
public class Rescuer extends Drone {
    // AGENT CONFIGURATION -------------------------------------------
    int targetPositionX;
    int targetPositionY;
    String currentMission;
    // END CONFIGURATION ---------------------------------------------
    @Override
    public void plainExecute() {
        this.printMessages = true;
        this.color = "\033[36m";
        this._communications.setPrintMessages(this.printMessages);
        
        while (!_exitRequested) {
            
            print("Current Status: " + this.status);
            
            switch (this.status) {

                case SUBSCRIBED_TO_PLATFORM:
                    this.requestSessionIdAndMap();
                    if (this.status == DroneStatus.SUBSCRIBED_TO_PLATFORM) {
                        this.checkingRadio("rescuer");
                    }
                    break;
                case SUBSCRIBED_TO_WORLD:
                    this.sendCashToAPB();
                    if (this._communications.getDronesNumber().equals(2) && this.getLocalName().equals("Migue al Rescate")) {
                        this._communications.waitForFinish();
                        this.status = DroneStatus.FINISHED;
                    }
                    break;
                case WAITING_INIT_DATA:
                    this.receiveLoginData();
                    if (this.status == DroneStatus.WAITING_INIT_DATA) {
                        this.loginWorld(this.knowledge.currentPositionX, this.knowledge.currentPositionY);
                    }
                    this.initialRecharge();
                    this.status = DroneStatus.FREE;
                    break;
                case FREE:
                    this.receivePlan();
                    break;
                case BUSY:
                    this.executeReactive();
                    break;
                case ABOVE_TARGET:
                    this.getLudwig();
                    break;
                case ABOVE_END:
                    this.landHome();
                    break;
                case NEED_RECHARGE:
                    this.claimRecharge();
                    break;
                case RECHARGING:
                    this.recharge();
                    break;
                case WAITING_FOR_FINISH:
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

        } else {
            this.status = DroneStatus.FINISHED;
        }
    }

    @Override
    void loginWorld(int x, int y) {
        String result = this._communications.loginWorld("rescuer", x, y, new ArrayList<String>());
        if (result.equals("error")) {
            this.status = DroneStatus.FINISHED;
        }
    }
     /**
     * Recibe un plan de una misión
     * 
     * @author Miguel García Tenorio
     *
     */
    void receivePlan() {
        JsonObject content = new JsonObject();
        content.add("request", "mission");
        int number;
        if (this.getLocalName().equals("Migue al Rescate")) {
            number = 2;
        } else {
            number = 1;
        }
        content.add("number", number);
        JsonObject response = this._communications.sendAndReceiveToAPB(ACLMessage.REQUEST, content, "mission");
        if (response != null) {
            this.currentMission = response.get("content").asObject().get("mission").asString();

            if (this.currentMission.equals("rescue")) {
                this.targetPositionX = response.get("content").asObject().get("x").asInt();
                this.targetPositionY = response.get("content").asObject().get("y").asInt();
                this.status = DroneStatus.BUSY;
            }

            if (this.currentMission.equals("backHome")) {
                this.targetPositionX = response.get("content").asObject().get("x").asInt();
                this.targetPositionY = response.get("content").asObject().get("y").asInt();
                this.status = DroneStatus.BUSY;
            }

        } else {
            this.status = DroneStatus.FINISHED;
        }
    }


    @Override
    public void recharge() {
        if (this.toLand()) {
            if (this.rechargeTicket != null) { //Si tengo ticket, lo consumo y recargo

                String result = this._communications.requestRecharge(this.rechargeTicket);
                if (result.equals("ok")) {
                    this.knowledge.fullRecharge();
                    this.rechargeTicket = null;
                    if (this.plan != null && this.plan.size() > 0) {
                        this.status = DroneStatus.BUSY;
                    } else {
                        this.status = DroneStatus.FREE;
                    }
                } else {
                    this.status = DroneStatus.FINISHED;
                }
                print("Changed status to: " + this.status);
            }
        }
    }
    
    /**
     * Realiza el comportamiento reactivo del Rescuer
     * 
     * @author Miguel García Tenorio
     *
     */
    public void executeReactive() {
        if (this.knowledge.amIAboveTarget(this.targetPositionX, this.targetPositionY)) {
            print("Above Target " + this.targetPositionX + ", " + this.targetPositionY + "\n");
            if (this.currentMission.equals("rescue")) {
                this.status = DroneStatus.ABOVE_TARGET;
            } else {
                this.status = DroneStatus.ABOVE_END;
            }
            this.plan = null;
        } else {
            if (this.knowledge.rescuerNeedRecharge()) {
                this.status = DroneStatus.NEED_RECHARGE;
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
     * @author Miguel García Tenorio
     *
     */
    void thinkPlan() {
        ArrayList<DroneOption> options = this.generateOptions();
        if (options != null) {
            DroneOption winner;
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
     * @author Miguel García Tenorio
     *
     * @return options generadas
     */
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
                    if (targetHeight < this.knowledge.maxFlight) {
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
     * Piensa un plan reactivo a la mejor casilla cercana
     * @param xPosition posicionX de la opción
     * @param yPosition posiciónY de la opción
     * @param height altura de la opción
     * @param orientation orientación de la opción
     * @author Miguel García Tenorio
     * @return option DroneOption generada
     *
     */
    DroneOption generateOption(int xPosition, int yPosition, int height, int orientation) {
        DroneOption option = new DroneOption(xPosition, yPosition, height, this.knowledge.visitedAtMap.get(xPosition).get(yPosition));
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
            cost += (this.knowledge.energyCost(nextAction) * 4);
        }
        option.plan = plan;
        option.cost = cost;
        option.calculateDistanceToTarget(this.targetPositionX, this.targetPositionY);
        return option;
    }

    /**
     * Elige la mejor opción de las generadas
     * @param options array de opciones
     * @return bestOption
     * @author Miguel García Tenorio
     *
     */
    DroneOption chooseBestOption(ArrayList<DroneOption> options) {
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
  
    
    @Override
     void useEnergy(DroneAction action) {
        this.knowledge.energy -= (this.knowledge.energyCost(action) * 4);
        if (this.printMessages) {
            print("Executed action: " + action + " energy left: " + this.knowledge.energy);
        }
    }
    
     /**
     * Obtiene a Ludwig si puede aterrizar
     * @author Miguel García Tenorio
     *
     */
    void getLudwig() {
        if (this.toLand()) {
            this.doAction(DroneAction.rescue);
            this.status = DroneStatus.FREE;
            print("Rescatado Ludwig");
            print("Changed status to: " + this.status);
        }
    }

    /**
     * Aterriza en el lugar de inicio
     * @author Miguel García Tenorio, Jose Saldaña
     *
     */
    void landHome() {
        if (this.toLand()) {
            print("Aterrizado en casa");
            this._communications.sendFinishMsgToAPB();
            this.status = DroneStatus.WAITING_FOR_FINISH;
            print("Changed status to: " + this.status);
        }
    }
    
    @Override
    boolean toLand() {
        if (this.knowledge.canTouchDown()) {
            this.doAction(DroneAction.touchD);
            return true;
        } else {
            this.doAction(DroneAction.moveD);
            return false;
        }
    }
}
