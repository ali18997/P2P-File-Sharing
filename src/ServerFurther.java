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
                        Object receivedMsg = MessageConversion.deserialize((byte[]) in.readObject());
                        if (receivedMsg instanceof HandshakeMessage) {
                            HandshakeMessage handshakeMessage = (HandshakeMessage) receivedMsg;
                            if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ") && handshakes.get(handshakeMessage.getPeerID()) == null) {
                                System.out.println("Peer " + sPort + " received Successful Handshake from " + handshakeMessage.getPeerID());
                                HandshakeMessage handshakeMessageBack = new HandshakeMessage(sPort);
                                handshakes.put(handshakeMessage.getPeerID(), true);
                                sendMessage(MessageConversion.serialize(handshakeMessageBack));
                            }
                            else if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ") && handshakes.get(handshakeMessage.getPeerID()) == true) {
                                System.out.println("Peer " + sPort + " Completed Handshake from " + handshakeMessage.getPeerID());
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
