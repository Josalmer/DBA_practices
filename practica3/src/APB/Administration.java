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

    int angentsSubscribed;
    int collectedMoney;
    
    ArrayList<String> bitcoins = new ArrayList();
    
    // Sensores
    String sensor1;
    String sensor2;
    
    
    // Info del mundo
    ArrayList<ArrayList<Integer> > map = new ArrayList();
    Integer maxFlight = 256;
    Integer initialPosition1;
    Integer initialPosition2;
    Integer initialPosition3;
    Integer initialPosition4;
    
    // Ticket de recarga
    Queue<String> rechargeTickets = new PriorityQueue();

    public Administration() {
        angentsSubscribed = 0;
        collectedMoney = 0;
    }
    
    public ArrayList<String> getMoney(int coins){
        if(coins > this.bitcoins.size())
            return null;
        
        ArrayList<String> payment = new ArrayList<>();
        for(int i=0; i<coins; i++){
            payment.add(this.bitcoins.get(i));
        }
        
        return payment;
    }
    
    
    //Coins may have been used
    public void updateWastedMoney(int coins){
        for(int i=0; i<coins; i++){
            this.bitcoins.remove(0);
        }
    }

    public String popRechargeTicket() {
        return this.rechargeTickets.poll();
    }
}
