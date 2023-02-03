package shared.messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Message implements KVMessage{

    private String key;
    private String value;
    private StatusType statusType;
    private String msg;         // Serialized message string
    private byte[] msgBytes;    // Serialized message represented using byte array

    private static final char LINE_FEED = 0x0A;		// "ASCII for '/n' "
    private static final char RETURN = 0x0D;		// "ASCII for '/r' "

    /**
     * Constructs a Message object with a given array of bytes that
     * forms the message.
     *
     * @param bytes the bytes that form the message in ASCII coding.
     */
    public Message(byte[] bytes) {
        this.msgBytes = addCtrChars(bytes);
        this.msg = new String(this.msgBytes).trim();

        try {
            List<String> msgArgs = new ArrayList<String>(Arrays.asList(this.msg.split(",")));
            if(msgArgs.size() != 3){
                throw new Exception();
            }
            this.key = msgArgs.get(0);
            this.value = msgArgs.get(1);
            this.statusType = StatusType.valueOf(msgArgs.get(2));
        } catch(Exception e){
            throw new IllegalArgumentException("Message must be in format of '<KEY>,<VALUE>,<STATUS>'");
        }
    }

    /**
     * Constructs a Message object with given key, value, statusType that
     * forms the message.
     */
    public Message(String key, String value, StatusType statusType){
        this.key = key;
        this.value = value;
        this.statusType = statusType;
        this.msg = String.format("%s,%s,%s", key, value, statusType);
        this.msgBytes = toByteArray(this.msg);
    }

    @Override
    public String getKey() { return this.key; }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public StatusType getStatus() {
        return this.statusType;
    }

    @Override
    public String getMsg() { return this.msg; }

    @Override
    public byte[] getMsgBytes() { return this.msgBytes;}

    private byte[] addCtrChars(byte[] bytes) {
        byte[] ctrBytes = new byte[]{LINE_FEED, RETURN};		//"/n/r"
        byte[] tmp = new byte[bytes.length + ctrBytes.length];

        System.arraycopy(bytes, 0, tmp, 0, bytes.length);
        System.arraycopy(ctrBytes, 0, tmp, bytes.length, ctrBytes.length);

        return tmp;
    }

    private byte[] toByteArray(String s){
        byte[] bytes = s.getBytes();
        byte[] ctrBytes = new byte[]{LINE_FEED, RETURN};
        byte[] tmp = new byte[bytes.length + ctrBytes.length];

        System.arraycopy(bytes, 0, tmp, 0, bytes.length);
        System.arraycopy(ctrBytes, 0, tmp, bytes.length, ctrBytes.length);

        return tmp;
    }
}
