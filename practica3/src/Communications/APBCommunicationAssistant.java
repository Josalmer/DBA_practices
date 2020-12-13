/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communications;

import IntegratedAgent.IntegratedAgent;
import PublicKeys.PublicCardID;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;

/**
 *
 * @author Jose Saldaña, Manuel Pancorbo
 */
public class APBCommunicationAssistant extends CommunicationAssistant {
    ACLMessage shoppingChannel = new ACLMessage();
    String problem = "Playground1";
    
    public APBCommunicationAssistant(IntegratedAgent _agent, String identityManager, PublicCardID cardId) {
        super(_agent, identityManager, cardId);
    }

    /**
     * Crea una cuenta en el banco para el agente
     *
     * @author Jose Saldaña
     * @return nº de cuenta, formato: ACC#ejemplo, si algo sale mal devuelve
     * "error"
     */
    public JsonObject checkingWorld() {
        worldChannel = message(agentName, worldManager, ACLMessage.SUBSCRIBE, "ANALYTICS");
        worldChannel.setReplyWith("subscribeworld");

        // Set content
        JsonObject params = new JsonObject();
        params.add("problem", this.problem);
        String parsedParams = params.toString();
        worldChannel.setContent(parsedParams);

        this.agent.send(worldChannel);
        this.printSendMessage(worldChannel);
        
        MessageTemplate t = MessageTemplate.MatchInReplyTo("subscribeworld");
        ACLMessage in = this.agent.blockingReceive(t);
        
        if(this.checkError(ACLMessage.INFORM, in)){
            return null;
        }
        
        this.printReceiveMessage(in);
        worldChannel = in.createReply();
        this.sessionId = in.getConversationId();
        return Json.parse(in.getContent()).asObject();
    }
    
    /**
     * Queda a la escucha para compartir numero de cuenta con los drones
     * 
     * @author Jose Saldaña, Manuel pancorbo
     */
    public void listenAndShareSessionId(JsonArray map) {
        //MessageTemplate t = MessageTemplate.MatchInReplyTo("session");
        ACLMessage in = this.agent.blockingReceive();
        this.printReceiveMessage(in);
        
        ACLMessage agentChannel = in.createReply();
        agentChannel.setPerformative(ACLMessage.INFORM);
        
        JsonObject params = new JsonObject();
        params.add("sessionId", sessionId);   
        params.add("map", map);
        String parsedParams = params.toString();
        agentChannel.setContent(parsedParams);
        
        this.agent.send(agentChannel);
        this.printSendMessage(in);
    }
    
    /**
     * Queda a la escucha para recibir los bitcoins de los drones
     * 
     * @author Jose Saldaña, Domingo Lopez, Manuel pancorbo
     */
    public JsonObject listenAndCollectMoney() {
        MessageTemplate t = MessageTemplate.MatchInReplyTo("money");
        ACLMessage in = this.agent.blockingReceive(t);
        this.printReceiveMessage(in);
        
        String response = in.getContent();
        JsonObject parsedAnswer = Json.parse(response).asObject();
        return parsedAnswer.get("cash").asObject();
    }

    /**
     * Construye el catalogo de productos necesarios
     *
     * @author Jose Saldaña, Manuel Pancorbo
     * @return array con los catalogos
     * "error"
     */
    public JsonArray askShoppingCenters() {
        String service = "Shopping Center";
        ArrayList<String> agents = new ArrayList(yp.queryProvidersofService(service));
        
        JsonArray array = new JsonArray();
        JsonObject catalogue = new JsonObject();
        for (String shoppingCenter : agents) {
            catalogue.add("shop", shoppingCenter);
            catalogue.add("products",this.askSingleShoppingCenter(shoppingCenter));
            array.add(catalogue);
        }
        return array;
    }
    
    
    public JsonArray askSingleShoppingCenter(String receiver) {
        shoppingChannel = message(agentName, receiver, ACLMessage.QUERY_REF, "REGULAR");
        shoppingChannel.setReplyWith("shopping" + receiver);
        shoppingChannel.setContent("");
        
        this.agent.send(shoppingChannel);
        this.printSendMessage(shoppingChannel);
        
        MessageTemplate t = MessageTemplate.MatchInReplyTo("shopping" + receiver);
        ACLMessage in = this.agent.blockingReceive(t);
        
        if(this.checkError(ACLMessage.INFORM, in)){
            return null;     
        }
         
        this.printReceiveMessage(in);
        JsonObject parsedAnswer = Json.parse(in.getContent()).asObject();
        return parsedAnswer.get("details").asArray();        
    }
    
    
    public String buyCommunication(String sensorName, String seller, JsonArray payment) {
        shoppingChannel = message(agentName, seller, ACLMessage.REQUEST, "REGULAR");
        shoppingChannel.setReplyWith("shopping" + sensorName);
        
        // Set content
        JsonObject params = new JsonObject();
        params.add("operation", "buy");
        params.add("reference", sensorName);
        params.add("payment", payment);
        String parsedParams = params.toString();
        shoppingChannel.setContent(parsedParams);
        
        this.agent.send(shoppingChannel);
        this.printSendMessage(shoppingChannel);
        
        MessageTemplate t = MessageTemplate.MatchInReplyTo("shopping" + sensorName);
        ACLMessage in = this.agent.blockingReceive(t);

        if (this.checkError(ACLMessage.INFORM,in)) {
            return null;
        }
        
        this.printReceiveMessage(in);
        JsonObject parsedAnswer = Json.parse(in.getContent()).asObject();
        return parsedAnswer.get("details").asString();
    }
    
    
    
    
    
    
    public boolean checkMessagesAndOrderToLogout() {
        ACLMessage in = this.agent.blockingReceive(3000);
        if (in != null) {
            System.out.println("APB received " + in.getPerformative(in.getPerformative()) + " from: " + in.getSender() + " and respond with NOT_UNDERSTOOD");
            ACLMessage agentChannel = in.createReply();
            agentChannel.setPerformative(ACLMessage.NOT_UNDERSTOOD);
            this.agent.send(agentChannel);
            return true;
        } else {
            return false;
        }
    }
}
