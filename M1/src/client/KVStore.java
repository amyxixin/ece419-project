package client;

import java.net.Socket;

import java.util.HashSet;
import shared.messages.KVMessage;
import shared.messages.Message;
import shared.messages.KVMessage.StatusType;
import shared.messages.MessengerModule;

public class KVStore extends Thread implements KVCommInterface {
	private String KVServerAddress;
	private int KVServerPort;
	private MessengerModule msgModule;
	
	/**
	 * Initialize KVStore with address and port of KVServer
	 * @param address the address of the KVServer
	 * @param port the port of the KVServer
	 */
	public KVStore(String address, int port) {
		// set the address and port 
		this.KVServerAddress = address;
		this.KVServerPort = port;
		listeners = new HashSet<ClientSocketListener>();
	}

	/* creates a socket and connects it to the server */ 
	@Override
	public void connect() throws Exception {
		ClientSocket = new Socket(this.KVServerAddress, this.KVServerPort);
		this.msgModule = MessengerModule(ClientSocket);
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
					for(ClientSocketListener listener : listeners) {
						listener.handleNewMessage(latestMsg);
					}
				} catch (IOException ioe) {
					if(isRunning()) {
						logger.error("Connection lost!");
						try {
							tearDownConnection();
							for(ClientSocketListener listener : listeners) {
								listener.handleStatus(
										SocketStatus.CONNECTION_LOST);
							}
						} catch (IOException e) {
							logger.error("Unable to close connection!");
						}
					}
				}				
			}
		} catch (IOException ioe) {
			logger.error("Connection could not be established!");
			
		} finally {
			if(isRunning()) {
				closeConnection();
			}
		}
	}

	@Override
	public void disconnect() {
		logger.info("try to close connection ...");
		
		try {
			tearDownConnection();
			for(ClientSocketListener listener : listeners) {
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

	public void addListener(ClientSocketListener listener){
		listeners.add(listener);
	}

	/* check if key and value are valid
	 * constructs a message following the protocol key, value, statusType
	 * uses sendMessage() from MessengerModule to send the msg
	 */
	@Override
	public KVMessage put(String key, String value) throws Exception {
		if(isValidKey(key)){
			Message msg = new Message(key, value, StatusType.PUT);
		} else{
			Message msg = new Message("error: the key is invalid. The key cannot contain space and cannot be empty", null, StatusType.PUT_ERROR);
		}
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
		if(isValidKey(key)){
			Message msg = new Message(key, value, StatusType.GET);
		} else{
			Message msg = new Message("error: the key is invalid. The key cannot contain space and cannot be empty", null, StatusType.GET_ERROR);
		}
		msgModule.sendMessage(msg);
		Message receiveMsg = msgModule.receiveMessage();
		return receiveMsg;
	}

	/*
	 * return false if the key contains space or the key is empty
	 * return true otherwise
	 */
	private boolean isValidKey(String key){
		if(key.contains(' ')){
			return false;
		}
		if(key.isEmpty()){
			return false;
		}
		return true;
	}
}
