/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APB;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 *
 * @author Jose Saldaña
 */
public class Administration {

    int agentsSubscribed;
    int collectedMoney;

    ArrayList<String> bitcoins = new ArrayList();

    // Sensores
    String sensor1;
    String sensor2;

    // Info del mundo
    ArrayList<ArrayList<Integer>> map = new ArrayList();
    Integer maxFlight = 256;
    Coordinates initialPosition1;
    Coordinates initialPosition2;
    Coordinates initialPosition3;
    Coordinates initialPosition4;
    
    ArrayList<Coordinates> alemanes = new ArrayList();
    int rescued;
    boolean rescuerIddle;
    Coordinates rescuer1Position;
    Coordinates rescuer2Position;

    // Ticket de recarga
    Queue<String> rechargeTickets = new PriorityQueue();

    public Administration() {
        agentsSubscribed = 0;
        collectedMoney = 0;
        rescued = 0;
        rescuerIddle = false;
    }

    public ArrayList<String> getMoney(int coins) {
        if (coins > this.bitcoins.size()) {
            return null;
        }

        ArrayList<String> payment = new ArrayList<>();
        for (int i = 0; i < coins; i++) {
            payment.add(this.bitcoins.get(i));
        }

        return payment;
    }

    //Coins may have been used
    public void updateWastedMoney(int coins) {
        for (int i = 0; i < coins; i++) {
            this.bitcoins.remove(0);
        }
    }

    public String popRechargeTicket() {
        return this.rechargeTickets.poll();
    }
    
    public Coordinates rescueAleman(){
        Coordinates aleman = this.alemanes.get(0);
        this.alemanes.remove(0);
        this.rescued++;
        return aleman;
    }
    
}
