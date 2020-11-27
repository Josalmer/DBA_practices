
package JSONParser;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.ArrayList;

/**
 *
 * @author manuel
 */
public class APBjsonParser extends JSONParser{
    
    public ArrayList<String> getMoney(JsonObject object){
       return convertToStringArray(object.asArray());
    }
    
    public JsonArray parseMoney(ArrayList<String> money){
        return convertStringArrayToJSONArray(money);
    }
}
