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
    
    ACLMessage APBChannel = new ACLMessage(); // Solo Drones
    ArrayList<Integer> acceptedPerformative = new ArrayList<Integer>(Arrays.asList(ACLMessage.AGREE, ACLMessage.INFORM, ACLMessage.REFUSE)); // Posibles respuestas de APB a drones

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
        System.out.println(this.agent.getLocalName() + " waiting for APB to send id and Map");
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
        APBChannel.setSender(this.agent.getAID());
        APBChannel.addReceiver(new AID("Ana Patricia Botin", AID.ISLOCALNAME));
        APBChannel.setPerformative(ACLMessage.QUERY_REF);
        
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
        
       
        int resPerformative = in.getPerformative();
        System.out.println(this.agent.getLocalName() + " received " + ACLMessage.getPerformative(resPerformative)+" from  Ana Patricia Botin");
        
        if (acceptedPerformative.contains(resPerformative)){
            APBChannel = in.createReply();
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            this.sessionId = parsedAnswer.get("sessionId").asString();
            return parsedAnswer; 
        } else {
            System.out.println(this.agent.getLocalName() + "got error while waiting for APB id and map");
            return null;
        }
    }
    
    /**
     * Los drones mandan su dinero a APB
     *
     * @author Jose Saldaña, Domingo Lopez, Manuel Pancorbo
     * @param performative, content
     * @return JsonObject de respuesta
     */
    public void sendCashToAPB() {
        System.out.println(this.agent.getLocalName() + " send cash to Ana Patricia Botin");
        APBChannel.setSender(this.agent.getAID());
        APBChannel.addReceiver(new AID("Ana Patricia Botin", AID.ISLOCALNAME));
        APBChannel.setPerformative(ACLMessage.INFORM);
        
        JsonObject content = new JsonObject();
        content.add("cash", bitcoins);
        String parsedContent = content.toString();
        
        APBChannel.setContent(parsedContent);
        APBChannel.setReplyWith("money");
        this.agent.send(APBChannel);
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
        if (acceptedPerformative.contains(resPerformative)) {
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
        System.out.println(this.agent.getLocalName() + " REQUEST recharge with ticket: " + ticket);
        worldChannel.setSender(this.agent.getAID());
        worldChannel.addReceiver(new AID(this.worldManager, AID.ISLOCALNAME));
        worldChannel.setPerformative(ACLMessage.REQUEST);
        JsonObject content = new JsonObject();
        content.add("operation", "recharge");
        content.add("recharge", ticket);

        worldChannel.setContent(content.asString());
        this.agent.send(worldChannel);
        ACLMessage in = this.agent.blockingReceive();
        System.out.println(this.agent.getLocalName() + " sent recharge to " + "WorldManager " + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.CONFIRM) {
            worldChannel = in.createReply();
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            String result = parsedAnswer.asObject().get("result").asString();
            System.out.println(this.agent.getLocalName() + " was  recharged");
            return result;
        } else {
            System.out.println(this.agent.getLocalName() + " get ERROR while REQUEST to " + "WorldManager: " + this.worldManager);
            return "error";
        }
    }

    /**
     * Un drone manda un movimiento al worldManager
     *
     * @author Miguel García
     * @return resultado de la perticion
     */

    public String sendActionWorldManager(String content) {
        System.out.println(this.agent.getLocalName() + " send action to WorldManager: " + content);
        worldChannel.setSender(this.agent.getAID());
        worldChannel.addReceiver(new AID(this.worldManager, AID.ISLOCALNAME));
        worldChannel.setPerformative(ACLMessage.REQUEST);
        worldChannel.setProtocol("ANALYTICS");
        worldChannel.setEncoding(this._myCardID.getCardID());
        worldChannel.setReplyWith("REPLY###");
        JsonObject request = new JsonObject();
        request.add("operation", content);

        worldChannel.setContent(request.asString());
        this.agent.send(worldChannel);
        ACLMessage in = this.agent.blockingReceive();
        System.out.println(this.agent.getLocalName() + " sent action to " + "WorldManager " + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.CONFIRM) {
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            String result = parsedAnswer.asObject().get("result").asString();
            System.out.println(this.agent.getLocalName() + " do action: " + content);
            return result;
        } else {
            System.out.println(this.agent.getLocalName() + " get ERROR while REQUEST to " + "WorldManager: " + this.worldManager);
            return "error";
        }
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
        System.out.println(this.agent.getLocalName() + " login to WorldManager: " + role);
        worldChannel.setSender(this.agent.getAID());
        worldChannel.addReceiver(new AID(this.worldManager, AID.ISLOCALNAME));
        worldChannel.setPerformative(ACLMessage.REQUEST);
        worldChannel.setProtocol("ANALYTICS");
        worldChannel.setEncoding(this._myCardID.getCardID());
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
        ACLMessage in = this.agent.blockingReceive();
        System.out.println(this.agent.getLocalName() + " sent Login to " + "WorldManager " + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.CONFIRM) {
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            String result = parsedAnswer.asObject().get("result").asString();
            System.out.println(this.agent.getLocalName() + " was  Logged to: " + this.worldManager);
            return result;
        } else {
            System.out.println(this.agent.getLocalName() + " get ERROR while REQUEST to " + "WorldManager: " + this.worldManager);
            return "error";
        }
    }
}
