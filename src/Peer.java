
import java.io.*;
import java.nio.file.Files;
import java.util.*;


public class Peer {

    private int peerID;
    private String hostName;
    private int peerPort;   //The server will be listening on this port number
    private Server server;

    private HashMap<Integer, Client> clients = new HashMap<Integer, Client>();
    private HashMap<String, byte[]> requestBitFields = new HashMap<String, byte[]>();
    private HashMap<String, BitField> bitFields = new HashMap<String, BitField>();
    private HashMap<String, byte[]> files = new HashMap<String, byte[]>();
    private HashMap<Integer, Integer> connectedPeersRates = new HashMap<Integer, Integer>();
    private HashMap<Integer, Boolean> interestedPeers = new HashMap<Integer, Boolean>();


    private int PieceSize;
    private int k;
    private int p;
    private int m;

    FlagObservable flag = new FlagObservable(true);
    FlagObservable flag2 = new FlagObservable(true);

    public Peer(int peerID, int port) throws IOException {
        peerPort = port;
        this.peerID = peerID;
        readCommon();


        server = new Server(this.peerID, peerPort, requestBitFields, bitFields, files, PieceSize, flag, flag2, connectedPeersRates, interestedPeers);
        Timer timer = new Timer();
        timer.schedule(new preferredNeighbours(), 0, p*1000);
        timer.schedule(new optimisticallyUnchokedNeighbor(), 0, m*1000);
    }

    public void readCommon() throws IOException {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("Common.cfg"));
            String line = reader.readLine();
            while (line != null) {
                String[] splitted = line.split(" ");
                if (splitted[0].equals("NumberOfPreferredNeighbors")) {
                    k = Integer.parseInt(splitted[1]);
                }
                else if (splitted[0].equals("UnchokingInterval"))  {
                    p = Integer.parseInt(splitted[1]);
                }
                else if (splitted[0].equals("OptimisticUnchokingInterval"))  {
                    m = Integer.parseInt(splitted[1]);
                }
                else if (splitted[0].equals("PieceSize"))  {
                    PieceSize = Integer.parseInt(splitted[1]);
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connectToPeer(int otherPeerID, String otherPeerHostName, int otherPeerPort) throws IOException {
        Client temp = new Client(otherPeerID, otherPeerHostName, otherPeerPort, this.peerID, requestBitFields, bitFields, files, PieceSize, flag, flag2, connectedPeersRates, interestedPeers);
        clients.put(otherPeerPort, temp);
        handShakePeer(otherPeerPort);
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

    public int getHighestInterested() {
        int highestRate = -1;
        int highestPeer = -1;
        for (Map.Entry mapElement : interestedPeers.entrySet()) {
            Integer peerPort = (Integer) mapElement.getKey();
            Integer downloadRate = connectedPeersRates.get(peerPort);
            if (downloadRate > highestRate) {
                highestRate = downloadRate;
                highestPeer = peerPort;
            }
        }
        return highestPeer;
    }

    class preferredNeighbours extends TimerTask {
        public void run() {
            HashMap<Integer, Boolean> topInterested = new HashMap<Integer, Boolean>();

            for (int i = 0; i < k; i++){
                int highest = getHighestInterested();
                if (highest != -1) {
                    topInterested.put(highest, false);
                }
                connectedPeersRates.replace(highest, -1);
            }
            for (Map.Entry mapElement : connectedPeersRates.entrySet()) {
                Integer peerID = (Integer) mapElement.getKey();
                connectedPeersRates.replace(peerID, 0);
            }

            for (Map.Entry mapElement : interestedPeers.entrySet()) {
                Integer peerID = (Integer) mapElement.getKey();
                interestedPeers.replace(peerID, false);
            }

            System.out.print("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] has the preferred neighbors [");
            for (Map.Entry mapElement : topInterested.entrySet()) {
                Integer peerID = (Integer) mapElement.getKey();
                interestedPeers.replace(peerID, true);
                System.out.print(peerID + ",");
            }
            System.out.println("]");

            flag2.setFlag(!flag2.getFlag());

        }
    }

    class optimisticallyUnchokedNeighbor extends TimerTask {
        public void run() {
            List<Integer> chokedInterested = new ArrayList<>();
            for (Map.Entry mapElement : interestedPeers.entrySet()) {
                Integer peerID = (Integer) mapElement.getKey();
                if(interestedPeers.get(peerID) == false){
                    chokedInterested.add(peerID);
                }
            }
            Random rand = new Random();
            if(!chokedInterested.isEmpty()) {
                int randomPeer = chokedInterested.get(rand.nextInt(chokedInterested.size()));
                interestedPeers.replace(randomPeer, true);
                System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] has  the  optimistically  unchoked  neighbor [" + randomPeer + "]");
            }
            flag2.setFlag(!flag2.getFlag());
        }
    }







}

