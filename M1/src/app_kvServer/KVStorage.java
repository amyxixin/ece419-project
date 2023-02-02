package app_kvServer;

import java.io.*;
import java.util.Properties;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class KVStorage {
    private static Logger logger = Logger.getRootLogger();
    /* data size is the maximum byte length of a key value pair.
        key max size = 20 Bytes
        value max size = 120 kByte
       also need to store the size of existing key and value represented as Integers 4 bytes
     */
    private static final Integer dataSize = 4 + 4 + 20 + 120000;
    // no concurrent exception, can't use null key or value
    static HashMap<String, Long> idx = new HashMap<String, Long>();
    private String dbPath= "db.dat";
    private String idxPath = "hash.idx";

    public KVStorage() {
        initStorage();
    }

    public void put(String key, String value){
        Long begin = idx.get(key);
        try {
            RandomAccessFile f = new RandomAccessFile(dbPath, "rw");

            if (begin == null) {
                // new data added to end of file
                begin = f.length();
            }

            if (value.equals("")) { // change how delete is handled
                this.idx.remove(key);
                f.close();
                logger.info("Deleted data from key " + key);
                return;
            }

            this.idx.put(key, begin);
            f.seek(begin);
            f.writeInt(key.length());
            f.writeInt(value.length());
            f.writeBytes(key);
            f.seek(begin+4+4+20);
            f.writeBytes(value);
            f.close();

            logger.info("Complete writing data from key " + key);

        } catch (Exception e) {
            logger.error("Error writing to database! " + e.getMessage());
        }

    }

    public String get(String key){
        if (this.idx.containsKey(key)) {
            Long begin = idx.get(key);
            try {
                RandomAccessFile f = new RandomAccessFile(dbPath, "r");
                f.seek(begin+1+4);
                int valSize = f.readInt();
                byte[] value = new byte[valSize];
                f.read(value, (int)(begin+1+4+4+20), valSize);
                f.close();
                logger.info("Complete getting data from key " + key);
                return new String(value);

            } catch (Exception e) {
                logger.error("Error getting data from database! " + e.getMessage());
                return null;
            }
        } else {
            logger.error("Error accessing non-existent key."); // is this a real error? or we just return null?
            return null;
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

        if (idxFile.exists()){
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

    public boolean save(){
        try {
            FileOutputStream out = new FileOutputStream(idxPath);
            Properties prop = new Properties();
            prop.putAll(this.idx);
            prop.store(out, null);
            out.close();
            return true;
        } catch (Exception e) {
            logger.error("Error saving index file " + e.getMessage());
            return false;
        }
    }

}
