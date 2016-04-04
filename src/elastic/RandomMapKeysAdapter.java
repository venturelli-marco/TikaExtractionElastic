package elastic;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class RandomMapKeysAdapter implements JsonDeserializer<Map<String, Object>>
{
	@Override
	public Map<String, Object> deserialize(JsonElement json, Type unused, JsonDeserializationContext context)
			throws JsonParseException
	{
		// if not handling primitives, nulls and arrays, then just 
		if (!json.isJsonObject()) throw new JsonParseException("some meaningful message");

		Map<String, Object> result = new HashMap<String, Object> ();
		JsonObject jsonObject = json.getAsJsonObject();
		for (Entry<String, JsonElement> entry : jsonObject.entrySet())
		{
			String orKey = entry.getKey();
			String key = orKey;
			if(orKey.contains("."))
				key = orKey.replace('.',':');			
			
			JsonElement element = entry.getValue();
			if (element.isJsonPrimitive())
			{
				result.put(key, element.getAsString());
			}
			else if (element.isJsonObject())
			{
				result.put(key, context.deserialize(element, unused));
			}
			else if (element.isJsonArray())
			{
				JsonArray array = element.getAsJsonArray();
				//JsonArray newArray = new JsonArray();
				ArrayList<Object> newArray = new ArrayList<Object>();
				
				for(JsonElement childElement : array){
					if (childElement.isJsonPrimitive()){
						newArray.add(childElement);
					}else{
						Object res = context.deserialize(childElement, unused);
												
						newArray.add(res);
					}
				}
				result.put(key, newArray);
			}
			// if not handling nulls and arrays
			else
			{
				throw new JsonParseException("some meaningful message");
			}
		}
		return result;
	}
}