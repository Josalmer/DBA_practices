/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica3;

import AppBoot.ConsoleBoot;

public class Practica3 {

    public static void main(String[] args) {
        ConsoleBoot app = new ConsoleBoot("P3", args);
        app.selectConnection();

        app.launchAgent("Ana Patricia Botin", AnaPatriciaBotin.class);
        app.launchAgent("Buscador Saldaña", Seeker.class);
        app.launchAgent("Buscador Domingo", Seeker.class);
        app.launchAgent("Manuel al Rescate", Rescuer.class);
        app.launchAgent("Migue al Rescate", Rescuer.class);
        app.shutDown();
    }

}
