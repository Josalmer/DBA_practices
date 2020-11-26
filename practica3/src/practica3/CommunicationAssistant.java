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

/*
    LEER ESTO:
        + revisar lo del mundo, hay que ver como se le dice al agente a que mundo
        se conecta.
        + El reply with no lo tengo muy claro lo que tiene que ir dentro
        + Creo que se podria modularizar mas lo de mandar los mensajes.
        + Combiar los estados para que haya una lista de estados para el dron 
            ej. DroneStatus
        + Revisar como lo vamos a hacer para cuando un drone se mueva y tenga que 
            comprobar si hay o no otro dron en la siguiente casilla.
            Yo lo he hecho que antes de moverse pregunte a APB si se hay otro drone
            Pero no se si sera lo más eficiente
*/
        

/**
 *
 * @author Jose Saldaña
 */
public class CommunicationAssistant {

    ACLMessage identityManagerChannel = new ACLMessage(); // Todos
    ACLMessage bankChannel = new ACLMessage(); // Solo APB
    ACLMessage APBChannel = new ACLMessage(); // Solo Drones
    ACLMessage outChannel = new ACLMessage();
    

    String bankAccountNumber;

    IntegratedAgent agent;
    String _identitymanager;
    String world;
    PublicCardID _myCardID;
    YellowPages yp;
    String converIDWorld;

    /**
     * Constructor del canal de comunicaciones de los agentes
     * 
     * @param _agent
     * @param identityManager
     * @param cardId
     */
    public CommunicationAssistant(IntegratedAgent _agent, String identityManager, PublicCardID cardId ) {
        this.agent = _agent;
        this._identitymanager = identityManager;
        this._myCardID = cardId;
        this.world = ""; //ESTO HAY QUE VER COMO LO HACEMOS
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
    
   public JsonObject requestAPBPlan(String content){
        System.out.println(this.agent.getLocalName() + " REQUEST to Ana Patricia Botin: " + content);
        APBChannel.setSender(this.agent.getAID());
        APBChannel.addReceiver(new AID("Ana Patricia Botin", AID.ISLOCALNAME));
        APBChannel.setPerformative(ACLMessage.REQUEST);
        APBChannel.setContent(content);
        this.agent.send(APBChannel);
        ACLMessage in = this.agent.blockingReceive();
        System.out.println(this.agent.getLocalName() + " sent REQUEST to " + "Ana Patricia Botin" + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.INFORM) {
            APBChannel = in.createReply();
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            System.out.println(this.agent.getLocalName() + " received APB mision: "+ parsedAnswer.get("mission").asString());
            return parsedAnswer;
        } else {
            System.out.println(
                    this.agent.getLocalName() + " get ERROR while REQUEST to " + "Ana Patricia Botin: " + content);
            return null;
        }
   }
   
   public String queryAccount(String content){
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
   

   //ESTO HAY QUE REVISARLO
   public Boolean queryAPBMove(String content){
       System.out.println(this.agent.getLocalName() + " QUERY_REF to Ana Patricia Botin: " + content);
        APBChannel.setSender(this.agent.getAID());
        APBChannel.addReceiver(new AID("Ana Patricia Botin", AID.ISLOCALNAME));
        APBChannel.setPerformative(ACLMessage.QUERY_REF);
        APBChannel.setContent(content);
        this.agent.send(APBChannel);
        ACLMessage in = this.agent.blockingReceive();
        System.out.println(this.agent.getLocalName() + " sent QUERY_REF to " + "Ana Patricia Botin" + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.INFORM) {
            APBChannel = in.createReply();
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            Boolean canMove = parsedAnswer.asObject().get("move").asBoolean();
            System.out.println(this.agent.getLocalName() + " received APB permission to move to: " + canMove);
            return canMove;
        } else {
            System.out.println(
                    this.agent.getLocalName() + " get ERROR while QUERY_REF to " + "Ana Patricia Botin: " + content);
            return null;
        }
   }
   
   /**
     * Un  dron pide ticket de recarga a APB 
     * 
     * @author Miguel García
     * @return resultado de la perticion
     */
   public String requestAPBRecharge(String content){
        System.out.println(this.agent.getLocalName() + " REQUEST to Ana Patricia Botin: " + content);
        APBChannel.setSender(this.agent.getAID());
        APBChannel.addReceiver(new AID("Ana Patricia Botin", AID.ISLOCALNAME));
        APBChannel.setPerformative(ACLMessage.REQUEST);
        APBChannel.setContent(content);
        this.agent.send(APBChannel);
        ACLMessage in = this.agent.blockingReceive();
        System.out.println(this.agent.getLocalName() + " sent REQUEST to " + "Ana Patricia Botin" + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.AGREE) {
            APBChannel = in.createReply();
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            String ticket = parsedAnswer.get("rechargeTicket").asString();
            System.out.println(this.agent.getLocalName() + " received APB recharge ticket: " + parsedAnswer.asString());
            return ticket;
        } else if(in.getPerformative() == ACLMessage.REFUSE){
            System.out.println(
                    this.agent.getLocalName() + " get REFUSE while REQUEST to " + "Ana Patricia Botin: " + content);
            return  "refuse";
        }else{
             System.out.println(
                    this.agent.getLocalName() + " get ERROR while REQUEST to " + "Ana Patricia Botin: " + content);
            return  "error";
        }
   }
    /**
     * Un  dron pide recargar al WorldManager de un mundo
     * 
     * @author Miguel García
     * @return resultado de la perticion
     */
   public String requestRecharge(String ticket){
        System.out.println(this.agent.getLocalName() + " REQUEST recharge with ticket: " + ticket);
        outChannel.setSender(this.agent.getAID());
        outChannel.addReceiver(new AID(this.world, AID.ISLOCALNAME));
        outChannel.setPerformative(ACLMessage.REQUEST);
        outChannel.setProtocol("ANALYTICS");
        outChannel.setEncoding(this._myCardID.getCardID());
        outChannel.setConversationId(this.converIDWorld);
        outChannel.setReplyWith("REPLY###"); 
        JsonObject content  = new JsonObject();
        content.add("operation", "recharge");
        content.add("recharge", ticket);
      
        outChannel.setContent(content.asString());
        this.agent.send(outChannel);
        ACLMessage in = this.agent.blockingReceive();
        System.out.println(this.agent.getLocalName() + " sent recharge to " + "WorldManager " + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.CONFIRM) {
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            String result = parsedAnswer.asObject().get("result").asString();
            System.out.println(this.agent.getLocalName() + " was  recharged");
            return result;
        } else {
            System.out.println(
                    this.agent.getLocalName() + " get ERROR while REQUEST to " + "WorldManager: " + world);
            return "error";
        }
   }
    /**
     * Un drone manda un movimiento al worldManager
     * 
     * @author Miguel García
     * @return resultado de la perticion
     */

    String sendActionWorldManager(String content) {
        System.out.println(this.agent.getLocalName() + " send action to WorldManager: " + content);
        outChannel.setSender(this.agent.getAID());
        outChannel.addReceiver(new AID(this.world, AID.ISLOCALNAME));
        outChannel.setPerformative(ACLMessage.REQUEST);
        outChannel.setProtocol("ANALYTICS");
        outChannel.setEncoding(this._myCardID.getCardID());
        outChannel.setConversationId(this.converIDWorld);
        outChannel.setReplyWith("REPLY###");
        JsonObject request  = new JsonObject();
        request.add("operation", content);
       
       
        outChannel.setContent(request.asString());
        this.agent.send(outChannel);
        ACLMessage in = this.agent.blockingReceive();
        System.out.println(this.agent.getLocalName() + " sent action to " + "WorldManager " + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.CONFIRM) {
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            String result = parsedAnswer.asObject().get("result").asString();
            System.out.println(this.agent.getLocalName() + " do action: " + content);
            return result;
        } else {
            System.out.println(
                    this.agent.getLocalName() + " get ERROR while REQUEST to " + "WorldManager: " + world);
            return "error";
        }
    }
    
    /**
     * Subscribe a un agente al WorldManager de un mundo
     * 
     * @author Miguel García
     * @return resultado de la perticion
     */
   public String checkingWorld(String account , String role){
        System.out.println(this.agent.getLocalName() + " SUBSCRIBE to WorldManager: " + role);
        outChannel.setSender(this.agent.getAID());
        outChannel.addReceiver(new AID(this.world, AID.ISLOCALNAME));
        outChannel.setPerformative(ACLMessage.SUBSCRIBE);
        outChannel.setProtocol("ANALYTICS");
        outChannel.setEncoding(this._myCardID.getCardID());
        outChannel.setConversationId("");
        outChannel.setReplyWith("REPLY###");
        JsonObject content = new JsonObject();
        content.add("type", role.toUpperCase());
        content.add("account", account);
        
        outChannel.setContent(content.asString());
        this.agent.send(outChannel);
        ACLMessage in = this.agent.blockingReceive();
        System.out.println(this.agent.getLocalName() + " sent SUBSCRIBE to " + "WorldManager " + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.CONFIRM) {
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            String result = parsedAnswer.asObject().get("result").asString();
            this.converIDWorld =  in.getConversationId();
            System.out.println(this.agent.getLocalName() + " was  subscribed to: " + world);
            return result;
        } else {
            System.out.println(
                    this.agent.getLocalName() + " get ERROR while SUBSCRIBE to " + "WorldManager: " + world);
            return "error";
        }
   }
    /**
     * Le pide a APB los datos correspondientes para poder moverse
     * 
     * @author Miguel García
     * @return json con el mapa , las coordenadas inicales y el ticket de recarga
     */
   public JsonObject queryLogin(String content){
        System.out.println(this.agent.getLocalName() + " QUERY_REF to Ana Patricia Botin: " + content);
        APBChannel.setSender(this.agent.getAID());
        APBChannel.addReceiver(new AID("Ana Patricia Botin", AID.ISLOCALNAME));
        APBChannel.setPerformative(ACLMessage.QUERY_REF);
        APBChannel.setContent(content);
        this.agent.send(APBChannel);
        ACLMessage in = this.agent.blockingReceive();
        System.out.println(this.agent.getLocalName() + " sent QUERY_REF to " + "Ana Patricia Botin" + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.INFORM) {
            APBChannel = in.createReply();
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            System.out.println(this.agent.getLocalName() + " received APB map data: " + parsedAnswer.asString());
            return parsedAnswer;
        } else {
            System.out.println(
                    this.agent.getLocalName() + " get ERROR while QUERY_REF to " + "Ana Patricia Botin: " + content);
            return  null;
        }
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
   public String requestLoginWorldManager(String role,int x ,int y, ArrayList<String> sensors ){
        System.out.println(this.agent.getLocalName() + " login to WorldManager: " + role);
        outChannel.setSender(this.agent.getAID());
        outChannel.addReceiver(new AID(this.world, AID.ISLOCALNAME));
        outChannel.setPerformative(ACLMessage.REQUEST);
        outChannel.setProtocol("ANALYTICS");
        outChannel.setEncoding(this._myCardID.getCardID());
        outChannel.setConversationId(this.converIDWorld);
        outChannel.setReplyWith("REPLY###");
        JsonObject content  = new JsonObject();
        content.add("operation", "login");
        if(role.equals("rescuer")){
            content.add("attach", "[]");
        }else{
            //SE AÑADEN SENSORES SEEKER
        }
        
        content.add("posx", x);
        content.add("posy", y);
   
        outChannel.setContent(content.asString());
        this.agent.send(outChannel);
        ACLMessage in = this.agent.blockingReceive();
        System.out.println(this.agent.getLocalName() + " sent Login to " + "WorldManager " + " and get: " + in.getPerformative(in.getPerformative()));
        if (in.getPerformative() == ACLMessage.CONFIRM) {
            String response = in.getContent();
            JsonObject parsedAnswer = Json.parse(response).asObject();
            String result = parsedAnswer.asObject().get("result").asString();
            System.out.println(this.agent.getLocalName() + " was  Logged to: " + world);
            return result;
        } else {
            System.out.println(
                    this.agent.getLocalName() + " get ERROR while REQUEST to " + "WorldManager: " + world);
            return "error";
        }
   }
   
}
