package batch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.index.IndexResponse;
import org.json.JSONException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import elastic.ElasticSearchClient;
import tika.TIKAContentExtraction;

/**
 * Launch application
 * @author marco
 *
 */
public class TikaBatch {
	private static Logger logger = LogManager.getRootLogger();
	private File input;
	private boolean save;
	private File output;
	private boolean uuid;
	private boolean clear;
	private boolean index;
	
	private ElasticSearchClient elasticClient = null;
	private TIKAContentExtraction tika = null;
	private MatchKeyword keywords = null;
		
	/**
	 * Extract content from file/s under input using Tika, search for given keywords and index the document using elasticSearch
	 * @param JSONconfig
	 * @param input file or directory
	 * @param save
	 * @param output
	 * @param uuid
	 * @param clear
	 * @param index
	 */
	public TikaBatch(JsonObject JSONconfig, File input, boolean save, File output, boolean uuid, boolean clear, boolean index){
		elasticClient = null;
		tika = null;
		keywords = null;
		
		this.input = input;
		this.output = output;
		this.uuid = uuid;
		this.save = save;
		this.clear = clear;
		this.index = index;
		
		try{
			// initiate tika from configuration
			tika = initTIKA(JSONconfig);
			// initiate elastic from configuration
			elasticClient = initElastic(JSONconfig);
			// initiate MatchKeyword from configuration
			keywords = initKeyword(JSONconfig);
			
			// discard index
			if(this.clear)
				elasticClient.deleteIndex(true);
			
			// extract all the files under input directory
			batchExtraction();
		}catch(Exception ex){
			System.err.println(ex.getMessage());
			ex.printStackTrace();
		}finally{
			if(elasticClient!=null){
				try{
					Thread.sleep(1000);
					elasticClient.stopClient();
				}catch(Exception ex){}
			}

		}
	}
	
	/**
	 * Search using elasticSearch for the given regular expression
	 * @param JSONconfig
	 * @param keys
	 */
	public TikaBatch(JsonObject JSONconfig){
		elasticClient = null;
						
		try{						
			elasticClient = initElastic(JSONconfig);
			
			// limit fields results
			String[] fields = new String[]{"resourceName", "Content-SHA1", "hostname"};
						
			Map<String, String> regex = getRegexp(JSONconfig);
					
			for(String key: regex.keySet()){
				String reg = regex.get(key);
								
				System.out.print("\nMatch for \""+key+"\"  ");
				elasticClient.searchDocumentRegexpQuery(reg, true, true, fields);
				
				System.out.print("Match for \""+key+"\" recursively  ");
				elasticClient.searchDocumentRegexpQueryRecursive(reg, true, true, fields);
			}
			
		}catch(Exception ex){
			System.err.println(ex.getMessage());
			ex.printStackTrace();
		}finally{
			if(elasticClient!=null){
				try{
					Thread.sleep(1000);
					elasticClient.stopClient();
				}catch(Exception ex){}
			}

		}
	}
	
	/**
	 * Search using elasticSearch for the given keywords
	 * @param JSONconfig
	 * @param keys
	 */
	public TikaBatch(JsonObject JSONconfig, List<String> keys){
		elasticClient = null;
		ArrayList<String> keywords = new ArrayList<String>();;
 		if(keys!=null)
			keywords = new ArrayList<String>(keys);
				
		try{
						
			elasticClient = initElastic(JSONconfig);
			
			// limit fields results
			String[] fields = new String[]{"resourceName", "Content-SHA1", "hostname"};
						
			List<String> nkeys = getKeywords(JSONconfig);
			
			// merge given keywords and keywords from config file
			if(nkeys!=null && nkeys.size()>0){
				// keywords.addAll(nkeys);
				for(String key: nkeys)
					if(!keywords.contains(key))
						keywords.add(key);
			}
			
			for(String key: keywords){
				System.out.print("\nMatch for \""+key+"\"  ");
				elasticClient.searchDocumentSimpleQuery(key, true, true, fields);
			}
			
		}catch(Exception ex){
			System.err.println(ex.getMessage());
			ex.printStackTrace();
		}finally{
			if(elasticClient!=null){
				try{
					Thread.sleep(1000);
					elasticClient.stopClient();
				}catch(Exception ex){}
			}

		}
	}
	
	/**
	 * Initialize tika from configuration
	 * @param JSONconfig
	 * @return
	 */
	private TIKAContentExtraction initTIKA(JsonObject JSONconfig){
		TIKAContentExtraction tika = null;
		try{
			// if no tika config use default
			if(!JSONconfig.has("TIKA"))
				return new TIKAContentExtraction();				 
			JsonObject config = JSONconfig.get("TIKA").getAsJsonObject();

			try{
				// look for config file
				if(config.has("tika-config-file")){
					File confFile = new File(config.get("tika-config-file").getAsString());
					if(!confFile.exists()){
						//System.err.println("TIKA configuration file not found: use default configuration");
						//logger.error("TIKA configuration file not found: use default configuration");
						throw new FileNotFoundException("TIKA configuration file not found");
					}
					tika = new TIKAContentExtraction(confFile);
				}

				// use values from configuration
				if(tika==null){
					if(!(config.has("disableOCR") && config.has("recursiveJSON") 				 
							&& config.has("maxOutputFile") && config.has("maxEmbeddedFile") && config.has("maxInputRecursiveFile")))
						throw new JSONException("Missing TIKA configuration parameter");

					int maxOutputFile = config.get("maxOutputFile").getAsInt();
					int maxEmbeddedFile = config.get("maxEmbeddedFile").getAsInt();
					int maxInputRecursiveFile = config.get("maxInputRecursiveFile").getAsInt();

					boolean recursiveJSON = config.get("recursiveJSON").getAsBoolean();
					boolean disableOCR = config.get("disableOCR").getAsBoolean();

					tika = new TIKAContentExtraction(disableOCR, recursiveJSON, maxOutputFile, maxEmbeddedFile, maxInputRecursiveFile);

					if(config.has("tesseractOCR")){
						JsonObject tesseractConf = config.get("tesseractOCR").getAsJsonObject();
						String tessData = tesseractConf.get("TessdataPath").getAsString();
						String tessPath = tesseractConf.get("TesseractPath").getAsString();
						String language = tesseractConf.get("language").getAsString();
						tika.OCRConfig(language, tessData, tessPath);
					}
				}

			}catch(Exception ex){
				System.err.println(ex.getMessage() + ": TIKA initiate with default settings");
				logger.warn(ex.getMessage() + " : TIKA initiate with default settings");
				tika = new TIKAContentExtraction();
			}


			// logging configuration
			if(config.has("log-properties-file")){
				File logFile = new File(config.get("log-properties-file").getAsString());
				tika.setupLog(logFile);
			}else
				tika.setupLog();

		}catch(Exception ex){
			System.err.println(ex.getMessage());
			logger.error(ex.getMessage());
			System.exit(1);
		}
		return tika;
	}
	
	/**
	 * Initialize MatchKeyword from configuration
	 * @param JSONconfig
	 * @return
	 */
	private MatchKeyword initKeyword(JsonObject JSONconfig){
		MatchKeyword keywords = null;
		
		try{
			if(!JSONconfig.has("alert-config"))
				return new MatchKeyword(getKeywords(JSONconfig));
			
			JsonObject config = JSONconfig.get("alert-config").getAsJsonObject();
			keywords = new MatchKeyword(getKeywords(JSONconfig), config);
		}catch(Exception ex){
			System.err.println(ex.getMessage());
			logger.error(ex.getMessage());
			System.exit(1);
		}
		return keywords;
	}
	
	/**
	 * Initialize elasticSearch client from configuration
	 * @param JSONconfig
	 * @return
	 */
	private ElasticSearchClient initElastic(JsonObject JSONconfig){
		ElasticSearchClient elastic = null;
		
		try{
			if(!JSONconfig.has("elasticSearch"))
				throw new JsonSyntaxException("Config file malformed: missing 'elasticSearch' field ");
			JsonObject config = JSONconfig.get("elasticSearch").getAsJsonObject();
							
			elastic = new ElasticSearchClient(config);
			
		}catch(Exception ex){
			System.err.println(ex.getMessage());
			logger.error(ex.getMessage());
			System.exit(1);
		}finally{
			try{Thread.sleep(500);}catch(InterruptedException e){}
		}
		return elastic;
	}

	/**
	 * read keywords from configuration
	 * @param JSONconfig
	 * @return
	 */
	private List<String> getKeywords(JsonObject JSONconfig){
		ArrayList<String> keywords;
		try{
			Gson gson = new Gson();		
			
			if(!JSONconfig.has("keywords"))
				throw new JsonSyntaxException
				("Config file malformed: missing 'keywords' field ");

			if(!JSONconfig.get("keywords").isJsonArray())
				throw new JsonSyntaxException
				("Config file malformed: 'keywords' field is not an array");
			JsonArray keys = JSONconfig.get("keywords").getAsJsonArray();

			keywords = gson.fromJson(keys, new TypeToken<List<String>>(){}.getType());
		}catch(Exception e){
			System.err.println(e.toString());
			logger.error(e.toString());
			keywords = new ArrayList<String>();
			//throw e;
		}
		
		return keywords;
	}
	
	/**
	 * read regular expression from configuration
	 * @param JSONconfig
	 * @return
	 */
	//TODO: remove public static
	public static Map<String, String> getRegexp(JsonObject JSONconfig){
		Map<String, String> regexp;
		try{			
			if(!JSONconfig.has("regexp"))
				throw new JsonSyntaxException
				("Config file malformed: missing 'regexp' field ");

			if(!JSONconfig.get("regexp").isJsonObject())
				throw new JsonSyntaxException
				("Config file malformed: 'regexp' field is not an object");
			
			JsonObject regex = JSONconfig.get("regexp").getAsJsonObject();
			
			regexp = new Gson().fromJson(regex, new TypeToken<LinkedTreeMap<String, String>>(){}.getType());
			
		}catch(Exception e){
			System.err.println(e.toString());
			logger.error(e.toString());
			regexp = new LinkedTreeMap<String, String>();
			//throw e;
		}
		
		return regexp;
	}
	
	private void batchExtraction() throws Exception{
		// create folder for intermediate json files
		if(save && !output.exists())
			output.mkdirs();

		// check if input exists
		if(!input.exists())
			return;
		
		ArrayList<File> files;
		
		// read all files
		if(input.isDirectory()){
			files = new ArrayList<File>(FileUtils.listFiles(input, TrueFileFilter.TRUE, TrueFileFilter.TRUE));
			//files = new ArrayList<File>(Arrays.asList(input.listFiles()));
		}
		else{
			files = new ArrayList<File>();
			files.add(input);
		}
				
		// for each file
		System.out.println("----- Begin extraction -----");
		long startTime = System.nanoTime();
		for(File file: files){			
			try{
				if(file.isDirectory()) continue;			

				String start_msg = "Extraction from file: " + file.toString() + " ";
				System.out.print(start_msg);
				long fileStartTime = System.nanoTime();

				String fileOutput;
				if(uuid)
					fileOutput = "TIKA_" + getBase64UUID();
				else
					fileOutput = file.getName()+"_TIKA";


				// extract content and metadata 
				//tika.extractToJSONFile(file, new File(outputPath, fileOutput));
				byte[] json = tika.extractToJSONByteArray(file);

				// search inside document
				boolean findKeys = keywords.searchKeywords(json);
				if(findKeys){
					String sJson = new String(json, StandardCharsets.UTF_8);
					// call module to generate alert
					keywords.generateAlert(sJson);
				}

				// index with elasticSearch
				IndexResponse response=null;
				if(index)
					response = elasticClient.indexDocument(null, json, false);

				if(save) toFile(json, new File(output, fileOutput));

				long fileEndTime = System.nanoTime();
				long fileDuration = (fileEndTime-fileStartTime)/1000000;
				String index = "[index not created]";
				if(response!=null)
					index = "\t Index: " + response.getId();
				String stop_msg="\t\t[COMPLETED "+fileDuration+ " msec]";
				System.out.println(index+stop_msg);

				tika.log(start_msg+index+stop_msg);
			}catch(Exception ex){
				String msg = "Something went wrong during processing file: "+file.toString();
				System.err.println(msg);
				tika.log(msg);
			}						
		}//end batch
		long endTime = System.nanoTime();
		long duration = (endTime-startTime)/1000000;
		System.out.println("--------------------------------------------------");
		System.out.println("Extraction and indexing of "+ files.size() +" completed in "+ duration  +"msec");
		System.out.println("--------------------------------------------------");
		tika.log("Extraction and indexing of "+ files.size() +" completed in "+ duration  +"msec");
	}
	
	private static String getBase64UUID(){
		String uuid = UUID.randomUUID().toString();
		return Base64.encodeBase64String(uuid.getBytes());		
	}
	
	private static void toFile(byte[] json, File output){
		try(FileOutputStream oStream = new FileOutputStream(output)){
			oStream.write(json);
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	
}
