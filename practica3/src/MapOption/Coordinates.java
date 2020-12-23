/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MapOption;

import com.eclipsesource.json.JsonObject;

/**
 * @author manuel
 */
public class Coordinates {

    public int x;
    public int y;

    public Coordinates(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Boolean checkCoordinates(Coordinates target) {
        return this.x == target.x && this.y == target.y;
    }
    
    public JsonObject getJSON(){
        JsonObject coordinates = new JsonObject();
        coordinates.add("x", this.x);
        coordinates.add("y", this.y);
        
        return coordinates;
    }
    
    public double calculateDistance(Coordinates target){
        return Math.abs(this.x - target.x) + Math.abs(this.y - target.y);
    }
}
