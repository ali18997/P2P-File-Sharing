import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Client {
    public Socket requestSocket;           //socket connect to the server
    public ObjectOutputStream out;         //stream write to the socket
    public ObjectInputStream in;          //stream read from the socket
    public int clientPort;
    public int serverPort;
    private Boolean requestFlag = false;


    private HashMap<String, BitField> bitFields = new HashMap<String, BitField>();
    private HashMap<String, byte[]> files = new HashMap<String, byte[]>();
    private HashMap<String, BitField> serverBitFields = new HashMap<String, BitField>();

    public Client(int port, int clientPort) throws IOException {
        this.clientPort = clientPort;
        this.serverPort = port;
        requestSocket = new Socket("localhost", port);
        System.out.println("Connected to localhost in port " + port);
        out = new ObjectOutputStream(requestSocket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(requestSocket.getInputStream());
        new MessageReceiving().start();
    }

    public void sendMessage(byte[] msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }

    public void setFile(HashMap<String, BitField> bitFields, HashMap<String, byte[]> files){
        this.bitFields = bitFields;
        this.files = files;
    }

    public String getMessage() throws IOException, ClassNotFoundException {
        String MESSAGE = (String)in.readObject();
        return MESSAGE;
    }

    public void closeConnection() throws IOException {
        in.close();
        out.close();
        requestSocket.close();
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
        for (Map.Entry mapElement : serverBitFields.entrySet()) {
            String name = (String) mapElement.getKey();
            BitField bitFieldServer = ((BitField) mapElement.getValue());

            if (bitFields.containsKey(name)) {
                BitField bitFieldClient = bitFields.get(name);
                int length = bitFieldClient.bitField.length;
                for (int i = 0; i < length; i++) {
                    if (bitFieldClient.bitField[i] == 0 && bitFieldServer.bitField[i] == 1) {
                        byte[] pieceIndex = ByteBuffer.allocate(4).putInt(i).array();
                        Request request = new Request(name, pieceIndex);
                        PayloadMessage pieceRequest = new PayloadMessage(MessageConversion.messageToBytes(request));
                        ActualMessage requestMessage = new ActualMessage(1, 6, pieceRequest);
                        sendMessage(MessageConversion.messageToBytes(requestMessage));
                        break outerloop;
                    }
                }
            } else {
                prepareToReceiveFile(bitFieldServer);

                byte[] pieceIndex = ByteBuffer.allocate(4).putInt(0).array();
                Request request = new Request(name, pieceIndex);
                PayloadMessage pieceRequest = new PayloadMessage(MessageConversion.messageToBytes(request));
                ActualMessage requestMessage = new ActualMessage(1, 6, pieceRequest);
                sendMessage(MessageConversion.messageToBytes(requestMessage));

                break outerloop;
            }
        }
    }

    public class MessageReceiving extends Thread {
        public void run() {
            try {
                try {
                    while (true) {
                        Object receivedMsg = MessageConversion.bytesToMessage((byte[]) in.readObject());
                        if (receivedMsg instanceof HandshakeMessage) {
                            HandshakeMessage handshakeMessage = (HandshakeMessage) receivedMsg;
                            if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ")) {
                                System.out.println("Peer " + clientPort + " received Successful Handshake from " + handshakeMessage.getPeerID());
                                HandshakeMessage handshakeMessageBack = new HandshakeMessage(clientPort);
                                sendMessage(MessageConversion.messageToBytes(handshakeMessageBack));
                                for (Map.Entry mapElement : bitFields.entrySet()) {
                                    String name = (String)mapElement.getKey();
                                    BitField bitField = ((BitField)mapElement.getValue());
                                    ActualMessage bitFieldMessage = new ActualMessage(files.get(name).length, 5, new PayloadMessage(MessageConversion.messageToBytes(bitField)));
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
                                System.out.println("Peer " + clientPort + " received UnChoke Message from " + serverPort);
                                requestPiece();
                            }
                            else if (actualMessage.getMessageType() == 2) {
                                //Interested
                                //IMPLEMENT CHOKING AND UNCHOKING LATER ON
                                System.out.println("Peer " + clientPort + " received interested Message from " + serverPort);

                                ActualMessage unchokeMessage = new ActualMessage(1, 1, null);
                                sendMessage(MessageConversion.messageToBytes(unchokeMessage));
                            }
                            else if (actualMessage.getMessageType() == 3) {
                                System.out.println("Peer " + clientPort + " received not interested Message from " + serverPort);
                            }
                            else if (actualMessage.getMessageType() == 4) {
                                //HAVE
                            }
                            else if (actualMessage.getMessageType() == 5) {
                                System.out.println("Peer " + clientPort + " received Bitfield Message from " + serverPort);

                                BitField bitField = (BitField)MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());

                                serverBitFields.put(bitField.getFileName(), bitField);

                                if (requestFlag == false) {
                                    requestFlag = true;
                                    Boolean flag = true;
                                    outerloop:
                                    for (Map.Entry mapElement : serverBitFields.entrySet()) {
                                        String name = (String) mapElement.getKey();
                                        BitField bitFieldServer = ((BitField) mapElement.getValue());

                                        if (bitFields.containsKey(name)) {
                                            BitField bitFieldClient = bitFields.get(name);
                                            int length = bitFieldClient.bitField.length;
                                            for (int i = 0; i < length; i++) {
                                                if (bitFieldClient.bitField[i] == 0 && bitFieldServer.bitField[i] == 1) {
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
                                            prepareToReceiveFile(bitFieldServer);
                                            break outerloop;
                                        }
                                    }
                                    if (flag) {
                                        ActualMessage notInterestMessage = new ActualMessage(1, 3, null);
                                        sendMessage(MessageConversion.messageToBytes(notInterestMessage));
                                    }
                                }

                            }
                            else if (actualMessage.getMessageType() == 6) {
                                //REQUEST

                                Request msg = (Request) MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());
                                String name = msg.FileName;
                                int pieceNum = ByteBuffer.wrap(msg.pieceIndex).getInt();

                                System.out.println("Peer " + clientPort + " received Request Message from " + serverPort + " for file: " + name + " piece: " + pieceNum);

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
                                System.out.println("Peer " + clientPort + " received Piece: " + pieceNum + " of File: " + fname + " from "  + serverPort);


                                byte[] piece = a.getPiece();
                                for (int i = 0; i < bitFields.get(fname).getPieceSize(); i++) {
                                    if(pieceNum*bitFields.get(fname).getPieceSize() + i < files.get(fname).length) {
                                        files.get(fname)[pieceNum * bitFields.get(fname).getPieceSize() + i] = piece[i];
                                    }
                                }
                                bitFields.get(fname).getBitField()[pieceNum] = 1;

                                if (pieceNum == bitFields.get(fname).getBitField().length-1) {
                                    System.out.println("Peer " + clientPort + " received complete file " + fname + " from "  + serverPort);

                                    Files.write(Path.of(System.getProperty("user.dir") + "/peerFolder/" + clientPort + "/" + fname), files.get(fname));
                                }
                                requestPiece();
                            }
                        }
                    }
                } catch (ClassNotFoundException classnot) {
                    System.err.println("Data received in unknown format");
                }
            } catch (IOException ioException) {
                System.out.println("Disconnect with Peer " + ioException);
            }
        }
    }


}
