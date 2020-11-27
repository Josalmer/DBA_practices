/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica3;

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
    ArrayList<ArrayList<Integer> > map = new ArrayList();
    
    // Ticket de recarga
    Queue<String> rechargeTickets = new PriorityQueue();

    public Administration() {
        angentsSubscribed = 0;
        collectedMoney = 0;
    }

}
