/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APB;

import MapOption.Coordinates;
import MapOption.MinimalOption;
import java.util.ArrayList;

/**
 *
 * @author Manuel Pancorbo
 */
class PathfinderOption extends MinimalOption {

    private ArrayList<Coordinates> path;

    public PathfinderOption(Integer x, Integer y, Integer floorHeight) {
        super(x, y, floorHeight);
        this.path = new ArrayList<>();
    }

    public PathfinderOption(Coordinates coordinates) {
        super(coordinates.x, coordinates.y, 0);
        this.path = new ArrayList<>();
    }

    void calculateDistance(PathfinderOption target) {
        this.distance = this.coordinates.calculateDistance(target.coordinates);
    }

    void setPath(ArrayList<Coordinates> parentPath) {
        this.path = parentPath;
        this.path.add(this.coordinates);
    }

    ArrayList<Coordinates> getPath() {
        return this.path;
    }

    Boolean checkCoordinates(PathfinderOption target) {
        return this.coordinates.checkCoordinates(target.coordinates);
    }
}
