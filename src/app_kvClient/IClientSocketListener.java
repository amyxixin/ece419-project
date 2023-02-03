package app_kvClient;

import shared.messages.KVMessage;

public interface IClientSocketListener {

	public enum SocketStatus{CONNECTED, DISCONNECTED, CONNECTION_LOST};
	
	public void handleNewMessage(KVMessage msg);
	
	public void handleStatus(SocketStatus status);
}
