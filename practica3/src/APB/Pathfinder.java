/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APB;

import MapOption.Coordinates;
import com.eclipsesource.json.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

/**
 *
 * @author Manuel Pancorbo
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

    PathfinderOption initialPosition;
    PathfinderOption targetPosition;

    public Pathfinder(ArrayList<ArrayList<Integer>> map, int maxHeight) {
        this.mapWidth = map.size();
        this.mapHeight = map.get(0).size();
        this.skyLimit = maxHeight;
        this.map = map;
    }

    public JsonObject pathFinding_a(int rescuerPositionX, int rescuerPositionY, int targetPositionX, int targetPositionY) {

        this.initialPosition = new PathfinderOption(new Coordinates(rescuerPositionX, rescuerPositionY));
        this.targetPosition = new PathfinderOption(new Coordinates(rescuerPositionX, rescuerPositionY));

        plan = new ArrayList<>();

        //Aclarar como va ir esto, tener en cuenta la Y del mapa, habria que ordenar el set
        generated = new HashSet<Coordinates>();

        Comparator<PathfinderOption> comparator = new Comparator<PathfinderOption>() {
            @Override
            public int compare(PathfinderOption arg0, PathfinderOption arg1) {
                return Double.compare(arg0.distance, arg1.distance);
            }
        };
        PriorityQueue<PathfinderOption> queue = new PriorityQueue(comparator);

        PathfinderOption current = initialPosition;
        queue.add(current);

        //Mientras haya casillas que estudiar o no lleguemos al objetivo, seguimos evaluando
        while (queue.size() != 0 && !current.checkCoordinates(targetPosition)) {

            //Sacamos el primero de la cola
            queue.poll();

            //Si no lo hemos evaluado, lo metemos en evaluados            
            Coordinates posicion = current.coordinates;
            if (!generated.contains(posicion)) {
                generated.add(posicion);
            }

            ArrayList<PathfinderOption> child = generateChilds(current);

            //Introducimos aquellas que no hemos visitado
            for (PathfinderOption option : child) {
                if (!generated.contains(option)) {
                    generated.add(option.coordinates);
                    queue.add(option);
                }
            }

            //cogemos el siguiente 
            current = queue.peek();
        }

        JsonObject parsedPlan = null;
        //Hemos terminado, recuperamos el plan si hemos logrado llegar
        if (current.checkCoordinates(targetPosition)) {
            this.plan = current.getPath();
            parsedPlan = this.parsePlan();
        }

        return parsedPlan;
    }

    /*
    *   { plan : { {x: 0, y:3},{x: 0, y: 4},.... } }
     */
    JsonObject parsePlan() {
        JsonObject parsedPlan = new JsonObject();
        JsonArray coordinatesArray = new JsonArray();

        for (Coordinates coordinate : this.plan) {
            coordinatesArray.add(coordinate.getJSON());
        }

        parsedPlan.add("plan", coordinatesArray);
        return parsedPlan;
    }

    private ArrayList<PathfinderOption> generateChilds(PathfinderOption parent) {

        ArrayList<PathfinderOption> options = new ArrayList<>();
        for (int i = 0; i < Pathfinder.POSSIBLE_ORIENTATIONS; i++) {
            if (i != 4) { // Dont check current position
                int xPosition = parent.coordinates.x - 1 + (i % 3);
                int yPosition = parent.coordinates.y - 1 + (i / 3);
                if (insideMap(xPosition, yPosition)) {
                    int childHeight = this.map.get(xPosition).get(yPosition);
                    if (childHeight < this.skyLimit) {
                        PathfinderOption next = this.generateOption(parent, xPosition, yPosition, childHeight);
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

    PathfinderOption generateOption(PathfinderOption parent, int xPosition, int yPosition, int height) {
        //Creamos la opcion
        PathfinderOption option = new PathfinderOption(xPosition, yPosition, height);
        option.calculateDistance(this.targetPosition);
        option.setPath(parent.getPath());

        return option;
    }
}
