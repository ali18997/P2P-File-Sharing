import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

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

    public static void redundantRequests(HashMap<String, BitField> bitFields, HashMap<String, byte[]> requestBitFields){
        for (Map.Entry mapElement : bitFields.entrySet()) {
            String name = (String)mapElement.getKey();
            byte[] bitField = ((BitField)mapElement.getValue()).getBitField();
            byte[] bitField1 = requestBitFields.get(name);
            for (int i = 0; i < bitField.length; i++){
                if(bitField[i] == 0){
                    bitField1[i] = 0;
                }
            }
        }
    }

    public static void readActualFile(String name, HashMap<String, byte[]> files, int peerID) throws IOException {
        final File folder = new File(System.getProperty("user.dir") + "/peerFolder/" + peerID);
        for (final File fileEntry : folder.listFiles()) {
            if (name.equals(fileEntry.getName())) {
                byte[] bytes = Files.readAllBytes(fileEntry.toPath());
                files.put(name, bytes);
            }
        }
    }




}

