package utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnection;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import main.Application;

public class ConnectionFactory {
	//private JsonObject config;
	private String username;
	private String password;
	private String driverClassName;
	private String url;
	
	private static interface Singleton {
		final ConnectionFactory INSTANCE = new ConnectionFactory();
	}

	private final DataSource dataSource;

	private ConnectionFactory() {
		loadConfig();
		
		try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(2);
        }
		
		Properties properties = new Properties();
		properties.setProperty("user", username);
		properties.setProperty("password", password);

		GenericObjectPool<PoolableConnection> pool = new GenericObjectPool<PoolableConnection>();
        DriverManagerConnectionFactory connectionFactory = new DriverManagerConnectionFactory(url, properties);
		
        new PoolableConnectionFactory(
                connectionFactory, pool, null, "SELECT 1", 3, false, false, Connection.TRANSACTION_READ_COMMITTED
        );
		
		this.dataSource = new PoolingDataSource(pool);
	}
	
	
	public static Connection getDatabaseConnection() throws SQLException {
		return Singleton.INSTANCE.dataSource.getConnection();
	}
	
	private void loadConfig(){
		JsonObject conf = Application.JSONconfig;
		
		try{
			if(!conf.has("alert-config"))
				throw new JsonSyntaxException("alert-config field is missing in configuration file");
			conf = conf.get("alert-config").getAsJsonObject();
			
			if(!conf.has("DB"))
				throw new JsonSyntaxException("DB field is missing in configuration file");
			conf = conf.get("DB").getAsJsonObject();
			
			if(!conf.has("username") || !conf.has("password") || !conf.has("driverClassName") || !conf.has("url"))
				throw new JsonSyntaxException("DB connection parameters are missing in configuration file");
			
			this.username = conf.get("username").getAsString();
			this.password = conf.get("password").getAsString();
			this.driverClassName = conf.get("driverClassName").getAsString();
			this.url = conf.get("url").getAsString();

		}catch (Exception e) {
			System.err.println(e.getMessage());;
			System.exit(2);
		}
		//this.config = conf;
	}
}