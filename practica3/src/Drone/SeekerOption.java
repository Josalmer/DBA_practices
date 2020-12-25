
package Drone;

import Drone.DroneOption;

/**
 *
 * @author domin
 */
public class SeekerOption extends DroneOption{
    
    double thermalPuntuation;

    /**
     * Constructor
     * @author Jose Saldaña
     * @param x componente x de casilla destino
     * @param y componente y de casillo destino
     * @param floorHeight altura de casilla destino
     * @param visited última vez visitada la casilla destino (-1 = no visitada)
     */
    public SeekerOption(Integer x, Integer y, Integer floorHeight, Integer visited, Double thermalValue) {
        super(x,y,floorHeight, visited);
        this.thermalValue = thermalValue;
    }
    
    @Override
    public void calculateDistanceToTarget(int targetX, int targetY) {
        super.calculateDistanceToTarget(targetX, targetY);
        this.thermalPuntuation = thermalValue/20;
	this.puntuation = this.distance*0.8 + this.thermalPuntuation*0.3;
    }
}
