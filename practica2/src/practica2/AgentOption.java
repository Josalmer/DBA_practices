/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica2;

import java.util.ArrayList;

/**
 * Opciones a las que puede moverse un agente desde su posición actual
 * @author Jose Saldaña
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
     * Constructor
     * @author Jose Saldaña
     * @param x componente x de casilla destino
     * @param y componente y de casillo destino
     * @param floorHeight altura de casilla destino
     * @param visited última vez visitada la casilla destino (-1 = no visitada)
     */
    public AgentOption(Integer x, Integer y, Integer floorHeight, Integer visited) {
        this.x = x;
        this.y = y;
        this.floorHeight = floorHeight;
        this.visitedAt = visited;
    }
    
    /**
     * Calcula e inicializa la distancia a Ludwig desde la casilla,
     * mediante el método de distancia Manhattan
     * @author Jose Saldaña
     */
    void calculateDistanceToLudig(int ludwigX, int ludwigY) {
        // Distancia Manhattan
        this.distanceToLudwig = Math.abs(x - ludwigX) + Math.abs(y - ludwigY);
//         Distancia de Euclides
//	this.puntuation = (Math.sqrt(Math.pow(2,(x - ludwigX)) + Math.pow(2,(y - ludwigY))));
    }
}
