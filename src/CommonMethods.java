import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class CommonMethods {
    public static void sendHave(int i, BitField currentPeerBitfield, HashMap<String, byte[]> haveBitFields, ObjectOutputStream out) {

        byte[] pieceIndex = ByteBuffer.allocate(4).putInt(i).array();
        Have haveMsg = new Have(currentPeerBitfield.FileName, pieceIndex, new BitField(currentPeerBitfield.FileName, currentPeerBitfield.FileSize, currentPeerBitfield.PieceSize, new byte[currentPeerBitfield.getBitField().length]));
        for (int j = 0; j < haveMsg.getBitField().bitField.length; j++) {
            haveMsg.getBitField().bitField[j] = 0;
        }
        try {
            PayloadMessage pieceHave = new PayloadMessage(MessageConversion.messageToBytes(haveMsg));
            ActualMessage haveMessage = new ActualMessage(1, 4, pieceHave);
            sendMessage(MessageConversion.messageToBytes(haveMessage), out);
            haveBitFields.get(currentPeerBitfield.FileName)[i] = 1;
        } catch (IOException e) {
            System.out.println("Send Have Error:  " + e.toString());
        }

    }

    //send a message to the output stream
    public static void sendMessage(byte[] msg, ObjectOutputStream out) {
        try{
            synchronized (out) {
                out.writeObject(msg);
                out.flush();
            }
        }
        catch(IOException ioException){
            System.out.println("Send Message Error: " + ioException.toString());
        }
    }
}

