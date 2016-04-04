package tika;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.serialization.JsonMetadata;
import org.apache.tika.metadata.serialization.JsonMetadataList;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.RecursiveParserWrapper;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.sax.BasicContentHandlerFactory;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.istack.Nullable;

public class TIKAContentExtraction {
	private final static Logger logger =
			//Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
			Logger.getLogger(TIKAContentExtraction.class.getName());
				

	private final BasicContentHandlerFactory.HANDLER_TYPE handlerType = BasicContentHandlerFactory.HANDLER_TYPE.BODY;
	private int DIM_MAX = -1;
	private int FILE_DIM_MAX = 1024*1024*100;  // 100M
	private int MAX_EMB_FILE = -1; 
	
	Parser parser;
	ParseContext context;
	TesseractOCRConfig config;
	
	BasicContentHandlerFactory content_handler;
	
	private boolean recursiveJSON = true;
	private boolean disableOCR = false;
	
	String hostname;
	
	
	public TIKAContentExtraction(){
		parser = new AutoDetectParser();
		context = new ParseContext();
		content_handler = new BasicContentHandlerFactory(handlerType, DIM_MAX);
		hostname = getHostName();

		OCRConfig();

		//MyTIKALogger.setup(logger);		
	}
	
	public TIKAContentExtraction(boolean disableOCR, boolean recursiveJSON, int maxOutputFile, int maxEmbeddedFile, int maxInputRecursiveFile){
		parser = new AutoDetectParser();
		context = new ParseContext();
		content_handler = new BasicContentHandlerFactory(handlerType, DIM_MAX);
		hostname = getHostName();
		
		this.DIM_MAX = maxOutputFile;
		this.FILE_DIM_MAX = maxInputRecursiveFile;
		this.MAX_EMB_FILE = maxEmbeddedFile;
		
		this.disableOCR = disableOCR;
		this.recursiveJSON = recursiveJSON;
		
		OCRConfig(this.disableOCR);
		
		//MyTIKALogger.setup(logger);	
	}
	
	public TIKAContentExtraction(File config){
		parser = new AutoDetectParser();
		context = new ParseContext();
		content_handler = new BasicContentHandlerFactory(handlerType, DIM_MAX);
		hostname = getHostName();
		
		loadTikaConfig(config);
		
		//MyTIKALogger.setup(logger);	
	}
	
	public void setupLog(){
		MyTIKALogger.setup(logger);
	}
	
	public void setupLog(File config){
		MyTIKALogger.setup(config);
	}
	
	public void OCRConfig(){
		// tesseract config
		config = new TesseractOCRConfig();
		// Needed if tesseract is not on system path
		config.setTessdataPath("/usr/share/tesseract-ocr/tessdata");
		config.setTesseractPath("/usr/bin");
		config.setLanguage("eng");
		context.set(TesseractOCRConfig.class, config);	
	}
	
	public void OCRConfig(boolean disable){
		// tesseract config
		OCRConfig();
		
		//disable OCR
		if(disable){
			config.setTessdataPath("/");
			config.setTesseractPath("/");
			config.setLanguage("");
			context.set(TesseractOCRConfig.class, config);
		}
	}
	
	public void OCRConfig(String language, String tessData, String tessPath){
		// tesseract config
		OCRConfig();
		config.setTessdataPath(tessData);
		config.setTesseractPath(tessPath);
		config.setLanguage(language);
		context.set(TesseractOCRConfig.class, config);	
	}
	
	/**
	 * load xml tika config	
	 * @param xml_config
	 */
	public void loadTikaConfig(File xml_config){
		try{
			TikaConfig config = new TikaConfig(xml_config);
			parser = new AutoDetectParser(config);
		}catch(Exception ex){
			String msg = "Error loading TIKA config: "+ex.getMessage() 
					+ "\n Use default settings";
			System.err.println(msg);
			logger.severe(msg);
			parser = new AutoDetectParser();
		}
	}
	
	private String getHostName(){
		String hostname = "unknown";
		try{
			return InetAddress.getLocalHost().getHostName();
		}catch(UnknownHostException ex){
			return hostname;
		}
	}
	
	private void addRequestedMetadata(Metadata metadata, InputStream input, String filename) {
		//add requested metadata
		try{		
			metadata.set(Metadata.RESOURCE_NAME_KEY, filename);
			metadata.add("hostname", hostname);
			metadata.add("Content-SHA1", DigestUtils.sha1Hex(input));

		}catch(IOException ex){
			metadata.add("Content-SHA1", "Error during digest evaluation");
		}
	}
	
	/**
	 * Extract recursively content from file 
	 * @param inputFile
	 * @param output
	 * @throws IOException
	 * @throws SAXException
	 * @throws TikaException
	 */
	private void handleRecursiveJSON(File inputFile, OutputStream output) throws IOException, SAXException, TikaException {
		Metadata metadata = new Metadata();	
		
		// recursive parser
		RecursiveParserWrapper wrapper = new RecursiveParserWrapper(parser, content_handler);
		if(!recursiveJSON || inputFile.length()>FILE_DIM_MAX)
			wrapper.setMaxEmbeddedResources(0);	
		else
			wrapper.setMaxEmbeddedResources(MAX_EMB_FILE);
		
		Writer writer = null;
		Writer outWriter = null;
		
		try (InputStream input = TikaInputStream.get(inputFile.toURI().toURL(), metadata);
				InputStream istream = new FileInputStream(inputFile);) {

			// parse the document
			wrapper.parse(input, null, metadata, context);

			// write the content into a string
			writer = new StringWriter();

			// set JSON pretty printing
			JsonMetadata.setPrettyPrinting(true);
			
			// get metadata list
			List<Metadata> listMetadata = wrapper.getMetadata();

			//get first document metadata
			metadata = listMetadata.get(0);		
			
			//add requested metadata
			// need a FileInputStream in order to evaluate correct SHA digest
			addRequestedMetadata(metadata, istream, inputFile.getAbsolutePath());
					
			// serialize metadata to JSON object into StringWriter
			JsonMetadata.toJson(metadata, writer);
			writer.flush();
			
			// JSON String representation
			String jsonText;
			
			// if recursive is enabled and there are other document inside the file
			if(recursiveJSON && listMetadata.size()>1){
				
				// remove external file metadata (already parsed)
				listMetadata.remove(0);
				
				// serialize internal document metadata into an other StringWriter
				Writer listWriter = new StringWriter();
				JsonMetadataList.toJson(listMetadata, listWriter);
					    
				// prepare JSON object
			    Gson gson = new GsonBuilder().setPrettyPrinting().create();
			    JsonParser jp = new JsonParser();

			    // String representation
			    String meta = writer.toString();
			    String metaList = listWriter.toString();
			    
			    JsonObject obj = jp.parse(meta).getAsJsonObject();
			    JsonArray aobj = jp.parse(metaList).getAsJsonArray();
			    
			    // add an other metadata field
			    obj.add("Content-Package", aobj);			    
			    jsonText = gson.toJson(obj);
			}
			else{
				 jsonText = writer.toString();
			}
						
			// create new file
			outWriter = new OutputStreamWriter(output, UTF_8);
			outWriter.write(jsonText);
			
		} finally {
			wrapper.reset();
			
			if(outWriter!=null){
				outWriter.flush();
				outWriter.close();
			}
		}
	}
	
	
	private void handleTikaParseError(File input, OutputStream output){
		Metadata metadata = new Metadata();

		try( 	Writer out = new OutputStreamWriter(output);
				InputStream is = new FileInputStream(input); ){
			addRequestedMetadata(metadata, is, input.getAbsolutePath());
			JsonMetadata.setPrettyPrinting(true);
			JsonMetadata.toJson(metadata, out);
		}
		catch(Exception ex){
			System.out.println("Something went wrong: "+ex.getMessage());
			logger.severe("Something went wrong: "+ex.getMessage());
		}
	}

	public void extractToJSONFile(File input, File output) throws IOException  {
		
		OutputStream outStream = null;

		try{
			outStream = new FileOutputStream(output);
			//URL url = input.toURI().toURL();				
			handleRecursiveJSON(input, outStream);

		}
		catch(TikaException ex){
			System.err.println("Error during extraction of file "+input.getName()+":"+ ex.getMessage());
			logger.warning("Error during extraction of file "+input.getName()+":"+ ex.getMessage());
			handleTikaParseError(input, outStream);			
		}
		catch(Exception ex){
			System.err.println("Error during extraction of file "+input.getName()+":"+ ex.getMessage());
			logger.warning("Error during extraction of file "+input.getName()+":"+ ex.getMessage());
			//handleTikaParseError(input, outStream);			
		}finally{
			if(outStream!=null){
				outStream.flush();
				outStream.close();
			}
		}
	}
	
	public byte[] extractToJSONByteArray(File input) throws IOException {

		OutputStream outStream = null;

		try{
			outStream = new ByteArrayOutputStream();
						
			handleRecursiveJSON(input, outStream);
			
		}
		catch(TikaException ex){
			System.err.println("Error during extraction of file "+input.getName()+":"+ ex.getMessage());
			logger.warning("Error during extraction of file "+input.getName()+":"+ ex.getMessage());
			handleTikaParseError(input, outStream);			
		}
		catch(Exception ex){
			System.err.println("Error during extraction of file "+input.getName()+":"+ ex.getMessage());
			logger.warning("Error during extraction of file "+input.getName()+":"+ ex.getMessage());
			//handleTikaParseError(input, outStream);			
		}finally{
			if(outStream!=null){
				outStream.flush();
			}
		}
		return ((ByteArrayOutputStream)outStream).toByteArray();
	}
	
	public void log(String msg){
		logger.info(msg);
	}
	
	public void log(String msg, @Nullable Level level){
		if(level==null)
			logger.info(msg);
		else
			logger.log(level, msg);
	}
	
	
}


