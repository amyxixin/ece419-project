package app_kvServer;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import app_kvServer.ClientConnection;

import java.io.*;
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

    /* data size is the maximum byte length of a key value pair.
        key max size = 20 Bytes
        value max size = 120 kBytes
       also need to store the size of existing value as Integer = 4 bytes
     */
    private static final Integer dataSize = 4 + 20 + 120000;
    // no concurrent exception, can't use null key or value
    static HashMap<String, Long> idx = new HashMap<String, Long>();
    private String dbPath= "database.dat";
    private String idxPath = "hash.idx";

    public KVServer(int port, int cacheSize, String strategy) {
        this.port = port;
//        this.cacheSize = cacheSize;
//        this.cacheStrategy = CacheStrategy.valueOf(strategy);
        this.initStorage();
        this.run();
    }

    @Override
    public int getPort() { return this.port; }

    @Override
    public String getHostname() {
        if (this.serverSocket != null) {
            return this.serverSocket.getInetAddress().getHostName();
        } else {
            logger.error("Error identifying hostname.");
            return null;
        }

    }

    @Override
    public CacheStrategy getCacheStrategy(){ return this.cacheStrategy; }

    @Override
    public int getCacheSize(){ return this.cacheSize; }

    @Override
    public boolean inStorage(String key){
        return this.idx.containsKey(key);
    }

    @Override
    public boolean inCache(String key){
        // TODO Auto-generated method stub
        return false;
    }

    private boolean isRunning() { return this.running; }

    @Override
    public String getKV(String key) throws IllegalArgumentException, IOException{
        if (inStorage(key)) {
            Long begin = this.idx.get(key);
            try {
                RandomAccessFile f = new RandomAccessFile(dbPath, "r");

                if (key.length() == 0 || key.length() >= 20){
                    logger.error("Key has wrong length!");
                    throw new IllegalArgumentException("Key has wrong length!!");
                }

                f.seek(begin);
                int valSize = f.readInt();
                byte[] value = new byte[valSize];
                f.read(value, (int)(begin+4+20), valSize);
                f.close();
                logger.info("Complete getting data from key " + key);
                return new String(value);

            } catch (IOException e) {
                logger.error("Error getting data from database! " + e.getMessage());
                throw new IOException("Error getting data from database!");
            }
        } else {
            logger.error("Error accessing non-existent key.");
            throw new IllegalArgumentException("Key does not exist in database!");
        }
    }

    @Override
    public void putKV(String key, String value) throws IllegalArgumentException, IOException {
        Long begin = idx.get(key);
        try {
            RandomAccessFile f = new RandomAccessFile(dbPath, "rw");

            if (begin == null) {
                // new data added to end of file
                begin = f.length();
            }

            if (key.length() == 0 || key.length() >= 20 || value.length() >= 120000){
                logger.error("Key or value has wrong length!");
                throw new IllegalArgumentException("Key or value has wrong length!!");
            }

            if (value.equals("")) {
                this.idx.remove(key);
                f.close();
                logger.info("Complete delete data from key " + key);
                return;
            }

            this.idx.put(key, begin);
            f.seek(begin);
            f.writeInt(value.length());
            f.writeBytes(key);
            f.seek(begin+4+20);
            f.writeBytes(value);
            f.close();

            logger.info("Complete write data to key " + key);

        } catch (IOException e) {
            logger.error("Error writing to database! " + e.getMessage());
            throw new IOException("Error writing to database!");
        }
        this.save();
    }

    @Override
    public void clearCache(){
        // TODO Auto-generated method stub
    }

    @Override
    public void clearStorage(){
        File dbFile = new File(dbPath);
        File idxFile = new File(idxPath);
        try {
            dbFile.delete();
            idxFile.delete();
            this.initStorage();
            logger.info("Cleared storage files.");
        } catch (Exception e){
            logger.error("Error clearing storage files: " + e.getMessage());
        }
    }

    public void initStorage() {
        File dbFile = new File(dbPath);
        File idxFile = new File(idxPath);

        // If no existing database file, create one
        if (!dbFile.isFile()) {
            try {
                dbFile.createNewFile();
                logger.info("Created new database file at: " + dbPath);
            } catch (IOException e) {
                logger.error("Error creating database! " + e.getMessage());
                logger.error("Error creating database! " + e);
            }
        }

        if (idxFile.isFile()){
            try {
                FileInputStream in = new FileInputStream(idxPath);
                Properties prop = new Properties();
                prop.load(in);
                for (String key : prop.stringPropertyNames()) {
                    this.idx.put(key, Long.parseLong(prop.getProperty(key)));
                }
                logger.info("Loaded index file.");
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Error loading index file! " + e.getMessage());
                logger.debug("Error loading index file! " + e);
            }
        } else {
            try {
                idxFile.createNewFile();
                logger.info("Created new index file at: " + idxPath);
            } catch (IOException e) {
                logger.error("Error creating index file! " + e.getMessage());
                logger.error("Error creating index file! " + e);
            }
        }
    }

    @Override
    public void run(){
        this.running = initializeServer();

        if(serverSocket != null) {
            while(isRunning()){
                try {
                    Socket client = serverSocket.accept();
                    ClientConnection connection =
                            new ClientConnection(client, this);
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

    public void save(){
        try {
            FileOutputStream out = new FileOutputStream(idxPath);
            Properties prop = new Properties();
            prop.putAll(this.idx);
            prop.store(out, null);
            out.close();
        } catch (Exception e) {
            logger.error("Error saving index file: " + e.getMessage());
        }
    }

    @Override
    public void kill(){
        this.running = false;
        this.save();
        try {
            serverSocket.close();
            logger.info("Server killed.");
        } catch (IOException e) {
            logger.error("Error! " +
                    "Unable to close socket on port: " + port, e);
        }
    }

    @Override
    public void close(){
        this.running = false;
        this.save();
        try {
            serverSocket.close();
            logger.info("Server closed.");
        } catch (IOException e) {
            logger.error("Error! " +
                    "Unable to close socket on port: " + port, e);
        }
    }

    public static void main(String[] args) {
        try {
            new LogSetup("logs/server.log", Level.ALL);

            if(args.length != 3) {
                System.out.println("Error! Invalid number of arguments!");
                System.out.println("Usage: Server <port>!");
            } else {
                int port = Integer.parseInt(args[0]);
                int cacheSize = Integer.parseInt(args[1]);
                String strategy = args[2];
                new KVServer(port, cacheSize, strategy);
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
