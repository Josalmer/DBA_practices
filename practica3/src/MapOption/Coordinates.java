/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MapOption;

import com.eclipsesource.json.JsonObject;

/**
 * Clase Coordinates, que contiene unicamente las coordenadas de una casilla
 * @author Manuel Pancorbo
 */
public class Coordinates {

    public int x;
    public int y;

    /**
     * Constructor
     * @param x posicionX de la coordenada
     * @param y posicionY de la coordenada
     * @author Manuel Pancorbo
     *
     */
    public Coordinates(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Comprueba silas coordenadas coinciden con las del target que se le pasa
     * @param target Coordinates
     * @author Manuel Pancorbo
     * @return booleano indicando si el target está en esas coordenadas
     *
     */
    public Boolean checkCoordinates(Coordinates target) {
        return this.x == target.x && this.y == target.y;
    }
    
     /**
     * Convierte las coordenadas a JSON
     * @author Manuel Pancorbo
     * @return JsonObject con las coordenadas
     *
     */
    public JsonObject getJSON(){
        JsonObject coordinates = new JsonObject();
        coordinates.add("x", this.x);
        coordinates.add("y", this.y);
        
        return coordinates;
    }
    
     /**
     * Calcula la distancia de la coordenada a la que se le pasa
     * @param target Coordinates
     * @author Manuel Pancorbo
     * @return distancia a la coordenada pasada
     *
     */
    public double calculateDistance(Coordinates target){
        return Math.abs(this.x - target.x) + Math.abs(this.y - target.y);
    }
}
