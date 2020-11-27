/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica3;

import IntegratedAgent.IntegratedAgent;
import com.eclipsesource.json.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author migue
 */
public class Drone extends IntegratedAgent{
    String APBAccountNumber;

    CommunicationAssistant _communications;
    DroneStatus status;
    AgentKnowledge knowledge = new AgentKnowledge();
    Perception perception = new Perception();
    DroneAction lastAction;
    Boolean needRecharge = true;
    ArrayList<DroneAction> plan;
    ArrayList<Integer> planInMap;
    ArrayList<String> authorizedSensors = new ArrayList();
    String rechargeTicket = "";
    
    @Override
    public void setup() {
        super.setup();
            
        this._communications = new CommunicationAssistant(this, _identitymanager, _myCardID);

        if (this._communications.chekingPlatform()) {
            this.status = DroneStatus.SUBSCRIBED_TO_PLATFORM;
            _exitRequested = false;
        } else {
            System.out.println(this.getLocalName() + " failed subscribing to" + _identitymanager + " and DIE");
            _exitRequested = true;
        }
    }
        @Override
    public void plainExecute() {
      //Se sobrecarga en el hijo
    }
    
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
    }

    void requestSessionIdAndMap() {
        JsonObject response = this._communications.requestSessionKeyToAPB();
        if (response != null) {
//            this.knowledge.map = this.perception.convertToIntegerMatrix(response);
        } else {
            this.status = DroneStatus.FINISHED;
        }
    }
    
    void requestLoginData() {
        //Este metodo tendra que ser sobrecargado en el hijo    
    }
    void loginWorld(int x , int y) {
        //Este metodo tendra que ser sobrecargado en el hijo    
    }
    void logout() {
        _exitRequested = true;
    }
    
     void claimRecharge(){
        JsonObject content = new JsonObject();
        content.add("request", "recharge");
        JsonObject response = this._communications.sendAndReceiveToAPB(ACLMessage.REQUEST, content, null);
        if (response != null) {
            if (response.get("performative").asInt() == ACLMessage.REFUSE) {
                this.rechargeTicket = null;
                // Si es el seeker pasa a estado finished
                // Si es el rescuer pasa a estado backing_home
            } else {
                this.rechargeTicket = response.get("content").asObject().get("rechargeTicket").asString();
            }
        } else {
            this.status = DroneStatus.FINISHED;
        }
    }
    
    
           
          
    public void recharge(){
     
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
     
        String answer = this._communications.sendActionWorldManager(action.toString());

        if (answer.equals("ok")) {
            this.executeAction(action);
            Info("Acción realizada:" + action.toString());
            this.lastAction = action;
        } else {
            this.status = DroneStatus.FINISHED;
        }
    }
     
      void executePlan(){
       
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
        //Se sobrecarga en el hijo
    }
    
    void receivePlan(){
       //Se sobrecarga en el hijo
        
    }
    
    @Override
    public void takeDown() {
        super.takeDown();
    }
    
   

    

}
