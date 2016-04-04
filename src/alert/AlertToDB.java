package alert;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Class to generate the alert and write it to a DB.
 * Use log4j configuration instead of this class if you want to use logger.
 * @author marco
 *
 */
public class AlertToDB {
	// JDBC driver name and database URL
	private String driverClassName;  
	private String url;

	//  Database credentials
	private String username;
	private String password;
	
	// Table info
	private String tableName;
	private Map<String, String> column;
	private List<HashMap<String, Object>> rows;
	
	private Connection conn;
		
	public AlertToDB(JsonObject config){
		loadConfig(config);
		getConnection();
	}
	
	/**
	 * Load configuration from a JSON object
	 * @param conf
	 */
	private void loadConfig(JsonObject conf){
		try{
			if(!conf.has("username") || !conf.has("password") || !conf.has("driverClassName") || !conf.has("url"))
				throw new JsonSyntaxException("DB connection parameters are missing in configuration file");
			
			if(!conf.has("TableName") || !conf.has("ColumnsName"))
				throw new JsonSyntaxException("DB table parameters are missing in configuration file");
			
			this.username = conf.get("username").getAsString();
			this.password = conf.get("password").getAsString();
			this.driverClassName = conf.get("driverClassName").getAsString();
			this.url = conf.get("url").getAsString();
			
			this.tableName = conf.get("TableName").getAsString();
			
			column = new HashMap<String, String>();
			JsonObject columns = conf.get("ColumnsName").getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : columns.entrySet()){
				column.put(entry.getKey(), entry.getValue().getAsString());
			}

		}catch (Exception e) {
			System.err.println(e.getMessage());;
			System.exit(2);
		}
		//this.config = conf;
	}
	
	/**
	 * Connect to database
	 */
	public void getConnection(){
		try{
			//STEP 2: Register JDBC driver
			Class.forName(driverClassName);

			//STEP 3: Open a connection
			System.out.println("[INFO] Connecting to selected database...");
			conn = DriverManager.getConnection(url, username, password);
			System.out.println("[INFO] Connected database successfully...");				

		}catch(SQLException se){
			//Handle errors for JDBC
			System.err.println(se.toString());
			System.exit(1);
		}catch(Exception e){
			//Handle errors for Class.forName
			System.err.println(e.toString());
			System.exit(1);
		}
		
		
	}
	
	/**
	 * Build and execute an insert query for given results
	 * @param results
	 */
	public void insertEntry(ParseTikaJsonObject results){
		PreparedStatement stmt = null;
		try {
			
			buildRow(results);
			
			StringBuilder inserSql = new StringBuilder();
			StringBuilder valuesSql = new StringBuilder();
			
			inserSql.append("INSERT INTO " + tableName + " (");
			valuesSql.append("VALUES (");
			
			int size = column.entrySet().size();
			for(Map.Entry<String, String> col: column.entrySet()){
				inserSql.append(col.getKey());
				valuesSql.append("?");
				if(--size>0){
					inserSql.append(",");
					valuesSql.append(",");
				}
			}
			inserSql.append(") ");
			valuesSql.append(")");
			
			String sql = inserSql.toString()+valuesSql.toString();
			
			conn.setAutoCommit(false);
			stmt = conn.prepareStatement(sql.toString());

			
			for(HashMap<String, Object> row: rows){
				int i=1;
				for(Map.Entry<String, String> col: column.entrySet()){
					stmt.setObject(i++, getContent(col.getValue(), row));
				}
				stmt.executeUpdate();
				conn.commit();
			}
			
		} catch (SQLException e) {
			System.err.println(e.getMessage());;
			System.exit(2);
		}finally{
			try{
				if(stmt!=null)
					stmt.close();
				if(conn!=null)
					conn.rollback();
			}catch(SQLException se){}// do nothing			
		}
	}
	
	/**
	 * Create a list of rows to insert.
	 * A row has the all the properties that can be inserted in the table.
	 * Depending on given qualifier only a part will be used.
	 * @param results
	 */
	private void buildRow(ParseTikaJsonObject results){
		rows = new ArrayList<HashMap<String, Object>>();
		
		boolean skipKey = false;
		boolean skipInd = false;		

		if(!column.containsValue("%k")) skipKey=true;
		if(!column.containsValue("%i")) skipInd=true;
		
		for (Map.Entry<String, List<Integer>> entry : results.getKeywords().entrySet()){
			for(int index: entry.getValue()){
				HashMap<String, Object> row = new HashMap<String, Object>();
				//row.put("d", null);
				row.put("s", results.getShaDigest());
				row.put("r", results.getResourceName());
				row.put("h", results.getHostName());
				if(!skipKey) row.put("k", entry.getKey());
				if(!skipInd) row.put("i", index);
				row.put("m", results.getMessage());
				
				if(!rows.contains(row))
					rows.add(row);				
			}
		}
	}
	
	/**
	 * Get the content according to the qualifier inside a row
	 * @param content qualifier
	 * @param row 
	 * @return
	 */
	private Object getContent(String content, HashMap<String, Object> row){
		if(!content.startsWith("%")){
			System.err.println("Column database content is malformed: doesn't contain %");;
			System.exit(2);
		}
		String key = content.substring(1, content.length());
		List<String> keys = Arrays.asList("s", "r", "h", "m", "k", "i");
		
		if(!keys.contains(key)){
			System.err.println("Column database content is malformed: "+content+" is meaningless");;
			return null;
		}

		return row.get(key);
	}
	
	public void close(){
		try{
			if(conn!=null)
				conn.close();
		}catch(SQLException se){
			se.printStackTrace();
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		try{
			if(conn!=null)
				conn.close();
		}catch(SQLException se){
			se.printStackTrace();
		}
		super.finalize();
	}

}
