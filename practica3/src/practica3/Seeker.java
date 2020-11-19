package practica3;

import IntegratedAgent.IntegratedAgent;
import com.eclipsesource.json.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Arrays;

public class Seeker extends IntegratedAgent {
    
    // AGENT CONFIGURATION  -------------------------------------------
    // END CONFIGURATION  ---------------------------------------------
    
    ACLMessage outChannel = new ACLMessage(); // 1 para cada agente con el que se comunique

    SeekerStatus status;
    AgentKnowledge knowledge = new AgentKnowledge();
    DronePerception perception = new DronePerception();

    @Override
    public void setup() {
        super.setup();
        this.status = SeekerStatus.FINISHED;
        _exitRequested = false;
    }

    @Override
    public void plainExecute() {
        while (!_exitRequested) {
            Info("Current Status: " + this.status);
            switch (this.status) {
                case FINISHED:
                    this.logout();
                    break;
            }
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
