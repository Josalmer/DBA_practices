package MapOption;

/**
 * Clase MinimalOption 
 * @author manuel
 */
public class MinimalOption {
    public Coordinates coordinates;
    public double distance;
    public Integer floorHeight;
    
     /**
     * Constructor
     * @param x posicion x de la casilla opción
     * @param y posicion y de la casilla opción
     * @param floorHeight altura de la casilla opción
     * @author Manuel Pancorbo
     *
     */
    public MinimalOption(int x, int y, int floorHeight){
        this.coordinates = new Coordinates(x,y);
        this.floorHeight = floorHeight;
    }
    
    /**
     * Calcula la distancia de una opción viable al objetivo viable
     * @param target MinimalOption
     * @author Manuel Pancorbo
     *
     */
    public void calculateDistance(MinimalOption target) {
        this.distance = this.coordinates.calculateDistance(target.coordinates);
    }
}
