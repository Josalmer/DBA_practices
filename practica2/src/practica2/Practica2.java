/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica2;

import AppBoot.ConsoleBoot;

public class Practica2 {

    public static void main(String[] args) {
        ConsoleBoot app = new ConsoleBoot("P2", args);
        app.selectConnection();
        
        app.launchAgent("Ana Patricia Botin", AnaPatriciaBotin.class);
        app.shutDown();        
    }
    
}
