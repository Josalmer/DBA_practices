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
import java.util.ArrayList;

/**
 *
 * @author Jose Saldaña, Manuel Pancorbo
 */
public class APBCommunicationAssistant extends CommunicationAssistant {
    ACLMessage bankChannel = new ACLMessage();
    ACLMessage shoppingChanel = new ACLMessage();
    
    ProductCatalogue shopsInfo = new ProductCatalogue();
    
    public APBCommunicationAssistant(IntegratedAgent _agent, String identityManager, PublicCardID cardId, String _world) {
        super(_agent, identityManager, cardId, _world);
    }

    /**
     * Crea una cuenta en el banco para el agente
     *
     * @author Jose Saldaña
     * @return nº de cuenta, formato: ACC#ejemplo, si algo sale mal devuelve
     * "error"
     */
    public String openBankAccount() {
        String service = "Bank";
        ArrayList<String> agents = new ArrayList(yp.queryProvidersofService(service));
        String serviceAgent = agents.get(0);

        System.out.println(this.agent.getLocalName() + " requesting open bank account to " + serviceAgent);
        bankChannel.setSender(this.agent.getAID());
        bankChannel.addReceiver(new AID(serviceAgent, AID.ISLOCALNAME));
        bankChannel.setPerformative(ACLMessage.REQUEST);
        bankChannel.setProtocol("REGULAR");

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
