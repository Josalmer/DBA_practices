package practica3;

import IntegratedAgent.IntegratedAgent;
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
            Info("Current Status: " + this.status);
            switch (this.status) {
               
                case SUBSCRIBED_TO_PLATFORM:
                    this.getAPBAccountNumber();
                    this.checkingWorld("rescuer");
                    break;
                case SUBSCRIBED_TO_WORLD:
                    // Wait APB for instrucción and tickets for login
                    this.login();
                    this.login(this.knowledge.currentPositionX,this.knowledge.currentPositionY);
                    this.recharge();
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

   @Override
    void login() {
        JsonObject parsedData = this._communications.queryLogin("login");
        this.knowledge.currentPositionX = parsedData.get("x").asInt();
        this.knowledge.currentPositionY = parsedData.get("y").asInt();
        this.rechargeTicket = parsedData.get("rechargeTicket").asString();
        
        JsonArray array = parsedData.get("map").asArray();
        
        this.knowledge.map = this.perception.convertToIntegerMatrix(array);
    }

    @Override
    void login(int x , int y){
        String result = this._communications.requestLoginWorldManager("rescuer",x, y, new ArrayList<String>());
        if(result.equals("error")){
            _exitRequested =true;
        }
    }
   
    @Override
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
    @Override
    void receivePlan(){
        //this.planInMap = this._communications.queryPlan("Need Plan");
        this.elaboratePlan();
        this.status = RescuerStatus.BUSY;
        
    }
    

  
}
