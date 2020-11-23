package practica3;

import IntegratedAgent.IntegratedAgent;
import com.eclipsesource.json.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Arrays;

public class Rescuer extends IntegratedAgent {

    // AGENT CONFIGURATION -------------------------------------------
    // END CONFIGURATION ---------------------------------------------

    String APBAccountNumber;

    CommunicationAssistant _communications;
    RescuerStatus status;
    AgentKnowledge knowledge = new AgentKnowledge();
    DronePerception perception = new DronePerception();
    DroneAction lastAction;
    Boolean needRecharge = true;
    ArrayList<DroneAction> plan;
    ArrayList<Integer> planInMap;
    ArrayList<String> authorizedSensors = new ArrayList();
    
    @Override
    public void setup() {
        super.setup();

        this._communications = new CommunicationAssistant(this, _identitymanager, _myCardID);

        if (this._communications.chekingPlatform()) {
            this.status = RescuerStatus.SUBSCRIBED_TO_PLATFORM;
            _exitRequested = false;
        } else {
            System.out.println(this.getLocalName() + " failed subscribing to" + _identitymanager + " and DIE");
            _exitRequested = true;
        }
    }

    @Override
    public void plainExecute() {
        while (!_exitRequested) {
            Info("Current Status: " + this.status);
            switch (this.status) {
                case SUBSCRIBED_TO_PLATFORM:
                    this.getAPBAccountNumber();
                    break;
                case SUBSCRIBED_TO_WORLD:
                    // Wait APB for instrucción and tickets for login
                    this.login();
                    break;
                case FREE:
                    this.receivePlan();
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

    void getAPBAccountNumber() {
        APBAccountNumber = this._communications.queryRefAPB("subscribedToPlatform");
        if (APBAccountNumber == "error") {
            this.status = RescuerStatus.FINISHED;
        } else {
            // Suscribirse al mundo
            this.status = RescuerStatus.SUBSCRIBED_TO_WORLD;
        }
    }

    void login() {

    }

    void logout() {
        _exitRequested = true;
    }
    void useEnergy(DroneAction action) {
        this.knowledge.energy -= this.knowledge.energyCost(action, this.authorizedSensors.size());
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
    void doAction(DroneAction action){
        JsonObject params = new JsonObject();
        params.add("command", "execute");
        params.add("action", action.toString());
        //params.add("key", this.sessionKey);

        JsonObject answer = this._communications.sendAndReceiveMessage(params);

        if (answer.get("result").asString().equals("ok")) {
            this.executeAction(action);
            Info("Acción realizada:" + action.toString());
            this.lastAction = action;
        } else {
            this.logout();
        }
    }
    void executePlan(){
        if(this._communications.queryMove()){
            if(this.knowledge.needRecharge()){
                if(this.toLand()){
                    this.doAction(DroneAction.recharge);  
                }
            }else{
                this.doAction(this.plan.get(0));
                this.plan.remove(0);
                if(this.plan.size() == 0){
                    this.status = RescuerStatus.FREE;
                    this.plan = null;
                }
            }
            
        }
    }
    
    void executeAction(DroneAction action){
        switch(action){
            case recharge:
                //FALTA
                break;
            default:
                this.knowledge.manageMovement(action);
                this.useEnergy(action);
                break;
        }
    }
    void elaboratePlan(){
       
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
        for(int i = 0 ; i < this.planInMap.size(); i += 3){
            height = this.planInMap.get(i+2);
            destinyYPosition = this.planInMap.get(i + 1);
            destinyXPosition = this.planInMap.get(i);
           if(currentXPosition > destinyXPosition &&  currentYPosition == destinyYPosition  ){
               orientation = -90;
           }else if(currentXPosition < destinyXPosition &&  currentYPosition == destinyYPosition ){
               orientation = -90;
           }else if(currentXPosition == destinyXPosition &&  currentYPosition > destinyYPosition){
               orientation = 0;
           }else if(currentXPosition == destinyXPosition &&  currentYPosition < destinyYPosition){
               orientation = 180;
           }else if(currentXPosition > destinyXPosition &&  currentYPosition > destinyYPosition){
               orientation = -45;
           }else if(currentXPosition < destinyXPosition &&  currentYPosition > destinyYPosition){
               orientation = 45;
           }else if(currentXPosition > destinyXPosition &&  currentYPosition < destinyYPosition){
               orientation = -135;
           }else if(currentXPosition < destinyXPosition &&  currentYPosition < destinyYPosition){
               orientation = 135;
           }
           
          
           while(!onWantedBox){
                if(orientation != provisionalOrientation){
                   int turns = this.knowledge.howManyTurns(orientation);
                   if(this.knowledge.shouldTurnRight(turns)){
                       nextAction = DroneAction.rotateR;
                       provisionalOrientation = this.knowledge.getNextOrientation(provisionalOrientation, true);
                   }else{
                       nextAction = DroneAction.rotateL;
                       provisionalOrientation = this.knowledge.getNextOrientation(provisionalOrientation, false);
                   }
                }else if(provisionalHeigth < height ){
                    nextAction = DroneAction.moveUP;
                    provisionalHeigth += 5;
                }else{
                    nextAction = DroneAction.moveUP;
                    onWantedBox = true;
                }
                
                this.plan.add(nextAction);
           }
           
           
           
        }
    }
    void receivePlan(){
        this.planInMap = this._communications.queryPlan("Need Plan");
        this.elaboratePlan();
        this.status = RescuerStatus.BUSY;
        
    }
    

    @Override
    public void takeDown() {
        super.takeDown();
    }

}
