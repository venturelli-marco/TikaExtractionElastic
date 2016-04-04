package alert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.arabidopsis.ahocorasick.SearchResult;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Class to create an alert object from the result of a keyword research
 * @author marco
 *
 */
public class ParseTikaJsonObject {
	// interesting properties
	private String sha;
	private String name;
	private String host;
	// map with key=keyword and value=list of index
	private Map<String, List<Integer>> matches;

	public ParseTikaJsonObject(String json, Iterator<SearchResult> searcher){
		
		// parse Json object to extract property
		JsonElement element = new JsonParser().parse(json);
		JsonObject  jobject = element.getAsJsonObject();

		sha = jobject.get("Content-SHA1").getAsString();
		name = jobject.get("resourceName").getAsString();
		host = jobject.get("hostname").getAsString();
		
		matches = new HashMap<String, List<Integer>>();
		while (searcher.hasNext()) {
			SearchResult result = (SearchResult) searcher.next();
			
			String key = result.getOutputs().toArray()[0].toString();
			
			List<Integer> l;
			if(matches.get(key)==null)
				l = new ArrayList<Integer>();
			else
				l=matches.get(key);
			l.add(result.getLastIndex());			
			matches.put(key, l);
		}
	}
	
	public String getShaDigest(){return sha;}
	public String getResourceName(){return name;}
	public String getHostName(){return host;}
	
	/**
	 * Create a string representation of the object
	 * @return
	 */
	public String getMessage(){
		StringBuilder res = new StringBuilder();		
		for (Map.Entry<String, List<Integer>> entry : matches.entrySet()) {
			res.append("\n\t\t\"" + entry.getKey() + "\"");
			res.append(" (index " + entry.getValue() + ")");
		}

		String info = "\nFound a match for: " + res.toString() + "\n"
				+ "\tContent-SHA1: "+ sha +"\n"
				+ "\tresourceName: "+ name + "\n"
				+ "\thostname: "+ host;
		return info;
	}
	
	public Map<String, List<Integer>> getKeywords(){return matches;}
	
	@Override
	public String toString(){
		return getMessage();
	}
}
