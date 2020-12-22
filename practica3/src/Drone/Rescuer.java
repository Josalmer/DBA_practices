package Drone;

import com.eclipsesource.json.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Arrays;

public class Rescuer extends Drone {
    // AGENT CONFIGURATION -------------------------------------------
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
                    this.receivePlan();
                    break;
                case BUSY:
                    this.executePlan();
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
        } else {
            this.status = DroneStatus.FINISHED;
        }

    }

    @Override
    void receiveLoginData() {
        JsonObject response = this._communications.receiveFromAPB("login");
        if (response != null) {
            this.knowledge.currentPositionX = response.get("content").asObject().get("x").asInt();
            this.knowledge.currentPositionY = response.get("content").asObject().get("y").asInt();
            this.knowledge.currentHeight = this.knowledge.map.get(this.knowledge.currentPositionX).get(this.knowledge.currentPositionY);
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

    @Override
    void executePlan() {
        if (this.knowledge.needRecharge()) {
            this.status = DroneStatus.NEED_RECHARGE;
        } else {
            // Comprobar si puede moverse (mirar la radio del mundo)
            this.doAction(this.plan.get(0));
            this.plan.remove(0);
            if (this.plan.isEmpty()) {
                this.status = DroneStatus.FREE;
                this.plan = null;
            }
        }
    }

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
}
