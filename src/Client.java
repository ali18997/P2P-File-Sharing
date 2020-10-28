import java.net.*;
import java.io.*;

public class Client {
    public Socket requestSocket;           //socket connect to the server
    public ObjectOutputStream out;         //stream write to the socket
    public ObjectInputStream in;          //stream read from the socket
    public int ownPort;

    public Client(int port, int ownPort) throws IOException {
        this.ownPort = ownPort;
        requestSocket = new Socket("localhost", port);
        System.out.println("Connected to localhost in port " + port);
        out = new ObjectOutputStream(requestSocket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(requestSocket.getInputStream());
        new Handler().start();
    }

    public void sendMessage(byte[] msg) throws IOException {
        out.writeObject(msg);
        out.flush();
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

    public class Handler extends Thread {
        public void run() {
            try {
                try {
                    while (true) {
                        Object receivedMsg = MessageConversion.deserialize((byte[]) in.readObject());
                        if (receivedMsg instanceof HandshakeMessage) {
                            HandshakeMessage handshakeMessage = (HandshakeMessage) receivedMsg;
                            if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ")) {
                                System.out.println("Peer " + ownPort + " received Successful Handshake from " + handshakeMessage.getPeerID());
                                HandshakeMessage handshakeMessageBack = new HandshakeMessage(ownPort);
                                sendMessage(MessageConversion.serialize(handshakeMessageBack));
                            }
                        }
                    }
                } catch (ClassNotFoundException classnot) {
                    System.err.println("Data received in unknown format");
                }
            } catch (IOException ioException) {
                System.out.println("Disconnect with Peer ");
            }
        }
    }


}
