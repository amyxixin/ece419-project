package app_kvClient;

public interface IClientSocketListener {

	public enum SocketStatus{CONNECTED, DISCONNECTED, CONNECTION_LOST};
	
	public void handleNewMessage(String msg);
	
	public void handleStatus(SocketStatus status);
}
