package app_kvClient;

import logger.LogSetup;
import shared.messages.KVMessage;
import shared.messages.Message;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import client.KVCommInterface;
import client.KVStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.Arrays;

public class KVClient implements IKVClient, IClientSocketListener {
    private static Logger logger = Logger.getRootLogger();
    private KVStore client = null;
    private static final String PROMPT = "KVClient> ";
    private boolean stop = false;
    private BufferedReader stdin;
    private String serverAddress;
    private int serverPort;

    @Override
    public void newConnection(String hostname, int port) throws Exception{
        this.client = new KVStore(hostname, port);
        this.client.addListener(this);
        this.client.connect();
    }

    @Override
    public KVCommInterface getStore(){
        return this.client;
    }


    public void run() {
        while(!stop) {
            this.stdin = new BufferedReader(new InputStreamReader(System.in));
            System.out.print(PROMPT);

            try {
                String cmdLine = this.stdin.readLine();
                this.handleCommand(cmdLine);
            } catch (IOException e) {
                stop = true;
                printError("CLI does not respond - KVClient Application terminated ");
            }
        }
    }

    private void handleCommand(String cmdLine) {
        String[] tokens = cmdLine.split("\\s+");

        if(tokens[0].equals("quit")) {
            stop = true;
            this.client.disconnect();
            System.out.println(PROMPT + "Application exit!");

        } else if (tokens[0].equals("connect")){
            if(tokens.length == 3) {
                try{
                    serverAddress = tokens[1];
                    serverPort = Integer.parseInt(tokens[2]);
                    newConnection(serverAddress, serverPort);
                } catch(NumberFormatException nfe) {
                    printError("No valid address. Port must be a number!");
                    logger.info("Unable to parse argument <port>", nfe);
                } catch (UnknownHostException e) {
                    printError("Unknown Host!");
                    logger.info("Unknown Host!", e);
                } catch (Exception e) {
                    printError("Could not establish connection!");
                    logger.warn("Could not establish connection!", e);
                }
            } else {
                printError("Invalid number of parameters!");
            }

        } else if (tokens[0].equals("put")){
            /* get put command abd send out put request */
            if(tokens.length >= 3) {
                /* parameter length should be >= 3 for PUT request */
                if (this.client!= null && this.client.isRunning()){
                    String inputKey = tokens[1];
                    /* all inputs starting from token[2] are appended to inputValue */
                    String[] subTokenArray = Arrays.copyOfRange(tokens, 2, tokens.length);
                    String inputValue = String.join( " ", subTokenArray);
                    try {
                        KVMessage receiveMsg = this.client.put(inputKey, inputValue);
                        this.handleNewMessage(receiveMsg.getMsg());
                    }
                    catch (Exception e){
                        printError("Unable to perform put");
                        this.client.disconnect();
                    }
                }
                else{
                    this.printError("Client not connected.");
                }

            }
            else {
                this.printError("Invalid number of parameters. Usage: put <key> <value>.");
            }

        } else if (tokens[0].equals("get")){
            /* get put command abd send out put request */
            if(tokens.length == 2) {
                /* parameter length should be >= 3 for PUT request */
                if (this.client!= null && this.client.isRunning()){
                    String inputKey = tokens[1];
                    try {
                        KVMessage receiveMsg = this.client.get(inputKey);
                        this.handleNewMessage(receiveMsg.getMsg());
                    }
                    catch (Exception e) {
                        printError("Unable to perform get");
                        this.client.disconnect();
                    }
                }
                else{
                    this.printError("Client not connected.");
                }

            }
            else {
                this.printError("Invalid number of parameters. Usage: put <key> <value>.");
            }
        } else if(tokens[0].equals("disconnect")) {
            this.client.disconnect();

        } else if(tokens[0].equals("logLevel")) {
            if(tokens.length == 2) {
                String level = setLevel(tokens[1]);
                if(level.equals(LogSetup.UNKNOWN_LEVEL)) {
                    printError("No valid log level!");
                    printPossibleLogLevels();
                } else {
                    System.out.println(PROMPT +
                            "Log level changed to level " + level);
                }
            } else {
                printError("Invalid number of parameters!");
            }

        } else if(tokens[0].equals("help")) {
            printHelp();
        } else {
            printError("Unknown command");
            printHelp();
        }
    }

    private void printHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(PROMPT).append("KVCLIENT HELP (Usage):\n");
        sb.append(PROMPT);
        sb.append("::::::::::::::::::::::::::::::::");
        sb.append("::::::::::::::::::::::::::::::::\n");
        sb.append(PROMPT).append("connect <host> <port>");
        sb.append("\t establishes a connection to a server\n");
        sb.append(PROMPT).append("send <text message>");
        sb.append("\t\t sends a text message to the server \n");
        sb.append(PROMPT).append("disconnect");
        sb.append("\t\t\t disconnects from the server \n");

        sb.append(PROMPT).append("logLevel");
        sb.append("\t\t\t changes the logLevel \n");
        sb.append(PROMPT).append("\t\t\t\t ");
        sb.append("ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n");

        sb.append(PROMPT).append("quit ");
        sb.append("\t\t\t exits the program");
        System.out.println(sb.toString());
    }

    private void printPossibleLogLevels() {
        System.out.println(PROMPT
                + "Possible log levels are:");
        System.out.println(PROMPT
                + "ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF");
    }
    /*
     * set logger to specified log level
     * level: One of the following log4j log levels:
     * (ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF)
     * status message: Print out current log status.
     */
    private String setLevel(String logLevelString){
        if (logLevelString.equals((Level.ALL).toString())){
            this.logger.setLevel(Level.ALL);
            return logLevelString;
        }
        else if (logLevelString.equals((Level.DEBUG).toString())){
            this.logger.setLevel(Level.DEBUG);
            return logLevelString;
        }
        else if (logLevelString.equals((Level.INFO).toString())){
            this.logger.setLevel(Level.INFO);
            return logLevelString;
        }
        else if (logLevelString.equals((Level.WARN).toString())){
            this.logger.setLevel(Level.WARN);
            return logLevelString;
        }
        else if (logLevelString.equals((Level.FATAL).toString())){
            this.logger.setLevel(Level.FATAL);
            return logLevelString;
        }
        else if (logLevelString.equals((Level.OFF).toString())){
            this.logger.setLevel(Level.OFF);
            return logLevelString;
        }
        else {
            return LogSetup.UNKNOWN_LEVEL;
        }
    }


    private void printError(String error){
        System.out.println(PROMPT + "Error! " +  error);
    }


    /**
     * Main entry point for the KVServer application.
     * @param args contains the port number at args[0].
     */
    public static void main(String[] args) {
        try {
            new LogSetup("logs/client.log", Level.OFF);
            KVClient app = new KVClient();
            app.run();
        } catch (IOException e) {
            System.out.println("Error! Unable to initialize logger!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void handleNewMessage(String msg) {
        if(!stop) {
            System.out.print(PROMPT);
            System.out.println(msg);
        }
    }

    @Override
    public void handleStatus(SocketStatus status) {
        if(status == SocketStatus.CONNECTED) {

        } else if (status == SocketStatus.DISCONNECTED) {
            System.out.print(PROMPT);
            System.out.println("Connection terminated: "
                    + serverAddress + " / " + serverPort);

        } else if (status == SocketStatus.CONNECTION_LOST) {
            System.out.println("Connection lost: "
                    + serverAddress + " / " + serverPort);
            System.out.print(PROMPT);
        }

    }
}


