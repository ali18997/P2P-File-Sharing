package messages;

public class MessagePayload {
    private byte[] payload;

    public byte[] getPayload(){
        return payload;
    }

    public MessagePayload(byte[] payload) {
        this.payload = payload;
    }

    public MessagePayload () {
        this.payload = null;
    }
}
