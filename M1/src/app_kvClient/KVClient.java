package app_kvClient;

import client.KVCommInterface;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

public class KVClient implements IKVClient {
    private static Logger logger = Logger.getRootLogger(); 
    
    @Override
    public void newConnection(String hostname, int port) throws Exception{
        // TODO Auto-generated method stub
    }

    @Override
    public KVCommInterface getStore(){
        // TODO Auto-generated method stub
        return null;
    }
}
