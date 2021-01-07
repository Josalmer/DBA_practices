/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communications;

import IntegratedAgent.IntegratedAgent;
import MapOption.Coordinates;
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
     * @author Jose Saldaña, Manuel Pancorbo, Domingo Lopez, Miguel García
     * @param performative Perfomartiva del mensaje a enviar
     * @param content Contenido del mensaje a enviar
     * @param key (opcional) Clave del mensaje a enviar
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
     * @param performative Perfomartiva del mensaje a enviar
     * @param content Contenido del mensaje a enviar
     * @param key (opcional) Clave del mensaje a enviar
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
     * Espera un mensaje de APB
     *
     * @author Jose Saldaña, Manuel Pancorbo
     * @param key, (opcional) clave del mensjae que se espera
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
     * @author Miguel García, Domingo Lopez
     * @param ticket Ticket para recargar
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
     * @author Miguel García, Domingo Lopez
     * @param content operación
     * @return resultado de la perticion
     */
    public String sendActionWorldManager(String content) {
        worldChannel.setSender(agentName);
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
     * El drone se logea en el mundo con su posición y sensores
     *
     * @author Domingo Lopez, Miguel García
     * @param role Role del drone
     * @param x Posición x del drone
     * @param y Posición y del drone
     * @param sensors Array de sensores del
     * @return resultado de la perticion
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
    }

    public String getSessionID() {
        return this.sessionId;
    }

    /**
     * Lectura de sensores
     *
     * @author Domingo Lopez
     * @return JsonObject con la percepción
     */
    public JsonObject readSensorMessage() {
        //Creamos mensaje para leer sensores
        worldChannel.setSender(agentName);
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
     * Informa de que se ha encontrado un aleman
     *
     * @author Jose Saldaña, Domingo Lopez
     * @param x coordenada x del aleman
     * @param y coordenada y del aleman
     * @return booleano que indica si se continua buscando alemanes
     */
    public boolean informGermanFound(int x, int y) {
        APBChannel = message(agentName, APBName, ACLMessage.QUERY_REF, "REGULAR");
        APBChannel.setReplyWith("aleman");

        JsonObject aleman = new JsonObject();
        aleman.set("x", x);
        aleman.set("y", y);

        APBChannel.setContent(aleman.toString());
        this.printSendMessage(APBChannel);
        this.agent.send(APBChannel);

        MessageTemplate t = MessageTemplate.MatchInReplyTo("aleman");
        ACLMessage in = this.agent.blockingReceive(t);

        if (this.checkError(ACLMessage.INFORM, in)) {
            return false;
        }

        this.printReceiveMessage(in);

        String mission = Json.parse(in.getContent()).asObject().get("mission").asString();
        return mission.equals("continue");
    }

    /**
     * Informa que se ha recogido el último aleman
     *
     * @author Jose Saldaña
     */
    public void sendFinishMsgToAPB() {
        APBChannel.setPerformative(ACLMessage.INFORM);
        APBChannel.setSender(agentName);
        APBChannel.setReplyWith("end");

        this.printSendMessage(APBChannel);
        this.agent.send(APBChannel);
    }

    /**
     * Comprueba si la posición a la que se quiere mover el drone esta libre o
     * no
     *
     * @author Jose Saldaña, Domingo Lopez, Miguel García
     * @param newPosition Coordenadas de posición objetivo (x, y)
     * @param z Altura objetivo
     * @return booleano que indica si esta libre o no
     */
    public boolean checkIfFree(Coordinates newPosition, int z) {
        boolean free = true;
        // Pendiente de desarrollar el checkeo de posición en radio
//        ACLMessage in = this.agent.blockingReceive();
//
//        if (checkError(ACLMessage.INFORM, in)) {
//            return false;
//        }
//        this.printReceiveMessage(in);

        return free;
    }
}
