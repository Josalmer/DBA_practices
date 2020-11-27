package Drone;

import JSONParser.AgentJSONParser;
import com.eclipsesource.json.JsonArray;
import java.util.ArrayList;

/**
 *
 * @author Manuel Pancorbo
 * @author Jose Saldaña
 */
public class DronePerception {
    
    AgentJSONParser parser;

    Boolean alive;
    Boolean ontarget;
    ArrayList<Integer> gps;
    Integer compass;
    Integer payload;
    Double distance;
    Double angular;
    Integer altimeter;
    ArrayList<ArrayList<Integer>> visual;
    ArrayList<ArrayList<Integer>> lidar;
    ArrayList<ArrayList<Double>> thermal;
    Integer energy;

    /**
     * Método constructor de la clase, inicializa a null todos los valores.
     * 
     * @author Manuel Pancorbo
     */
    public DronePerception() {
        this.alive = null;
        this.ontarget = null;
        this.gps = null;
        this.compass = null;
        this.payload = null;
        this.distance = null;
        this.angular = null;
        this.altimeter = null;
        this.visual = null;
        this.lidar = null;
        this.thermal = null;
        this.energy = null;
    }

    /**
     * Parsea y actualiza los datos recibidos en un JSONArray que serán guardados
     * variables de la clase. Estas variables harán más sencilla la tarea de
     * construir el resto del código y la legibilidad. El JSONArray contiene los
     * sensores recibidos y sus datos.
     * 
     * @author Manuel Pancorbo
     * @param perception, JSONArray que contiene los sensores y sus datos.
     */
    public void update(JsonArray perception) {
        for (int i = 0; i < perception.size(); i++) {
            switch (perception.get(i).asObject().get("sensor").asString()) {
                case "alive":
                    this.alive = parser.getIntSensorData(perception.get(i).asObject()) == 1;
                    break;
                case "ontarget":
                    this.ontarget = parser.getIntSensorData(perception.get(i).asObject()) == 1;
                    break;
                case "gps":
                    this.gps = parser.convertToIntegerArray(perception.get(i).asObject().get("data").asArray().get(0).asArray());
                    break;
                case "compass":
                    this.compass = (int) Math.round(parser.getDoubleSensorData(perception.get(i).asObject()));
                    break;
                case "payload":
                    this.payload = parser.getIntSensorData(perception.get(i).asObject());
                    break;
                case "distance":
                    this.distance = parser.getDoubleSensorData(perception.get(i).asObject());
                    break;
                case "angular":
                    this.angular = parser.getDoubleSensorData(perception.get(i).asObject());
                    break;
                case "altimeter":
                    this.altimeter = parser.getIntSensorData(perception.get(i).asObject());
                    break;
                case "visual":
                    this.visual = parser.convertToIntegerMatrix(perception.get(i).asObject().get("data").asArray());
                    break;
                case "lidar":
                    this.lidar = parser.convertToIntegerMatrix(perception.get(i).asObject().get("data").asArray());
                    break;
                case "thermal":
                    this.thermal = parser.convertToDoubleMatrix(perception.get(i).asObject().get("data").asArray());
                    break;
                case "energy":
                    this.energy = parser.getIntSensorData(perception.get(i).asObject());
                    break;
            }
        }
    }

   
    /**
     * Devuelve todos los valores de los sensores de la clase DronePerception.
     * 
     * @author Manuel Pancorbo
     * @return devuelve en un string las variables de la clase y sus valores,
     *         incluidas las que tienen valor null.
     */
    @Override
    public String toString() {
        return "Perception{" + "alive=" + alive + ", ontarget=" + ontarget + ", gps=" + gps + ", compass=" + compass
                + ", payload=" + payload + ", distance=" + distance + ", angular=" + angular + ", altimeter="
                + altimeter + ", visual=" + visual + ", lidar=" + lidar + ", thermal=" + thermal + ", energy=" + energy
                + '}';
    }
}
