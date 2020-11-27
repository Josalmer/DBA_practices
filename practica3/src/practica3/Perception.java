package practica3;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.ArrayList;

/**
 *
 * @author Manuel Pancorbo
 * @author Jose Saldaña
 */
public class Perception {

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
    public Perception() {
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
                    this.alive = perception.get(i).asObject().get("data").asArray().get(0).asInt() == 1;
                    break;
                case "ontarget":
                    this.ontarget = perception.get(i).asObject().get("data").asArray().get(0).asInt() == 1;
                    break;
                case "gps":
                    this.gps = convertToIntegerArray(
                            perception.get(i).asObject().get("data").asArray().get(0).asArray());
                    break;
                case "compass":
                    this.compass = (int) Math
                            .round(perception.get(i).asObject().get("data").asArray().get(0).asDouble());
                    break;
                case "payload":
                    this.payload = perception.get(i).asObject().get("data").asArray().get(0).asInt();
                    break;
                case "distance":
                    this.distance = perception.get(i).asObject().get("data").asArray().get(0).asDouble();
                    break;
                case "angular":
                    this.angular = perception.get(i).asObject().get("data").asArray().get(0).asDouble();
                    break;
                case "altimeter":
                    this.altimeter = perception.get(i).asObject().get("data").asArray().get(0).asInt();
                    break;
                case "visual":
                    this.visual = convertToIntegerMatrix(perception.get(i).asObject().get("data").asArray());
                    break;
                case "lidar":
                    this.lidar = convertToIntegerMatrix(perception.get(i).asObject().get("data").asArray());
                    break;
                case "thermal":
                    this.thermal = convertToDoubleMatrix(perception.get(i).asObject().get("data").asArray());
                    break;
                case "energy":
                    this.energy = perception.get(i).asObject().get("data").asArray().get(0).asInt();
                    break;
            }
        }
    }

    /**
     * Parsea los datos recibidos en un JSONArray a un ArrayList de enteros. El
     * JSONArray contiene únicamente los datos de un sensor (campo 'data').
     * 
     * @author Manuel Pancorbo
     * @param array, JSONArray que contiene los datos de un sensor
     * @return Array de enteros con los datos parseados.
     */
    public ArrayList<Integer> convertToIntegerArray(JsonArray array) {
        ArrayList<Integer> intArray = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            intArray.add(array.get(i).asInt());
        }
        return intArray;
    }

    /**
     * Parsea los datos recibidos en un JSONArray en una matriz de enteros. El
     * JSONArray contiene únicamente los datos de un sensor (campo 'data'). La
     * matriz está construida con la estructura ArrayList (ArrayList de ArrayList de
     * enteros).
     * 
     * @author Manuel Pancorbo
     * @param array, JSONArray que contiene los datos de un sensor
     * @return Matriz de enteros con los datos parseados.
     */
    public ArrayList<ArrayList<Integer>> convertToIntegerMatrix(JsonArray array) {
        ArrayList<ArrayList<Integer>> intMatrix = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            intMatrix.add(convertToIntegerArray(array.get(i).asArray()));
        }
        return intMatrix;
    }

    /**
     * Parsea los datos recibidos en un JSONArray a un ArrayList de doubles. El
     * JSONArray contiene únicamente los datos de un sensor (campo 'data').
     * 
     * @author Manuel Pancorbo
     * @param array, JSONArray que contiene los datos de un sensor
     * @return Array de doubles con los datos parseados.
     */
    public ArrayList<Double> convertToDoubleArray(JsonArray array) {
        ArrayList<Double> doubleArray = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            doubleArray.add(array.get(i).asDouble());
        }
        return doubleArray;
    }

    /**
     * Parsea los datos recibidos en un JSONArray en una matriz de doubles. El
     * JSONArray contiene únicamente los datos de un sensor (campo 'data'). La
     * matriz está construida con la estructura ArrayList (ArrayList de ArrayList de
     * doubles).
     * 
     * @author Manuel Pancorbo
     * @param array, JSONArray que contiene los datos de un sensor
     * @return Matriz de doubles con los datos parseados.
     */
    public ArrayList<ArrayList<Double>> convertToDoubleMatrix(JsonArray array) {
        ArrayList<ArrayList<Double>> doubleMatrix = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            doubleMatrix.add(convertToDoubleArray(array.get(i).asArray()));
        }
        return doubleMatrix;
    }

    /**
     * Devuelve todos los valores de los sensores de la clase Perception.
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
