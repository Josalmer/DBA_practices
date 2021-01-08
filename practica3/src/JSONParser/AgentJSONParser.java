
package JSONParser;

import com.eclipsesource.json.*;
import java.util.ArrayList;

/**
 * Clase AgenJSONParser para los agentes que hereda del JSONParser
 * @author Manuel Pancorbo
 */
public class AgentJSONParser extends JSONParser {
    
    /**
     * Obtiene los datos recibidos y los parsea
     * @param object Json
     * @author Manuel Pancorbo
     * @return Array de datos
     *
     */
    private JsonValue getData(JsonObject object){
         return object.get("data").asArray().get(0);
    }
    
    /**
     * Obtiene los datos del sensor como enteros
     * @param object Json
     * @author Manuel Pancorbo
     * @return Integer del valor del sensor
     *
     */
    public int getIntSensorData(JsonObject object){
        return this.getData(object).asInt();
    }
    
    /**
     * Obtiene los datos del sensor como doubles
     * @param object Json
     * @author Manuel Pancorbo
     * @return Double del valor del sensor
     *
     */
    public Double getDoubleSensorData(JsonObject object){
        return getData(object).asDouble();
    }
    
    /**
     * Obtiene los datos del sensor como cadena string
     * @param object Json
     * @author Manuel Pancorbo
     * @return Cadena string con los datos del sensor
     *
     */
    public String getStringSensorData(JsonObject object){
        return getData(object).asString();
    }
    
    /**
     * Obtiene los datos del mapa y los convierte a una matriz de enteros
     * @param map JsonArray
     * @author Manuel Pancorbo
     * @return Matriz de enteros
     *
     */
    public ArrayList<ArrayList<Integer>> getMap(JsonArray map){
        return convertToIntegerMatrix(map);
    }
    
    /**
     * Parsea el mapa. Convierte la matriz de enteros a JSONArray
     * @param map Matriz de Integers
     * @author Manuel Pancorbo
     * @return JsonArray del mapa
     *
     */
    @Override
    public JsonArray parseMap (ArrayList<ArrayList<Integer>> map){
        return convertIntegerMatrixtoJSONArray(map);
    }
    
    /**
     * Obtiene el plan como JsonObject y lo transforma a un JsonArray
     * @param object Json
     * @author Manuel Pancorbo
     * @return JsonArray del plan
     *
     */
    public ArrayList<Integer> getPlan(JsonObject object){
        ArrayList<Integer> plan = new ArrayList<>();
        JsonArray array = object.get("plan").asArray();
        for(int i=0; i < array.size(); i++){
            JsonObject coordinates = array.get(i).asObject();
            Integer x = coordinates.get("x").asInt();
            Integer y = coordinates.get("y").asInt();
            
            plan.add(x);
            plan.add(y);
        }
        return plan;
    }
    
    
    
}
