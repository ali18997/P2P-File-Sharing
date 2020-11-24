import java.net.*;
import java.io.*;
import java.util.*;

public class ServerFurther {


    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for dealing with a single client's requests.
     */
    public static class Handler extends Thread {
        private Socket connection;
        private ObjectInputStream in;    //stream read from the socket
        private ObjectOutputStream out;    //stream write to the socket
        private int no;        //The index number of the client
        private int peerID;
        private int otherPeerID;

        private HashMap<String, BitField> bitFields;
        private HashMap<Integer, Boolean> handshakes = new HashMap<Integer, Boolean>();
        private HashMap<Integer, Integer> connectedPeersRates;
        private ActualMessageProcessor actualMessageProcessor;

        public Handler(Socket connection, int no, int peerID, HashMap<String, byte[]> requestBitFields, HashMap<String, BitField> bitFields, HashMap<String, byte[]> files, int PieceSize, FlagObservable flagHave, FlagObservable flagNeighbours, HashMap<Integer, Integer> connectedPeersRates, HashMap<Integer, Boolean> interestedPeers) throws IOException {
            this.connection = connection;
            this.no = no;
            this.peerID = peerID;
            this.bitFields = bitFields;
            this.connectedPeersRates = connectedPeersRates;

            out = new ObjectOutputStream(connection.getOutputStream());
            out.flush();
            in = new ObjectInputStream(connection.getInputStream());

            actualMessageProcessor = new ActualMessageProcessor(peerID, otherPeerID, bitFields, requestBitFields, this.connectedPeersRates,
                    interestedPeers, files, flagHave, flagNeighbours, PieceSize, out);
        }

        public void run() {
            try {
                while (true) {
                    Object receivedMsg = MessageConversion.bytesToMessage((byte[]) in.readObject());
                    if (receivedMsg instanceof HandshakeMessage) {
                        HandshakeMessage handshakeMessage = (HandshakeMessage) receivedMsg;
                        otherPeerID = handshakeMessage.getPeerID();
                        if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ") && handshakes.get(otherPeerID) == null) {
                            HandshakeMessage handshakeMessageBack = new HandshakeMessage(peerID);
                            handshakes.put(otherPeerID, true);
                            CommonMethods.sendMessage(MessageConversion.messageToBytes(handshakeMessageBack), out);
                        } else if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ") && handshakes.get(otherPeerID) == true) {
                            System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] is connected from Peer [" + otherPeerID + "]");
                            connectedPeersRates.put(otherPeerID, 0);

                            for (Map.Entry mapElement : bitFields.entrySet()) {
                                String name = (String) mapElement.getKey();
                                BitField bitField = ((BitField) mapElement.getValue());
                                ActualMessage bitFieldMessage = new ActualMessage(1, 5, new PayloadMessage(MessageConversion.messageToBytes(bitField)));
                                CommonMethods.sendMessage(MessageConversion.messageToBytes(bitFieldMessage), out);
                            }
                        }
                    } else if (receivedMsg instanceof ActualMessage) {
                        ActualMessage actualMessage = (ActualMessage) receivedMsg;
                        actualMessageProcessor.process(actualMessage);
                    }
                }
            } catch (ClassNotFoundException classnot) {
                System.out.println("Server Error 2 " + classnot.toString());
            } catch (IOException ioException) {
                System.out.println("Disconnect with Client " + no);
            } finally {
                //Close connections
                try {
                    in.close();
                    out.close();
                    connection.close();
                } catch (IOException ioException) {
                    System.out.println("Disconnect with Client " + no);
                }
            }


        }

    }
}
