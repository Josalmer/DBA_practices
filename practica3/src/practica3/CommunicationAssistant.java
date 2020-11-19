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
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import com.eclipsesource.json.JsonObject;

/**
 *
 * @author Jose Saldaña
 */
public class CommunicationAssistant {

    ACLMessage identityManagerChannel = new ACLMessage(); // Todos
    ACLMessage bankChannel = new ACLMessage(); // Solo APB
    ACLMessage APBChannel = new ACLMessage(); // Solo Drones

    String bankAccountNumber;

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
        identityManagerChannel.setContent("");
        identityManagerChannel.setEncoding(_myCardID.getCardID());
        identityManagerChannel.setPerformative(ACLMessage.SUBSCRIBE);
        this.agent.send(identityManagerChannel);
        ACLMessage in = this.agent.blockingReceive();
        System.out.println(this.agent.getLocalName() + " sent SUBSCRIBE to " + _identitymanager + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.CONFIRM || in.getPerformative() == ACLMessage.INFORM) {
            System.out.println(this.agent.getLocalName() + ": Chekin confirmed in the platform");
            // Get YellowPages
            identityManagerChannel = in.createReply();
            identityManagerChannel.setPerformative(ACLMessage.QUERY_REF);
            this.agent.send(identityManagerChannel);
            in = this.agent.blockingReceive();
            yp = new YellowPages();
            yp.updateYellowPages(in);
            if (this.agent.getLocalName() == "Ana Patricia Botin") {
                System.out.println("\n" + yp.prettyPrint());
            }
            return true;
        }
        return false;
    }

    /**
     * Crea una cuenta en el banco para el agente
     * 
     * @author Jose Saldaña
     * @return nº de cuenta, formato: ACC#ejemplo, si algo sale mal devuelve "error"
     */
    public String openBankAccount() {
        String service = "Bank";
        ArrayList<String> agents = new ArrayList(yp.queryProvidersofService(service));
        String serviceAgent = agents.get(0);

        System.out.println(this.agent.getLocalName() + " requesting open bank account to " + serviceAgent);
        bankChannel.setSender(this.agent.getAID());
        bankChannel.addReceiver(new AID(serviceAgent, AID.ISLOCALNAME));
        bankChannel.setPerformative(ACLMessage.REQUEST);

        // Set content
        JsonObject params = new JsonObject();
        params.add("operation", "open");
        params.add("reference", "ACC##");
        String parsedParams = params.toString();
        bankChannel.setContent(parsedParams);

        this.agent.send(bankChannel);
        ACLMessage in = this.agent.blockingReceive();
        System.out.println(this.agent.getLocalName() + " sent REQUEST to " + serviceAgent + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.CONFIRM || in.getPerformative() == ACLMessage.INFORM) {
            bankChannel = in.createReply();
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            bankAccountNumber = parsedAnswer.asObject().get("details").asString();
            System.out.println(this.agent.getLocalName() + " created bank account with id: " + bankAccountNumber);
            return bankAccountNumber;
        } else {
            System.out
                    .println(this.agent.getLocalName() + " get ERROR while creating bank account with " + serviceAgent);
            return "error";
        }
    }

    /**
     * Manda un mensaje de QUERY_REF a Ana Patricia Botin
     * 
     * @param content
     * @return ACLMessage de respuesta
     */
    public String queryRefAPB(String content) {
        System.out.println(this.agent.getLocalName() + " QUERY_REF to Ana Patricia Botin: " + content);
        APBChannel.setSender(this.agent.getAID());
        APBChannel.addReceiver(new AID("Ana Patricia Botin", AID.ISLOCALNAME));
        APBChannel.setPerformative(ACLMessage.QUERY_REF);
        APBChannel.setContent(content);
        this.agent.send(bankChannel);
        ACLMessage in = this.agent.blockingReceive();
        System.out.println(this.agent.getLocalName() + " sent QUERY_REF to " + "Ana Patricia Botin" + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.INFORM) {
            APBChannel = in.createReply();
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            bankAccountNumber = parsedAnswer.asObject().get("account").asString();
            System.out.println(this.agent.getLocalName() + " received APB account number: " + bankAccountNumber);
            return bankAccountNumber;
        } else {
            System.out.println(
                    this.agent.getLocalName() + " get ERROR while QUERY_REF to " + "Ana Patricia Botin: " + content);
            return "error";
        }
    }

}
