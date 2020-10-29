

import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Client {
    public Socket requestSocket;           //socket connect to the server
    public ObjectOutputStream out;         //stream write to the socket
    public ObjectInputStream in;          //stream read from the socket
    public int ownPort;
    public int othersPort;

    public BitField bitfield;
    public byte[] file;

    public Client(int port, int ownPort) throws IOException {
        this.ownPort = ownPort;
        this.othersPort = port;
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

    public void setFile(BitField bitfield, byte[] file){
        this.bitfield = bitfield;
        this.file = file;
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

    public class MessageReceiving extends Thread {
        public void run() {
            try {
                try {
                    while (true) {
                        Object receivedMsg = MessageConversion.bytesToMessage((byte[]) in.readObject());
                        if (receivedMsg instanceof HandshakeMessage) {
                            HandshakeMessage handshakeMessage = (HandshakeMessage) receivedMsg;
                            if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ")) {
                                System.out.println("Peer " + ownPort + " received Successful Handshake from " + handshakeMessage.getPeerID());
                                HandshakeMessage handshakeMessageBack = new HandshakeMessage(ownPort);
                                sendMessage(MessageConversion.messageToBytes(handshakeMessageBack));

                                //LATER ON IMPLEMENT ONLY SENDING IF THERE ARE PIECES
                                ActualMessage bitFieldMessage = new ActualMessage(file.length, 5, new PayloadMessage(MessageConversion.messageToBytes(bitfield)));
                                sendMessage(MessageConversion.messageToBytes(bitFieldMessage));
                            }
                        }
                        else if (receivedMsg instanceof ActualMessage) {
                            ActualMessage actualMessage = (ActualMessage) receivedMsg;
                            if (actualMessage.getMessageType() == 0) {
                                //CHOKE
                            }
                            else if (actualMessage.getMessageType() == 1) {
                                //UNCHOKE
                            }
                            else if (actualMessage.getMessageType() == 2) {
                                System.out.println("Peer " + ownPort + " received interested Message from " + othersPort);

                                ActualMessage unchokeMessage = new ActualMessage(1, 1, null);
                                sendMessage(MessageConversion.messageToBytes(unchokeMessage));
                            }
                            else if (actualMessage.getMessageType() == 3) {
                                System.out.println("Peer " + ownPort + " received not interested Message from " + othersPort);
                            }
                            else if (actualMessage.getMessageType() == 4) {
                                //HAVE
                            }
                            else if (actualMessage.getMessageType() == 5) {
                                System.out.println("Peer " + ownPort + " received Bitfield Message from " + othersPort);

                                //LATER ON IMPLEMENT INTEREST OR NOT INTEREST
                                ActualMessage interestMessage = new ActualMessage(1, 3, null);
                                sendMessage(MessageConversion.messageToBytes(interestMessage));
                            }
                            else if (actualMessage.getMessageType() == 6) {
                                //REQUEST

                                int pieceNum = ByteBuffer.wrap(actualMessage.getPayload().getMessage()).getInt();

                                System.out.println("Peer " + ownPort + " received Request Message from " + othersPort + " for piece " + pieceNum);

                                if (bitfield.bitField[pieceNum] == 1) {
                                    byte[] piece = Arrays.copyOfRange(file, pieceNum*bitfield.PieceSize, pieceNum*bitfield.PieceSize + bitfield.PieceSize);
                                    Piece pieceMsg = new Piece(actualMessage.getPayload().getMessage() ,piece);
                                    ActualMessage interestMessage = new ActualMessage(1, 7, new PayloadMessage(MessageConversion.messageToBytes(pieceMsg)));
                                    sendMessage(MessageConversion.messageToBytes(interestMessage));
                                }

                            }
                            else if (actualMessage.getMessageType() == 7) {
                                //PIECE
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
