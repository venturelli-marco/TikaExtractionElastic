package tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.elasticsearch.bootstrap.Elasticsearch;
import org.junit.Ignore;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import batch.MatchKeyword;
import batch.TikaBatch;
import elastic.ElasticSearchClient;
import tika.TIKAContentExtraction;

public class test {
	
	
	@Test
	public void testRegex() throws FileNotFoundException, IOException{
		ElasticSearchClient elastic= null;
		try(Reader reader = new FileReader("resources/app-config.json")){

			JsonElement element = new JsonParser().parse(reader);
			JsonObject JSONconfig = element.getAsJsonObject();			
			
			elastic = new ElasticSearchClient("/home/marco/TesinaSicurezza/elasticsearch-2.1.1");
			
			String[] fields = new String[]{"resourceName", "Content-SHA1", "hostname"};
			
			String regex;
			regex = TikaBatch.getRegexp(JSONconfig).get(1);
			
			elastic.searchDocumentRegexpQuery(regex, true, true, fields);
			
		}catch(Exception e){
			e.printStackTrace();
		
		}finally{
			if(elastic!=null){
				try{
					Thread.sleep(1000);
					elastic.stopClient();
				}catch(Exception ex){}
			}

		}
	}
	
	@Ignore
	@Test
	public void testDB() {

		System.out.println("-------- MySQL JDBC Connection Testing ------------");

		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			System.out.println("Where is your MySQL JDBC Driver?");
			e.printStackTrace();
			return;
		}

		System.out.println("MySQL JDBC Driver Registered!");
		Connection connection = null;

		try {
			connection = DriverManager
			.getConnection("jdbc:mysql://localhost:3306/TIKA_LOGS_DB","marco", "marco");

		} catch (SQLException e) {
			System.out.println("Connection Failed! Check output console");
			e.printStackTrace();
			return;
		}

		if (connection != null) {
			System.out.println("You made it, take control your database now!");
		} else {
			System.out.println("Failed to make connection!");
		}
	  }
	
	
//	@Test
//	public void testSearch() throws Exception {
//		File input = new File("test");
//		
//		TIKAContentExtraction tika = new TIKAContentExtraction();
//		MatchKeyword keys = new MatchKeyword();
//		
//		ArrayList<File> files = new ArrayList<File>(FileUtils.listFiles(input, TrueFileFilter.TRUE, TrueFileFilter.TRUE));
//		
//		for(File file: files){
//			if(file.isDirectory()) continue;
//
//			String start_msg = "Extraction from file: " + file.toString() + " ";
//			System.out.print(start_msg);
//			long fileStartTime = System.nanoTime();
//			
//			//tika.extractToJSONFile(file, new File(outputPath, fileOutput));
//			byte[] json = tika.extractToJSONByteArray(file);
//			
//			long fileEndTime = System.nanoTime();
//			long fileDuration = (fileEndTime-fileStartTime)/1000000;
//			String stop_msg="\t\t[COMPLETED "+fileDuration+ " msec]";
//			System.out.println(stop_msg);
//			
//			//String s = new String(json, StandardCharsets.UTF_8);
//			String s = new String(json, "UTF-8");
//			boolean result;
//			
//			fileStartTime = System.nanoTime();
//			result = keys.findMatches(s);
//			fileEndTime = System.nanoTime();
//			fileDuration = (fileEndTime-fileStartTime)/1000000;
//			System.out.println("Method index_of: "+fileDuration+" msec \t Found: "+result);
//			
//			fileStartTime = System.nanoTime();
//			result = keys.findMatches2(s);
//			fileEndTime = System.nanoTime();
//			fileDuration = (fileEndTime-fileStartTime)/1000000;
//			System.out.println("Method aho_corasick: "+fileDuration+" msec \t Found: "+result);
//			
//			fileStartTime = System.nanoTime();
//			result = keys.findMatchesAhoCorasick(s);
//			fileEndTime = System.nanoTime();
//			fileDuration = (fileEndTime-fileStartTime)/1000000;
//			System.out.println("Method aho_corasick2: "+fileDuration+" msec \t Found: "+result);
//
//		}
//			
//	
//	}

}
