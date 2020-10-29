import java.io.Serializable;

public class ActualMessage implements Serializable {
    private int messageLength;
    private byte messageType;
    private PayloadMessage payload;


    public ActualMessage (int messageLength, int type, PayloadMessage payload) {
        if (type == 0 || type == 1 ||type == 2 ||type == 3) {
            this.payload = null;
        }
        else if (type == 4) {
            byte[] pieceIndex =  new byte[4];
            this.payload = payload;
        }
        else if (type == 5) {
            byte[] bitField = new byte[16];
            this.payload = payload;
        }
        else if(type == 6) {
            byte[] pieceIndex = new byte[4];
            this.payload = payload;
        }
        else if(type == 7) {
            byte[] pieceIndex = new byte[4];
            byte[] piece = new byte[200];
            byte[] overall = new byte[pieceIndex.length + piece.length];
            System.arraycopy(pieceIndex, 0, overall, 0, pieceIndex.length);
            System.arraycopy(piece, 0, overall, pieceIndex.length, piece.length);
            this.payload = payload;
        }
        this.messageLength = messageLength;
        this.messageType = (byte) type;
    }

    public byte getMessageType() {
        return messageType;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public PayloadMessage getPayload() {
        return payload;
    }
}
