package practica3;

import IntegratedAgent.IntegratedAgent;
import com.eclipsesource.json.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Arrays;

public class Seeker extends IntegratedAgent {

    // AGENT CONFIGURATION -------------------------------------------
    // END CONFIGURATION ---------------------------------------------

    String APBAccountNumber;

    CommunicationAssistant _communications;
    RescuerStatus status;
    AgentKnowledge knowledge = new AgentKnowledge();
    DronePerception perception = new DronePerception();

    @Override
    public void setup() {
        super.setup();

        this._communications = new CommunicationAssistant(this, _identitymanager, _myCardID);

        if (this._communications.chekingPlatform()) {
            this.status = RescuerStatus.SUBSCRIBED_TO_PLATFORM;
            _exitRequested = false;
        } else {
            System.out.println(this.getLocalName() + " failed subscribing to" + _identitymanager + " and DIE");
            _exitRequested = true;
        }
    }

    @Override
    public void plainExecute() {
        while (!_exitRequested) {
            Info("Current Status: " + this.status);
            switch (this.status) {
                case SUBSCRIBED_TO_PLATFORM:
                    this.getAPBAccountNumber();
                    break;
                case SUBSCRIBED_TO_WORLD:
                    // Wait APB for instrucción and tickets for login
                    this.login();
                    break;
                case FINISHED:
                    this.logout();
                    break;
            }
        }
    }

    void getAPBAccountNumber() {
        APBAccountNumber = this._communications.queryRefAPB("subscribedToPlatform");
        if (APBAccountNumber == "error") {
            this.status = RescuerStatus.FINISHED;
        } else {
            // Suscribirse al mundo
            this.status = RescuerStatus.SUBSCRIBED_TO_WORLD;
        }
    }

    void login() {

    }

    void logout() {
        _exitRequested = true;
    }

    @Override
    public void takeDown() {
        super.takeDown();
    }

}
