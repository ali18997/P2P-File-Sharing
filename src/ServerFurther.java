
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ServerFurther {



    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for dealing with a single client's requests.
     */
    public static class Handler extends Thread {
        private Socket connection;
        private ObjectInputStream in;	//stream read from the socket
        private ObjectOutputStream out;    //stream write to the socket
        private int no;		//The index number of the client
        private int serverPort;
        private int clientPort;
        private Boolean requestFlag = false;

        private HashMap<String, BitField> bitFields = new HashMap<String, BitField>();
        private HashMap<String, byte[]> files = new HashMap<String, byte[]>();
        private HashMap<String, BitField> clientBitFields = new HashMap<String, BitField>();

        private HashMap<Integer, Boolean> handshakes = new HashMap<Integer, Boolean>();

        public Handler(Socket connection, int no, int serverPort) throws IOException {
            this.connection = connection;
            this.no = no;
            this.serverPort = serverPort;
            prepareBitFields();
        }


        public void readActualFile(String name) throws IOException {
            final File folder = new File(System.getProperty("user.dir") + "/peerFolder/" + serverPort);
            for (final File fileEntry : folder.listFiles()) {
                if (name.equals(fileEntry.getName())) {
                    byte[] bytes = Files.readAllBytes(fileEntry.toPath());
                    files.put(name, bytes);
                }
            }
        }

        public void prepareBitFields() throws IOException {
            final File folder = new File(System.getProperty("user.dir") + "/peerFolder/" + serverPort);
            for (final File fileEntry : folder.listFiles()) {
                String fileName = fileEntry.getName();
                int PieceSize = 2;
                int fsize = (int) Files.size(fileEntry.toPath());
                byte[] bitFieldArr = new byte[(int) Math.ceil((double)fsize/PieceSize)];
                for (int i = 0; i < bitFieldArr.length; i++) {
                    bitFieldArr[i] = 1;
                }
                BitField bitField = new BitField(fileName, fsize, PieceSize, bitFieldArr);
                this.bitFields.put(fileName, bitField);
            }
        }


        public void prepareToReceiveFile(BitField bitField){
            BitField temp = new BitField(bitField.getFileName(), bitField.getFileSize(), bitField.getPieceSize(), new byte[bitField.getBitField().length]);
            for (int i = 0; i < temp.getBitField().length; i++) {
                temp.getBitField()[i] = 0;
            }
            byte[] temp2 = new byte[temp.getFileSize()];
            bitFields.put(bitField.getFileName(), temp);
            files.put(bitField.getFileName(), temp2);
        }

        public void requestPiece() throws IOException {
            outerloop:
            for (Map.Entry mapElement : clientBitFields.entrySet()) {
                String name = (String)mapElement.getKey();
                BitField bitFieldClient = ((BitField)mapElement.getValue());

                if (bitFields.containsKey(name)){
                    BitField bitFieldServer = bitFields.get(name);
                    int length = bitFieldServer.bitField.length;
                    for (int i = 0; i < length; i++) {
                        if (bitFieldServer.bitField[i] == 0 &&  bitFieldClient.bitField[i] == 1) {
                            byte[] pieceIndex = ByteBuffer.allocate(4).putInt(i).array();
                            Request request = new Request(name, pieceIndex);
                            PayloadMessage pieceRequest = new PayloadMessage(MessageConversion.messageToBytes(request));
                            ActualMessage requestMessage = new ActualMessage(1, 6, pieceRequest);
                            sendMessage(MessageConversion.messageToBytes(requestMessage));
                            break outerloop;
                        }
                    }
                }
                else {
                    prepareToReceiveFile(bitFieldClient);

                    byte[] pieceIndex = ByteBuffer.allocate(4).putInt(0).array();
                    Request request = new Request(name, pieceIndex);
                    PayloadMessage pieceRequest = new PayloadMessage(MessageConversion.messageToBytes(request));
                    ActualMessage requestMessage = new ActualMessage(1, 6, pieceRequest);
                    sendMessage(MessageConversion.messageToBytes(requestMessage));

                    break outerloop;
                }
            }
        }

        public void run() {
            try{
                //initialize Input and Output streams
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());
                try{
                    while(true)
                    {
                        Object receivedMsg = MessageConversion.bytesToMessage((byte[]) in.readObject());
                        if (receivedMsg instanceof HandshakeMessage) {
                            HandshakeMessage handshakeMessage = (HandshakeMessage) receivedMsg;
                            clientPort = handshakeMessage.getPeerID();
                            if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ") && handshakes.get(clientPort) == null) {
                                System.out.println("Peer " + serverPort + " received Successful Handshake from " + clientPort);
                                HandshakeMessage handshakeMessageBack = new HandshakeMessage(serverPort);
                                handshakes.put(clientPort, true);
                                sendMessage(MessageConversion.messageToBytes(handshakeMessageBack));
                            }
                            else if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ") && handshakes.get(clientPort) == true) {
                                System.out.println("Peer " + serverPort + " Completed Handshake from " + clientPort);

                                //LATER ON IMPLEMENT ONLY SENDING IF THERE ARE PIECES

                                for (Map.Entry mapElement : bitFields.entrySet()) {
                                    String name = (String)mapElement.getKey();
                                    BitField bitField = ((BitField)mapElement.getValue());
                                    ActualMessage bitFieldMessage = new ActualMessage(1, 5, new PayloadMessage(MessageConversion.messageToBytes(bitField)));
                                    sendMessage(MessageConversion.messageToBytes(bitFieldMessage));
                                }
                            }
                        }
                        else if (receivedMsg instanceof ActualMessage) {
                            ActualMessage actualMessage = (ActualMessage) receivedMsg;
                            if (actualMessage.getMessageType() == 0) {
                                //CHOKE
                            }
                            else if (actualMessage.getMessageType() == 1) {
                                //UNCHOKE
                                System.out.println("Peer " + serverPort + " received UnChoke Message from " + clientPort);
                                requestPiece();
                            }
                            else if (actualMessage.getMessageType() == 2) {
                                System.out.println("Peer " + serverPort + " received interested Message from " + clientPort);
                                //Interested
                                //IMPLEMENT CHOKING AND UNCHOKING LATER ON
                                ActualMessage unchokeMessage = new ActualMessage(1, 1, null);
                                sendMessage(MessageConversion.messageToBytes(unchokeMessage));
                            }
                            else if (actualMessage.getMessageType() == 3) {
                                System.out.println("Peer " + serverPort + " received not interested Message from " + clientPort);
                            }
                            else if (actualMessage.getMessageType() == 4) {
                                //HAVE
                            }
                            else if (actualMessage.getMessageType() == 5) {
                                System.out.println("Peer " + serverPort + " received Bitfield Message from " + clientPort);

                                BitField bitField = (BitField)MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());

                                clientBitFields.put(bitField.getFileName(), bitField);

                                if(requestFlag == false) {
                                    requestFlag = true;
                                    Boolean flag = true;
                                    outerloop:
                                    for (Map.Entry mapElement : clientBitFields.entrySet()) {
                                        String name = (String) mapElement.getKey();
                                        BitField bitFieldClient = ((BitField) mapElement.getValue());

                                        if (bitFields.containsKey(name)) {
                                            BitField bitFieldServer = bitFields.get(name);
                                            int length = bitFieldServer.bitField.length;
                                            for (int i = 0; i < length; i++) {
                                                if (bitFieldServer.bitField[i] == 0 && bitFieldClient.bitField[i] == 1) {
                                                    ActualMessage interestMessage = new ActualMessage(1, 2, null);
                                                    sendMessage(MessageConversion.messageToBytes(interestMessage));
                                                    flag = false;
                                                    break outerloop;
                                                }
                                            }
                                        } else {
                                            ActualMessage interestMessage = new ActualMessage(1, 2, null);
                                            sendMessage(MessageConversion.messageToBytes(interestMessage));
                                            flag = false;
                                            prepareToReceiveFile(bitFieldClient);
                                            break outerloop;
                                        }
                                    }
                                    if (flag) {
                                        ActualMessage notInterestMessage = new ActualMessage(1, 3, null);
                                        sendMessage(MessageConversion.messageToBytes(notInterestMessage));
                                        requestFlag = false;
                                    }
                                }

                            }
                            else if (actualMessage.getMessageType() == 6) {
                                //REQUEST

                                Request msg = (Request) MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());
                                String name = msg.FileName;

                                if(!files.containsKey(name)){
                                    readActualFile(name);
                                }

                                int pieceNum = ByteBuffer.wrap(msg.pieceIndex).getInt();

                                System.out.println("Peer " + serverPort + " received Request Message from " + clientPort + " for file: " + name + " piece: " + pieceNum);

                                if (bitFields.get(name).bitField[pieceNum] == 1) {
                                    byte[] piece = Arrays.copyOfRange(files.get(name), pieceNum*bitFields.get(name).PieceSize, pieceNum*bitFields.get(name).PieceSize + bitFields.get(name).PieceSize);
                                    Piece pieceMsg = new Piece(name, msg.pieceIndex ,piece);
                                    ActualMessage interestMessage = new ActualMessage(1, 7, new PayloadMessage(MessageConversion.messageToBytes(pieceMsg)));
                                    sendMessage(MessageConversion.messageToBytes(interestMessage));
                                }
                            }
                            else if (actualMessage.getMessageType() == 7) {
                                //PIECE
                                Piece a = (Piece) MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());
                                int pieceNum = ByteBuffer.wrap(a.getPieceIndex()).getInt();
                                String fname = a.getName();
                                System.out.println("Peer " + serverPort + " received Piece: " + pieceNum + " of File: " + fname + " from "  + clientPort);


                                byte[] piece = a.getPiece();
                                for (int i = 0; i < bitFields.get(fname).getPieceSize(); i++) {
                                    if(pieceNum*bitFields.get(fname).getPieceSize() + i < files.get(fname).length) {
                                        files.get(fname)[pieceNum * bitFields.get(fname).getPieceSize() + i] = piece[i];
                                    }
                                }
                                bitFields.get(fname).getBitField()[pieceNum] = 1;

                                if (pieceNum == bitFields.get(fname).getBitField().length-1) {
                                    System.out.println("Peer " + serverPort + " received complete file " + fname + " from "  + clientPort);

                                    Files.write(Path.of(System.getProperty("user.dir") + "/peerFolder/" + serverPort + "/" + fname), files.get(fname));
                                }
                                requestPiece();
                            }
                        }
                    }
                }
                catch(ClassNotFoundException classnot){
                    System.err.println("Data received in unknown format");
                }
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client " + no);
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    connection.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with Client " + no);
                }
            }
        }

        //send a message to the output stream
        public void sendMessage(byte[] msg)
        {
            try{
                out.writeObject(msg);
                out.flush();
                //System.out.println("Send message: " + msg + " to Client " + no);
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }

    }

}
