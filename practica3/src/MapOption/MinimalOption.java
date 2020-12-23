package MapOption;

/**
 *
 * @author manuel
 */
public class MinimalOption {
    public Coordinates coordinates;
    public double distance;
    public Integer floorHeight;
    
    public MinimalOption(int x, int y, int floorHeight){
        this.coordinates = new Coordinates(x,y);
        this.floorHeight = floorHeight;
    }
    
    public void calculateDistance(MinimalOption target) {
        this.distance = this.coordinates.calculateDistance(target.coordinates);
    }
}
