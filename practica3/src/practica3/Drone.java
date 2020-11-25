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
    RescuerStatus status; // ESTO DEBERIA SER DRONESTATUS
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
      //Se sobrecarga en el hijo
    }
    
    public void checkingWorld(String role){
        String result = this._communications.checkingWorld(this.APBAccountNumber, role);
        
        switch(role){
            case "rescuer":
                if(result.equals("ok")){
                    this.status = RescuerStatus.SUBSCRIBED_TO_WORLD;
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
        APBAccountNumber = this._communications.queryAccount("bank");
        if (APBAccountNumber == "error") {
            this.status = RescuerStatus.FINISHED;
        } 
    }
    
    void login() {
        //Este metodo tendra que ser sobrecargado en el hijo    
    }
    void login(int x , int y) {
        //Este metodo tendra que ser sobrecargado en el hijo    
    }
    void logout() {
        _exitRequested = true;
    }
    
    void claimRecharge(){
        
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
                if(this.plan.isEmpty()){
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
