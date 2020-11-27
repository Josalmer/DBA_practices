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
    ArrayList<DroneAction> plan;
    Integer cost;
    double distanceToTarget;
    double puntuationCostRelation;

    /**
     * Constructor
     * @author Jose Saldaña
     * @param x componente x de casilla destino
     * @param y componente y de casillo destino
     * @param floorHeight altura de casilla destino
     * @param visited última vez visitada la casilla destino (-1 = no visitada)
     */
    public DroneOption(Integer x, Integer y, Integer floorHeight, Integer visited) {
        this.x = x;
        this.y = y;
        this.floorHeight = floorHeight;
        this.visitedAt = visited;
    }
    

    void calculateDistanceToTarget(int targetX, int targetY) {
        // Distancia Manhattan
        this.distanceToTarget = Math.abs(x - targetX) + Math.abs(y - targetY);
//         Distancia de Euclides
//	this.puntuation = (Math.sqrt(Math.pow(2,(x - ludwigX)) + Math.pow(2,(y - ludwigY))));
    }
}
