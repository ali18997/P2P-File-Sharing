package messages;

public abstract class Message {
    private  byte[] message_length;
    private  byte message_type;
	private byte[] messagePayload;

    public byte[] getMessage_length() {
		return message_length;
	}

	public byte getMessage_type() {
		return message_type;
	}

	public byte[] getMessagePayload() {
		return messagePayload;
	}


    public Message(byte message_type) {
        this.message_type = message_type;
        this.message_length = MessageUtil.intToByteArray(1);
        this.messagePayload = null;
    }
    
    public Message(byte message_type, byte[] messagePayload) {
        this.message_type = message_type;
        this.messagePayload = messagePayload;
        this.message_length = MessageUtil.intToByteArray(messagePayload.length + 1);

    }
}
