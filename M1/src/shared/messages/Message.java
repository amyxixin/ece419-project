package shared.messages;

public class Message implements KVMessage{

    private String key;
    private String value;
    private StatusType statusType;

    private String serializedMsg;


    public Message(String key, String value, StatusType statusType){
        this.key = key;
        this.value = value;
        this.statusType = statusType;
        this.serializedMsg = String.format("%s,%s,%s", key, value, statusType);
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public StatusType getStatus() {
        return null;
    }
}
