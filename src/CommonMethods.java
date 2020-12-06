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
            //System.out.println("Send Have Error:  " + e.toString());
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
            //System.out.println("Send Message Error: " + ioException.toString());
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
        final File folder = new File(System.getProperty("user.dir") + "/" + peerID);
        for (final File fileEntry : folder.listFiles()) {
            if (name.equals(fileEntry.getName())) {
                byte[] bytes = Files.readAllBytes(fileEntry.toPath());
                files.put(name, bytes);
            }
        }
    }

    public static void haveDetect(HashMap<String, BitField> currentPeerBitFields, HashMap<String, BitField> otherPeerBitFields, HashMap<String, byte[]> haveBitFields, ObjectOutputStream out) {
        for (Map.Entry mapElement : currentPeerBitFields.entrySet()) {
            String name = (String) mapElement.getKey();
            BitField bitFieldCurrent = ((BitField) mapElement.getValue());

            if (otherPeerBitFields.containsKey(name)) {
                BitField bitFieldOther = otherPeerBitFields.get(name);
                int length = bitFieldOther.bitField.length;
                for (int i = 0; i < length; i++) {
                    if (haveBitFields.containsKey(name)) {
                        if (bitFieldCurrent.bitField[i] == 1 && haveBitFields.get(name)[i] == 0) {
                            sendHave(i, bitFieldCurrent, haveBitFields, out);
                        }
                    }
                    else{
                        byte[] temp2 = new byte[bitFieldCurrent.getBitField().length];
                        for (int j = 0; j < bitFieldCurrent.getBitField().length; j++) {
                            temp2[j] = 0;
                        }
                        haveBitFields.put(name, temp2);
                        for (int k = 0; k < length; k++) {
                            if (bitFieldCurrent.bitField[k] == 1 && haveBitFields.get(name)[k] == 0) {
                                sendHave(k, bitFieldCurrent, haveBitFields, out);
                            }
                        }
                    }
                }
            }
            else {
                BitField temp = new BitField(name, bitFieldCurrent.getFileSize(), bitFieldCurrent.getPieceSize(), new byte[bitFieldCurrent.getBitField().length]);
                byte[] temp2 = new byte[bitFieldCurrent.getBitField().length];
                for (int i = 0; i < temp.getBitField().length; i++) {
                    temp.getBitField()[i] = 0;
                    temp2[i] = 0;
                }
                otherPeerBitFields.put(name, temp);
                haveBitFields.put(name, temp2);
                BitField bitFieldOther = otherPeerBitFields.get(name);
                int length = bitFieldOther.bitField.length;
                for (int i = 0; i < length; i++) {
                    if (bitFieldCurrent.bitField[i] == 1 && haveBitFields.get(name)[i] == 0) {
                        sendHave(i, bitFieldCurrent, haveBitFields, out);
                    }
                }
            }
        }
    }

    public static void prepareBitFields(int peerID, int PieceSize, HashMap<String, BitField> bitFields) throws IOException {
        final File folder = new File(System.getProperty("user.dir") + "/" + peerID);
        if (!folder.exists()){folder.mkdir();}
        for (final File fileEntry : folder.listFiles()) {
            String fileName = fileEntry.getName();

            int fsize = (int) Files.size(fileEntry.toPath());
            byte[] bitFieldArr = new byte[(int) Math.ceil((double)fsize/PieceSize)];
            for (int i = 0; i < bitFieldArr.length; i++) {
                bitFieldArr[i] = 1;
            }
            BitField bitField = new BitField(fileName, fsize, PieceSize, bitFieldArr);
            bitFields.put(fileName, bitField);
        }
    }

    public static void prepareToReceiveFile(BitField bitField, HashMap<String, BitField> bitFields, HashMap<String, byte[]> files, HashMap<String, byte[]> requestBitFields){
        BitField temp = new BitField(bitField.getFileName(), bitField.getFileSize(), bitField.getPieceSize(), new byte[bitField.getBitField().length]);
        byte[] temp3 = new byte[bitField.getBitField().length];
        for (int i = 0; i < temp.getBitField().length; i++) {
            temp.getBitField()[i] = 0;
            temp3[i] = 0;
        }
        byte[] temp2 = new byte[temp.getFileSize()];
        bitFields.put(bitField.getFileName(), temp);
        files.put(bitField.getFileName(), temp2);
        requestBitFields.put(bitField.getFileName(), temp3);
    }

    public static void requestPiece(Boolean canRequestOtherPeer, HashMap<String, BitField> currentPeerBitField, HashMap<String, BitField> otherPeerBitField, HashMap<String, byte[]> requestBitFields, HashMap<String, byte[]> files, ObjectOutputStream out) throws IOException {
        if(canRequestOtherPeer) {
            outerloop:
            for (Map.Entry mapElement : otherPeerBitField.entrySet()) {
                String name = (String) mapElement.getKey();
                BitField bitFieldClient = ((BitField) mapElement.getValue());

                if (currentPeerBitField.containsKey(name)) {
                    BitField bitFieldServer = currentPeerBitField.get(name);
                    int length = bitFieldServer.bitField.length;
                    for (int i = 0; i < length; i++) {
                        if (bitFieldServer.bitField[i] == 0 && bitFieldClient.bitField[i] == 1 && requestBitFields.get(name)[i] == 0) {
                            requestBitFields.get(name)[i] = 1;
                            byte[] pieceIndex = ByteBuffer.allocate(4).putInt(i).array();
                            Request request = new Request(name, pieceIndex);
                            PayloadMessage pieceRequest = new PayloadMessage(MessageConversion.messageToBytes(request));
                            ActualMessage requestMessage = new ActualMessage(1, 6, pieceRequest);
                            sendMessage(MessageConversion.messageToBytes(requestMessage), out);
                            break outerloop;
                        }
                    }
                } else {
                    prepareToReceiveFile(bitFieldClient, currentPeerBitField, files, requestBitFields);

                    byte[] pieceIndex = ByteBuffer.allocate(4).putInt(0).array();
                    Request request = new Request(name, pieceIndex);
                    PayloadMessage pieceRequest = new PayloadMessage(MessageConversion.messageToBytes(request));
                    ActualMessage requestMessage = new ActualMessage(1, 6, pieceRequest);
                    sendMessage(MessageConversion.messageToBytes(requestMessage), out);
                    break outerloop;
                }
            }
        }
    }

    public static Boolean updateNeighbours(int otherPeerID, HashMap<Integer, Boolean> interestedPeers, Boolean sendToOtherPeer, ObjectOutputStream out) {
        if(interestedPeers.containsKey(otherPeerID)) {
            if (sendToOtherPeer == false && interestedPeers.get(otherPeerID) == true) {
                //UNCHOKE
                ActualMessage unchokeMessage = new ActualMessage(1, 1, null);
                try {
                    sendMessage(MessageConversion.messageToBytes(unchokeMessage), out);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (sendToOtherPeer == true && interestedPeers.get(otherPeerID) == false) {
                //CHOKE
                ActualMessage chokeMessage = new ActualMessage(1, 0, null);
                try {
                    sendMessage(MessageConversion.messageToBytes(chokeMessage), out);
                    return false;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sendToOtherPeer;
    }







}

