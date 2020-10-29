import messages.Handshake;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ServerFurther {

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for dealing with a single client's requests.
     */
    public static class Handler extends Thread {
        private String message;    //message received from the client
        private String MESSAGE;    //uppercase message send to the client
        private Socket connection;
        private ObjectInputStream in;	//stream read from the socket
        private ObjectOutputStream out;    //stream write to the socket
        private int no;		//The index number of the client
        private int sPort;
        private int clientPort;

        private HashMap<Integer, Boolean> handshakes = new HashMap<Integer, Boolean>();

        public Handler(Socket connection, int no, int sPort) {
            this.connection = connection;
            this.no = no;
            this.sPort = sPort;
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
                                System.out.println("Peer " + sPort + " received Successful Handshake from " + clientPort);
                                HandshakeMessage handshakeMessageBack = new HandshakeMessage(sPort);
                                handshakes.put(clientPort, true);
                                sendMessage(MessageConversion.messageToBytes(handshakeMessageBack));
                            }
                            else if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ") && handshakes.get(clientPort) == true) {
                                System.out.println("Peer " + sPort + " Completed Handshake from " + clientPort);

                                //LATER ON IMPLEMENT ONLY SENDING IF THERE ARE PIECES
                                ActualMessage bitFieldMessage = new ActualMessage(16, 5);
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
                                System.out.println("Peer " + sPort + " received interested Message from " + clientPort);
                            }
                            else if (actualMessage.getMessageType() == 3) {
                                System.out.println("Peer " + sPort + " received not interested Message from " + clientPort);
                            }
                            else if (actualMessage.getMessageType() == 4) {
                                //HAVE
                            }
                            else if (actualMessage.getMessageType() == 5) {
                                System.out.println("Peer " + sPort + " received Bitfield Message from " + clientPort);

                                //LATER ON IMPLEMENT INTEREST OR NOT INTEREST
                                ActualMessage interestMessage = new ActualMessage(1, 2);
                                sendMessage(MessageConversion.messageToBytes(interestMessage));
                            }
                            else if (actualMessage.getMessageType() == 6) {
                                //REQUEST
                            }
                            else if (actualMessage.getMessageType() == 7) {
                                //PIECE
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
