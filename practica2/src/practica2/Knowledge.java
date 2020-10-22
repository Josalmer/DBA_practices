/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica2;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.ArrayList;

/**
 *
 * @author jose
 */
public class Knowledge {
    Integer currentPositionX;
    Integer currentPositionY;
    Integer currentHeight;
    Integer energy;
    Integer orientation;
    Integer mapWidth;
    Integer mapHeight;
    Integer maxFlight;
    Float angular;
    Float distanceToObjective;
    Integer nActionsExecuted;
    
    ArrayList<ArrayList<Integer>> map;
    
    /**
     * @author Jose Saldaña, Manuel Pancorbo
     * @param answer 
     */
    public void initializeKnowledge(JsonObject answer) {
        this.energy = 1000;
        this.mapWidth = answer.get("width").asInt();
        this.mapHeight = answer.get("height").asInt();
        this.maxFlight = answer.get("maxflight").asInt();
        this.initializeMap();
    }
    
    /**
     * @author Jose Saldaña, Manuel Pancorbo
     * -1 (not known)
     */
    public void initializeMap() {
        this.map = new ArrayList<>();
        for (int i = 0; i < this.mapWidth; i++) {
            ArrayList<Integer> row = new ArrayList<>();
            for (int j = 0; j < this.mapHeight; j++) {
                row.add(-1);
            }
            this.map.add(row);
        }
    }
    
    public int getFloorHeight() {
        return this.map.get(this.currentPositionX).get(this.currentPositionY);
    }

    public void moveForward(){
        int ABScurrentOrientation = Math.abs(this.orientation); //Valor absoluto de la orientación
        
        //Posición X
        if(ABScurrentOrientation != 0 || ABScurrentOrientation != 180)
            this.currentPositionX += this.orientation/ABScurrentOrientation;

        //Posición Y
        if(ABScurrentOrientation != 90)
            if(ABScurrentOrientation == 45 || ABScurrentOrientation == 0)
                this.currentPositionY -= 1;
            else
                this.currentPositionY += 1;
    }
    
    /**
     * @author Domingo Lopez
     * @param action
     * @return 
     */
    public int energyCost(AgentAction action, int nSensors) {
        int energy = 0;
        switch (action) {
            case moveF: energy = 1; break;
            case moveUp: energy = 5; break;
            case moveD: energy = 5; break;
            case rotateL: energy = 1; break;
            case rotateR: energy = 1; break;
            case touchD: energy = 1; break;
            case LECTURA_SENSORES: energy = nSensors; break;
        }
        return energy;
    }
}
