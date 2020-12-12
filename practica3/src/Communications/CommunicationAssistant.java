/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communications;

import IntegratedAgent.IntegratedAgent;
import PublicKeys.PublicCardID;
import YellowPages.YellowPages;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Arrays;
import com.eclipsesource.json.JsonObject;
import jade.lang.acl.MessageTemplate;

/*
    LEER ESTO:
        + revisar lo del mundo, hay que ver como se le dice al agente a que mundo
        se conecta.
        + El reply with no lo tengo muy claro lo que tiene que ir dentro
        + Revisar como lo vamos a hacer para cuando un drone se mueva y tenga que 
            comprobar si hay o no otro dron en la siguiente casilla.
            Yo lo he hecho que antes de moverse pregunte a APB si se hay otro drone
            Pero no se si sera lo más eficiente
 */

 /*
De Jose para Migue:
1. recuerStatus ahora se llama drone Status y se ha borrado seekerStatus
2. outChannel ahora se llama worldChannel
 */
/**
 *
 * @author Jose Saldaña, Miguel García Tenorio
 */
public class CommunicationAssistant {

    ACLMessage identityManagerChannel = new ACLMessage(); // Todos
    ACLMessage worldChannel = new ACLMessage(); // Todos
    ACLMessage APBChannel = new ACLMessage(); // Solo Drones
    
    ArrayList<Integer> acceptedPerformative = new ArrayList<Integer>(Arrays.asList(ACLMessage.AGREE, ACLMessage.INFORM, ACLMessage.REFUSE)); // Posibles respuestas de APB a drones

    String bankAccountNumber;
    String worldManager;
    String sessionId;
    
    JsonArray bitcoins;

    IntegratedAgent agent;
    String _identitymanager;
    PublicCardID _myCardID;
    YellowPages yp;

    /**
     * Constructor del canal de comunicaciones de los agentes
     *
     * @param _agent
     * @param identityManager
     * @param cardId
     */
    public CommunicationAssistant(IntegratedAgent _agent, String identityManager, PublicCardID cardId) {
        this.agent = _agent;
        this._identitymanager = identityManager;
        this._myCardID = cardId;
    }

    /**
     * Registra al agente en la plataforma de agentes
     *
     * @author Jose Saldaña
     * @return boolean que indica si el registro ha sido exitoso
     */
    public boolean chekingPlatform() {
        System.out.println(this.agent.getLocalName() + " requesting checkin to " + _identitymanager);
        identityManagerChannel.setSender(this.agent.getAID());
        identityManagerChannel.addReceiver(new AID(_identitymanager, AID.ISLOCALNAME));
        identityManagerChannel.setProtocol("ANALYTICS");
        identityManagerChannel.setReplyWith("subscribe" + this.agent.getLocalName());
        identityManagerChannel.setContent("");
        identityManagerChannel.setEncoding(_myCardID.getCardID());
        identityManagerChannel.setPerformative(ACLMessage.SUBSCRIBE);
        this.agent.send(identityManagerChannel);
        MessageTemplate t = MessageTemplate.MatchInReplyTo("subscribe" + this.agent.getLocalName());
        ACLMessage in = this.agent.blockingReceive(t);
        System.out.println(this.agent.getLocalName() + " sent SUBSCRIBE to " + _identitymanager + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.INFORM) {
            System.out.println(this.agent.getLocalName() + ": Chekin confirmed in the platform");
            // Get YellowPages
            identityManagerChannel = in.createReply();
            identityManagerChannel.setPerformative(ACLMessage.QUERY_REF);
            identityManagerChannel.setReplyWith("yp" + this.agent.getLocalName());
            this.agent.send(identityManagerChannel);
            t = MessageTemplate.MatchInReplyTo("yp" + this.agent.getLocalName());
            in = this.agent.blockingReceive(t);
            yp = new YellowPages();
            yp.updateYellowPages(in);
            System.out.println(this.agent.getLocalName() + " request Yellow Pages to " + _identitymanager + " and get: " + in.getPerformative(in.getPerformative()));
            if (this.agent.getLocalName().equals("Ana Patricia Botin")) {
                System.out.println("\n" + yp.prettyPrint());
            }
            String service = "Analytics group Banco Santander";
            ArrayList<String> agents = new ArrayList(yp.queryProvidersofService(service));
            if (agents.isEmpty()) {
                System.out.println("The service " + service + " is not provided by any running agent currently");
                return false;
            } else {
                this.worldManager = agents.get(0);
                return true;
            }
        }
        return false;
    }

    /**
     * Subscribe a un agente al WorldManager de un mundo
     *
     * @author Miguel García
     * @return resultado de la perticion
     */
    public boolean checkingRadio(String role) {
        System.out.println(this.agent.getLocalName() + " SUBSCRIBE to World as " + role);
        worldChannel.setSender(this.agent.getAID());
        worldChannel.addReceiver(new AID(this.worldManager, AID.ISLOCALNAME));
        worldChannel.setPerformative(ACLMessage.SUBSCRIBE);
        worldChannel.setProtocol("REGULAR");
        worldChannel.setReplyWith("radio");
        worldChannel.setConversationId(this.sessionId);
        JsonObject content = new JsonObject();
        content.add("type", role.toUpperCase());
        worldChannel.setContent(content.asString());
        this.agent.send(worldChannel);
        ACLMessage in = this.agent.blockingReceive();
        System.out.println(this.agent.getLocalName() + " sent SUBSCRIBE to " + this.worldManager + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.INFORM) {
            worldChannel = in.createReply();
            if (!role.equals("LISTENER")) {
                String response = in.getContent();
                JsonObject parsedAnswer = Json.parse(response).asObject();
                this.bitcoins = parsedAnswer.asObject().get("coins").asArray();
            }
            return true;
        } else {
            System.out.println(this.agent.getLocalName() + " get ERROR while SUBSCRIBE to " + this.worldManager);
            return false;
        }
    }

    /**
     * Cancela suscripción con el identity manager
     *
     * @author Jose Saldaña
     * @return boolean que indica si el registro ha sido exitoso
     */
    public void checkoutPlatform() {
        System.out.println(this.agent.getLocalName() + " cancelling subscription with " + _identitymanager);
        identityManagerChannel.setSender(this.agent.getAID());
        identityManagerChannel.addReceiver(new AID(_identitymanager, AID.ISLOCALNAME));
        identityManagerChannel.setProtocol("ANALYTICS");
        identityManagerChannel.setContent("");
        identityManagerChannel.setPerformative(ACLMessage.CANCEL);
        this.agent.send(identityManagerChannel);
    }

    /**
     * Cancela suscripción con el world manager
     *
     * @author Jose Saldaña
     * @return boolean que indica si el registro ha sido exitoso
     */
    public void checkoutWorld() {
        System.out.println(this.agent.getLocalName() + " cancelling subscription with " + this.worldManager);
        worldChannel.setSender(this.agent.getAID());
        worldChannel.addReceiver(new AID(this.worldManager, AID.ISLOCALNAME));
        worldChannel.setContent("");
        worldChannel.setPerformative(ACLMessage.CANCEL);
        this.agent.send(worldChannel);
    }
   
}
