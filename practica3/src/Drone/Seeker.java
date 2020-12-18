package Drone;

import com.eclipsesource.json.*;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;

public class Seeker extends Drone {
    
    
    String sensorTicket;
    int targetPositionX;
    int targetPositionY;
    
    
    @Override
    public void plainExecute() {
        while (!_exitRequested) {
            Info("\n\n\033[36m " + this.getLocalName() + " - Current Status: " + this.status);
            switch (this.status) {
               
                case SUBSCRIBED_TO_PLATFORM:
                    this.requestSessionIdAndMap();
                    if (this.status == DroneStatus.SUBSCRIBED_TO_PLATFORM) {
                        this.checkingRadio("seeker");
                     
                    }
                    break;
                case SUBSCRIBED_TO_WORLD:
                    this.sendCashToAPB();
                    if (this.status == DroneStatus.SUBSCRIBED_TO_WORLD) {
                        this.requestLoginData();
                    }
                    if (this.status == DroneStatus.SUBSCRIBED_TO_WORLD) {
                        this.loginWorld(this.knowledge.currentPositionX,this.knowledge.currentPositionY);
                    }
                    this.status = DroneStatus.RECHARGING;
                    break;
                case FREE:
                    this.receivePlan();
                    break;
                case EXPLORING:
                    //this.reactiveBehaviour();
                    break;
                case NEED_RECHARGE:
                    this.claimRecharge();
                    if (this.rechargeTicket == null) {
                        this.status = DroneStatus.RECHARGING;
                    } else {
                        this.status = DroneStatus.FINISHED;
                    }
                    break;
                case RECHARGING:
                    this.recharge();
                    break;
                case BUSY:
                    this.executePlan();
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
        JsonObject response  = this._communications.sendAndReceiveToAPB(ACLMessage.QUERY_REF, content, "login");
        if(response != null){
            this.knowledge.currentPositionX = response.get("content").asObject().get("x").asInt();
            this.knowledge.currentPositionY = response.get("content").asObject().get("y").asInt();
            this.rechargeTicket = response.get("content").asObject().get("rechargeTicket").asString();
            this.sensorTicket = response.get("content").asObject().get("sensorTicket").asString();
        } else {
            this.status = DroneStatus.FINISHED;
        }
        
    }
    
    
    @Override
    void loginWorld(int x , int y){
        
        ArrayList<String> sensors = new ArrayList<>();
        sensors.add(this.sensorTicket);
        
        String result = this._communications.loginWorld("rescuer",x, y,sensors);
        if(result.equals("error")){
            this.status = DroneStatus.FINISHED;
        }
    }
    
    
    @Override         
    public void recharge(){ 
        if (this.toLand()) {
            String result = this._communications.requestRecharge(this.rechargeTicket);
            if (result.equals("ok")){
                this.knowledge.energy = 1000;
                this.rechargeTicket = null;
                if(this.plan.isEmpty() || this.plan == null)
                    this.status = DroneStatus.FREE;
                else
                   this.status = DroneStatus.EXPLORING; 
            } else {
                this.status = DroneStatus.FINISHED;
            }
            Info("Changed status to: " + this.status);
        }
    }
    
    void receivePlan(){
        
        JsonObject position = new JsonObject();
        position.add("x",this.knowledge.currentPositionX);
        position.add("y",this.knowledge.currentPositionY);
        
        JsonObject content = new JsonObject();
        content.add("request", "mission");
        content.add("currentPosition",position); 
        
        JsonObject response = this._communications.sendAndReceiveToAPB(ACLMessage.REQUEST, content, null);
        if(response != null){
            String mission = response.get("content").asObject().get("mission").asString();
            if(mission.equals("explore")){
               this.targetPositionX = response.get("content").asObject().get("target").asObject().get("x").asInt();
               this.targetPositionY = response.get("content").asObject().get("target").asObject().get("y").asInt();
               this.status = DroneStatus.EXPLORING;
            }else{
               this.status = DroneStatus.FINISHED;                
            }
            
        } else {
            this.status = DroneStatus.FINISHED;
        }
    }
    
    
    
    void reactiveBehaviour() {
        
       if (this.knowledge.cantReachTarget()) {
            Info("No encuentro el objetivo");
            this.status = DroneStatus.FINISHED;
            Info("Changed status to: " + this.status);
        } else {
            if (this.knowledge.needRecharge()) {
                this.status = DroneStatus.RECHARGING;
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
            } else {
                DroneOption bestOption;
                bestOption = chooseFromAlreadyVisitedOptions(options);
                noVisitedOptions.add(bestOption);
                options = noVisitedOptions;
            }
            DroneOption winner;
            winner = chooseFromNoVisitedOptions(options);
            if (winner != null) {
                this.plan = winner.plan;
                if (this.knowledge.shouldIRechargueFirst(winner))
                    this.status = DroneStatus.RECHARGING;
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
                    if (targetHeight == -1) {
                        this.status = DroneStatus.NEED_SENSOR;
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
            cost += this.knowledge.energyCost(nextAction, 0);
        }
        option.plan = plan;
        option.cost = cost;
        option.calculateDistanceToTarget(this.targetPositionX, this.targetPositionY);
        return option;
    }
    
    

    
    DroneOption chooseFromAlreadyVisitedOptions(ArrayList<DroneOption> options){
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
    
    DroneOption chooseFromNoVisitedOptions(ArrayList<DroneOption> options){
        double min = options.get(0).distanceToTarget;
        DroneOption bestOption = options.get(0);
        for (DroneOption o : options) {
            if (o.distanceToTarget < min) {
                min = o.distanceToTarget;
                bestOption = o;
            }
        }
         return bestOption;
    }
    
    @Override
    void executePlan() {
        this.doAction(this.plan.get(0));
        this.plan.remove(0);
        if (this.plan.size() == 0)  {
            this.plan = null;
        }
    }
    

}
