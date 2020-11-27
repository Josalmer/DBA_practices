/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica3;

import IntegratedAgent.IntegratedAgent;
import PublicKeys.PublicCardID;
import YellowPages.YellowPages;
import com.eclipsesource.json.Json;
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
    
    ProductCatalogue shopsInfo = new ProductCatalogue();
    
    String problem = "playground1";
    
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

        // Set content
        JsonObject params = new JsonObject();
        params.add("problem", this.problem);
        String parsedParams = params.toString();
        worldChannel.setContent(parsedParams);

        this.agent.send(worldChannel);
        ACLMessage in = this.agent.blockingReceive();
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
    public void listenAndShareSessionId(ArrayList<ArrayList<Integer> > map) {
        MessageTemplate t = MessageTemplate.MatchInReplyTo("session");
        ACLMessage in = this.agent.blockingReceive(t);
        System.out.println("APB received " + in.getPerformative(in.getPerformative()) + " from: " + in.getSender());
        ACLMessage agentChannel = in.createReply();
        agentChannel.setPerformative(ACLMessage.INFORM);
        JsonObject params = new JsonObject();
        params.add("sessionId", sessionId);
        // Parse map
        params.add("map", "mapaParseado");
        String parsedParams = params.toString();
        agentChannel.setContent(parsedParams);
        this.agent.send(agentChannel);
    }
    
    /**
     * Queda a la escucha para recibir los bitcoins de los drones
     * 
     * @author Jose Saldaña, Domingo Lopez, Manuel pancorbo
     */
    public ArrayList<String> listenAndCollectMoney() {
        MessageTemplate t = MessageTemplate.MatchInReplyTo("money");
        ACLMessage in = this.agent.blockingReceive(t);
        System.out.println("APB received " + in.getPerformative(in.getPerformative()) + " from: " + in.getSender());
        String response = in.getContent();
        JsonObject parsedAnswer = Json.parse(response).asObject();
//        return parsedAnswer.get("cash").asArray();
        return null;
    }

    /**
     * Construye el catalogo de productos necesarios
     *
     * @author Jose Saldaña, Manuel Pancorbo
     * @return nº de cuenta, formato: ACC#ejemplo, si algo sale mal devuelve
     * "error"
     */
    public void askShoppingCenters() {
        String service = "Shopping Center";
        ArrayList<String> agents = new ArrayList(yp.queryProvidersofService(service));
        for (String shoppingCenter : agents) {
            this.askSingleShoppingCenter(shoppingCenter);
        }
    }
    
    
    public void askSingleShoppingCenter(String receiver) {
        System.out.println(this.agent.getLocalName() + " requesting shopping catalogue to " + receiver);
        shoppingChanel.setSender(this.agent.getAID());
        shoppingChanel.addReceiver(new AID(receiver, AID.ISLOCALNAME));
        shoppingChanel.setPerformative(ACLMessage.QUERY_REF);
        shoppingChanel.setProtocol("REGULAR");
        shoppingChanel.setContent("");
        this.agent.send(shoppingChanel);
        ACLMessage in = this.agent.blockingReceive();
        System.out.println(this.agent.getLocalName() + " sent QUERY_REF to " + receiver + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.INFORM) {
            shoppingChanel = in.createReply();
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            this.shopsInfo.update(receiver, parsedAnswer.get("details").asArray());
        }
    }
    
    public String buy(String sensorName) {
        int option = 0;
        String sensorCode = null;
        Product product = null;
        while (sensorCode == null && option < 3) {
            product = this.shopsInfo.bestOption(sensorName, option);
            if (product != null) {
                sensorCode = this.buyCommunication(product.getSensorTicket(), product.getShop());
            }
            option ++;
        }
        return sensorCode;
    }
    
    public String buyCommunication(String sensorName, String seller) {
        System.out.println(this.agent.getLocalName() + "buying " + sensorName + " to: " + seller);
        shoppingChanel.setSender(this.agent.getAID());
        shoppingChanel.addReceiver(new AID(seller, AID.ISLOCALNAME));
        shoppingChanel.setPerformative(ACLMessage.REQUEST);
        shoppingChanel.setProtocol("REGULAR");
        
        // Set content
        JsonObject params = new JsonObject();
        params.add("operation", "buy");
        params.add("reference", sensorName);
        // Hace falta hacer antes withdraw del banco
//        params.add("payment", this.money);
        String parsedParams = params.toString();
        shoppingChanel.setContent(parsedParams);
        
        this.agent.send(shoppingChanel);
        ACLMessage in = this.agent.blockingReceive();
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
}
