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
    void calculatePuntuation(int squareOrientation, double targetOrientation) {
        double desviation = Math.abs(Math.round(targetOrientation - squareOrientation));
        this.puntuation = (180-desviation);
        this.puntuationCostRelation = Math.pow(this.puntuation, 4) / this.cost;
    }
}
