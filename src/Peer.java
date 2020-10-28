import java.io.*;
import java.util.HashMap;


public class Peer {
    private int peerPort;   //The server will be listening on this port number
    private Server server;
    private HashMap<Integer, Client> clients = new HashMap<Integer, Client>();

    public Peer(int port) throws IOException {
        peerPort = port;
        server = new Server(peerPort);
    }

    public void connectToPeer(int port) throws IOException {
        clients.put(port, new Client(port, peerPort));
    }

    public void handShakePeer(int port) throws IOException {
        HandshakeMessage handshakeMessage = new HandshakeMessage(peerPort);
        clients.get(port).sendMessage(MessageConversion.serialize(handshakeMessage));
    }

    public void messageToPeer(int port, String msg) throws IOException {
        clients.get(port).sendMessage(msg.getBytes());
    }

    public void disconnectPeer(int port) throws IOException {
        clients.get(port).closeConnection();
    }






}

