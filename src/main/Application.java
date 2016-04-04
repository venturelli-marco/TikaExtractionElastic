package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import batch.TikaBatch;

/**
 * Application main class
 * @author marco
 *
 */
public class Application {
	private final static String log4j_configurationFile = "resources/log4j2.xml";
	private static final String _pathname = "resources";
	private static String _filename = "app-config.json";
	private static String input;
	private static String output;
	
	private static boolean uuid = false;
	private static boolean save = false;
	private static boolean discard = false;
	private static boolean index = true;
	private static boolean search = false;
	private static boolean regex = false;
	
	private static List<String> keywords = null;
	
	private static File config=null;
	
	public static JsonObject JSONconfig;
	
	// -c resources/app-config.json -i test -o test_out --uuid -d

	public static void main(String[] args) throws Exception{
		
		readArgLine(args);
		
		JsonObject JSONconfig;
		if(config==null)
			JSONconfig = loadConfig(new File(_pathname, _filename));
		else
			JSONconfig = loadConfig(config);
		
		// if regex flag run only regex search
		if(regex && search){
			new TikaBatch(JSONconfig);
			Thread.sleep(100);
			new TikaBatch(JSONconfig, keywords);
			return;
		}
		
		// if regex flag run only regex search
		if(regex){
			new TikaBatch(JSONconfig);
			return;
		}
		
		// if search flag run only elasticSearch 
		if(search){
			new TikaBatch(JSONconfig, keywords);
			return;
		}
		
		File in = (input==null) ? null : new File(input);
		File out = (output==null) ? null : new File(output);
		
		new TikaBatch(JSONconfig, in, save, out, uuid, discard, index);
	}
	
	/**
	 * Read and parse command line
	 * @param args
	 */
	public static void readArgLine(String[] args){
		// create Options object
		Options options = new Options();		
		
		options.addOption("c", "config", true, "Configuration file path");
		options.addOption("i", "input", true, "Input file or directory");
		options.addOption("o", "output", true, "Save Tika extracted JSON content into a file under given directory");
		options.addOption("d", "clear-elastic", false, "Discard ElasticSearch index before extraction");
		options.addOption("u", "uuid", false, "Save Tika extracted JSON content into a file with uuid as name");
		options.addOption("x", "not-index",false, "Not index the Extracted content");
		options.addOption("r", "regex",false, "Search for given regular expression");
		options.addOption("h", "help",false, "Print help");
		
		options.addOption(Option.builder("s")
				.required(false)
				.hasArgs()
				.longOpt("search")
				.optionalArg(true)
				.valueSeparator(',')
				.desc("Search with ElasticSearch for the given values")
				.build());
		
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse( options, args);
			
			if(cmd.hasOption("help")){
				printHelp(options);
				System.exit(0);
			}
				
			
			if(cmd.hasOption("config")) 
			    config = new File(cmd.getOptionValue("config"));
									
			if(cmd.hasOption("input")) {
			    // initialise the member variable
			    input = cmd.getOptionValue( "input" );
			}else if(!cmd.hasOption("search"))
				// if input or search are not found throw an exception
				throw new ParseException("Missing input file/dir parameter");
			
			if(cmd.hasOption("output")) {
			    // initialise the member variable
				save = true;
			    output = cmd.getOptionValue( "output" );
			}else
				save = false;
			
			if(cmd.hasOption("uuid"))			    
			    uuid = true;
			
			if(cmd.hasOption("clear-elastic"))			    
			    discard = true;
			
			if(cmd.hasOption("not-index"))			    
			    index = false;
			
			if(cmd.hasOption("regex"))			    
			    regex = true;
			
			if(cmd.hasOption("search")){
				search=true;
				String[] keys = cmd.getOptionValues("search");
				if(keys!=null)
					keywords = Arrays.asList(keys);
				else
					keywords = new ArrayList<String>(); 
			}
				
			
			
		} catch (ParseException e) {
			System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
			// automatically generate the help statement
			printHelp(options);
			System.exit(1);
		}
	}
	
	
	private static void printHelp(Options options){
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( "TikaExtractionElastic", options );
	}
	
	/**
	 * Load application configuration into a JsonObject from a given file
	 * @param config
	 * @return
	 */
	public static JsonObject loadConfig(File config){
		JsonObject JSONconfig=null;
		
		try(Reader reader = new FileReader(config)){

			JsonElement element = new JsonParser().parse(reader);
			JSONconfig = element.getAsJsonObject();
		
		}catch(FileNotFoundException e){
			System.err.println(e.getMessage());
			System.exit(1);
		}catch(JsonSyntaxException e){
			System.err.println(e.getMessage());
			System.exit(1);
		}catch (Exception e) {
			System.err.println(e.toString());
			JSONconfig = new JsonObject();
		}
		
		// set system property in order to configure properly log4j environment
		if(JSONconfig.has("log4j.configurationFile"))
			System.setProperty("log4j.configurationFile", JSONconfig.get("log4j.configurationFile").getAsString());
		else if (new File(log4j_configurationFile).exists())
			System.setProperty("log4j.configurationFile", log4j_configurationFile);
				
		Application.JSONconfig = JSONconfig;
		
		return JSONconfig;
	}
	
	

}
