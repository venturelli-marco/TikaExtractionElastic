package tika;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MyTIKALogger {
	static private FileHandler fileTxt;
	static private SimpleFormatter formatterTxt;
	
	static private Logger logger;
	
	private final static String _path = "logs";
	private final static String _fileName = "tika-log%u.out";

	static private void setupFile(File fileName) throws IOException {
				
		// suppress the logging output to the console
		for(Handler handler: logger.getHandlers())
			if (handler instanceof ConsoleHandler) {
				logger.removeHandler(handler);
			}	
		Logger rootLogger = Logger.getLogger("");
		for(Handler handler: rootLogger.getHandlers())
			if (handler instanceof ConsoleHandler) {
				rootLogger.removeHandler(handler);
			}	
		
		logger.setLevel(Level.INFO);
		fileTxt = new FileHandler(fileName.getAbsolutePath());
				
		// create a TXT formatter
		formatterTxt = new SimpleFormatter();
		fileTxt.setFormatter(formatterTxt);
		logger.addHandler(fileTxt);
		
	}
	
	static private void setupConfig(File config){
		try {
		    FileInputStream configFile = new FileInputStream(config);
		    LogManager.getLogManager().readConfiguration(configFile);
		} catch (Exception ex)
		{
		    System.err.println("WARNING: Could not open configuration file");
		    System.err.println("WARNING: Logging not configured (console output only)");
		    setupConsole();
		}
	}

	static private void setupConsole(){
		
		// suppress the logging output to the console
		for(Handler handler: logger.getHandlers()){
			if (handler instanceof FileHandler) {
				logger.removeHandler(handler);
			}
		}
		logger.addHandler(new ConsoleHandler());
		logger.setLevel(Level.INFO);
	}
	
	static public void setup(File config){
		disableLog4j();
		setupConfig(config);
	}
	
	static public void setup(Logger log){
		File dir = new File(_path);
		if(!dir.exists() || !dir.isDirectory()){
			dir.delete();
			dir.mkdir();
		}
		setup(log, new File(_path, _fileName));
	}
	
	static public void setup(Logger log, File logFile){
		try {
			disableLog4j();
			
			logger = log;
			setupFile(logFile);
		} catch (IOException e) {
			System.err.println("Problems creating the log files");
			setupConsole();
		}
	}
	
	static private void disableLog4j(){
		org.apache.log4j.Logger
			.getLogger(org.apache.tika.parser.ocr.TesseractOCRParser.class.getName())
			.setLevel(org.apache.log4j.Level.OFF);
		
		org.apache.log4j.Logger
			.getLogger(org.apache.pdfbox.pdfparser.PDFObjectStreamParser.class.getName())
			.setLevel(org.apache.log4j.Level.OFF);
		
		org.apache.log4j.Logger
			.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		
		//org.apache.pdfbox.pdfparser.PDFObjectStreamParser

	}

}
 

