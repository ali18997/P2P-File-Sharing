
import java.io.*;
import java.util.HashMap;


public class Peer {
    private int peerPort;   //The server will be listening on this port number
    private Server server;

    private HashMap<Integer, Client> clients = new HashMap<Integer, Client>();
    private HashMap<String, byte[]> requestBitFields = new HashMap<String, byte[]>();
    private HashMap<String, BitField> bitFields = new HashMap<String, BitField>();
    private HashMap<String, byte[]> files = new HashMap<String, byte[]>();
    private int PieceSize = 3;
    FlagObservable flag = new FlagObservable(true);

    public Peer(int port) throws IOException {
        peerPort = port;
        server = new Server(peerPort, requestBitFields, bitFields, files, PieceSize, flag);
    }

    public void connectToPeer(int port) throws IOException {
        Client temp = new Client(port, peerPort, requestBitFields, bitFields, files, PieceSize, flag);
        clients.put(port, temp);
        handShakePeer(port);
    }

    public void handShakePeer(int port) throws IOException {
        HandshakeMessage handshakeMessage = new HandshakeMessage(peerPort);
        clients.get(port).sendMessage(MessageConversion.messageToBytes(handshakeMessage));
    }

    public void messageToPeer(int port, String msg) throws IOException {
        clients.get(port).sendMessage(msg.getBytes());
    }

    public void disconnectPeer(int port) throws IOException {
        clients.get(port).closeConnection();
    }

}

