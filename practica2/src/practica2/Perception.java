package practica2;

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
                    this.gps = convertToIntegerArray(perception.get(i).asObject().get("data").asArray().get(0).asArray());
                    break;
                case "compass":
                    this.compass = perception.get(i).asObject().get("data").asArray().get(0).asInt();
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

    public ArrayList<Integer> convertToIntegerArray(JsonArray array) {
        ArrayList<Integer> intArray = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            intArray.add(array.get(i).asInt());
        }
        return intArray;
    }

    public ArrayList<ArrayList<Integer>> convertToIntegerMatrix(JsonArray array) {
        ArrayList<ArrayList<Integer>> intMatrix = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            intMatrix.add(convertToIntegerArray(array.get(i).asArray()));
        }
        return intMatrix;
    }

    public ArrayList<Double> convertToDoubleArray(JsonArray array) {
        ArrayList<Double> doubleArray = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            doubleArray.add(array.get(i).asDouble());
        }
        return doubleArray;
    }

    public ArrayList<ArrayList<Double>> convertToDoubleMatrix(JsonArray array) {
        ArrayList<ArrayList<Double>> doubleMatrix = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            doubleMatrix.add(convertToDoubleArray(array.get(i).asArray()));
        }
        return doubleMatrix;
    }

    @Override
    public String toString() {
        return "Perception{" + "alive=" + alive + ", ontarget=" + ontarget + ", gps=" + gps + ", compass=" + compass + ", payload=" + payload + ", distance=" + distance + ", angular=" + angular + ", altimeter=" + altimeter + ", visual=" + visual + ", lidar=" + lidar + ", thermal=" + thermal + ", energy=" + energy + '}';
    }
}
