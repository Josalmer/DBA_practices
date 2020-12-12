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
    ACLMessage shoppingChanel = new ACLMessage();
    String problem = "4";
    
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
        System.out.println(this.agent.getLocalName() + " requesting sessionId to " + this.worldManager);
        worldChannel.setSender(this.agent.getAID());
        worldChannel.addReceiver(new AID(this.worldManager, AID.ISLOCALNAME));
        worldChannel.setPerformative(ACLMessage.SUBSCRIBE);
        worldChannel.setProtocol("ANALYTICS");
        worldChannel.setReplyWith("subscribeworld");

        // Set content
        JsonObject params = new JsonObject();
        params.add("problem", this.problem);
        String parsedParams = params.toString();
        worldChannel.setContent(parsedParams);

        this.agent.send(worldChannel);
        System.out.println(worldChannel);
        MessageTemplate t = MessageTemplate.MatchInReplyTo("subscribeworld");
        ACLMessage in = this.agent.blockingReceive(t);
        System.out.println(in);
        System.out.println(this.agent.getLocalName() + " sent SUSCRIBE to " + this.worldManager + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.INFORM) {
            worldChannel = in.createReply();
            this.sessionId = in.getConversationId();
            return Json.parse(in.getContent()).asObject();
        } else {
            System.out.println(this.agent.getLocalName() + " get ERROR while suscribing to " + this.worldManager);
            return null;
        }
    }
    
    /**
     * Queda a la escucha para compartir numero de cuenta con los drones
     * 
     * @author Jose Saldaña, Manuel pancorbo
     */
    public void listenAndShareSessionId(JsonArray map) {
        MessageTemplate t = MessageTemplate.MatchInReplyTo("session");
        ACLMessage in = this.agent.blockingReceive(t);
        System.out.println("APB received " + in.getPerformative(in.getPerformative()) + " from: " + in.getSender());
        ACLMessage agentChannel = in.createReply();
        agentChannel.setPerformative(ACLMessage.INFORM);
        JsonObject params = new JsonObject();
        params.add("sessionId", sessionId);   
        params.add("map", map);
        String parsedParams = params.toString();
        agentChannel.setContent(parsedParams);
        this.agent.send(agentChannel);
    }
    
    /**
     * Queda a la escucha para recibir los bitcoins de los drones
     * 
     * @author Jose Saldaña, Domingo Lopez, Manuel pancorbo
     */
    public JsonObject listenAndCollectMoney() {
        MessageTemplate t = MessageTemplate.MatchInReplyTo("money");
        ACLMessage in = this.agent.blockingReceive(t);
        System.out.println("APB received " + in.getPerformative(in.getPerformative()) + " from: " + in.getSender());
        String response = in.getContent();
        JsonObject parsedAnswer = Json.parse(response).asObject();
        return parsedAnswer;
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
        System.out.println(this.agent.getLocalName() + " requesting shopping catalogue to " + receiver);
        shoppingChanel.setSender(this.agent.getAID());
        shoppingChanel.addReceiver(new AID(receiver, AID.ISLOCALNAME));
        shoppingChanel.setPerformative(ACLMessage.QUERY_REF);
        shoppingChanel.setReplyWith("shoping" + receiver);
        shoppingChanel.setProtocol("REGULAR");
        shoppingChanel.setContent("");
        this.agent.send(shoppingChanel);
        MessageTemplate t = MessageTemplate.MatchInReplyTo("shoping" + receiver);
        ACLMessage in = this.agent.blockingReceive(t);
        System.out.println(this.agent.getLocalName() + " sent QUERY_REF to " + receiver + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.INFORM) {
            shoppingChanel = in.createReply();
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            return parsedAnswer.get("details").asArray();
        }
        return null;
    }
    
    
    public String buyCommunication(String sensorName, String seller, JsonArray payment) {
        System.out.println(this.agent.getLocalName() + "buying " + sensorName + " to: " + seller);
        shoppingChanel.setSender(this.agent.getAID());
        shoppingChanel.addReceiver(new AID(seller, AID.ISLOCALNAME));
        shoppingChanel.setPerformative(ACLMessage.REQUEST);
        shoppingChanel.setProtocol("REGULAR");
        shoppingChanel.setReplyWith("shoping" + sensorName);
        
        // Set content
        JsonObject params = new JsonObject();
        params.add("operation", "buy");
        params.add("reference", sensorName);
        params.add("payment", payment);
        String parsedParams = params.toString();
        shoppingChanel.setContent(parsedParams);
        
        this.agent.send(shoppingChanel);
        MessageTemplate t = MessageTemplate.MatchInReplyTo("shoping" + sensorName);
        ACLMessage in = this.agent.blockingReceive(t);
        System.out.println(this.agent.getLocalName() + " sent REQUEST to " + seller + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.INFORM) {
            shoppingChanel = in.createReply();
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            return parsedAnswer.get("details").asString();
        } else {
            return null;
        }
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
