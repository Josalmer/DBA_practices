/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Drone;

import java.util.ArrayList;

/**
 *
 * @author domin
 */
public class DroneOption {
    Integer x;
    Integer y;
    Integer floorHeight;
    Integer visitedAt;
    Double thermalValue;
    ArrayList<DroneAction> plan;
    Integer cost;
    double distanceToTarget;
    double thermalPuntuation;
    double puntuation;

    /**
     * Constructor
     * @author Jose Saldaña
     * @param x componente x de casilla destino
     * @param y componente y de casillo destino
     * @param floorHeight altura de casilla destino
     * @param visited última vez visitada la casilla destino (-1 = no visitada)
     */
    public DroneOption(Integer x, Integer y, Integer floorHeight, Integer visited, Double thermalValue) {
        this.x = x;
        this.y = y;
        this.floorHeight = floorHeight;
        this.visitedAt = visited;
        this.thermalValue = thermalValue;
    }
    

    void calculateDistanceToTarget(int targetX, int targetY) {
        
        //REVISAR EL CÁLCULO
        this.distanceToTarget = Math.abs(x - targetX) + Math.abs(y - targetY);
        this.thermalPuntuation = thermalValue/20;
	this.puntuation = this.distanceToTarget*0.8 + this.thermalPuntuation*0.3;
    }
}
