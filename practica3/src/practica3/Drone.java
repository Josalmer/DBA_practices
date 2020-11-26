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
    GeneralInfo info = new GeneralInfo();

    CommunicationAssistant _communications;
    DroneStatus status;
    AgentKnowledge knowledge = new AgentKnowledge();
    DronePerception perception = new DronePerception();
    DroneAction lastAction;
    Boolean needRecharge = true;
    ArrayList<DroneAction> plan;
    ArrayList<Integer> planInMap;
    ArrayList<String> authorizedSensors = new ArrayList();
    String rechargeTicket = "";
    
    @Override
    public void setup() {
        super.setup();
            
        this._communications = new CommunicationAssistant(this, _identitymanager, _myCardID, this.info.getWorld());

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
    
    public void checkingWorld(String role){
        String result = this._communications.checkingWorld(this.APBAccountNumber, role);
        
        switch(role){
            case "rescuer":
                if(result.equals("ok")){
                    this.status = DroneStatus.SUBSCRIBED_TO_WORLD;
                }else{
                    _exitRequested =  true;
                }
                break;
            case "seeker":
               //estado del seeker
                 break;
                 
            default:
                System.out.println("Rol erroneo, no es un dron");
                _exitRequested = true;
        }
        
    }

    void getAPBAccountNumber() {
        JsonObject content = new JsonObject();
        content.add("request", "bank");
        JsonObject response = this._communications.sendAndReceiveToAPB(ACLMessage.QUERY_REF, content);
        if (response != null) {
            APBAccountNumber = response.get("content").asObject().get("acc").asString();
        } else {
            this.status = DroneStatus.FINISHED;
        }
    }
    
    void loginAPB() {
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
        JsonObject response = this._communications.sendAndReceiveToAPB(ACLMessage.REQUEST, content);
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
