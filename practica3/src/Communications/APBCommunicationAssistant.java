/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communications;

import APB.Coordinates;
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
    ACLMessage currentDroneConversation = new ACLMessage();
    String problem = "Playground1";

    public APBCommunicationAssistant(IntegratedAgent _agent, String identityManager, PublicCardID cardId,  boolean printMessages) {
        super(_agent, identityManager, cardId, printMessages);
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

        if (this.checkError(ACLMessage.INFORM, in)) {
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

        MessageTemplate t = MessageTemplate.MatchReplyWith("session");
        ACLMessage in = this.agent.blockingReceive(t);
        this.printReceiveMessage(in);

        ACLMessage agentChannel = in.createReply();
        agentChannel.setPerformative(ACLMessage.INFORM);

        JsonObject params = new JsonObject();
        params.add("sessionId", sessionId);
        params.add("map", map);
        String parsedParams = params.toString();
        agentChannel.setContent(parsedParams);

        this.agent.send(agentChannel);
        this.printSendMessage(agentChannel);
    }

    public void shareSessionIdWithAwacs() {
        System.out.println("\n------SHARING SESSION WITH AWACS------\n");
        ACLMessage out = this.message(this.agentName, "AWACSBancoSantander", 0, "REGULAR");
//        ACLMessage out = this.message(this.agentName, "AWACS", 0, "REGULAR");
        out.setConversationId(this.sessionId);
        this.agent.send(out);
    }

    /**
     * Queda a la escucha para recibir los bitcoins de los drones
     *
     * @author Jose Saldaña, Domingo Lopez, Manuel pancorbo
     */
    public JsonArray listenAndCollectMoney() {
        MessageTemplate t = MessageTemplate.MatchReplyWith("money");
        ACLMessage in = this.agent.blockingReceive(t);
        this.printReceiveMessage(in);

        String response = in.getContent();
        JsonObject parsedAnswer = Json.parse(response).asObject();
        return parsedAnswer.get("cash").asArray();
    }

    /**
     * Construye el catalogo de productos necesarios
     *
     * @author Jose Saldaña, Manuel Pancorbo
     * @return array con los catalogos "error"
     */
    public JsonArray askShoppingCenters() {
        String[] id = this.sessionId.split("#");
        String service = "shop@SESSION#" + id[1];
        this.getYellowPages();
        System.out.println("\nShopping centers: " + yp.queryProvidersofService(service));
        ArrayList<String> agents = new ArrayList(yp.queryProvidersofService(service));

        JsonArray array = new JsonArray();
        for (String shoppingCenter : agents) {
            JsonObject catalogue = new JsonObject();
            System.out.println("\nMARKET: " + shoppingCenter);
            JsonArray products = this.askSingleShoppingCenter(shoppingCenter);
            catalogue.add("shop", shoppingCenter);
            catalogue.add("products", products);
            array.add(catalogue);
        }
        return array;
    }

    public JsonArray askSingleShoppingCenter(String receiver) {
        shoppingChannel = message(agentName, receiver, ACLMessage.QUERY_REF, "REGULAR");
        shoppingChannel.setReplyWith("shopping" + receiver);
        shoppingChannel.setContent("{}");
        shoppingChannel.setConversationId(this.sessionId);

        this.agent.send(shoppingChannel);
        this.printSendMessage(shoppingChannel);

        MessageTemplate t = MessageTemplate.MatchInReplyTo("shopping" + receiver);
        ACLMessage in = this.agent.blockingReceive(t);

        if (this.checkError(ACLMessage.INFORM, in)) {
            return null;
        }

        this.printReceiveMessage(in);
        JsonObject parsedAnswer = Json.parse(in.getContent()).asObject();
        return parsedAnswer.get("products").asArray();
    }

    public String buyCommunication(String sensorName, String seller, JsonArray payment) {
        shoppingChannel = message(agentName, seller, ACLMessage.REQUEST, "REGULAR");
        shoppingChannel.setReplyWith("shopping" + sensorName);
        shoppingChannel.setConversationId(this.sessionId);

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

        if (this.checkError(ACLMessage.INFORM, in)) {
            return null;
        }

        this.printReceiveMessage(in);
        JsonObject parsedAnswer = Json.parse(in.getContent()).asObject();
        return parsedAnswer.get("reference").asString();
    }

    public void sendInitialInstructions(String DroneName, Integer initialPos, String rechargeTicket, String sensor) {
        ACLMessage drone = new ACLMessage();
        drone.setPerformative(ACLMessage.INFORM);
        drone.setSender(this.agentName);
        drone.addReceiver(new AID(DroneName, AID.ISLOCALNAME));
        drone.setReplyWith("login");

        // Set content
        JsonObject params = new JsonObject();
        params.add("x", initialPos);
        params.add("y", initialPos);
        params.add("rechargeTicket", rechargeTicket);
        if (sensor != null) {
            params.add("sensorTicket", sensor);
        }
        String parsedParams = params.toString();
        drone.setContent(parsedParams);

        this.agent.send(drone);
        this.printSendMessage(drone);
    }

    public boolean checkMessagesAndOrderToLogout() {
        ACLMessage in = this.agent.blockingReceive(3000);
        if (in != null) {
            System.out.println("APB received " + in.getPerformative(in.getPerformative()) + " from: " + in.getSender() + " and respond with NOT_UNDERSTOOD");
            ACLMessage agentChannel = in.createReply();
            agentChannel.setPerformative(ACLMessage.CANCEL);
            this.agent.send(agentChannel);
            return true;
        } else {
            return false;
        }
    }

    public void switchOffAwacs() {
        ACLMessage awacs = new ACLMessage();
        awacs.setPerformative(ACLMessage.CANCEL);
        awacs.setSender(this.agentName);
        awacs.addReceiver(new AID("AWACSBancoSantander", AID.ISLOCALNAME));
        this.agent.send(awacs);
    }
    
    public JsonObject coordinateTeam(String key) {
        JsonObject response = new JsonObject();

        MessageTemplate t = MessageTemplate.MatchReplyWith(key);
        ACLMessage in = this.agent.blockingReceive(t, 1000);

        if(in != null){
            this.printReceiveMessage(in);
            if (key.equals("mission")) {
                this.currentDroneConversation = in.createReply();
            }
            response.add("content", Json.parse(in.getContent()).asObject());
            response.add("key", key);
            return response;
        }

        return null;
    }
    
    public void sendRescueMission(Coordinates aleman) {
        currentDroneConversation.setPerformative(ACLMessage.INFORM);

        JsonObject params = new JsonObject();
        params.add("mission", "rescue");
        params.add("x", aleman.getX());
        params.add("y", aleman.getY());
        String parsedParams = params.toString();
        currentDroneConversation.setContent(parsedParams);
        
        this.agent.send(currentDroneConversation);
        this.printSendMessage(currentDroneConversation);
    }
    
    public void sendBackHomeMission(Coordinates initialPos) {
        currentDroneConversation.setPerformative(ACLMessage.INFORM);

        JsonObject params = new JsonObject();
        params.add("mission", "backHome");
        params.add("x", initialPos.getX());
        params.add("y", initialPos.getY());
        String parsedParams = params.toString();
        currentDroneConversation.setContent(parsedParams);
        
        this.agent.send(currentDroneConversation);
        this.printSendMessage(currentDroneConversation);
    }

    public void sendRecharge(String ticket) {
        JsonObject params = new JsonObject();
        // NO HE COMPROBADO ESTO SI SE MANDA ASI; LO DEJO POR AQUI
        params.add("ticket", ticket);

        String parsedParams = params.toString();
        this.currentDroneConversation.setContent(parsedParams);

        this.printSendMessage(currentDroneConversation);
        this.agent.send(this.currentDroneConversation);

    }
    
    public void waitForFinish() {
        MessageTemplate t = MessageTemplate.MatchReplyWith("finish");
        ACLMessage in = this.agent.blockingReceive(t);
    }
}
