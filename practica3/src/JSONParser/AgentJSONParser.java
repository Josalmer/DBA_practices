
package JSONParser;

import com.eclipsesource.json.*;
import java.util.ArrayList;

/**
 *
 * @author manuel
 */
public class AgentJSONParser extends JSONParser {
    
    private JsonValue getData(JsonObject object){
         return object.get("data").asArray().get(0);
    }
    
    public int getIntSensorData(JsonObject object){
        return this.getData(object).asInt();
    }
    
    public Double getDoubleSensorData(JsonObject object){
        return getData(object).asDouble();
    }
    
    public String getStringSensorData(JsonObject object){
        return getData(object).asString();
    }
    
    public ArrayList<ArrayList<Integer>> getMap(JsonArray map){
        return convertToIntegerMatrix(map);
    }
    
    public JsonArray parseMap (ArrayList<ArrayList<Integer>> map){
        return convertIntegerMatrixtoJSONArray(map);
    }
    
    //Como me lo pidas migue pero bueno aqui se puede cambiar
    //Ahora mismo mete {x1,y1,x2,y2,x3,y3,..}
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
