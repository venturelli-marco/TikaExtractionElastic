package batch;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.layout.PatternSelector;
import org.apache.logging.log4j.core.pattern.RegexReplacement;

//import org.apache.log4j.ConsoleAppender;
//import org.apache.log4j.Level;
//import org.apache.log4j.LogManager;
//import org.apache.log4j.Logger;
//import org.apache.log4j.PatternLayout;
//import org.apache.log4j.PropertyConfigurator;

import org.arabidopsis.ahocorasick.AhoCorasick;
import org.arabidopsis.ahocorasick.SearchResult;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import alert.AlertLogger;
import alert.AlertToDB;
import alert.ParseTikaJsonObject;
import utils.StringSearchUsingAhoCorasickAlgo;

public class MatchKeyword {
	//private static final Logger logger = Logger.getLogger(MatchKeyword.class.getName());
	//private static Logger logger = LogManager.getLogger(MatchKeyword.class.getName());
		
	private List<String> keywords;
	private Iterator<SearchResult> searcher;
	private Map<String, Boolean> mode;
	private AlertToDB alertDB;
	
	public MatchKeyword(List<String> keywords){
		searcher = null;
		this.keywords = keywords;
		mode = new HashMap<String, Boolean>();
		mode.put("log4j", true);
		mode.put("LOG", false);
		mode.put("DB", false);
	}
	
	public MatchKeyword(List<String> keywords, JsonObject config) throws Exception{
		searcher = null;
		this.keywords = keywords;
		
		mode = new HashMap<String, Boolean>();
			
		// parse MODE field
		if(!config.has("MODE") || !config.get("MODE").isJsonArray())
			throw new JsonSyntaxException
			("Config file malformed: 'MODE' field is not an array");
		JsonArray jmodes = config.get("MODE").getAsJsonArray();
		
		Gson gson = new Gson();	
		ArrayList<String> modes = gson.fromJson(jmodes, new TypeToken<List<String>>(){}.getType());
		
		boolean value;
		// accepted values
		value = (modes.contains("log4j")) ? true : false;
		mode.put("log4j", value);
		value = (modes.contains("LOG")) ? true : false;
		mode.put("LOG", value);
		value = (modes.contains("DB")) ? true : false;
		mode.put("DB", value);
		
		// new log4j configuration 
		if(mode.get("LOG") && !mode.get("log4j"))
			configureNewLog(config.get("LOG").getAsJsonObject());
		// add properties to current log4j configuration
		if(mode.get("LOG") && mode.get("log4j"))
			configureLog(config.get("LOG").getAsJsonObject());
		if(mode.get("DB"))
			alertDB = new AlertToDB(config.get("DB").getAsJsonObject());			
		
	}
	
	/** 
	 * Search for keywords using String indexOf	
	 * @param s
	 * @return
	 */
	public boolean findMatches(String s){
		for(String key: keywords){
			if(s.indexOf(key) != -1)
				return true;
		}
		return false;
	}
		
	@Deprecated
	public boolean findMatches2(String s){
		StringSearchUsingAhoCorasickAlgo ahoCorasick = new StringSearchUsingAhoCorasickAlgo(1000);
		return ahoCorasick.findPattern(s, keywords.toArray(new String[keywords.size()]));
	}
	
	/**
	 * Search inside given string for the keywords using AhoCorasick algorithm
	 * @param s
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean findMatchesAhoCorasick(String s){
		try{
			AhoCorasick tree = new AhoCorasick();

			for(String key: keywords){
				tree.add(key.getBytes(), key);
				tree.add(key.getBytes(), key);
			}
			tree.prepare();

			searcher = tree.search(s.getBytes());		
			if(searcher.hasNext())
				return true;
		}catch(Exception ex){
			System.err.println("Error during searching keywords: "+ex.getMessage());
			return false;
		}
		return false;
	}
	
	/**
	 * Search inside given string for the keywords using AhoCorasick algorithm 
	 * @param s
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean findMatchesAhoCorasick(byte[] s){
		try{
			AhoCorasick tree = new AhoCorasick();

			for(String key: keywords){
				tree.add(key.getBytes(), key);
				tree.add(key.getBytes(), key);
			}
			tree.prepare();

			searcher = tree.search(s);		
			if(searcher.hasNext())
				return true;
		}catch(Exception ex){
			System.err.println("Error during searching keywords: "+ex.getMessage());
			return false;
		}
		return false;
	}
	
	/**
	 * Search inside given string for the keywords
	 * @param s
	 * @return
	 */
	public boolean searchKeywords(byte[] s){
		return findMatchesAhoCorasick(s);
	}
	
	/**
	 * Search inside given string for the keywords
	 * @param s
	 * @return
	 */
	public boolean searchKeywords(String s){
		return findMatchesAhoCorasick(s);
	}
	
	/**
	 * Generate appropriate alert
	 * @param json
	 */
	public void generateAlert(String json){
		try{
			ParseTikaJsonObject results = new ParseTikaJsonObject(json, searcher);

			if(mode.get("log4j") || mode.get("LOG"))
				AlertLogger.log(results);
			if(mode.get("DB"))
				alertDB.insertEntry(results);			
		}catch(Exception ex){
			System.err.println("Error during alert generation: "+ex.getMessage());
		}
	}
	
	/**
	 * Overwrite log4j configuration with new given configuration	
	 * @param config
	 */
 	private void configureNewLog(JsonObject config){
		
		Level level=null;
		String pattern = PatternLayout.SIMPLE_CONVERSION_PATTERN;
		String fileName = null;
		boolean console = false;
		boolean append = false;
		
		try{
			if(config.has("level"))
				level = Level.getLevel(config.get("level").getAsString());
			if(level==null)
				level = Level.getLevel("INFO");

			if(config.has("console"))
				console = config.get("console").getAsBoolean();			
			if(config.has("fileName"))
				fileName = config.get("fileName").getAsString();
			if(config.has("pattern"))
				pattern = config.get("pattern").getAsString();	
			if(config.has("append"))
				append = config.get("append").getAsBoolean();

		}catch(Exception ex){
			level = Level.INFO;
			pattern = PatternLayout.SIMPLE_CONVERSION_PATTERN;
			console = false;
			fileName = null;
			append = false;
		}
		
		
		ConfigurationBuilder<BuiltConfiguration> builder = 
				ConfigurationBuilderFactory.newConfigurationBuilder();
		builder.setStatusLevel(Level.ERROR);
		builder.setConfigurationName("BuilderTest");
		builder.add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.NEUTRAL)
		    .addAttribute("level", Level.DEBUG));
		
		AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target",
		    ConsoleAppender.Target.SYSTEM_ERR);
		appenderBuilder.add(builder.newLayout("PatternLayout").
		    addAttribute("pattern", pattern));
		appenderBuilder.add(builder.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL).
		    addAttribute("marker", "FLOW"));
		builder.add(appenderBuilder);
		
		appenderBuilder = builder.newAppender("customFileAppender", "FILE")
				.addAttribute("fileName", fileName)
				.addAttribute("append", append);
		appenderBuilder.add(builder.newLayout("PatternLayout").
				addAttribute("pattern", pattern));
		appenderBuilder.add(builder.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL).
				addAttribute("marker", "FLOW"));
		builder.add(appenderBuilder);
		
		LoggerComponentBuilder myLog = builder.newLogger(AlertLogger.class.getName(), level);
		if(console)
			builder.add(myLog.add(builder.newAppenderRef("Stdout")).addAttribute("additivity", false));
		
		if(fileName!=null && fileName.length()>1)
			builder.add(myLog.add(builder.newAppenderRef("customFileAppender")).addAttribute("additivity", false));
		
		Configurator.initialize(builder.build());
	}

 	/**
 	 * Add given configuration properties to current log4j configuration
 	 * @param jconfig
 	 */
	private void configureLog(JsonObject jconfig){
		Level level=null;
		String pattern = PatternLayout.SIMPLE_CONVERSION_PATTERN;
		String fileName = null;
		boolean console = false;
		boolean append = false;
		
		try{
			if(jconfig.has("level"))
				level = Level.getLevel(jconfig.get("level").getAsString());
			if(level==null)
				level = Level.getLevel("INFO");

			if(jconfig.has("console"))
				console = jconfig.get("console").getAsBoolean();			
			if(jconfig.has("fileName"))
				fileName = jconfig.get("fileName").getAsString();
			if(jconfig.has("pattern"))
				pattern = jconfig.get("pattern").getAsString();
			if(jconfig.has("append"))
				append = jconfig.get("append").getAsBoolean();

		}catch(Exception ex){
			level = Level.INFO;
			pattern = PatternLayout.SIMPLE_CONVERSION_PATTERN;
			console = false;
			fileName = null;
			append = false;
		}
		
		
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
                
        PatternLayout layout = PatternLayout.createLayout(pattern, 
        		(PatternSelector)null, config, (RegexReplacement)null, StandardCharsets.UTF_8, 
        		true, false, "", "");
        
        Appender fileAppender = FileAppender.createAppender(fileName, ""+append, "false", "customFileAppenderUser", "true",
        		"false", "false", "4000", layout, null, "false", null, config);
        fileAppender.start();
        
        Appender consoleAppender = ConsoleAppender.createAppender(layout, (Filter)null, ConsoleAppender.Target.SYSTEM_ERR, 
        		"customConsoleAppenderUser", false, true);
        consoleAppender.start();
                
        LoggerConfig loggerConfig = config.getLoggers().get(AlertLogger.class.getName());
        loggerConfig.setAdditive(false);
        loggerConfig.setLevel(level);
        
        if(console)
        	loggerConfig.addAppender(consoleAppender, level, null);
                
        if(fileName!=null && fileName.length()>1)
        	loggerConfig.addAppender(fileAppender, level, null);
        
        config.addLogger(AlertLogger.class.getName(), loggerConfig);
        ctx.updateLoggers();
	}
}
