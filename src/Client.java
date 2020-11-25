import java.net.*;
import java.io.*;
import java.util.*;

public class Client {
    public Socket requestSocket;           //socket connect to the server
    public ObjectOutputStream out;         //stream write to the socket
    public ObjectInputStream in;          //stream read from the socket

    private int peerID;
    private int otherPeerID;
    private String otherPeerHostName;

    private HashMap<String, BitField> bitFields;
    private HashMap<Integer, Integer> connectedPeersRates;
    private ActualMessageProcessor actualMessageProcessor;


    public Client(int otherPeerID, String otherPeerHostName, int otherPeerPort, int peerID, HashMap<String, byte[]> requestBitFields, HashMap<String, BitField> bitFields, HashMap<String, byte[]> files, int PieceSize, FlagObservable flagHave, FlagObservable flagNeighbours, HashMap<Integer, Integer> connectedPeersRates, HashMap<Integer, Boolean> interestedPeers) throws IOException {
        this.bitFields = bitFields;
        this.peerID = peerID;
        this.otherPeerID = otherPeerID;
        this.connectedPeersRates = connectedPeersRates;
        this.otherPeerHostName = otherPeerHostName;

        requestSocket = new Socket(this.otherPeerHostName, otherPeerPort);
        out = new ObjectOutputStream(requestSocket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(requestSocket.getInputStream());
        actualMessageProcessor = new ActualMessageProcessor(peerID, otherPeerID, bitFields, requestBitFields, this.connectedPeersRates,
                interestedPeers, files, flagHave, flagNeighbours, PieceSize, out);
        new MessageReceiving().start();
    }

    public void handShake() throws IOException {
        HandshakeMessage handshakeMessage = new HandshakeMessage(otherPeerID);
        CommonMethods.sendMessage(MessageConversion.messageToBytes(handshakeMessage), out);
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

                    while (true) {
                        Object receivedMsg = null;
                        try {
                            receivedMsg = MessageConversion.bytesToMessage((byte[]) in.readObject());
                            if (receivedMsg instanceof HandshakeMessage) {
                                HandshakeMessage handshakeMessage = (HandshakeMessage) receivedMsg;
                                if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ")) {
                                    Logging.writeLog(peerID, "[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] makes a connection to Peer [" + otherPeerID + "]");
                                    HandshakeMessage handshakeMessageBack = new HandshakeMessage(peerID);
                                    try {
                                        CommonMethods.sendMessage(MessageConversion.messageToBytes(handshakeMessageBack), out);
                                        connectedPeersRates.put(otherPeerID, 0);
                                    } catch (IOException e) {
                                        System.out.println("Client Error 4 " + e.toString());
                                    }
                                    for (Map.Entry mapElement : bitFields.entrySet()) {
                                        String name = (String)mapElement.getKey();
                                        BitField bitField = ((BitField)mapElement.getValue());
                                        ActualMessage bitFieldMessage = null;
                                        try {
                                            bitFieldMessage = new ActualMessage(1, 5, new PayloadMessage(MessageConversion.messageToBytes(bitField)));
                                        } catch (IOException e) {
                                            System.out.println("Client Error 41 " + e.toString());
                                        }
                                        try {
                                            CommonMethods.sendMessage(MessageConversion.messageToBytes(bitFieldMessage), out);
                                        } catch (IOException e) {
                                            System.out.println("Client Error 5 " + e.toString());
                                        }
                                    }

                                }
                            }
                            else if (receivedMsg instanceof ActualMessage) {
                                ActualMessage actualMessage = (ActualMessage) receivedMsg;
                                actualMessageProcessor.process(actualMessage);
                            }
                        } catch (IOException e) {
                            System.out.println("Client Error 2 " + e.toString() + " " + receivedMsg + "Client " + peerID) ;
                        } catch (ClassNotFoundException e) {
                            System.out.println("Client Error 3 " + e.toString());
                        }

                    }
        }
    }


}
