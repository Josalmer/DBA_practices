/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p1;

import AppBoot.ConsoleBoot;

public class P1 {

    public static void main(String[] args) {
        ConsoleBoot app = new ConsoleBoot("P1", args);
        app.selectConnection();
        
        app.launchAgent("Banco Santander", AnaPatriciaBotin.class);
        app.shutDown();        
    }
    
}
