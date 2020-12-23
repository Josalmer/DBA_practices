
package JSONParser;

import MapOption.Coordinates;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.ArrayList;

/**
 *
 * @author manuel
 */
public class APBjsonParser extends JSONParser{
    
    public ArrayList<String> getMoney(JsonArray object){
       return convertToStringArray(object);
    }
    
    public JsonArray parseMoney(ArrayList<String> money){
        return convertStringArrayToJSONArray(money);
    }

    public Coordinates getAleman(JsonObject object){
        int posX = object.get("x").asInt();
        int posY = object.get("y").asInt();
        return new Coordinates(posX, posY);
    }

}
