/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communications;

import IntegratedAgent.IntegratedAgent;
import PublicKeys.PublicCardID;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author manuel
 */
public class DroneCommunicationAssistant extends CommunicationAssistant{
    
    boolean printMessages;
    
    String APBName = "Ana Patricia Botin";
    
    ACLMessage APBChannel = new ACLMessage(); // Solo Drones
    ArrayList<Integer> acceptedPerformatives = new ArrayList<Integer>(Arrays.asList(ACLMessage.AGREE, ACLMessage.INFORM, ACLMessage.REFUSE)); // Posibles respuestas de APB a drones

    public DroneCommunicationAssistant(IntegratedAgent _agent, String identityManager, PublicCardID cardId) {
        super(_agent, identityManager, cardId);
    }

     
    /**
     * Los drones solicitan la sessionId y el mapa a APB y esperan la respuesta
     *
     * @author Jose Saldaña, Domingo Lopez, Manuel Pancorbo
     * @param performative, content
     * @return JsonObject de respuesta
     */
    public JsonObject requestSessionKeyToAPB() { 
        /*APBChannel.setSender(this.agent.getAID());
        APBChannel.addReceiver(new AID("Ana Patricia Botin", AID.ISLOCALNAME));
        APBChannel.setPerformative(ACLMessage.QUERY_REF);
        
        JsonObject content = new JsonObject();
        content.add("request", "session");
        String parsedContent = content.toString();
        
        APBChannel.setContent(parsedContent);
        APBChannel.setReplyWith("session");

        this.agent.send(APBChannel);*/
        
        //Carga de mensaje para reintentos de conexión con APB.
        APBChannel = message(agentName, APBName, ACLMessage.QUERY_REF, "REGULAR");
        
        JsonObject content = new JsonObject();
        content.add("request", "session");
        String parsedContent = content.toString();
        
        APBChannel.setContent(parsedContent);
        //APBChannel.setReplyWith("session");
        
        ACLMessage in = null;
        int tryouts = 0;
        boolean done = false;
        while(tryouts < 3 && !done){
            this.agent.send(APBChannel);
            in = this.agent.blockingReceive(5000);
            
            tryouts++;
            System.out.print("\nIntento "+tryouts + " para" + this.agent.getLocalName()+"\n" );
            if(in != null)
                done = true;   
        }
        
        if(!done){
            System.out.println(this.agent.getLocalName() + "got error while waiting for APB id and map");
            return null;
        }
        
        if(checkAPBError(in)){
            return null;
        }

        this.printReceiveMessage(in);
        JsonObject parsedAnswer = Json.parse(in.getContent()).asObject();
        this.sessionId = parsedAnswer.get("sessionId").asString();
        return parsedAnswer; 
    }
    
    /**
     * Los drones mandan su dinero a APB
     *
     * @author Jose Saldaña, Domingo Lopez, Manuel Pancorbo
     * @param performative, content
     * @return JsonObject de respuesta
     */
    public void sendCashToAPB() {
        APBChannel = message(agentName, APBName, ACLMessage.INFORM, "REGULAR");
        APBChannel.setReplyWith("money");
        
        JsonObject content = new JsonObject();
        content.add("cash", bitcoins);
        String parsedContent = content.toString();
        
        APBChannel.setContent(parsedContent);

        this.agent.send(APBChannel);
        this.printSendMessage(APBChannel);
    }
    
    
    /**
     * Manda un mensaje a Ana Patricia Botin y no espera respuesta
     *
     * @param content
     * @return ACLMessage de respuesta
     */
    public void sendMessageToAPB(int performative, JsonObject content, String key) {
        String parsedContent = content.toString();
        System.out.println(this.agent.getLocalName() + " " + ACLMessage.getPerformative(performative) + " to Ana Patricia Botin: " + parsedContent);
        APBChannel.setSender(this.agent.getAID());
        APBChannel.addReceiver(new AID("Ana Patricia Botin", AID.ISLOCALNAME));
        if (key != null) {
            APBChannel.setReplyWith(key);
        }
        APBChannel.setPerformative(performative);
        APBChannel.setContent(parsedContent);
        this.agent.send(APBChannel);
    }
    
    /**
     * Manda un mensaje a Ana Patricia Botin y espera respuesta
     *
     * @author Jose Saldaña, Manuel Pancorbo
     * @param performative, content
     * @return JsonObject de respuesta
     */
    public JsonObject sendAndReceiveToAPB(int performative, JsonObject content, String key) {
        String parsedContent = content.toString();
        System.out.println(this.agent.getLocalName() + " " + ACLMessage.getPerformative(performative) + " to Ana Patricia Botin: " + parsedContent);
        APBChannel.setSender(this.agent.getAID());
        APBChannel.addReceiver(new AID("Ana Patricia Botin", AID.ISLOCALNAME));
        APBChannel.setPerformative(performative);
        APBChannel.setContent(parsedContent);
        APBChannel.setReplyWith(content.get("request").asString());
        if (key != null) {
            APBChannel.setReplyWith(key);
        }
        this.agent.send(APBChannel);
        ACLMessage in = this.agent.blockingReceive();
        int resPerformative = in.getPerformative();
        System.out.println(this.agent.getLocalName() + " sent " + ACLMessage.getPerformative(performative) + " to Ana Patricia Botin and get: " + ACLMessage.getPerformative(resPerformative));
        if (acceptedPerformatives.contains(resPerformative)) {
            APBChannel = in.createReply();
            JsonObject response = new JsonObject();
            response.add("performative", resPerformative);
            JsonObject resContent = Json.parse(in.getContent()).asObject();
            response.add("content", resContent);
            return response;
        } else {
            System.out.println(this.agent.getLocalName() + " get ERROR while " + ACLMessage.getPerformative(performative) + "to Ana Patricia Botin: " + parsedContent);
            return null;
        }
    }
    
    /**
     * Un dron pide recargar al WorldManager de un mundo
     *
     * @author Miguel García
     * @return resultado de la perticion
     */
    public String requestRecharge(String ticket) {
        worldChannel = message(agentName, worldManager, ACLMessage.REQUEST, "REGULAR");
        worldChannel.setConversationId(this.sessionId);
        
        JsonObject content = new JsonObject();
        content.add("operation", "recharge");
        content.add("recharge", ticket);
        worldChannel.setContent(content.toString());
        
        this.agent.send(worldChannel);
        this.printSendMessage(worldChannel);
        
        ACLMessage in = this.agent.blockingReceive();
        if (checkError(ACLMessage.CONFIRM,in)){
            return "error";
        }
        
        this.printReceiveMessage(in);
        System.out.println(this.agent.getLocalName() + " was  recharged");
        return ok(in);
    }
    

    /**
     * Un drone manda un movimiento al worldManager
     *
     * @author Miguel García
     * @return resultado de la perticion
     */

    public String sendActionWorldManager(String content) {
        worldChannel = message(agentName, worldManager, ACLMessage.REQUEST, "ANALYTICS");
        worldChannel.setConversationId(this.sessionId);
        worldChannel.setReplyWith("REPLY###");
        
        JsonObject request = new JsonObject();
        request.add("operation", content);
        worldChannel.setContent(request.toString());
        
        this.agent.send(worldChannel);
        this.printSendMessage(worldChannel);
        
        ACLMessage in = this.agent.blockingReceive();
        if (checkError(ACLMessage.CONFIRM,in)) {
            return "error";
        }
        
        this.printReceiveMessage(in);
        return ok(in);
    }

    /**
     * EL drone se logue en el mundo
     *
     * @author Miguel García
     * @param role
     * @param x
     * @param y
     * @param sensors
     * @return
     */
    public String loginWorld(String role, int x, int y, ArrayList<String> sensors) {
        worldChannel = message(agentName, worldManager, ACLMessage.REQUEST, "ANALYTICS");
        worldChannel.setConversationId(sessionId);
        worldChannel.setReplyWith("REPLY###");
        
        JsonObject content = new JsonObject();
        content.add("operation", "login");
        if (role.equals("rescuer")) {
            content.add("attach", "[]");
        } else if(role.equals("seeker")) {
            content.add("attach","["+sensors.get(0)+"]");
        }
        content.add("posx", x);
        content.add("posy", y);
        worldChannel.setContent(content.asString());
        
        this.agent.send(worldChannel);
        this.printSendMessage(worldChannel);
        
        ACLMessage in = this.agent.blockingReceive();
        if (checkError(ACLMessage.CONFIRM, in)) {
            return "error";
        }
            
        this.printReceiveMessage(in);
        return ok(in);       
    }
    
    
    public String ok(ACLMessage in){
        return  Json.parse(in.getContent()).asObject().get("result").asString();
    }
    
    
    public boolean checkAPBError(ACLMessage in){
        if(in.getPerformative() == ACLMessage.AGREE || in.getPerformative() == ACLMessage.INFORM || in.getPerformative() == ACLMessage.REFUSE)
            return false;
        else
            return true;
        /*
        for(int performative : this.acceptedPerformatives){
            if(!checkError(performative, in)){
                return false;
            }
        }
        return true;*/
    }
}
