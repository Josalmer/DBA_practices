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
    ArrayList<AgentAction> plan;
    Integer cost;
    double puntuation;
    double puntuationCostRelation;

    /**
     * @author Jose Saldaña
     * @param x
     * @param y
     * @param floorHeight 
     */
    public AgentOption(Integer x, Integer y, Integer floorHeight) {
        this.x = x;
        this.y = y;
        this.floorHeight = floorHeight;
    }
    
    /**
     * @author Jose Saldaña
     * @param squareOrientation
     * @param targetOrientation 
     */
    void calculatePuntuationByAngular(int squareOrientation, double targetOrientation) {
        double desviation = Math.abs(Math.round(targetOrientation - squareOrientation));
        this.puntuation = (180-desviation);
        this.puntuationCostRelation = Math.pow(this.puntuation, 4) / this.cost;
    }
    
    /**
     * @author Jose Saldaña
     * @param ludwigX
     * @param ludwigY 
     */
    void calculatePuntuation(int ludwigX, int ludwigY) {
        // Distancia Manhattan
        this.puntuation = Math.abs(x - ludwigX) + Math.abs(y - ludwigY);
        // Distancia de Euclides
//	this.puntuation = (Math.sqrt(Math.pow(2,(x - ludwigX)) + Math.pow(2,(y - ludwigY))));
        this.puntuationCostRelation = Math.pow(this.puntuation, 4) / this.cost;
    }
}
