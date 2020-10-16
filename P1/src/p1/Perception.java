/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p1;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author jose
 * @author manuel
 */
public class Perception {
    
    //HashMap <String, ArrayList<Float> sensores;
    
    //sensores
    Boolean alive;
    Boolean ontarget;
    ArrayList<Integer> gps;
    Integer compass;
    Integer payload;
    Float distance;
    Integer angular;
    Integer altimeter;
    ArrayList<Integer> visual;
    ArrayList<ArrayList<Integer>> lidar;
    ArrayList<ArrayList<Integer>> thermal;
    Integer energy;
    
    public Perception(){
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
    
    public void asignValues(JsonArray perception){
        for(int i = 0; i<perception.size(); i++){
            switch(perception.get(i).asObject().get("sensor").asString()){
                case "alive":
                    this.alive = perception.get(i).asObject().get("data").asBoolean(); 
                    break;
                case "ontarget":
                    this.ontarget = perception.get(i).asObject().get("data").asBoolean(); 
                    break;
                case "gps":
                    this.gps = convertToIntegerArray(perception.get(i).asObject().get("data").asArray()); 
                    break;
                case "compass":
                    this.compass = perception.get(i).asObject().get("data").asInt(); 
                    break;
                case "payload":
                    this.payload = perception.get(i).asObject().get("data").asInt(); 
                    break;
                case "distance":
                    this.distance = perception.get(i).asObject().get("data").asFloat(); 
                    break;
                case "angular":
                    this.angular = perception.get(i).asObject().get("data").asInt(); 
                    break;
                case "altimeter":
                    this.altimeter = perception.get(i).asObject().get("data").asInt(); 
                    break;
                case "visual":
                    this.visual = convertToIntegerArray(perception.get(i).asObject().get("data").asArray()); 
                    break;
                case "lidar":
                    this.lidar = convertToIntegerMatrix(perception.get(i).asObject().get("data").asArray()); 
                    break;
                case "thermal":
                    this.thermal = convertToIntegerMatrix(perception.get(i).asObject().get("data").asArray()); 
                    break;
                case "energy":
                    this.energy = perception.get(i).asObject().get("data").asInt(); 
                    break;
            }
        }
    }
    
    public void convertToStringArray(JsonArray array){
            ArrayList<String> stringArray = new ArrayList<>();
            for(int i=0; i<array.size();i++){
                stringArray.add(array.get(i).asString());
            }
    }
    
    public ArrayList<Integer> convertToIntegerArray(JsonArray array){
            ArrayList<Integer> intArray = new ArrayList<>();
            for(int i=0; i<array.size();i++){
                intArray.add(array.get(i).asInt());
            }
            return intArray;
    }
    
    public ArrayList<ArrayList<Integer>> convertToIntegerMatrix(JsonArray array){
            ArrayList<ArrayList<Integer>> intMatrix= new ArrayList<>();
            for(int i=0; i<array.size();i++){
                intMatrix.add(convertToIntegerArray(array.get(i).asArray()));
            }
            return intMatrix;
    }
    
    //Usar solo para añadir sensor
    //añadimos el string noValue porque put nos devolvera null si no existe el sensor en updateSensor
    /*
    public boolean addSensor(String sensorName){
        if(this.sensores.containsKey(sensorName)) return false;
        String[] noValue = {"noValue"};
        this.sensores.put(sensorName, noValue);
        return true;
    }
    
    public boolean updateSensor(String sensorName, String[] value){
        String[] oldValue = this.sensores.put(sensorName, value);
        if (oldValue == null) return false;
        return true;
    }
    
    public String[] getSensorData(String sensorName){
        return this.sensores.get(sensorName);
    }*/
    
    public Boolean getAlive() {
        return alive;
    }

    public Boolean getOnTarget() {
        return ontarget;
    }

    public ArrayList<Integer> getGPS() {
        return gps;
    }

    public Integer getCompass() {
        return compass;
    }

    public Integer getPayload() {
        return payload;
    }

    public Float getDistance() {
        return distance;
    }

    public Integer getAngular() {
        return angular;
    }

    public Integer getAltimeter() {
        return altimeter;
    }

    public ArrayList<Integer> getVisual() {
        return visual;
    }

    public ArrayList<ArrayList<Integer>> getLidar() {
        return lidar;
    }

    public ArrayList<ArrayList<Integer>> getThermal() {
        return thermal;
    }

    public Integer getEnergy() {
        return energy;
    }
    
    
    
}