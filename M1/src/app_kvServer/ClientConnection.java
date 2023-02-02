package app_kvServer;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.log4j.*;
import shared.messages.KVMessage.StatusType;
import shared.messages.Message;
import shared.messages.MessengerModule;


/**
 * Represents a connection end point for a particular client that is
 * connected to the server. This class is responsible for message reception
 * and sending.
 * The class also implements the echo functionality. Thus whenever a message
 * is received it is going to be echoed back to the client.
 */
public class ClientConnection implements Runnable {

    private static Logger logger = Logger.getRootLogger();

    private boolean isOpen;

    private Socket clientSocket;
    private KVServer server;

    private MessengerModule messenger;

    private InputStream input;
    private OutputStream output;


    /**
     * Constructs a new CientConnection object for a given TCP socket.
     * @param clientSocket the Socket object for the client connection.
     * @param server the KVServer object for the client connection.
     */
    public ClientConnection(Socket clientSocket, KVServer server) throws IOException {
        this.clientSocket = clientSocket;
        this.server = server;
        this.messenger = new MessengerModule(clientSocket);
        this.isOpen = true;
    }

    /**
     * Initializes and starts the client connection.
     * Loops until the connection is closed or aborted by the client.
     */
    public void run() {
        try {
            output = clientSocket.getOutputStream();
            input = clientSocket.getInputStream();

            while(isOpen) {
                try {
                    Message receivedMsg = messenger.receiveMessage();
                    logger.info("RECEIVE \t<"
                            + clientSocket.getInetAddress().getHostAddress() + ":"
                            + clientSocket.getPort() + ">: '"
                            + receivedMsg.getMsg().trim() + "'");

                    String key = receivedMsg.getKey();
                    String value = receivedMsg.getValue();

                    Message res = null;
                    switch (receivedMsg.getStatus()) {
                        case GET:
                            res = this.handleGet(key);
                            break;
                        case PUT:
                            res = this.handlePut(key, value);
                            break;
                    }

                    if(res != null) {
                        messenger.sendMessage(res);
                        logger.info("SEND \t<"
                                + clientSocket.getInetAddress().getHostAddress() + ":"
                                + clientSocket.getPort() + ">: '"
                                + res.getMsg() +"'");
                    } else{
                        logger.error("Error! Received message has unsupported StatusType.");
                    }

                    /* connection either terminated by the client or lost due to
                     * network problems*/
                } catch (IOException ioe) {
                    logger.error("Error! Connection lost!");
                    isOpen = false;
                }
            }

        } catch (IOException ioe) {
            logger.error("Error! Connection could not be established!", ioe);

        } finally {

            try {
                if (clientSocket != null) {
                    input.close();
                    output.close();
                    clientSocket.close();
                }
            } catch (IOException ioe) {
                logger.error("Error! Unable to tear down connection!", ioe);
            }
        }
    }

    private Message handleGet(String key){
        try{
            String value = this.server.getKV(key);
            return new Message(key, value, StatusType.GET_SUCCESS);

        } catch(Exception e){
            return new Message(key, null, StatusType.GET_ERROR);
        }

    }

    private Message handlePut(String key, String value){
        StatusType statusType;

        if(this.server.inStorage(key)){
            statusType = StatusType.PUT_UPDATE;
        } else{
            statusType = StatusType.PUT_SUCCESS;
        }

        try{
            this.server.putKV(key, value);
        } catch(Exception e){
            statusType = StatusType.PUT_ERROR;
        }

        return new Message(key, value, statusType);
    }
}
