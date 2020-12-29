/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Drone;

import java.util.ArrayList;

/**
 *
 * @author jose
 */
public class ProvisionalDroneOption {
    Integer x;
    Integer y;
    Integer floorHeight;
    Integer visitedAt;
    Double thermalValue;
    ArrayList<DroneAction> plan;
    Integer cost;
    double distanceToTarget;
    double puntuation;

    /**
     * Constructor
     * @author Jose Saldaña
     * @param x componente x de casilla destino
     * @param y componente y de casillo destino
     * @param floorHeight altura de casilla destino
     * @param visited última vez visitada la casilla destino (-1 = no visitada)
     */
    public ProvisionalDroneOption(Integer x, Integer y, Integer floorHeight, Integer visited ) {
        this.x = x;
        this.y = y;
        this.floorHeight = floorHeight;
        this.visitedAt = visited;
    }
    
    /**
     * Calcula la distancia al objetivo
     * @author Jose Saldaña
     * @param targetX componente x de casilla destino
     * @param targetY componente y de casillo destino
     */
    void calculateDistanceToTarget(int targetX, int targetY) {
        
        this.distanceToTarget = Math.abs(x - targetX) + Math.abs(y - targetY);
        this.puntuation = this.distanceToTarget;
    }
}
