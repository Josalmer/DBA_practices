/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p2;

import java.util.ArrayList;

/**
 *
 * @author jose
 */
public class AgentOption {
    Integer x;
    Integer y;
    Integer floorHeight;
    Float distanceToObjetive;
    ArrayList<AgentAction> plan;
    Integer cost;
    Float puntuation;
    Float puntuationCostRelation;

    public AgentOption(Integer x, Integer y, Integer floorHeight) {
        this.x = x;
        this.y = y;
        this.floorHeight = floorHeight;
    }
    
    
}
