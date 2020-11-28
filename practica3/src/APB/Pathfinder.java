/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica3;

import com.eclipsesource.json.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

/**
 *
 * @author manuel
 */
public class Pathfinder {
    
    //POSSIBLE MOVES
    private static int POSSIBLE_ORIENTATIONS = 9;
    
    //MAP INFO
    private ArrayList<ArrayList<Integer>> map;
    private int mapHeight;
    private int mapWidth;
    private int skyLimit;
    
    //PLAN
    ArrayList<Coordinates> plan;
    Set<Coordinates> generated;
    
    MinimalAgentOption initialPosition;
    MinimalAgentOption targetPosition;
    
    public Pathfinder(ArrayList<ArrayList<Integer>> map, int maxHeight){
        this.mapWidth = map.size();
        this.mapHeight= map.get(0).size();
        this.skyLimit = maxHeight;
        this.map = map;
    }

    public JsonObject pathFinding_a(int rescuerPositionX, int rescuerPositionY, int targetPositionX, int targetPositionY){
        
        this.initialPosition = new MinimalAgentOption(new Coordinates(rescuerPositionX, rescuerPositionY));
        this.targetPosition = new MinimalAgentOption(new Coordinates(rescuerPositionX, rescuerPositionY));

	plan = new ArrayList<>();
	plan.clear();

        
	//Aclarar como va ir esto, tener en cuenta la Y del mapa, habria que ordenar el set
	generated = new HashSet<Coordinates>();
        Comparator<MinimalAgentOption> comparator = new Comparator<MinimalAgentOption>() {
            @Override
            public int compare(MinimalAgentOption arg0, MinimalAgentOption arg1) {
               return Double.compare(arg0.distance, arg1.distance);
            }
        };
	
	PriorityQueue<MinimalAgentOption> queue = new PriorityQueue(comparator);
     
        
        MinimalAgentOption current = initialPosition;
        queue.add(current);
        
        
       
        //Mientras haya casillas que estudiar o no lleguemos al objetivo, seguimos evaluando
        while(queue.size() != 0 && !current.checkCoordinates(targetPosition)){
            
            //Sacamos el primero de la cola
            queue.poll();
          
            //Si no lo hemos evaluado, lo metemos en evaluados            
            Coordinates posicion = current.coordinates;
            if(!generated.contains(posicion))
                generated.add(posicion);
            
            ArrayList<MinimalAgentOption> child = generateChilds(current);
            
            //Introducimos aquellas que no hemos visitado
            for(MinimalAgentOption option : child){
                if(!generated.contains(option)){
                    generated.add(option.coordinates);
                    queue.add(option);
                }
            }
            
           //cogemos el siguiente 
           current = queue.peek();
        }
        
        JsonObject parsedPlan = null;
        //Hemos terminado, recuperamos el plan si hemos logrado llegar
        if(current.checkCoordinates(targetPosition)){
            this.plan = current.getPath();
            parsedPlan = this.parsePlan();
        }
        
        
        return parsedPlan;
    }
    /*
    *   { plan : { {x: 0, y:3},{x: 0, y: 4},.... } }
    */
    JsonObject parsePlan(){
        JsonObject parsedPlan = new JsonObject();
        JsonArray coordinatesArray  = new JsonArray();
       
        for(Coordinates coordinate : this.plan){
            JsonObject coordinates = new JsonObject();
            coordinates.add("x", coordinate.getX());
            coordinates.add("y", coordinate.getY());
            
            coordinatesArray.add(coordinates);
        }
        
        parsedPlan.add("plan", coordinatesArray);
        return parsedPlan;
    }
    
    
    private ArrayList<MinimalAgentOption> generateChilds(MinimalAgentOption parent){
       
        ArrayList<MinimalAgentOption> options = new ArrayList<>();
        for (int i = 0; i < Pathfinder.POSSIBLE_ORIENTATIONS; i++) {
            if (i != 4) { // Dont check current position
                int xPosition = parent.coordinates.getX() - 1 + (i % 3);
                int yPosition = parent.coordinates.getY() - 1 + (i / 3);
                if (insideMap(xPosition, yPosition)) {
                    int childHeight = this.map.get(xPosition).get(yPosition);
                    if (childHeight < this.skyLimit) {
                        MinimalAgentOption next = this.generateOption(parent, xPosition, yPosition, childHeight);
                        options.add(next);
                    }
                }
            }
        }
        return options;
    }
        

    private boolean insideMap(int x, int y) {
        return (x >= 0 && x < this.mapWidth && y >= 0 && y < this.mapHeight);
    }
    
    
    MinimalAgentOption generateOption(MinimalAgentOption parent, int xPosition, int yPosition, int height) {
        //Creamos la opcion
        MinimalAgentOption option = new MinimalAgentOption(xPosition, yPosition, height);
        option.calculateDistance(this.targetPosition);
        option.setPath(parent.getPath());
        
        return option;
    }   
}

/**
* @author manuel
*/
class Coordinates{
    private int x;
    private int y;
    
    Coordinates(int x,int y){
        this.x = x;
        this.y = y;
    }
    
    int getX(){ return this.x; }
    int getY(){ return this.y; }
    
    Boolean checkCoordinates(Coordinates target){
        return this.x == target.x && this.y == target.y;
    }
}

/**
* 
* @author manuel
*/
class MinimalAgentOption {
    Coordinates coordinates;
    Integer floorHeight;
    double distance;

    private ArrayList<Coordinates> path;
    /**
     * Constructor
     * @author Jose Saldaña
     * @param x componente x de casilla destino
     * @param y componente y de casillo destino
     * @param floorHeight altura de casilla destino
     * @param visited última vez visitada la casilla destino (-1 = no visitada)
     */
    public MinimalAgentOption(Integer x, Integer y, Integer floorHeight) {
        this.coordinates = new Coordinates(x,y);
        this.floorHeight = floorHeight;
        this.path = new ArrayList<>();
    }
    
    public MinimalAgentOption(Coordinates coordinates){
        this.coordinates = coordinates;
        this.path = new ArrayList<>();
    }
    
    /**
     * Calcula e inicializa la distancia al objetivo desde la casilla,
     * mediante el método de distancia Manhattan
     * @author Jose Saldaña
     * @param target casilla del objetivo
     */
    void calculateDistance(MinimalAgentOption target) {
        // Distancia Manhattan
        this.distance = Math.abs(this.coordinates.getX() - target.coordinates.getX()) + Math.abs(this.coordinates.getY() - target.coordinates.getY());
    }
    
    void setPath(ArrayList<Coordinates> parentPath){
        this.path = parentPath;
        this.path.add(this.coordinates);
    }
    
    ArrayList<Coordinates> getPath(){
        return this.path;
    }
    
    Boolean checkCoordinates(MinimalAgentOption target){
        return this.coordinates.checkCoordinates(target.coordinates);
    }
}

