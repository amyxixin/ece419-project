package app_kvServer;

import logging.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import server.ClientConnection;
import server.Server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Properties;

public class KVServer implements IKVServer {
	/**
	 * Start KV Server at given port
	 * @param port given port for storage server to operate
	 * @param cacheSize specifies how many key-value pairs the server is allowed
	 *           to keep in-memory
	 * @param strategy specifies the cache replacement strategy in case the cache
	 *           is full and there is a GET- or PUT-request on a key that is
	 *           currently not contained in the cache. Options are "FIFO", "LRU",
	 *           and "LFU".
	 */

	private static Logger logger = Logger.getRootLogger();

	/* constants */
	private int port;
	private ServerSocket serverSocket;
	private boolean running;
	private int cacheSize;
	private CacheStrategy cacheStrategy;

	/* database attributes */
	private String dbPath= "database.dat";
	private HashMap<String, String> db;

	public KVServer(int port, int cacheSize, String strategy) {
		this.port = port;
		this.cacheSize = cacheSize;
		this.cacheStrategy = CacheStrategy.valueOf(strategy);
		this.initDB();
	}

	public void initDB() {
		File dbFile = new File(dbPath);
		if (dbFile.isFile()) {
			try {
				FileInputStream in = new FileInputStream(dbPath);
				Properties prop = new Properties();
				prop.load(in);

				for (String key : prop.stringPropertyNames()) {
					this.db.put(key, prop.get(key).toString());
				}
				logger.info("Loaded data from database file " + dbPath);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("Error loading database file! " + e.getMessage());
				logger.debug("Error loading database file! " + e);
			}
		} else {
			try {
				dbFile.createNewFile();
				this.db = new HashMap<String, String>();
				logger.info("Created new database file at " + dbPath);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("Error creating database file! " + e.getMessage());
				logger.debug("Error creating database file! " + e);
			}
		}
	}
	
	@Override
	public int getPort() { return this.port; }

	@Override
    public String getHostname(){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
    public CacheStrategy getCacheStrategy(){ return this.cacheStrategy; }

	@Override
    public int getCacheSize(){ return this.cacheSize; }

	@Override
    public boolean inStorage(String key){
		return this.db.containsKey(key);
	}

	@Override
    public boolean inCache(String key){
		// TODO Auto-generated method stub
		return false;
	}

	private boolean isRunning() { return this.running; }

	@Override
    public String getKV(String key) throws Exception{
		logger.info("Retrieving value from key.");
		return this.db.get(key);
	}

	@Override
    public void putKV(String key, String value) throws Exception{
		logger.info("Put value from key"); // TODO improve this log
		this.db.put(key, value);
	}

	@Override
    public void clearCache(){
		// TODO Auto-generated method stub
	}

	@Override
    public void clearStorage(){
		File dbFile = new File(dbPath);
		try {
			dbFile.delete();
			initDB();
			logger.info("Cleared database file at " + dbPath);
		} catch (Exception e){
			logger.info("Error clearing database file at " + dbPath);
		}
	}

	@Override
    public void run(){
		running = initializeServer();

		if(serverSocket != null) {
			while(isRunning()){
				try {
					Socket client = serverSocket.accept();
					ClientConnection connection =
							new ClientConnection(client);
					new Thread(connection).start();

					logger.info("Connected to "
							+ client.getInetAddress().getHostName()
							+  " on port " + client.getPort());
				} catch (IOException e) {
					logger.error("Error! " +
							"Unable to establish connection. \n", e);
				}
			}
		}
		logger.info("Server stopped.");
	}

	private boolean initializeServer() {
		logger.info("Initialize server ...");
		try {
			serverSocket = new ServerSocket(port);
			logger.info("Server listening on port: "
					+ serverSocket.getLocalPort());
			return true;

		} catch (IOException e) {
			logger.error("Error! Cannot open server socket:");
			if(e instanceof BindException){
				logger.error("Port " + port + " is already bound!");
			}
			return false;
		}
	}

	@Override
    public void kill(){
		this.running = false;
		try {
			serverSocket.close();
		} catch (IOException e) {
			logger.error("Error! " +
					"Unable to close socket on port: " + port, e);
		}
	}

	@Override
    public void close(){
		this.running = false;
		try {
			serverSocket.close();
		} catch (IOException e) {
			logger.error("Error! " +
					"Unable to close socket on port: " + port, e);
		}
	}

	public static void main(String[] args) {
		try {
			new LogSetup("logs/server.log", Level.ALL);
			if(args.length != 1) {
				System.out.println("Error! Invalid number of arguments!");
				System.out.println("Usage: Server <port>!");
			} else {
				int port = Integer.parseInt(args[0]);
				new Server(port).start();
			}
		} catch (IOException e) {
			System.out.println("Error! Unable to initialize logger!");
			e.printStackTrace();
			System.exit(1);
		} catch (NumberFormatException nfe) {
			System.out.println("Error! Invalid argument <port>! Not a number!");
			System.out.println("Usage: Server <port>!");
			System.exit(1);
		}
	}
}
