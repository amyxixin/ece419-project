package client;

import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import org.apache.log4j.Logger;
import shared.messages.KVMessage;
import shared.messages.Message;
import shared.messages.KVMessage.StatusType;
import shared.messages.MessengerModule;
import app_kvClient.IClientSocketListener;
import app_kvClient.IClientSocketListener.SocketStatus;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import logger.LogSetup;

public class KVStore extends Thread implements KVCommInterface {
	private static Logger logger = Logger.getRootLogger();
	private String KVServerAddress;
	private int KVServerPort;
	private MessengerModule msgModule;
	private Socket clientSocket;
	private boolean running;
	
	HashSet<IClientSocketListener> listeners;
	
	/**
	 * Initialize KVStore with address and port of KVServer
	 * @param address the address of the KVServer
	 * @param port the port of the KVServer
	 */
	public KVStore(String address, int port) {
		// set the address and port 
		this.KVServerAddress = address;
		this.KVServerPort = port; 
		listeners = new HashSet<IClientSocketListener>();
	}

	/* creates a socket and connects it to the server */ 
	@Override
	public void connect() throws Exception {
		clientSocket = new Socket(this.KVServerAddress, this.KVServerPort);
		this.msgModule = new MessengerModule(clientSocket);
		setRunning(true);
		logger.info("Connection established");
	}
	/**
	 * Initializes and starts the client connection. 
	 * Loops until the connection is closed or aborted by the client.
	 */
	public void run() {
		try {
			while(isRunning()) {
				try {
					String latestMsg = this.msgModule.receiveMessage().getMsg();
					for(IClientSocketListener listener : listeners) {
						listener.handleNewMessage(latestMsg);
					}
				} catch (IOException ioe) {
					if(isRunning()) {
						logger.error("Connection lost!");
						try {
							tearDownConnection();
							for(IClientSocketListener listener : listeners) {
								listener.handleStatus(
										SocketStatus.CONNECTION_LOST);
							}
						} catch (IOException e) {
							logger.error("Unable to close connection!");
						}
					}
				}				
			}
		} finally {
			if(isRunning()) {
				disconnect();
			}
		}
	}

	@Override
	public void disconnect() {
		logger.info("try to close connection ...");
		
		try {
			tearDownConnection();
			for(IClientSocketListener listener : listeners) {
				listener.handleStatus(SocketStatus.DISCONNECTED);
			}
		} catch (IOException ioe) {
			logger.error("Unable to close connection!");
		}
	}

	private void tearDownConnection() throws IOException {
		setRunning(false);
		logger.info("tearing down the connection ...");
		if (clientSocket != null) {
			clientSocket.close();
			clientSocket = null;
			logger.info("connection closed!");
		}
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void setRunning(boolean run) {
		running = run;
	}

	public void addListener(IClientSocketListener listener){
		listeners.add(listener);
	}

	/* check if key and value are valid
	 * constructs a message following the protocol key, value, statusType
	 * uses sendMessage() from MessengerModule to send the msg
	 */
	@Override
	public KVMessage put(String key, String value) throws Exception {
		Message msg = new Message(key, value, StatusType.PUT);
		msgModule.sendMessage(msg);
		Message receiveMsg = msgModule.receiveMessage();
		return receiveMsg;
	}
	
	/* check if key and value are valid
	 * constructs a message following the protocol key, value, statusType
	 * uses sendMessage() from MessengerModule to send the msg
	 */
	@Override
	public KVMessage get(String key) throws Exception {
		Message msg = new Message(key, "", StatusType.GET);
		msgModule.sendMessage(msg);
		Message receiveMsg = msgModule.receiveMessage();
		return receiveMsg;
	}
}
