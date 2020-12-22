package Drone;

import com.eclipsesource.json.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Arrays;

public class Rescuer extends Drone {
    // AGENT CONFIGURATION -------------------------------------------
    int targetPositionX;
    int targetPositionY;
    String currentMission;
    // END CONFIGURATION ---------------------------------------------
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
//                    this.receivePlan();
                    this.provisionalReceivePlan();
                    break;
                case BUSY:
//                    this.executePlan();
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
                    if (this.rechargeTicket == null) {
                        this.status = DroneStatus.RECHARGING;
                    } else {
                        this.status = DroneStatus.BACKING_HOME;
                    }
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

    @Override
    void elaboratePlan() {

        int currentYPosition = this.knowledge.currentPositionY;
        int currentXPosition = this.knowledge.currentPositionX;
        int destinyYPosition;
        int destinyXPosition;
        int height;
        int orientation = -1;

        int provisionalOrientation = this.knowledge.orientation;
        int provisionalHeigth = this.knowledge.currentHeight;
        boolean onWantedBox = false;
        DroneAction nextAction;

        //Esto todavia no se muy bien como se va a hacer
        // Pero mi idea es que APB le mande un array de int 
        // El array tendra:
        //  [ x,y,Altura,x,y,Altura ....]
        //Cada tres posiciones del array se corresponderia una casilla
        // Necesito tambien la altura para saber si el agente tiene que meter la
        // accion de elevarse en el plan
        for (int i = 0; i < this.planInMap.size(); i += 2) {
            destinyYPosition = this.planInMap.get(i + 1);
            destinyXPosition = this.planInMap.get(i);
            height = this.knowledge.map.get(destinyYPosition).get(destinyXPosition);

            if (currentXPosition > destinyXPosition && currentYPosition == destinyYPosition) {
                orientation = -90;
            } else if (currentXPosition < destinyXPosition && currentYPosition == destinyYPosition) {
                orientation = -90;
            } else if (currentXPosition == destinyXPosition && currentYPosition > destinyYPosition) {
                orientation = 0;
            } else if (currentXPosition == destinyXPosition && currentYPosition < destinyYPosition) {
                orientation = 180;
            } else if (currentXPosition > destinyXPosition && currentYPosition > destinyYPosition) {
                orientation = -45;
            } else if (currentXPosition < destinyXPosition && currentYPosition > destinyYPosition) {
                orientation = 45;
            } else if (currentXPosition > destinyXPosition && currentYPosition < destinyYPosition) {
                orientation = -135;
            } else if (currentXPosition < destinyXPosition && currentYPosition < destinyYPosition) {
                orientation = 135;
            }

            while (!onWantedBox) {
                if (orientation != provisionalOrientation) {
                    int turns = this.knowledge.howManyTurns(orientation);
                    if (this.knowledge.shouldTurnRight(turns)) {
                        nextAction = DroneAction.rotateR;
                        provisionalOrientation = this.knowledge.getNextOrientation(provisionalOrientation, true);
                    } else {
                        nextAction = DroneAction.rotateL;
                        provisionalOrientation = this.knowledge.getNextOrientation(provisionalOrientation, false);
                    }
                } else if (provisionalHeigth < height) {
                    nextAction = DroneAction.moveUP;
                    provisionalHeigth += 5;
                } else {
                    nextAction = DroneAction.moveUP;
                    onWantedBox = true;
                }

                this.plan.add(nextAction);

            }

            currentYPosition = destinyYPosition;
            currentXPosition = currentXPosition;

        }

        this.knowledge.currentPositionX = currentXPosition;
        this.knowledge.currentPositionY = currentYPosition;

    }

    void receivePlan() {
        JsonObject content = new JsonObject();
        content.add("request", "mission");
        JsonObject response = this._communications.sendAndReceiveToAPB(ACLMessage.REQUEST, content, "mission");
        if (response != null) {
            String mission = response.get("content").asObject().get("mission").asString();

            if (mission.equals("rescue")) {
                this.planInMap = this.parser.getPlan(response.get("content").asObject());
                this.elaboratePlan();
                this.status = DroneStatus.BUSY;
            }

            if (mission.equals("backHome")) {
                this.planInMap = this.parser.getPlan(response.get("content").asObject());
                this.elaboratePlan();
                this.status = DroneStatus.BACKING_HOME;
            }

        } else {
            this.status = DroneStatus.FINISHED;
        }
    }
    
    void provisionalReceivePlan() {
        JsonObject content = new JsonObject();
        content.add("request", "mission");
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

//    @Override
//    void executePlan() {
//        if (this.knowledge.needRecharge()) {
//            this.status = DroneStatus.NEED_RECHARGE;
//        } else {
//            // Comprobar si puede moverse (mirar la radio del mundo)
//            this.doAction(this.plan.get(0));
//            this.plan.remove(0);
//            if (this.plan.isEmpty()) {
//                this.status = DroneStatus.FREE;
//                this.plan = null;
//            }
//        }
//    }

    @Override
    public void recharge() {
        if (this.toLand()) {
            if (this.rechargeTicket != null) { //Si tengo ticket, lo consumo y recargo

                String result = this._communications.requestRecharge(this.rechargeTicket);
                if (result.equals("ok")) {
                    this.knowledge.energy = 1000;
                    this.rechargeTicket = null;
                    if (this.plan.isEmpty() || this.plan == null) {
                        this.status = DroneStatus.FREE;
                    } else {
                        this.status = DroneStatus.BUSY;
                    }
                } else {
                    this.status = DroneStatus.FINISHED;
                }
                Info("Changed status to: " + this.status);

            } else { //Si no tengo ticket

            }

        }
    }
    
    public void executeReactive() {
        if (this.knowledge.amIAboveTarget(this.targetPositionX, this.targetPositionY)) {
            Info("\n\033[36m " + "Above Target " + this.targetPositionX + ", " + this.targetPositionY + "\n");
            if (this.currentMission.equals("rescue")) {
                this.status = DroneStatus.ABOVE_TARGET;
            } else {
                this.status = DroneStatus.ABOVE_END;
            }
            this.plan = null;
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
        ArrayList<ProvisionalDroneOption> options = this.generateOptions();
        ArrayList<ProvisionalDroneOption> noVisitedOptions = new ArrayList<>();
        if (options != null) {
//            for (ProvisionalDroneOption o : options) {
//                if (o.visitedAt == -1) {
//                    noVisitedOptions.add(o);
//                }
//            }
//            if (noVisitedOptions.size() > 0) {
//                options = noVisitedOptions;
//            }
            ProvisionalDroneOption winner;
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

    ArrayList<ProvisionalDroneOption> generateOptions() {
        ArrayList<ProvisionalDroneOption> options = new ArrayList<>();
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

    ProvisionalDroneOption generateOption(int xPosition, int yPosition, int height, int orientation) {
        ProvisionalDroneOption option = new ProvisionalDroneOption(xPosition, yPosition, height, this.knowledge.visitedAtMap.get(xPosition).get(yPosition));
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

    ProvisionalDroneOption chooseFromAlreadyVisitedOptions(ArrayList<ProvisionalDroneOption> options) {
        double lastVisited = options.get(0).visitedAt;
        ProvisionalDroneOption bestOption = options.get(0);
        for (ProvisionalDroneOption o : options) {
            if (o.visitedAt < lastVisited) {
                bestOption = o;
                lastVisited = o.visitedAt;
            }
        }
        return bestOption;
    }

    ProvisionalDroneOption chooseFromNoVisitedOptions(ArrayList<ProvisionalDroneOption> options) {
        double min = options.get(0).puntuation;
        ProvisionalDroneOption bestOption = options.get(0);
        for (ProvisionalDroneOption o : options) {
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
    
    void getLudwig() {
        if (this.toLand()) {
            this.doAction(DroneAction.rescue);
            this.status = DroneStatus.FREE;
            Info("Rescatado Ludwig");
            Info("Changed status to: " + this.status);
        }
    }

    void landHome() {
        if (this.toLand()) {
            Info("Aterrizado en casa");
            this._communications.sendFinishMsgToAPB();
            this.status = DroneStatus.WAITING_FOR_FINISH;
            Info("Changed status to: " + this.status);
        }
    }
    
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
