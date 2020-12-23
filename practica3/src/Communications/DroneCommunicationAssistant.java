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
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author manuel
 */
public class DroneCommunicationAssistant extends CommunicationAssistant {

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
        APBChannel = message(agentName, APBName, ACLMessage.QUERY_REF, "REGULAR");
        APBChannel.setReplyWith("session");

        JsonObject content = new JsonObject();
        content.add("request", "session");
        APBChannel.setContent(content.toString());
 
        this.printSendMessage(APBChannel);
        this.agent.send(APBChannel);
        
        ACLMessage in = this.agent.blockingReceive();

        if (checkAPBError(in)) {
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

        APBChannel.setContent(content.toString());

        this.printSendMessage(APBChannel);
        this.agent.send(APBChannel);
    }

    /**
     * Manda un mensaje a Ana Patricia Botin y no espera respuesta
     *
     * @param content
     * @return ACLMessage de respuesta
     */
    public void sendMessageToAPB(int performative, JsonObject content, String key) {
        APBChannel = message(agentName, "Ana Patricia Botin", performative, "REGULAR");

        if (key != null) {
            APBChannel.setReplyWith(key);
        }

        APBChannel.setContent(content.toString());

        this.printSendMessage(APBChannel);
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
        APBChannel = message(agentName, "Ana Patricia Botin", performative, "REGULAR");
        APBChannel.setContent(content.toString());

        if (key != null) {
            APBChannel.setReplyWith(key);
        }

        this.printSendMessage(APBChannel);
        this.agent.send(APBChannel);

        ACLMessage in = this.agent.blockingReceive();

        if (this.checkAPBError(in)) {
            return null;
        }

        this.printReceiveMessage(in);
        int resPerformative = in.getPerformative();

        APBChannel = in.createReply();
        JsonObject response = new JsonObject();
        response.add("performative", resPerformative);
        response.add("content", Json.parse(in.getContent()).asObject());
        return response;
    }

    /**
     * Manda un mensaje a Ana Patricia Botin y espera respuesta
     *
     * @author Jose Saldaña, Manuel Pancorbo
     * @param performative, content
     * @return JsonObject de respuesta
     */
    public JsonObject receiveFromAPB(String key) {
        ACLMessage in = new ACLMessage();

        if (key != null) {
            MessageTemplate t = MessageTemplate.MatchReplyWith(key);
            in = this.agent.blockingReceive(t);
        } else {
            in = this.agent.blockingReceive();
        }

        if (this.checkAPBError(in)) {
            return null;
        }

        this.printReceiveMessage(in);
        int resPerformative = in.getPerformative();

        APBChannel = in.createReply(); //Esto para que??
        JsonObject response = new JsonObject();
        response.add("performative", resPerformative);
        response.add("content", Json.parse(in.getContent()).asObject());
        return response;
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

        this.printSendMessage(worldChannel);
        this.agent.send(worldChannel);

        ACLMessage in = this.agent.blockingReceive();
        if (checkError(ACLMessage.INFORM, in)) {
            return "error";
        }

        worldChannel = in.createReply();
        this.printReceiveMessage(in);
        System.out.println(this.agent.getLocalName() + " was  recharged");
        return "ok";
    }

    /**
     * Un drone manda un movimiento al worldManager
     *
     * @author Miguel García
     * @return resultado de la perticion
     */
    public String sendActionWorldManager(String content) {
        worldChannel.setPerformative(ACLMessage.REQUEST);
        worldChannel.setProtocol("REGULAR");
        worldChannel.setReplyWith("REPLY###");

        JsonObject request = new JsonObject();
        request.add("operation", content);
        worldChannel.setContent(request.toString());

        this.printSendMessage(worldChannel);
        this.agent.send(worldChannel);

        ACLMessage in = this.agent.blockingReceive();
        if (checkError(ACLMessage.INFORM, in)) {
            return "error";
        }

        worldChannel = in.createReply();
        this.printReceiveMessage(in);
        return "ok";
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
    /**
     * Nueva modifiación 19/12/2020
     *
     * @author Domingo
     * @param role
     * @param x
     * @param y
     * @param sensors
     * @return
     */
    public String loginWorld(String role, int x, int y, ArrayList<String> sensors) {
        worldChannel = message(agentName, worldManager, ACLMessage.REQUEST, "REGULAR");
        worldChannel.setConversationId(sessionId);

        /*ACLARAR LO DEL SETREPLYWITH*/
        worldChannel.setReplyWith("login" + role);

        JsonObject content = new JsonObject();
        //Hacemos un JsonArray con el sensor
        JsonArray sensorArray = new JsonArray();

        content.add("operation", "login");
        if (role.equals("rescuer")) {
            content.add("attach", sensorArray); //JsonArray vacío
        } else if (role.equals("seeker")) {
            sensorArray.add(sensors.get(0));
            content.add("attach", sensorArray); //JsonArray con el sensor
        }
        content.add("posx", x);
        content.add("posy", y);
        worldChannel.setContent(content.toString());

        this.printSendMessage(worldChannel);
        this.agent.send(worldChannel);

        MessageTemplate t = MessageTemplate.MatchInReplyTo("login" + role);
        ACLMessage in = this.agent.blockingReceive(t);

        if (checkError(ACLMessage.INFORM, in)) {
            return "error";
        }

        worldChannel = in.createReply();
        this.printReceiveMessage(in);
        return ok(in);
    }

    public String ok(ACLMessage in) {
        return Json.parse(in.getContent()).asObject().get("result").asString();
    }

    public boolean checkAPBError(ACLMessage in) {
        if (in.getPerformative() == ACLMessage.AGREE || in.getPerformative() == ACLMessage.INFORM || in.getPerformative() == ACLMessage.REFUSE) {
            return false;
        } else {
            return true;
        }
        /*
        for(int performative : this.acceptedPerformatives){
            if(!checkError(performative, in)){
                return false;
            }
        }
        return true;*/
    }

    /**
     * author: Domingo
     *
     * @return
     */
    public String getSessionID() {
        return this.sessionId;
    }

    /**
     * author: Domingo
     *
     * @return
     */
    public JsonObject readSensor() {
        //Creamos mensaje para leer sensores
        worldChannel.setPerformative(ACLMessage.QUERY_REF);
        worldChannel.setProtocol("REGULAR");

        JsonObject content = new JsonObject();
        content.add("operation", "read");
        this.worldChannel.setContent(content.toString());

        //Enviamos mensaje para leer sensores
        this.printSendMessage(worldChannel);
        this.agent.send(this.worldChannel);

        ACLMessage in = this.agent.blockingReceive();

        if (this.checkError(ACLMessage.INFORM, in)) {
            return null;
        }

        this.printReceiveMessage(in);

        JsonObject parsedResponse = Json.parse(in.getContent()).asObject();
        return parsedResponse;

    }

    /**
     * author: Domingo
     *
     * @return
     */
    public void sendGermanLocationToAPB(ArrayList<Integer> indicesAlemanes, ArrayList<JsonObject> alemanes) {

        for (int i = 0; i < indicesAlemanes.size(); i++) {
            APBChannel = message(agentName, APBName, ACLMessage.INFORM, "REGULAR");
            APBChannel.setReplyWith("aleman");

            JsonObject aleman = new JsonObject();
            aleman.set("aleman", alemanes.get(indicesAlemanes.get(i)));
            APBChannel.setContent(aleman.toString());
            
            this.printSendMessage(APBChannel);
            this.agent.send(APBChannel);
        }

    }

    /**
     * @author Jose Saldaña
     */
    public void informGermanFound(int x, int y) {
        APBChannel = message(agentName, APBName, ACLMessage.INFORM, "REGULAR");
        APBChannel.setReplyWith("aleman");

        JsonObject aleman = new JsonObject();
        aleman.set("x", x);
        aleman.set("y", y);

        APBChannel.setContent(aleman.toString());
        this.printSendMessage(APBChannel);
        this.agent.send(APBChannel);
    }
    
    public void sendFinishMsgToAPB() {
        APBChannel.setPerformative(ACLMessage.INFORM);
        APBChannel.setReplyWith("end");
        
        this.printSendMessage(APBChannel);
        this.agent.send(APBChannel);
    }
}
