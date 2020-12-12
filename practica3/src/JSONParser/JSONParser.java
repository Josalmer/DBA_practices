/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package JSONParser;

import Map2D.Map2DGrayscale;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import java.util.ArrayList;

/**
 *
 * @author manuel
 */
public class JSONParser {
    
    public ArrayList<ArrayList<Integer>> getMap(JsonObject map){
        Map2DGrayscale myMap = new Map2DGrayscale();
        ArrayList<ArrayList<Integer>> arrayMap = new ArrayList<>();
        if(myMap.fromJson(map)){    
            for(int i=0; i<myMap.getWidth();i++){           
                ArrayList<Integer> row = new ArrayList<>();
                for(int j=0; j<myMap.getHeight(); j++){
                    row.add(myMap.getLevel(i, j));
                }
                arrayMap.add(row);            
            }        
        }
        return arrayMap;
    }
    
    public JsonArray parseMap (ArrayList<ArrayList<Integer>> map){
        return convertIntegerMatrixtoJSONArray(map);
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
    
    
    public ArrayList<String> convertToStringArray(JsonArray array) {
        ArrayList<String> strings = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            strings.add(array.get(i).asString());
        }
        return strings;
    }
    
    
    public JsonArray convertIntegerArrayToJSONArray(ArrayList<Integer> array){
        JsonArray jsonArray  = new JsonArray();     
        for(Integer i : array){
            jsonArray.add(i);
        }
       return jsonArray;
    }
    
    
    public JsonArray convertIntegerMatrixtoJSONArray(ArrayList<ArrayList<Integer>> array){
        JsonArray matrix = new JsonArray();
       
        for(int i=0; i<array.size(); i++){
            JsonArray jsonArray = convertIntegerArrayToJSONArray(array.get(i));
            matrix.add(jsonArray);
        }
        
        return matrix;
    }
    
    public JsonArray convertStringArrayToJSONArray(ArrayList<String> array){
        JsonArray jsonArray = new JsonArray();
        for(String s : array){
            jsonArray.add(s);
        }
        return jsonArray;
    }
    
   
}
