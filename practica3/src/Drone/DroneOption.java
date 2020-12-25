
package Drone;

import Drone.DroneAction;
import MapOption.Coordinates;
import MapOption.MinimalOption;
import java.util.ArrayList;

/**
 *
 * @author jose
 */
public class DroneOption extends MinimalOption{
    Integer visitedAt;
    Double thermalValue;
    ArrayList<DroneAction> plan;
    Integer cost;
    double puntuation;

    /**
     * Constructor
     * @author Jose Saldaña
     * @param x componente x de casilla destino
     * @param y componente y de casillo destino
     * @param floorHeight altura de casilla destino
     * @param visited última vez visitada la casilla destino (-1 = no visitada)
     */
    public DroneOption(Integer x, Integer y, Integer floorHeight, Integer visited ) {
        super(x,y,floorHeight);
        this.visitedAt = visited;
    }
    
    public void calculateDistanceToTarget(int targetX, int targetY) {
        this.distance = Math.abs(this.coordinates.x - targetX) + Math.abs(this.coordinates.y - targetY);
        this.puntuation = this.distance;
    }
}
