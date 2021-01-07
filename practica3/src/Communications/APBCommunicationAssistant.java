/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communications;

import MapOption.Coordinates;
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
    ACLMessage currentSeekerConversation = new ACLMessage();
    ACLMessage currentRescuer1Conversation = new ACLMessage();
    ACLMessage currentRescuer2Conversation = new ACLMessage();
    ACLMessage currentRechargingConversation = new ACLMessage();
    String problem = "World4";

    public APBCommunicationAssistant(IntegratedAgent _agent, String identityManager, PublicCardID cardId) {
        super(_agent, identityManager, cardId);
    }

    /**
     * Subscribe con analytics en el world
     *
     * @return Devuelve la respuesta del world manager
     */
    public JsonObject checkingWorld() {
        worldChannel = message(agentName, worldManager, ACLMessage.SUBSCRIBE, "ANALYTICS");
        worldChannel.setReplyWith("subscribeworld");

        // Set content
        JsonObject params = new JsonObject();
        params.add("problem", this.problem);
        worldChannel.setContent(params.toString());

        this.printSendMessage(worldChannel);
        this.agent.send(worldChannel);

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
     * Queda a la escucha para compartir numero de session con los drones
     *
     * @param map Mapa para compartir
     * @author Jose Saldaña, Manuel Pancorbo
     */
    public void listenAndShareSessionId(JsonArray map) {
        MessageTemplate t = MessageTemplate.MatchReplyWith("session");
        ACLMessage in = this.agent.blockingReceive(t);
        this.printReceiveMessage(in);

        ACLMessage agentChannel = in.createReply();
        agentChannel.setSender(this.agentName);
        agentChannel.setPerformative(ACLMessage.INFORM);

        JsonObject params = new JsonObject();
        params.add("sessionId", sessionId);
        params.add("map", map);
        agentChannel.setContent(params.toString());

        this.printSendMessage(agentChannel);
        this.agent.send(agentChannel);
    }

    /**
     * Comparte el session id con Awacs
     *
     * @author Domingo Lopez, Miguel García
     */
    public void shareSessionIdWithAwacs() {
        System.out.println("\n------SHARING SESSION WITH AWACS------\n");
        ACLMessage out = this.message(this.agentName, "AWACSBancoSantander", 0, "REGULAR");
        out.setConversationId(this.sessionId);
        this.agent.send(out);
    }

    /**
     * Queda a la escucha para recibir los bitcoins de los drones
     *
     * @author Jose Saldaña, Domingo Lopez, Manuel Pancorbo
     * @return Devuelve el array con los bitcoins
     */
    public JsonArray listenAndCollectMoney() {
        MessageTemplate t = MessageTemplate.MatchReplyWith("money");
        ACLMessage in = this.agent.blockingReceive(t);
        this.printReceiveMessage(in);

        JsonObject parsedAnswer = Json.parse(in.getContent()).asObject();
        return parsedAnswer.get("cash").asArray();
    }

    /**
     * Construye el catalogo de productos necesarios
     *
     * @author Jose Saldaña, Manuel Pancorbo
     * @return array con los catalogos de los shopping centers
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

    /**
     * Pregunta a cada centro comercial individualmente
     *
     * @author Manuel Pancorbo
     * @param receiver nombre del shopping center
     * @return el catalogo de del shopping center especificado
     */
    public JsonArray askSingleShoppingCenter(String receiver) {
        shoppingChannel = message(agentName, receiver, ACLMessage.QUERY_REF, "REGULAR");
        shoppingChannel.setReplyWith("shopping" + receiver);
        shoppingChannel.setContent("{}");
        shoppingChannel.setConversationId(this.sessionId);

        this.printSendMessage(shoppingChannel);
        this.agent.send(shoppingChannel);

        MessageTemplate t = MessageTemplate.MatchInReplyTo("shopping" + receiver);
        ACLMessage in = this.agent.blockingReceive(t);

        if (this.checkError(ACLMessage.INFORM, in)) {
            return null;
        }

        this.printReceiveMessage(in);
        JsonObject parsedAnswer = Json.parse(in.getContent()).asObject();
        return parsedAnswer.get("products").asArray();
    }

    /**
     * Compra un objeto de la tienda
     *
     * @author Jose Saldaña, Manuel Pancorbo
     * @param sensorName ele objeto a comprar
     * @param seller la tienda donde comprarlo
     * @param payment array de bitcoins para pagar
     * @return el ticket del objeto comprado
     */
    public String buyCommunication(String sensorName, String seller, JsonArray payment) {
        shoppingChannel = message(agentName, seller, ACLMessage.REQUEST, "REGULAR");
        shoppingChannel.setReplyWith("shopping" + sensorName);
        shoppingChannel.setConversationId(this.sessionId);

        // Set content
        JsonObject params = new JsonObject();
        params.add("operation", "buy");
        params.add("reference", sensorName);
        params.add("payment", payment);
        shoppingChannel.setContent(params.toString());

        this.printSendMessage(shoppingChannel);
        this.agent.send(shoppingChannel);

        MessageTemplate t = MessageTemplate.MatchInReplyTo("shopping" + sensorName);
        ACLMessage in = this.agent.blockingReceive(t);

        if (this.checkError(ACLMessage.INFORM, in)) {
            return null;
        }

        this.printReceiveMessage(in);
        JsonObject parsedAnswer = Json.parse(in.getContent()).asObject();
        return parsedAnswer.get("reference").asString();
    }

    /**
     * Manda las instrucciones iniciales a un dron
     *
     * @author Jose Saldaña
     * @param DroneName Nombre del dron
     * @param initialPos Posición inicial del dron (x, y)
     * @param rechargeTicket Ticket de recarga inicial para el dron
     * @param sensor Ticket del sensor (solo si es seeker)
     */
    public void sendInitialInstructions(String DroneName, Coordinates initialPos, String rechargeTicket, String sensor) {
        ACLMessage drone = message(agentName, DroneName, ACLMessage.INFORM, "REGULAR");
        drone.setReplyWith("login");

        // Set content
        JsonObject params = new JsonObject();
        params.add("x", initialPos.x);
        params.add("y", initialPos.y);
        params.add("rechargeTicket", rechargeTicket);
        if (sensor != null) {
            params.add("sensorTicket", sensor);
        }
        drone.setContent(params.toString());

        this.printSendMessage(drone);
        this.agent.send(drone);
    }

    /**
     * Finaliza la misión de un drone
     *
     * @author Jose Saldaña
     * @param DroneName Nombre del drone
     */
    public void sendFinishMissionMsg(String DroneName) {
        ACLMessage drone = message(agentName, DroneName, ACLMessage.INFORM, "REGULAR");
        drone.setReplyWith("end");

        this.printSendMessage(drone);
        this.agent.send(drone);
    }

    /**
     * Apaga Awacs con un cancel
     *
     * @author Domingo Lopez, Miguel García
     */
    public void switchOffAwacs() {
        ACLMessage awacs = message(agentName, "AWACSBancoSantander", ACLMessage.CANCEL, "REGULAR");
        this.printSendMessage(awacs);
        this.agent.send(awacs);
    }

    /**
     * Recibe las peticiones de los drones
     *
     * @author Jose Saldaña
     * @param key Clave de la petición del drone
     * @return JsonObject con la petición del drone
     */
    public JsonObject coordinateTeam(String key) {
        JsonObject response = new JsonObject();

        MessageTemplate t = MessageTemplate.MatchReplyWith(key);
        ACLMessage in = this.agent.blockingReceive(t, 1000);

        if (in == null) {
            return null;
        }

        this.printReceiveMessage(in);
        if (key.equals("recharge")) {
            this.currentRechargingConversation = in.createReply();
        } else if (key.equals("aleman")) {
            this.currentSeekerConversation = in.createReply();
        } else if (key.equals("mission")) {
            int number = Json.parse(in.getContent()).asObject().get("number").asInt();
            if (number == 1) {
                this.currentRescuer1Conversation = in.createReply();
            } else {
                this.currentRescuer2Conversation = in.createReply();
            }
        }
        if (in.getContent() != null) {
            response.add("content", Json.parse(in.getContent()).asObject());
        }
        response.add("key", key);
        return response;
    }

    /**
     * Manda la próxima misión al seeker
     *
     * @author Jose Saldaña
     * @param found nº de alemanes encontrados
     */
    public void nextSeekerMission(int found) {
        currentSeekerConversation.setPerformative(ACLMessage.INFORM);
        currentSeekerConversation.setSender(this.agentName);

        JsonObject params = new JsonObject();
        if (found < 9) {
            params.add("mission", "continue");
        } else {
            params.add("mission", "finish");
        }
        currentSeekerConversation.setContent(params.toString());

        this.printSendMessage(currentSeekerConversation);
        this.agent.send(currentSeekerConversation);
    }

    /**
     * Manda una misión de rescate al rescuer
     *
     * @author Jose Saldaña, Manuel Pancorbo
     * @param aleman Coordenadas del aleman (x, y)
     * @param number orden del rescuer
     */
    public void sendRescueMission(Coordinates aleman, int number) {
        ACLMessage rescuerChannel = new ACLMessage();
        if (number == 1) {
            rescuerChannel = currentRescuer1Conversation;
        } else {
            rescuerChannel = currentRescuer2Conversation;
        }
        rescuerChannel.setPerformative(ACLMessage.INFORM);
        rescuerChannel.setSender(this.agentName);

        JsonObject params = aleman.getJSON();
        params.add("mission", "rescue");
        rescuerChannel.setContent(params.toString());

        this.printSendMessage(rescuerChannel);
        this.agent.send(rescuerChannel);
    }

    /**
     * Manda una misión de volver a casa al rescuer
     *
     * @author Jose Saldaña, Manuel Pancorbo
     * @param initialPos Coordenadas de inicio del rescuer (x, y)
     * @param number orden del rescuer
     */
    public void sendBackHomeMission(Coordinates initialPos, int number) {
        ACLMessage rescuerChannel = new ACLMessage();
        if (number == 1) {
            rescuerChannel = currentRescuer1Conversation;
        } else {
            rescuerChannel = currentRescuer2Conversation;
        }
        rescuerChannel.setPerformative(ACLMessage.INFORM);
        rescuerChannel.setSender(this.agentName);

        JsonObject params = initialPos.getJSON();
        params.add("mission", "backHome");
        rescuerChannel.setContent(params.toString());

        this.printSendMessage(rescuerChannel);
        this.agent.send(rescuerChannel);
    }

    /**
     * Manda el ticket de recarga al último drone que pidio una recarga
     *
     * @author Jose Saldaña, Manuel Pancorbo
     * @param ticket Ticket de la recarga
     */
    public void sendRecharge(String ticket) {
        currentRechargingConversation.setPerformative(ACLMessage.INFORM);
        currentRechargingConversation.setSender(this.agentName);

        JsonObject params = new JsonObject();
        params.add("rechargeTicket", ticket);
        currentRechargingConversation.setContent(params.toString());

        this.printSendMessage(currentRechargingConversation);
        this.agent.send(currentRechargingConversation);
    }
}
