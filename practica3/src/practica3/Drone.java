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
        String result = this._communications.requestAPBRecharge("recharge");
        
        if(result.equals("error")){
            this.status = RescuerStatus.FINISHED;
        }else if ( result.equals("refuse")){
            //Aqui no se que hace el dron
            this.rechargeTicket = null;
        }else{
            this.rechargeTicket = result;
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
            this.status = RescuerStatus.FINISHED;
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
