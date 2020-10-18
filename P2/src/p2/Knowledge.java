/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p2;

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
    Integer angular;
    Float distanceToObjective;
    
    ArrayList<ArrayList<Integer>> map;
    
    /**
     * @author Jose Saldaña, Manuel Pancorbo
     * @param answer 
     */
    public void initializeKnowledge(JsonObject answer) {
        this.energy = 1000;
        this.orientation = 90;
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
    
}
