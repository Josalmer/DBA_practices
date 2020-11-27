package practica3;

import IntegratedAgent.IntegratedAgent;
import com.eclipsesource.json.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Arrays;

public class AnaPatriciaBotin extends IntegratedAgent {

    APBCommunicationAssistant _communications;
    Administration adminData = new Administration();
    AgentStatus status;
    AgentKnowledge knowledge = new AgentKnowledge();
    Perception jsonParser = new Perception();

    @Override
    public void setup() {
        super.setup();

        this._communications = new APBCommunicationAssistant(this, "Sphinx", _myCardID);

        if (this._communications.chekingPlatform()) {
            this.status = AgentStatus.SUBSCRIBED_TO_PLATFORM;
            _exitRequested = false;
        } else {
            System.out.println(this.getLocalName() + " failed subscribing to" + "Sphinx" + " and DIE");
            _exitRequested = true;
        }
    }

    @Override
    public void plainExecute() {
        while (!_exitRequested) {
            Info("Current Status: " + this.status);
            switch (this.status) {
                case SUBSCRIBED_TO_PLATFORM:
                    this.checkingWorld();
                    break;
                case SUBSCRIBED_TO_WORLD:
                    this.shareSessionIdAndMap();
                    this.checkingRadio();
                    break;
                case SUBSCRIBED_TO_RADIO:
                    this.collectMoney();
                    this.investigateMarket();
                    break;
                case SHOPPING:
                    this.initialShopping();
                    break;
                case FINISHED_SHOPPING:

                    break;
                case RESCUEING:
                    
                    break;
                case FINISHED:
                    this.logout();
                    break;
            }
        }
    }
    
    void checkingWorld() {
        JsonObject response = this._communications.checkingWorld();
        if (response == null) {
            this.status = AgentStatus.FINISHED;
        } else {
            this.adminData.map = this.jsonParser.convertToIntegerMatrix(response.get("map").asArray()) ;
            this.status = AgentStatus.SUBSCRIBED_TO_WORLD;
        }
    }

    /**
     * @author Jose Saldaña, Manuel Pancorbo
     */
    void shareSessionIdAndMap() {
        while (this.adminData.angentsSubscribed < 4) {
            this._communications.listenAndShareSessionId(this.adminData.map);
            this.adminData.angentsSubscribed++;
        }
    }

    /**
     * @author Jose Saldaña, Manuel Pancorbo
     */
    void checkingRadio() {
        boolean logedIn = this._communications.checkingRadio("LISTENER");
        if (logedIn) {
            this.status = AgentStatus.SUBSCRIBED_TO_WORLD;
        } else {
            this.status = AgentStatus.FINISHED;
        }
    }
    
    void collectMoney() {
        while (this.adminData.collectedMoney < 4) {
            this.adminData.bitcoins.addAll(this._communications.listenAndCollectMoney());
            this.adminData.collectedMoney++;
        }
    }

    /**
     * @author Jose Saldaña, Manuel Pancorbo
     */
    void investigateMarket() {
        this._communications.askShoppingCenters();
        this.status = AgentStatus.SHOPPING;
    }

    /**
     * @author Jose Saldaña, Manuel Pancorbo
     */
    void initialShopping() {
        this.adminData.sensor1 = this.buy("THERMALDLX");
        this.adminData.sensor2 = this.buy("THERMALHQ");
//        this.adminData.map = this.buy("MAP");
        for (int i = 0; i < 4; i++) {
            this.adminData.rechargeTickets.add(this.buy("CHARGE"));
        }

        if (this.status != AgentStatus.FINISHED) {
            this.status = AgentStatus.FINISHED_SHOPPING; 
        }
    }
    
    String buy(String SensorName) {
        String sensorCode = this._communications.buy(SensorName);
        if (sensorCode == null) {
            this.status = AgentStatus.FINISHED;
            return null;
        } else {
            return sensorCode;
        }
    }

    void logout() {
        _exitRequested = true;
    }

    @Override
    public void takeDown() {
        super.takeDown();
    }

}
