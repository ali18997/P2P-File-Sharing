import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;

public class Server {

    private int peerID;
    private int PieceSize;

    private HashMap<String, BitField> bitFields;
    private HashMap<String, byte[]> files;
    private HashMap<String, byte[]> requestBitFields;
    private FlagObservable flag;
    private FlagObservable flag2;

    private HashMap<Integer, Integer> connectedPeersRates;
    private HashMap<Integer, Boolean> interestedPeers;

    private HashMap<Integer, Integer> stopping;


    public Server(int peerID, int port, HashMap<String, byte[]> requestBitFields, HashMap<String, BitField> bitFields, HashMap<String, byte[]> files, int PieceSize, FlagObservable flag, FlagObservable flag2, HashMap<Integer, Integer> connectedPeersRates, HashMap<Integer, Boolean> interestedPeers, HashMap<Integer, Integer> stopping) {

        this.peerID = peerID;
        this.requestBitFields = requestBitFields;
        this.bitFields = bitFields;
        this.files = files;
        this.PieceSize = PieceSize;
        this.flag = flag;
        this.flag2 = flag2;
        this.connectedPeersRates = connectedPeersRates;
        this.interestedPeers = interestedPeers;
        this.stopping = stopping;

        new Handler(port).start();
    }

    public class Handler extends Thread {
        private int sPort;   //The server will be listening on this port number

        public Handler (int port) {
            sPort = port;
        }

        public void run() {
            ServerSocket listener = null;
            try {
                listener = new ServerSocket(sPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int clientNum = 1;
            try {
                while(true) {
                    try {
                        new ServerFurther.Handler(listener.accept(),clientNum, peerID, requestBitFields, bitFields, files, PieceSize, flag, flag2, connectedPeersRates, interestedPeers).start();
                        stopping.replace(1, stopping.get(1)+1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    clientNum++;
                }
            } finally {
                try {
                    listener.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
