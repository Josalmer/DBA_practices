/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Drone;

import Communications.DroneCommunicationAssistant;
import IntegratedAgent.IntegratedAgent;
import JSONParser.AgentJSONParser;
import com.eclipsesource.json.*;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;



/**
 *
 * @author migue
 */
public class Drone extends IntegratedAgent{
    boolean printMessages = true;
    String color="";
    
    String APBAccountNumber;

    DroneCommunicationAssistant _communications;
    DroneStatus status;
    DroneKnowledge knowledge;
    DronePerception perception = new DronePerception();
    DroneAction lastAction;
    Boolean needRecharge = true;
    ArrayList<DroneAction> plan;
    ArrayList<Integer> planInMap;
    String rechargeTicket = "";
    
    AgentJSONParser parser = new AgentJSONParser();
    
    @Override
    public void setup() {
        super.setup();

        this._communications = new DroneCommunicationAssistant(this, "Sphinx", _myCardID);
        this.knowledge = new DroneKnowledge();

        if (this._communications.chekingPlatform()) {
            this.status = DroneStatus.SUBSCRIBED_TO_PLATFORM;
            _exitRequested = false;
        } else {
            System.out.println(this.getLocalName() + " failed subscribing to Sphinx and DIE");
            _exitRequested = true;
        }
    }
    
    @Override
    public void plainExecute() {}
    
    public void checkingRadio(String role){
        boolean logedIn = this._communications.checkingRadio(role);
        if (logedIn) {
            this.status = DroneStatus.SUBSCRIBED_TO_WORLD;
        } else {
            this.status = DroneStatus.FINISHED;
        }
    }
    
    void sendCashToAPB() {
        this._communications.sendCashToAPB();
        this.status = DroneStatus.WAITING_INIT_DATA;
    }

    void requestSessionIdAndMap() {
        JsonObject response = this._communications.requestSessionKeyToAPB();
        if (response != null) {
            this.knowledge.map = parser.getMap(response.get("map").asArray());
            //Inicializamos Width y Height
            this.knowledge.mapWidth = this.knowledge.map.get(0).size();
            this.knowledge.mapHeight = this.knowledge.map.size();
            this.status = DroneStatus.SUBSCRIBED_TO_PLATFORM;
        } else {
            this.status = DroneStatus.FINISHED;
        }
    }
    
    void requestLoginData(){}
    
    void receiveLoginData(){}
    
    void loginWorld(int x , int y) {}
    
    void logout() {
        this._communications.checkoutWorld();
        _exitRequested = true;
    }
    
    void claimRecharge(){
        JsonObject content = new JsonObject();
        content.add("request", "recharge");
        JsonObject response = this._communications.sendAndReceiveToAPB(ACLMessage.REQUEST, content, "recharge");
        if (response != null) {
            if (response.get("performative").asInt() == ACLMessage.REFUSE) {
                this.rechargeTicket = null;
                this.status = DroneStatus.WAITING_FOR_FINISH;
                // Si es el seeker pasa a estado finished
                // Si es el rescuer pasa a estado backing_home
            } else {
                this.rechargeTicket = response.get("content").asObject().get("rechargeTicket").asString();
                this.status = DroneStatus.RECHARGING;
            }
        } else {
            this.status = DroneStatus.FINISHED;
        }
    } 
          
    public void recharge(){}
  
    
    void useEnergy(DroneAction action) {
        this.knowledge.energy -= this.knowledge.energyCost(action);
        this.print(this.getLocalName() + ", Executed action: " + action + " energy left: " + this.knowledge.energy);  
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
        
        if (action == DroneAction.moveF) {
            this.print(this.getLocalName() + ", Executed action, currentPosition: (" + this.knowledge.currentPositionX + ", " + this.knowledge.currentPositionY + ")\n");  
            while (!this._communications.checkIfFree(this.knowledge.nextPosition(), this.knowledge.currentHeight)) { }
        }
     
        String answer = this._communications.sendActionWorldManager(action.toString());

        if (answer.equals("ok")) {
            this.executeAction(action);
            this.lastAction = action;
        } else {
            this.status = DroneStatus.FINISHED;
        }
    }
     
    void executePlan(){}
        
    void executeAction(DroneAction action){
        switch(action){
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
    
    void elaboratePlan(){}
    
    @Override
    public void takeDown() {
        super.takeDown();
    }
   
    public void initialRecharge(){
        String result = this._communications.requestRecharge(this.rechargeTicket);
        if (result.equals("ok")){
            this.knowledge.fullRecharge();
            this.rechargeTicket = null;
        } else {
            this.status = DroneStatus.FINISHED;
        }
    }

    public void print(String event){
        if(this.printMessages){
            Info("\n\n" + this.color + " " + this.getLocalName() + " - " + event);
        }
    }
    
}
