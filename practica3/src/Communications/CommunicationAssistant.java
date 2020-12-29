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
 * @author Jose Saldaña, Miguel García Tenorio, Manuel Pancorbo
 */
public class CommunicationAssistant {

    boolean printMessages;
    
    String serviceAgent = "Analytics group Banco Santander";
    Integer nDrones = 4;
    
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
    
    AID agentName;

    /**
     * Constructor del canal de comunicaciones de los agentes
     *
     * @param _agent
     * @param identityManager
     * @param cardId
     */
    public CommunicationAssistant(IntegratedAgent _agent, String identityManager, PublicCardID cardId) {
        this.agent = _agent;
        this.agentName = this.agent.getAID();
        this._identitymanager = identityManager;
        this._myCardID = cardId;
        this.printMessages = false;
    }

    public void setPrintMessages(boolean print){
        this.printMessages = print;
    }
    public Integer getDronesNumber() {
        return this.nDrones;
    }
    
    public ACLMessage message(AID sender, String receiver, int performative, String protocol){
        ACLMessage message = new ACLMessage();
        message.setSender(sender);
        message.addReceiver(new AID(receiver, AID.ISLOCALNAME));
        message.setPerformative(performative);
        message.setProtocol(protocol);
       
        return message;
    }
    
    /**
     * Registra al agente en la plataforma de agentes
     *
     * @author Jose Saldaña
     * @return boolean que indica si el registro ha sido exitoso
     */
    public boolean chekingPlatform() {
        identityManagerChannel = message(agentName, _identitymanager, ACLMessage.SUBSCRIBE, "ANALYTICS");
        identityManagerChannel.setReplyWith("subscribe" + this.agent.getLocalName());
        identityManagerChannel.setContent("");
        identityManagerChannel.setEncoding(_myCardID.getCardID());
        
        this.printSendMessage(identityManagerChannel);
        this.agent.send(identityManagerChannel);
        
        
        MessageTemplate t = MessageTemplate.MatchInReplyTo("subscribe" + this.agent.getLocalName());
        ACLMessage in = this.agent.blockingReceive(t);

        if (checkError(ACLMessage.INFORM, in)) {
            return false;
        }
                  
        this.printReceiveMessage(in);
       // identityManagerChannel = in.createReply(); 
        boolean validYP = this.getYellowPages();  
        return validYP;
    }
    
    
    
    public boolean getYellowPages(){
        identityManagerChannel = message(agentName, _identitymanager, ACLMessage.QUERY_REF, "ANALYTICS");
        identityManagerChannel.setReplyWith("yp" + this.agent.getLocalName());   
       
        this.printSendMessage(identityManagerChannel);
        this.agent.send(identityManagerChannel);
              
        MessageTemplate t = MessageTemplate.MatchInReplyTo("yp" + this.agent.getLocalName());
        ACLMessage in = this.agent.blockingReceive(t);
        this.printReceiveMessage(in);
          
        yp = new YellowPages();
        yp.updateYellowPages(in);
       
        if (this.agent.getLocalName().equals("Ana Patricia Botin")) { System.out.println("\n" + yp.prettyPrint());}
        
        ArrayList<String> agents = new ArrayList(yp.queryProvidersofService(serviceAgent));
        if (agents.isEmpty()) {
            System.out.println("The service " + serviceAgent + " is not provided by any running agent currently");
        } else {
            this.worldManager = agents.get(0); 
        }
        return !agents.isEmpty();
    }

    /**
     * Subscribe a un agente al WorldManager de un mundo
     *
     * @author Miguel García
     * @return resultado de la perticion
     */
    public boolean checkingRadio(String role) {
        worldChannel = message(agentName, worldManager,ACLMessage.SUBSCRIBE, "REGULAR");
        worldChannel.setReplyWith("radio");
        worldChannel.setConversationId(this.sessionId);
        
        JsonObject content = new JsonObject();
        content.add("type", role.toUpperCase());
        worldChannel.setContent(content.toString());
        
        this.printSendMessage(worldChannel);
        this.agent.send(worldChannel);
        
        MessageTemplate t = MessageTemplate.MatchInReplyTo("radio");
        ACLMessage in = this.agent.blockingReceive(t);
       
        if (checkError(ACLMessage.INFORM,in)) {
            return false;
        }
        
        this.printReceiveMessage(in);
        this.saveMoney(in, role);     
        return true;
    }

    /**
     * Cancela suscripción con el identity manager
     *
     * @author Jose Saldaña
     * @return boolean que indica si el registro ha sido exitoso
     */
    public void checkoutPlatform() {
        identityManagerChannel = message(agentName, _identitymanager, ACLMessage.CANCEL,"ANALYTICS");
        identityManagerChannel.setContent("");
        identityManagerChannel.setConversationId(this.sessionId);
         
        this.printSendMessage(identityManagerChannel);
        this.agent.send(identityManagerChannel);
    }

    /**
     * Cancela suscripción con el world manager
     *
     * @author Jose Saldaña
     * @return boolean que indica si el registro ha sido exitoso
     */
    public void checkoutWorld() {
        worldChannel = message(this.agent.getAID(), worldManager, ACLMessage.CANCEL,"ANALYTICS");
        worldChannel.setContent("");
        worldChannel.setConversationId(this.sessionId);
        
        this.printSendMessage(worldChannel);
        this.agent.send(worldChannel);
    }
    
    
    /** FUNCIONES APOYO **/
    public boolean checkError(int wantedPerformative, ACLMessage in){
        if(in.getPerformative() == wantedPerformative)
            return false;
        
        this.printErrorMessage(in);
        return true;
    }
    
    public void printSendMessage(ACLMessage in){
        if(this.printMessages){
            System.out.println("\033[32m \nMessage Im Sending \033[0m " + this.agent.getLocalName() + stringMessage(in));
        }
    }
    
    public void printReceiveMessage(ACLMessage in){
        if(this.printMessages){
            System.out.println("\033[35m \nMessage I Got\033[0m " + this.agent.getLocalName() + stringMessage(in)); 
        }
    }
    
     
    public void printErrorMessage(ACLMessage in){
        if(this.printMessages){
            System.out.println("\033[31m \nError Message \033[0m " + this.agent.getLocalName() + stringMessage(in));
        }  
    }
    
    private String stringMessage(ACLMessage in ){
        String _agentName = in.getSender().toString();
        String _otherName=""; 
        if(in.getAllReceiver().hasNext()) 
            _otherName = in.getAllReceiver().next().toString();
        String _performative = ACLMessage.getPerformative(in.getPerformative());
        String _inReplyTo ="";
        if(in.getInReplyTo() != null)
            _inReplyTo = in.getInReplyTo();
        return "\n-Sender: " + _agentName + "\n-Receiver: " + _otherName + "\n-Performative: " + _performative + "\n-Reply with: " + in.getReplyWith() + "\n-In Reply To: " + _inReplyTo + "\n-Content: " + in.getContent();
    }

    private void saveMoney(ACLMessage in, String role){
        if (!role.equals("LISTENER")) {
               String response = in.getContent();
               JsonObject parsedAnswer = Json.parse(response).asObject();
               this.bitcoins = parsedAnswer.asObject().get("coins").asArray(); 
        }
    }
    
    public void waitForFinish() {
        MessageTemplate t = MessageTemplate.MatchReplyWith("end");
        ACLMessage in = this.agent.blockingReceive(t);
    }

}
