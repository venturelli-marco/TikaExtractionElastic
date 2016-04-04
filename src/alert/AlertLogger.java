package alert;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class to log the alert
 * @author marco
 *
 */
public class AlertLogger {
	private static Logger logger = LogManager.getLogger(AlertLogger.class.getName());
	
	public static void log(ParseTikaJsonObject results){
		try{
			
//			String sha = ParseTikaJsonObject.getShaDigest(); 
//			String name = ParseTikaJsonObject.getResourceName();
//			String host = ParseTikaJsonObject.getHostName();
			
			String info = results.getMessage();
			
			logger.info(info);

		} catch (Exception e) {
			System.err.println(e.getMessage());
		} 
	}
	
	
}
