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
 * Clase Drone que hereda de IntegratedAgent
 * @author Miguel García 
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
    
    /**
     * Checking en el worldManager
     * @param role Role de los agentes
     * @author Jose Saldaña, Manuel Pancorbo
     */
    public void checkingRadio(String role){
        boolean logedIn = this._communications.checkingRadio(role);
        if (logedIn) {
            this.status = DroneStatus.SUBSCRIBED_TO_WORLD;
        } else {
            this.status = DroneStatus.FINISHED;
        }
    }
    
    /**
     * Los drones mandan su dinero a APB
     *
     * @author Jose Saldaña, Domingo Lopez, Manuel Pancorbo
     */
    void sendCashToAPB() {
        this._communications.sendCashToAPB();
        this.status = DroneStatus.WAITING_INIT_DATA;
    }

    /**
     * Solicitud de ID de sesión y mapa a APB
     *
     * @author Jose Saldaña, Domingo Lopez
     */
    void requestSessionIdAndMap() {
        JsonObject response = this._communications.requestSessionKeyToAPB();
        if (response != null) {
            this.knowledge.map = parser.getMap(response.get("map").asArray());
            //Inicializamos Width y Height
            this.knowledge.mapHeight = this.knowledge.map.get(0).size();
            this.knowledge.mapWidth = this.knowledge.map.size();
            this.status = DroneStatus.SUBSCRIBED_TO_PLATFORM;
        } else {
            this.status = DroneStatus.FINISHED;
        }
    }
    
    
//    void requestLoginData(){}
    
    /**
     * Recepción de datos iniciales (Id de sesión y mapa)
     * @author Jose Saldaña, Domingo Lopez, Miguel García
     */
    void receiveLoginData(){}
    
    /**
     * Login en el world de los drones
     * @param x Coordenada X inicial del drone
     * @param y Coordenada Y inicial del drone
     * @author Domingo Lopez
     */
    void loginWorld(int x , int y) {}
    
    /**
     * Deslogueo del mundo
     *
     * @author Jose Saldaña, Domingo Lopez, Manuel Pancorbo,Miguel García Tenorio
     */
    void logout() {
        this._communications.checkoutWorld();
        _exitRequested = true;
    }
    
    /**
     * Solicitud de ticket de recarga a APB
     *
     * @author Jose Saldaña, Domingo Lopez, Manuel Pancorbo, Miguel García Tenorio
     */
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
    
    /**
     * Realizar recarga de la energía del drone
     * @author Jose Saldaña, Domingo Lopez, Miguel García
     */
    public void recharge(){}
  
    /**
     * Consumir energía de los drones
     * @param action Acción a ejeutar
     * @author Domingo López, Jose Saldaña
     */
    void useEnergy(DroneAction action) {
        this.knowledge.energy -= this.knowledge.energyCost(action);
        this.print(this.getLocalName() + ", Executed action: " + action + " energy left: " + this.knowledge.energy);  
    }
     
    /**
     * Aterrizar el dron
     *
     * @author Jose Saldaña,Manuel Pancorbo, Miguel García Tenorio
     * @return boolean de aterrizaje
     */
    boolean toLand() {
        if (this.knowledge.canTouchDown()) {
            this.doAction(DroneAction.touchD);
            return true;
        } else {
            this.doAction(DroneAction.moveD);
            return false;
        }
    }
    
    /**
     * Realizar acción de drone pasada por parámetro. Se envía al WorldManager la acción a realizar
     * @param action, DroneAction la acción que va a realizar
     * @author Jose Saldaña, Domingo Lopez
     */
    void doAction(DroneAction action){
        
        if (action == DroneAction.moveF) {
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
    
    /**
     * Ejecución del plan para el comportamiento reactivo
     * @author Jose Saldaña, Domingo Lopez, Miguel García Tenorio
     */
    void executePlan(){}
    
    /**
     * Ejecutar acción pasada
     * @param action, DroneAction de la acción que va a ejecutar
     * @author Jose Saldaña, Domingo Lopez, Miguel García
     */
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

    /**
     * Función de deslogueo de la plataforma
     * @author Jose Saldaña, Domingo Lopez, Miguel García, Manuel Pancorbo
     */
    @Override
    public void takeDown() {
        super.takeDown();
    }
   
    /**
     * Recarga de energía inicial para los drones
     * @author Jose Saldaña, Domingo Lopez
     */
    public void initialRecharge(){
        String result = this._communications.requestRecharge(this.rechargeTicket);
        if (result.equals("ok")){
            this.knowledge.fullRecharge();
            this.rechargeTicket = null;
        } else {
            this.status = DroneStatus.FINISHED;
        }
    }

    /**
     * Función de impresión de mensaje formateado
     * @param event String
     * @author Jose Saldaña, Manuel Pancorbo
     */
    public void print(String event){
        if(this.printMessages){
            Info("\n\n" + this.color + " " + this.getLocalName() + " - " + event);
        }
    }
    
}
