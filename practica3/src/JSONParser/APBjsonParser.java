
package JSONParser;

import MapOption.Coordinates;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.ArrayList;

/**
 * Clase APBjsonParser para parsear los datos que recibe APB. Herada del JSONParser
 * @author Manuel Pancorbo
 */
public class APBjsonParser extends JSONParser{
    
    /**
     * Obtiene las COINS
     * @param object Json
     * @author Manuel Pancorbo
     * @return string
     *
     */
    public ArrayList<String> getMoney(JsonArray object){
       return convertToStringArray(object);
    }
    
    /**
     * Parsea las COINS
     * @param money Arraylist de Strings
     * @author Manuel Pancorbo
     * @return JsonArray
     *
     */
    public JsonArray parseMoney(ArrayList<String> money){
        return convertStringArrayToJSONArray(money);
    }

    /**
     * Convierte a Coordenadas las posiciones de los alemanes
     * @param object Json
     * @author Manuel Pancorbo
     * @return Coordenadas
     *
     */
    public Coordinates getAleman(JsonObject object){
        int posX = object.get("x").asInt();
        int posY = object.get("y").asInt();
        return new Coordinates(posX, posY);
    }

}
