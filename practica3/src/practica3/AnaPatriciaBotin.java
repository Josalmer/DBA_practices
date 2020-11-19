package practica3;

import IntegratedAgent.IntegratedAgent;
import com.eclipsesource.json.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Arrays;

public class AnaPatriciaBotin extends IntegratedAgent {

    // AGENT CONFIGURATION -------------------------------------------
    // END CONFIGURATION ---------------------------------------------

    CommunicationAssistant _communications;
    Administration adminData = new Administration();
    AgentStatus status;
    AgentKnowledge knowledge = new AgentKnowledge();

    @Override
    public void setup() {
        super.setup();

        this._communications = new CommunicationAssistant(this, _identitymanager, _myCardID);

        if (this._communications.chekingPlatform()) {
            this.status = AgentStatus.SUBSCRIBED_TO_PLATFORM;
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
                    this.createBankAccount();
                    break;
                case WITH_BANK_ACC:
                    // Esperar Query Ref subscribedToPlatform de los 4 agentes, contestarles con el
                    // nº de cuenta,
                    // cuando se haya contestado a los 4 (comprobar adminData.angentsSubscribed)
                    // Hacer subscribe al mundo indicando rol de Listener
                    break;
                case FINISHED:
                    this.logout();
                    break;
            }
        }
    }

    void createBankAccount() {
        this.adminData.bankAccountNumber = this._communications.openBankAccount();
        if (this.adminData.bankAccountNumber == "error") {
            this.status = AgentStatus.FINISHED;
        } else {
            this.status = AgentStatus.WITH_BANK_ACC;
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
