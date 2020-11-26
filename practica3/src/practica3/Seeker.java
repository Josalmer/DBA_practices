package practica3;

import IntegratedAgent.IntegratedAgent;
import com.eclipsesource.json.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Arrays;

public class Seeker extends Drone {
    @Override
    public void plainExecute() {
        while (!_exitRequested) {
            Info("Current Status: " + this.status);
            switch (this.status) {
               
                case SUBSCRIBED_TO_PLATFORM:
                    this.getAPBAccountNumber();
                    this.checkingWorld("rescuer");
                    break;
                case SUBSCRIBED_TO_WORLD:
                    // Wait APB for instrucción and tickets for login
                    this.loginAPB();
                    this.loginWorld(this.knowledge.currentPositionX,this.knowledge.currentPositionY);
                    this.status = DroneStatus.RECHARGING;
                    break;
                case FREE:
                    this.receivePlan();
                    break;
                case NEED_RECHARGE:
                    this.claimRecharge();
                    if (this.rechargeTicket == null) {
                        this.status = DroneStatus.RECHARGING;
                    } else {
                        this.status = DroneStatus.FINISHED;
                    }
                    break;
                case RECHARGING:
                    this.recharge();
                    break;
                case BUSY:
                    this.executePlan();
                    break;
                    
                case FINISHED:
                    this.logout();
                    break;
            }
        }
    }

}
