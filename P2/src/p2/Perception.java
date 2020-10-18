package p2;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.ArrayList;

/**
 *
 * @author Manuel Pancorbo, Jose Saldaña
 */
public class Perception {

    Boolean alive;
    Boolean ontarget;
    ArrayList<Integer> gps;
    Integer compass;
    Integer payload;
    Float distance;
    Integer angular;
    Integer altimeter;
    ArrayList<ArrayList<Integer>> visual;
    ArrayList<ArrayList<Integer>> lidar;
    ArrayList<ArrayList<Float>> thermal;
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

    public void asignValues(JsonArray perception) {
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
                    this.distance = perception.get(i).asObject().get("data").asArray().get(0).asFloat();
                    break;
                case "angular":
                    this.angular = Math.round(perception.get(i).asObject().get("data").asArray().get(0).asFloat());
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
                    this.thermal = convertToFloatMatrix(perception.get(i).asObject().get("data").asArray());
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

    public ArrayList<Float> convertToFloatArray(JsonArray array) {
        ArrayList<Float> floatArray = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            floatArray.add(array.get(i).asFloat());
        }
        return floatArray;
    }

    public ArrayList<ArrayList<Float>> convertToFloatMatrix(JsonArray array) {
        ArrayList<ArrayList<Float>> floatMatrix = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            floatMatrix.add(convertToFloatArray(array.get(i).asArray()));
        }
        return floatMatrix;
    }

    @Override
    public String toString() {
        return "Perception{" + "alive=" + alive + ", ontarget=" + ontarget + ", gps=" + gps + ", compass=" + compass + ", payload=" + payload + ", distance=" + distance + ", angular=" + angular + ", altimeter=" + altimeter + ", visual=" + visual + ", lidar=" + lidar + ", thermal=" + thermal + ", energy=" + energy + '}';
    }
}
