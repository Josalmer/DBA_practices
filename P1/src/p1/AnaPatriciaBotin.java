package p1;

import IntegratedAgent.IntegratedAgent;
import com.eclipsesource.json.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Arrays;

public class AnaPatriciaBotin extends IntegratedAgent{
    ACLMessage outChannel = new ACLMessage();
    AgentAction lastAction;
    String receiver;
    String sessionKey;
    AgentStatus status;
    AgentAttributes attributes = new AgentAttributes(); 
    ArrayList<String> requestedSensors = new ArrayList<String>(Arrays.asList("distance", "lidar"));
    ArrayList<String> authorizedSensors = new ArrayList();
    Perception perception = new Perception();
    

    
    @Override
    public void setup() {
        super.setup();
        doCheckinPlatform();
        doCheckinLARVA();
        this.receiver = this.whoLarvaAgent(); 
        this.status = AgentStatus.INITIALIZING;
        _exitRequested = false;
    }
    
    @Override
    public void plainExecute() {
        while (!_exitRequested) {
            Info("Current Status: " + this.status);
            switch(this.status) {
                case INITIALIZING:
                    this.login();
                    break;
                case INITIALIZED:
                    this.p1Body();
                    break;
                case FINISHED:
                    this.logout();
                    break;
            }
        }
    }

    @Override
    public void takeDown() {
        this.doCheckoutLARVA();
        this.doCheckoutPlatform();
        super.takeDown();
    }
    
    JsonObject sendAndReceiveLogin(JsonObject params) {
        ACLMessage out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(this.receiver, AID.ISLOCALNAME));
        String parsedParams = params.toString();
        Info("Request: " + parsedParams);
        out.setContent(parsedParams);
        this.sendServer(out);
        ACLMessage in = this.blockingReceive();
        this.outChannel = in.createReply();
        String answer = in.getContent();
        Info("Answer: " + answer);
        JsonObject parsedAnswer = Json.parse(answer).asObject();
        return parsedAnswer;
    }
    
    JsonObject sendAndReceiveMessage(JsonObject params) {
        String parsedParams = params.toString();
        Info("Request: " + parsedParams);
        this.outChannel.setContent(parsedParams);
        this.sendServer(this.outChannel);
        ACLMessage in = this.blockingReceive();
        String answer = in.getContent();
        Info("Answer: " + answer);
        JsonObject parsedAnswer = Json.parse(answer).asObject();
        return parsedAnswer;
    }
    
    void sendMessage(JsonObject params) {
        String parsedParams = params.toString();
        Info("Request: "+ parsedParams);
        this.outChannel.setContent(parsedParams);
        this.sendServer(this.outChannel);
    } 
    
    void login() {
        JsonObject params = new JsonObject();
        params.add("command", "login");
        params.add("world", "BasePlayground");
        JsonArray sensors = new JsonArray();
        for (String sensor : this.requestedSensors) {
            sensors.add(sensor);
        }
        params.add("attach", sensors);
        
        JsonObject answer = this.sendAndReceiveLogin(params);
        
        if (answer.get("result").asString().equals("ok")) {
            this.initializeAgent(answer);
            this.status = AgentStatus.INITIALIZED;
            Info("Changed status to: " + this.status);
        } else {
            this.logout();
        }
    }
    
    void initializeAgent(JsonObject answer) {
        this.sessionKey = answer.get("key").asString();
        this.attributes.energy = 1000;
        this.attributes.orientation = 90;
        this.attributes.mapWidth = answer.get("width").asInt();
        this.attributes.mapHeight = answer.get("height").asInt();
        this.attributes.maxFlight = answer.get("maxflight").asInt();
        for (JsonValue j : answer.get("capabilities").asArray()) {
            authorizedSensors.add(j.asString());
        }
    }
    
    void p1Body() {
        this.readSensors();
        this.doAction(AgentAction.rotateL);
        this.status = AgentStatus.FINISHED;
        Info("Changed status to: " + this.status);
    }

    void logout() {
        JsonObject params = new JsonObject();
        params.add("command", "logout");
        
        this.sendMessage(params);
        
        _exitRequested = true;
    }
    
    void readSensors() {
        JsonObject params = new JsonObject();
        params.add("command", "read");
        params.add("key", this.sessionKey);
        
        JsonObject answer = this.sendAndReceiveMessage(params);
        
        if (answer.get("result").asString().equals("ok")) {
            Info("Valores de los sensores leídos...");
            this.useEnergy(AgentAction.LECTURA_SENSORES);
            this.updatePerception(answer);
        } else {
            this.logout();
        }
    }
    
    void updatePerception(JsonObject newPerception) {
       /* for (String key : authorizedSensors) {
            // Actualizar parametros TO DO
        }*/
    }
    

    
    //TODO
    //Gestión de errores. En vez de void, que devuelva un Integer y si es código -1 por ejemplo ya sabemos que algo ha ido mal
    void doAction(AgentAction action) { // Recibe enumerado, hace 6 acciones, actualiza estado del mundo
       
        JsonObject params = new JsonObject();
        params.add("command", "execute");
        params.add("action",action.toString());
        params.add("key", this.sessionKey);
            
        JsonObject answer = this.sendAndReceiveMessage(params);
        
        if(answer.get("result").asString().equals("ok")){
            this.executeAction(action); //TOTO Gestión de errores al ejecutar la acción internamente
            Info("Acción realizada:" + action.toString());
            this.lastAction = action;
           
        }else{
            this.logout();
        }      
        
    }
    
    void executeAction(AgentAction action){
        
        
        switch(action){
        
            case moveF:
                

            break;
            
            case moveUP:

            break;
            
            case moveD:
                
            break;
            
            case rotateL:

                if(this.attributes.orientation != -135){
                    this.attributes.orientation -= 45;
                }else{
                    this.attributes.orientation = 180;                    
                }
            break;
            
            case rotateR:
                
                if(this.attributes.orientation != 180){
                    this.attributes.orientation += 45;
                }else{
                    this.attributes.orientation = -135;                    
                }
            break;
            
            case touchD:
                
            break;
            
  
        }
        
        this.useEnergy(action);
    
        
            

    }
    
    
    void useEnergy(AgentAction action){
        
        
        switch(action){
        
            case moveF: this.attributes.energy -= 2; break;
           
            case moveUP: this.attributes.energy -= 10; break;
            
            case moveD: this.attributes.energy -= 2; break;
            
            case rotateL: this.attributes.energy -= 2; break;
            
            case rotateR: this.attributes.energy -= 2; break;
            //TODO enterarse bien cuando es la energía del TOUCHD
            case touchD:this.attributes.energy -= 10; break;
            
            case LECTURA_SENSORES : this.attributes.energy -= this.authorizedSensors.size();
  
        }
    }
    
}