package elastic;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.regexpQuery;
import static org.elasticsearch.index.query.QueryBuilders.simpleQueryStringQuery;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.elasticsearch.index.query.QueryBuilders.*;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.lucene.util.automaton.RegExp;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RegexpFlag;
import org.elasticsearch.index.query.RegexpQueryBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class ElasticSearchClient {
	
	private static final String _path_home = System.getProperty("user.home");
	private static final long _sleep = 1500;
	
	private static String _clusterName = "test_cluster";
	private static String _index = "test_index";
//	private static String _index_RE = "test_index_re";
//	private boolean regex_enabled = false;
	private static String _type = "test_type";
	
	Node node;
	Client client;
	
	
	public static void main1(String[] args) throws InterruptedException, IOException, URISyntaxException{
		test();
		//test(args);
		//testDelete(args);
		//testBatch(args);
		//testBatchFile(args);
	
	}
	public static void testBatch(String[] args) throws InterruptedException, IOException, URISyntaxException{
		ElasticSearchClient myClient=null;

		try{
			myClient = new ElasticSearchClient("/home/marco/TesinaSicurezza/elasticsearch-2.1.1");
			//myClient = new ElasticSearchClient();
			
			
			File inputDir = new File("test_out");
			myClient.batchIndexing(inputDir, "mydocuments", "metadata");

			Thread.sleep(1500);
		}finally{
			if(myClient!=null)
				myClient.stopClient();
		}
	}
	
	public static void testBatchFile(String[] args) throws InterruptedException, IOException, URISyntaxException{
		ElasticSearchClient myClient=null;

		try{
			myClient = new ElasticSearchClient(new File("elastic-config.json"));
			
			File inputDir = new File("test_out");
			myClient.batchIndexing(inputDir, "mydocuments", "metadata");

			Thread.sleep(1500);
		}finally{
			if(myClient!=null)
				myClient.stopClient();
		}
	}
	
	public static void test() throws InterruptedException, IOException, URISyntaxException{
		ElasticSearchClient myClient=null;
		try{
			myClient = new ElasticSearchClient("/home/marco/TesinaSicurezza/elasticsearch-2.1.1", 
					"test_cluster", true);

			SearchResponse response;
						
						
			response = myClient
					.searchDocument(null, null, false, false);
			
			String[] fields = new String[]{"resourceName", "Author", "Content-SHA1", "user"};
			printResponse(response, true, fields);

			Thread.sleep(1500);
		}finally{
			if(myClient!=null)
				myClient.stopClient();
		}
	}
	
	public static void test(String[] args) throws InterruptedException, IOException, URISyntaxException{
		ElasticSearchClient myClient=null;
		try{
			myClient = new ElasticSearchClient("/home/marco/TesinaSicurezza/elasticsearch-2.1.1");

						
			//myClient.indexDocument("mydocuments", "metadata2", null, new File("sample2").toURI(), true);
			//myClient.indexDocument(new File("sample2").toURI(), true);
			//myClient.indexDocument("mydocuments", "metadata2", "AVKCrx5Bc7SSdLJOHIR22", new File("sample2").toURI(), true);
			
			//myClient.getDocument("mydocuments", "metadata", "AVJ53iKnwaHfRTzMsLVC");
			
			//myClient.updateDocument("mydocuments", "metadata2", "AVJ-mBBV0vOTBmbO7wn_", new File("sample2").toURI(), true);
			
			//myClient.upsertDocument("mydocuments", "metadata2", "ciaobelli", new File("sample2").toURI(), true);
			//myClient.upsertDocument("mydocuments", "metadata2", "AVKCrx5Bc7SSdLJOHIR22", new File("sample2").toURI(), true);
			
			//myClient.deleteDocument("mydocuments", "metadata2", "AVJ-mBBV0vOTBmbO7wn_", true);
			
			//myClient.deleteIndex("mydocuments", true);
			
			Thread.sleep(1500);			
			
			
			SearchResponse response;
//			response = myClient
//					.searchDocumentSimpleQuery("mydocuments", "metadata", "+Marco +Venturelli", false, false);
//			
//			response = myClient
//					.searchDocumentQuery("mydocuments", "metadata", "(Author:\"marco venturelli\")", false, false);
//			
//			XContentBuilder qb = jsonBuilder()
//					.startObject()
//				    .startObject("simple_query_string")
//				    .field("query", "kimchy")
//				    .endObject()			        
//				    .endObject();
//			response = myClient
//					.searchDocumentQuery("mydocuments", null, qb, false, false);
						
			response = myClient
					.searchDocumentSimpleQuery("mydocuments", "metadata", "image5.emf", false, false, null);
			
			response = myClient
					.searchDocument(null, null, false, false);
			
			String[] fields = new String[]{"resourceName", "Author", "Content-SHA1", "user"};
			printResponse(response, true, fields);

			Thread.sleep(1500);
		}finally{
			if(myClient!=null)
				myClient.stopClient();
		}
	}
	
	public static void testDelete(String[] args) throws InterruptedException, IOException, URISyntaxException{
		ElasticSearchClient myClient=null;
		try{
			myClient = new ElasticSearchClient("/home/marco/TesinaSicurezza/elasticsearch-2.1.1");
			//myClient = new ElasticSearchClient();
			
			myClient.deleteIndex("mydocuments", true);
			
			Thread.sleep(1500);			
			
			
			SearchResponse response = myClient
					.searchDocument(null, null, false, false);
			
			String[] fields = new String[]{"resourceName", "Author", "Content-SHA1", "user"};
			printResponse(response, true, fields);

			Thread.sleep(1500);
		}finally{
			if(myClient!=null)
				myClient.stopClient();
		}
	}
	
	public ElasticSearchClient(){
		this(_path_home);
	}
	
	public ElasticSearchClient(String intallationPath) {
		this(intallationPath, _clusterName, true);
	}
	
	public ElasticSearchClient(File settings){
		this(settings, _clusterName, true);
	}
	
	public ElasticSearchClient(JsonObject config) {

		try{
			
			if(!config.has("path_home"))
				throw new JsonSyntaxException
				("Settings file malformed: missing path_home field ");
			String intallationPath = config.get("path_home").getAsString();
			
			if(config.has("clusterName"))
				_clusterName = config.get("clusterName").getAsString();
			if(config.has("index"))
				_index = config.get("index").getAsString();
			
//			if(config.has("index_RE")){
//				_index_RE = config.get("index_RE").getAsString();
//				regex_enabled = true;
//			}
			
			if(config.has("type"))
				_type = config.get("type").getAsString();
			
			boolean local = true;
			if(config.has("local"))
				local = config.get("local").getAsBoolean();

			node = nodeBuilder()
					.settings(
							Settings.builder()
							.put("path.home", intallationPath))
					.local(local)
					.clusterName(_clusterName)
					.node();	

			client = node.client();
			
			System.out.println("[INFO] ElasticSearch Client started");
			start();
			
			createIndex(true);
			
		}catch(Exception ex){
			if(node!=null && !node.isClosed())
				stopClient();

			System.err.println();
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	private CreateIndexResponse createIndex(boolean verbose) {
		IndicesExistsResponse res = client.admin().indices().prepareExists(_index).execute().actionGet();
		if (res.isExists()) {
            return null;
        }
		CreateIndexResponse response=null;
		
		try{
			String settings = jsonBuilder()
				.startObject()
                    .startObject("settings")
	                    .startObject("analysis")
	                        .startObject("analyzer")
	                            .startObject("email_analyzer")
	                                .field("type","custom")
	                                .field("tokenizer","uax_url_email")
	                                .field("filter", "trim, classic")
	                            .endObject()
	                        .endObject()
	                    .endObject()
                    .endObject()
                
		            
	            	.startObject("mappings")
	            		.startObject(_type)
	            			.startObject("properties")
		            			.startObject("X-TIKA:content")
//				            		.field("type", "binary")
//				            		.field("index", "not_analyzed")
				            		.field("type", "string")
				            		.field("index", "analyzed")
				            		.field("analyzer", "email_analyzer")
		                            .field("search_analyzer", "email_analyzer")				            		
	                            .endObject()
	                            .startObject("Content-Package")
	                            	.field("type", "nested")
	                            	.startObject("properties")
		                            	.startObject("X-TIKA:content")
		                            		.field("type", "string")
						            		.field("index", "analyzed")
						            		.field("analyzer", "email_analyzer")
				                            .field("search_analyzer", "email_analyzer")				            		
			                            .endObject()	
			                        .endObject()
	                            .endObject()
			            	.endObject()
	            		.endObject()
	            	.endObject()
		            
            	.endObject().string();
		
			response = client.admin().indices().prepareCreate(_index).setSource(settings).execute().actionGet();
		
		}catch(Exception ex){
			System.err.println("Index \""+ _index +"\" cannot be created: "+ex.getMessage());
			return null;
		}

		if(verbose){
			System.out.println("Index \""+ _index +"\" successfully created.");
		}
		return response;
	}
	
	public ElasticSearchClient(File settings, String clusterName, boolean local) {
		
		try(Reader reader = new FileReader(settings)){
			JsonElement element = new JsonParser().parse(reader);
			JsonObject  jobject = element.getAsJsonObject();
			
			if(!jobject.has("path_home"))
				throw new JsonSyntaxException
				("Settings file malformed: missing path_home field ");
			
			String intallationPath = jobject.get("path_home").getAsString();
			
			node = nodeBuilder()
					.settings(
							Settings.builder()
							.put("path.home", intallationPath))
					.local(local)
					.clusterName(clusterName)
					.node();	
			
			_clusterName = clusterName;

			client = node.client();
			System.out.println("[INFO] ElasticSearch Client started");
			start();
			
		}catch(Exception ex){
			if(node!=null && !node.isClosed())
				stopClient();
				
			System.err.println();
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	public ElasticSearchClient(String intallationPath, String clusterName, boolean local) {
		node = nodeBuilder()
				.settings(
						Settings.builder()
						.put("path.home", intallationPath))
				.local(local)
				.clusterName(clusterName)
				.node();		
		
		_clusterName = clusterName;
		
		client = node.client();
		System.out.println("[INFO] ElasticSearch Client started");
		start();
	}
	
	/**
	 * start elasticSearchg client
	 */
	private void start(){
		try{
			Thread.sleep(_sleep);
		}catch(InterruptedException ex){}
	}
	
	public static void generateAutoId(){
		Strings.base64UUID();
	}
	
	/**
	 * Index with elasticSearch the given file
	 * @param id
	 * @param uri
	 * @param verbose
	 * @return
	 */
	public IndexResponse indexDocument(@Nullable String id, URI uri, boolean verbose){
		return indexDocument(_index, _type, id, uri, verbose);
	}
	
	/**
	 * Index with elasticSearch the given file
	 * @param index
	 * @param type
	 * @param id
	 * @param uri
	 * @param verbose
	 * @return
	 */
	public IndexResponse indexDocument(String index, String type, @Nullable String id, URI uri, boolean verbose){
		IndexResponse response;
		try{
			Path path = Paths.get(uri);
			byte[] json = Files.readAllBytes(path);
			
			response = client.prepareIndex(index, type, id)
					.setSource(json)
					.get();

		}catch(MapperParsingException ex){
			System.err.println("Error during indexing document: "+ex.getMessage());
			
			if(ex.getMessage().contains("cannot contain '.'")){
				System.err.println("Trying to correct document: "+uri.toString());
				response = correctJSON(index,type,id,uri,verbose);
			}else
				return null;
		}catch(Exception ex){
			System.err.println("Error during indexing document: "+ex.getMessage());
			return null;
		}		

		if(verbose){
			printResponse(response);
		}
		
		return response;
	}
	
	/**
	 * Index with elasticSearch the given file
	 * @param id
	 * @param json
	 * @param verbose
	 * @return
	 */
	public IndexResponse indexDocument(@Nullable String id, byte[] json, boolean verbose){
		
//		if(regex_enabled)
//			indexDocument(_index_RE, _type, id, json, verbose);
		
		return indexDocument(_index, _type, id, json, verbose);
	}
	
	/**
	 * Index with elasticSearch the given file
	 * @param index
	 * @param type
	 * @param id
	 * @param json
	 * @param verbose
	 * @return
	 */
	public IndexResponse indexDocument(String index, String type, @Nullable String id, byte[] json, boolean verbose){
		IndexResponse response;
		try{
			response = client.prepareIndex(index, type, id)
					.setSource(json)
					.get();

		}catch(MapperParsingException ex){
			System.err.println("Error during indexing document: "+ex.getMessage());
			
			if(ex.getMessage().contains("cannot contain '.'")){
				System.err.println("Trying to correct document: ");
				response = correctJSON(index,type,id,new String(json),verbose);
			}else
				return null;
		}catch(Exception ex){
			System.err.println("Error during indexing document: "+ex.getMessage());
			return null;
		}		

		if(verbose){
			printResponse(response);
		}
		
		return response;
	}
	
//	@SuppressWarnings("unchecked")
//	private void correctJSON2(String index, String type, @Nullable String id, URI uri, boolean verbose){
//		//TODO devo ancora farlo ricorsivo
//		
//		try(Reader reader = new FileReader(new File(uri))){
//			JSONParser parser = new JSONParser();
//			JSONObject obj = (JSONObject)parser.parse(reader);
//			
//			
//			JSONObject json = new JSONObject();
//			
//			for(Object key: obj.keySet()){
//				String field = (String)key;
//				if(!field.contains(".")){
//					Object value = obj.get(key);
//					json.put(field, value);
//					continue;
//				}
//					
//				String newField = field.replace('.', ':');
//				Object value = obj.get(key);
//				
//				json.put(newField, value);
//			}
//			indexDocument(index, type, id, json.toJSONString(), verbose);
//						
//		}catch(Exception pe){
//			System.out.println(pe.getMessage());
//		}
//
//	}
	
	/**
	 * Parse and replace JSON field contained '.' with ':' 
	 * @param index
	 * @param type
	 * @param id
	 * @param uri
	 * @param verbose
	 * @return
	 */
	private IndexResponse correctJSON(String index, String type, @Nullable String id, URI uri, boolean verbose){

		try(Reader reader = new FileReader(new File(uri))){
			Type mapStringObjectType = new TypeToken<Map<String, Object>>() {}.getType();

			GsonBuilder gsonBuilder = new GsonBuilder();
			gsonBuilder.registerTypeAdapter(mapStringObjectType, new RandomMapKeysAdapter());
			Gson gson = gsonBuilder.create();

			Map<String, Object> json = gson.fromJson(reader, mapStringObjectType);
			
			return indexDocument(index, type, id, json, verbose);

		}catch(Exception ex){
			System.out.println(ex.getMessage());
			return null;
		}

	}
	
	/**
	 * Parse and replace JSON field contained '.' with ':' 
	 * @param index
	 * @param type
	 * @param id
	 * @param uri
	 * @param verbose
	 * @return
	 */
	private IndexResponse correctJSON(String index, String type, @Nullable String id, String sJson, boolean verbose){

		try{
			Type mapStringObjectType = new TypeToken<Map<String, Object>>() {}.getType();

			GsonBuilder gsonBuilder = new GsonBuilder();
			gsonBuilder.registerTypeAdapter(mapStringObjectType, new RandomMapKeysAdapter());
			Gson gson = gsonBuilder.create();

			Map<String, Object> json = gson.fromJson(sJson, mapStringObjectType);
			
			return indexDocument(index, type, id, json, verbose);

		}catch(Exception ex){
			System.out.println(ex.getMessage());
			return null;
		}

	}

	/**
	 * Index with elasticSearch the given file
	 * @param id
	 * @param json
	 * @param verbose
	 * @return
	 */
	public IndexResponse indexDocument(@Nullable String id, String json, boolean verbose){
		return indexDocument(_index, _type, id, json, verbose);
	}
		
	/**
	 * Index with elasticSearch the given file
	 * @param index
	 * @param type
	 * @param id
	 * @param json
	 * @param verbose
	 * @return
	 */
	public IndexResponse indexDocument(String index, String type, @Nullable String id, String json, boolean verbose){
		IndexResponse response;
		try{
			response = client.prepareIndex(index, type, id)
					.setSource(json.getBytes())
					.get();
			
		}catch(MapperParsingException ex){
			System.err.println("Error during indexing document: "+ex.getMessage());
			
			if(ex.getMessage().contains("cannot contain '.'")){
				System.err.println("Trying to correct document: ");
				response = correctJSON(index,type,id,json,verbose);
			}else
				return null;
		}catch(Exception ex){
			System.err.println("Error during indexing document: "+ex.getMessage());
			return null;
		}		
		
		if(verbose){
			printResponse(response);
		}
		
		return response;
	}
	
	/**
	 * Index with elasticSearch the given file
	 * @param index
	 * @param type
	 * @param id
	 * @param json
	 * @param verbose
	 * @return
	 */
	public IndexResponse indexDocument(String index, String type, @Nullable String id, XContentBuilder json, boolean verbose){
		IndexResponse response;
		try{
			response = client.prepareIndex(index, type, id)
					.setSource(json)
					.get();
			
		}catch(Exception ex){
			System.err.println("Error during indexing document: "+ex.getMessage());
			return null;
		}		
		
		if(verbose){
			printResponse(response);
		}
		
		return response;
	}
	
	/**
	 * Index with elasticSearch the given file
	 * @param index
	 * @param type
	 * @param id
	 * @param json
	 * @param verbose
	 * @return
	 */
	public IndexResponse indexDocument(String index, String type, @Nullable String id, Map<String, Object> json, boolean verbose){
		IndexResponse response;
		try{
			response = client.prepareIndex(index, type, id)
					.setSource(json)
					.get();
			
		}catch(Exception ex){
			System.err.println("Error during indexing document: "+ex.getMessage());
			return null;
		}		
		
		if(verbose){
			printResponse(response);
		}
		
		return response;
	}

	private static void printResponse(IndexResponse response){
		System.out.println("Index: " + response.getIndex());			// Index name
		System.out.println("Index Type: " + response.getType());		// Type name
		System.out.println("Index Id: " + response.getId());			// Document ID (generated or not)
		System.out.println("Index Version: "+ response.getVersion());	// Version (if it's the first time you index this document, you will get: 1)
		System.out.println("Index Created: "+ response.isCreated());	// isCreated() is true if the document is a new one, false if it has been updated
	
	}
	
	private static void printResponse(UpdateResponse response){
		System.out.println("Update Index: " + response.getIndex());			// Index name
		System.out.println("Update Index Type: " + response.getType());		// Type name
		System.out.println("Update Index Id: " + response.getId());			// Document ID (generated or not)
		System.out.println("Update Index Version: "+ response.getVersion());	// Version (if it's the first time you index this document, you will get: 1)
		System.out.println("Update Index Created: "+ response.isCreated());	// isCreated() is true if the document is a new one, false if it has been updated

	}
	
	private static void printResponse(DeleteResponse response){
		System.out.println("Information on the deleted document:");
		System.out.println("Index: " + response.getIndex());
		System.out.println("Type: " + response.getType());
		System.out.println("Id: " + response.getId());
		System.out.println("Version: " + response.getVersion());
		System.out.println("Found: " + response.isFound());
	}

	public static void printResponse(SearchResponse response, boolean content, @Nullable String[] fields){
		SearchHit[] results = response.getHits().getHits();
		System.out.println("Current results: " + results.length);
		for (SearchHit hit : results) {
			System.out.println("------------------------------");

			System.out.println("Index: " + hit.getIndex());
			System.out.println("Type: " + hit.getType());
			System.out.println("Id: " + hit.getId());
			System.out.println("Version: " + hit.getVersion());
			System.out.println("Score: " + hit.getScore());

			if(content){
				Map<String,Object> result = hit.getSource();   
				if(fields!=null && fields.length>0){
					for(String field: fields){
						if(result.containsKey(field))
							System.out.println(field + ": "+ result.get(field));
					}
				}else
					System.out.println("Content: \n"+ result);
			}

		}
		System.out.println("------------------------------");
	}
	
	/**
	 * Build a query from string
	 * @param simpleQuery
	 * @param verbose
	 * @param content
	 * @param fields
	 * @return
	 */
	public SearchResponse searchDocumentSimpleQuery(String simpleQuery, boolean verbose, boolean content, @Nullable String[] fields){
		return searchDocumentSimpleQuery(_index, _type, simpleQuery, verbose, content, fields);
	}
	
	/**
	 * Build a query from string
	 * @param index
	 * @param type
	 * @param simpleQuery
	 * @param verbose
	 * @param content
	 * @param fields
	 * @return
	 */
	public SearchResponse searchDocumentSimpleQuery(String index, String type, String simpleQuery, boolean verbose, boolean content, @Nullable String[] fields) {
		SearchResponse response;

		try{
			//QueryBuilder qb = simpleQueryStringQuery(query).field("Author");
			QueryBuilder qb = simpleQueryStringQuery(simpleQuery);

			response = buildQueryResponse(index, type)
					.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
					.setQuery(qb)
					.setFrom(0).setSize(-1).setExplain(true)
					.execute()
					.actionGet();
		}catch(Exception ex){
			System.err.println("Error during searching documents: "+ex.getMessage());
			return null;
		}

		if(verbose){
			printResponse(response, content, fields);
		}
		return response;
	}
	
	/**
	 * Build a query from regular expression
	 * @param regexp
	 * @param verbose
	 * @param content
	 * @param fields
	 * @return
	 */
	public SearchResponse searchDocumentRegexpQuery(String regexp, boolean verbose, boolean content, @Nullable String[] fields){
//		return searchDocumentRegexpQuery(_index_RE, _type, regexp, verbose, content, fields);
		return searchDocumentRegexpQuery(_index, _type, regexp, verbose, content, fields);
	}
	
	/**
	 * Build a query from regular expression
	 * @param regexp
	 * @param verbose
	 * @param content
	 * @param fields
	 * @return
	 */
	public SearchResponse searchDocumentRegexpQueryRecursive(String regexp, boolean verbose, boolean content, @Nullable String[] fields){
//		return searchDocumentRegexpQueryRecursive(_index_RE, _type, regexp, verbose, content, fields);
		return searchDocumentRegexpQueryRecursive(_index, _type, regexp, verbose, content, fields);
	}
	
	private RegexpQueryBuilder buildRegExpQuery(String field, String regexp){
		try{
			RegExp reg = new RegExp(regexp, 0);

			return regexpQuery(field, reg.toString())
					.flags(RegexpFlag.NONE);
		}catch(Exception ex){
			System.err.println("Error during building regexQuery documents: "+ex.getMessage());
			return null;
		}
	}
	
	/**
	 * Build a query from regular expression
	 * @param index
	 * @param type
	 * @param regexp
	 * @param verbose
	 * @param content
	 * @param fields
	 * @return
	 */
	public SearchResponse searchDocumentRegexpQuery(String index, String type, String regexp, boolean verbose, boolean content, @Nullable String[] fields) {
		SearchResponse response;

		try{
			QueryBuilder qb = buildRegExpQuery("X-TIKA:content", regexp);

			response = buildQueryResponse(index, type)
					.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
					.setQuery(qb)
					.setFrom(0).setSize(-1).setExplain(true)
					.execute()
					.actionGet();

		}catch(Exception ex){
			System.err.println("Error during searching rdocuments: "+ex.getMessage());
			return null;
		}

		if(verbose){
			printResponse(response, content, fields);
		}
		return response;
	}
	
	/**
	 * Build a query from regular expression
	 * @param index
	 * @param type
	 * @param regexp
	 * @param verbose
	 * @param content
	 * @param fields
	 * @return
	 */
	public SearchResponse searchDocumentRegexpQueryRecursive(String index, String type, String regexp, boolean verbose, boolean content, @Nullable String[] fields) {
		SearchResponse response;

		try{
			QueryBuilder rqb = buildRegExpQuery("Content-Package.X-TIKA:content", regexp);
			
			QueryBuilder qb = nestedQuery(
					"Content-Package",
					rqb)
					.scoreMode("avg");

			response = buildQueryResponse(index, type)
					.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
					.setQuery(qb)
					.setFrom(0).setSize(-1).setExplain(true)
					.execute()
					.actionGet();

		}catch(Exception ex){
			System.err.println("Error during searching rdocuments: "+ex.getMessage());
			return null;
		}
		
		if(verbose){
			printResponse(response, content, fields);
		}
		return response;
	}
	
	
	/**
	 * Build a query from string
	 * @param index
	 * @param type
	 * @param queryString
	 * @param verbose
	 * @param content
	 * @return
	 */
	public SearchResponse searchDocumentQuery(String index, String type, String queryString, boolean verbose, boolean content) {
		SearchResponse response;

		try{
			//QueryBuilder qb = simpleQueryStringQuery(query).field("Author");
			QueryBuilder qb = queryStringQuery(queryString);

			response = buildQueryResponse(index, type)
					.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
					.setQuery(qb)
					.setFrom(0).setSize(-1).setExplain(true)
					.execute()
					.actionGet();
		}catch(Exception ex){
			System.err.println("Error during searching documents: "+ex.getMessage());
			return null;
		}

		if(verbose){
			printResponse(response, content, null);
		}
		return response;
	}
	
	/**
	 * Build a query from string
	 * @param index
	 * @param type
	 * @param jsonQuery
	 * @param verbose
	 * @param content
	 * @return
	 */
	public SearchResponse searchDocumentQuery(String index, String type, XContentBuilder jsonQuery, boolean verbose, boolean content) {
		SearchResponse response;

		try{			
			response = buildQueryResponse(index, type)
					.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
					.setQuery(jsonQuery)
					.setFrom(0).setSize(-1).setExplain(true)
					.execute()
					.actionGet();
		}catch(Exception ex){
			System.err.println("Error during searching documents: "+ex.getMessage());
			return null;
		}

		if(verbose){
			printResponse(response, content, null);
		}
		return response;
	}
	
	/**
	 * Get all documents from index with type
	 * @param index
	 * @param type
	 * @param verbose
	 * @param content
	 * @return
	 */
	public SearchResponse searchDocument(String index, String type, boolean verbose, boolean content){
		SearchResponse response;

		try{			
			response = buildQueryResponse(index, type)
					.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
					.setFrom(0).setSize(-1).setExplain(true)
					.execute()
					.actionGet();
		}catch(Exception ex){
			System.err.println("Error during searching documents: "+ex.getMessage());
			return null;
		}

		if(verbose){
			printResponse(response, content, null);
		}
		return response;
	}
	
	private SearchRequestBuilder buildQueryResponse(@Nullable String index, @Nullable String type){
		SearchRequestBuilder builder;
		
		if(index==null)
			builder = client.prepareSearch();
		else
			builder = client.prepareSearch(index);
		
		if(type==null)
			builder = builder.setTypes();
		else
			builder = builder.setTypes(type);
				
		return builder;
	}

	/**
	 * Retrieve document
	 * @param index
	 * @param type
	 * @param id
	 * @return
	 */
	public GetResponse getDocument(String index, String type, String id){
		GetResponse getResponse = client.prepareGet(index, type, id).execute().actionGet();
		
		Map<String, Object> source = getResponse.getSource();
		System.out.println("------------------------------");
		System.out.println("Index: " + getResponse.getIndex());
		System.out.println("Type: " + getResponse.getType());
		System.out.println("Id: " + getResponse.getId());
		System.out.println("Version: " + getResponse.getVersion());
		System.out.println(source);
		System.out.println("------------------------------");
		return getResponse;
	}
	

	public DeleteResponse deleteDocument(String index, String type, String id, boolean verbose){
		DeleteResponse response;
		try{
			response = client.prepareDelete(index, type, id).execute().actionGet();
		}catch(Exception ex){
			System.err.println("Error during searching documents: "+ex.getMessage());
			return null;
		}

		if(verbose){
			printResponse(response);
		}
		return response;
	}
	
	public DeleteIndexResponse deleteIndex(boolean verbose) throws IOException{
		DeleteIndexResponse response = deleteIndex(_index, verbose);
		createIndex(true);
		return response;
	}
	
	public DeleteIndexResponse deleteIndex(String index, boolean verbose){
		DeleteIndexResponse response;
		try{
			response = client.admin().indices().delete(new DeleteIndexRequest(index)).actionGet();
			if (!response.isAcknowledged()) {
			    throw new Exception("Index wasn't deleted");
			}

		}catch (IndexNotFoundException ex) {
			System.err.println("Cannot delete index \""+index+"\" because it doesn't exist.");
			return null;
	    }catch(Exception ex){
			System.err.println("Error during deleting index: "+ex.getMessage());
			return null;
		}		
		
		if(verbose){
			System.out.println("Index \""+index+"\" successfully removed.");
		}
		return response;
	}
		
	public UpdateResponse updateDocument(String index, String type, String id, URI uri, boolean verbose){
		UpdateResponse response;
		try{
			Path path = Paths.get(uri);
			byte[] json = Files.readAllBytes(path);
			
			response = client.prepareUpdate(index, type, id)
					.setDoc(json)
					.get();
		}catch(Exception ex){
			System.err.println("Error during updating documents: "+ex.getMessage());
			return null;
		}

		if(verbose){
			printResponse(response);
		}
		return response;
	}
	
	public UpdateResponse upsertDocument(String index, String type, String id, URI uri, boolean verbose){
		UpdateResponse response;
		try{
			Path path = Paths.get(uri);
			byte[] json = Files.readAllBytes(path);
			
			IndexRequest indexRequest = new IndexRequest(index, type, id)
					.source(json);
			
			UpdateRequest updateRequest = new UpdateRequest(index, type, id)
					.doc(json)
					.upsert(indexRequest);
			
			response = client.update(updateRequest).get();			
			
		}catch(Exception ex){
			System.err.println("Error during updating documents: "+ex.getMessage());
			return null;
		}

		if(verbose){
			printResponse(response);
		}
		return response;
	}
	
	
	public void stopClient(){
		try{
			if(node!=null && !node.isClosed())
				node.close();

			System.out.println("[INFO] ElasticSearch Client stopped");
		}catch(Exception ex){
			System.err.println("Error during closing client: "+ex.getMessage());
		}
	}


	public void finalize() throws Throwable{
		try{
			stopClient();
		}finally {
			super.finalize();
		}

	}
	
	public void batchIndexing(File input, String index, String type) {
		
		if(!input.exists())
			return;
		
		ArrayList<File> files;
		
		if(input.isDirectory()){
			files = new ArrayList<File>(FileUtils.listFiles(input, TrueFileFilter.TRUE, TrueFileFilter.TRUE));
		}
		else{
			files = new ArrayList<File>();
			files.add(input);
		}
				
		System.out.println("--------------------------------------------------");
		long startTime = System.nanoTime();
		for(File file: files){
			if(file.isDirectory()) continue;

			System.out.print("Indexing file: " + file.toString() + " ");
			indexDocument(index, type, null, file.toURI(), true);
			System.out.println("--------------------------------------------------");			
		}
		long endTime = System.nanoTime();
		long duration = (endTime-startTime)/1000000;
		System.out.println("--------------------------------------------------");
		System.out.println("Indexing completed in "+ duration  +"msec");
		System.out.println("--------------------------------------------------");
	}

}
