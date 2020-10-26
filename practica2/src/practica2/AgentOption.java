/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica2;

import java.util.ArrayList;

/**
 *
 * @author jose
 */
public class AgentOption {
    Integer x;
    Integer y;
    Integer floorHeight;
    Integer visitedAt;
    ArrayList<AgentAction> plan;
    Integer cost;
    double distanceToLudwig;
    double puntuationCostRelation;

    /**
     * @author Jose Saldaña
     * @param x
     * @param y
     * @param floorHeight 
     */
    public AgentOption(Integer x, Integer y, Integer floorHeight, Integer visited) {
        this.x = x;
        this.y = y;
        this.floorHeight = floorHeight;
        this.visitedAt = visited;
    }
    
    /**
     * @author Jose Saldaña
     * @param ludwigX
     * @param ludwigY 
     */
    void calculateDistanceToLudig(int ludwigX, int ludwigY) {
        // Distancia Manhattan
        this.distanceToLudwig = Math.abs(x - ludwigX) + Math.abs(y - ludwigY);
//         Distancia de Euclides
//	this.puntuation = (Math.sqrt(Math.pow(2,(x - ludwigX)) + Math.pow(2,(y - ludwigY))));
    }
}
