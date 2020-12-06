import java.io.*;
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
    private int StopCounter = 0;
    private HashMap<Integer, Integer> stopping = new HashMap<Integer, Integer>();
    private Boolean flagStopping = false;

    private int PieceSize;
    private int k;
    private int p;
    private int m;

    FlagObservable flag = new FlagObservable(true);
    FlagObservable flag2 = new FlagObservable(true);

    public Peer(int peerID) throws IOException {
        this.peerID = peerID;

        stopping.put(0,0);
        stopping.put(1,0);

        readCommon();
        readPeerInfo1();
        readPeerInfo2();

        PrintWriter writer = new PrintWriter("log_peer_" + peerID + ".log");
        writer.print("");
        writer.close();

        server = new Server(this.peerID, peerPort, requestBitFields, bitFields, files, PieceSize, flag, flag2, connectedPeersRates, interestedPeers, stopping);
        Timer timer = new Timer();
        timer.schedule(new preferredNeighbours(), 0, p*1000);
        timer.schedule(new optimisticallyUnchokedNeighbor(), 0, m*1000);
    }

    public void readPeerInfo1() throws IOException {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("PeerInfo.cfg"));
            String line = reader.readLine();
            while (line != null) {
                String[] splitted = line.split(" ");
                if(Integer.parseInt(splitted[0]) == peerID){
                    peerPort = Integer.parseInt(splitted[2]);
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readPeerInfo2() throws IOException {
        BufferedReader reader;
        Boolean flag = true;
        try {
            reader = new BufferedReader(new FileReader("PeerInfo.cfg"));
            String line = reader.readLine();
            while (line != null) {
                String[] splitted = line.split(" ");
                stopping.replace(0, stopping.get(0)+1);
                if(Integer.parseInt(splitted[0]) == peerID){
                    flag = false;
                }
                else {
                    if(flag) {
                        connectToPeer(Integer.parseInt(splitted[0]), splitted[1], Integer.parseInt(splitted[2]));
                    }
                }
                line = reader.readLine();
            }
            reader.close();
            flagStopping = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        clients.put(otherPeerID, temp);
        handShakePeer(otherPeerID);
        stopping.replace(1, stopping.get(1)+1);
    }

    public void handShakePeer(int otherPeerID) throws IOException {
        clients.get(otherPeerID).handShake();
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
            String log = "[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] has the preferred neighbors [";
            for (Map.Entry mapElement : topInterested.entrySet()) {
                Integer peerID = (Integer) mapElement.getKey();
                interestedPeers.replace(peerID, true);
                log = log + ",";
            }
            log = log + "]";
            Logging.writeLog(peerID, log);

            if(topInterested.isEmpty()){
                if(stopping.get(1) == stopping.get(0)-1 && flagStopping){
                    StopCounter += 1;
                }
            }

            if(StopCounter >= 2){
                System.out.println("Done");
                System.exit(0);
            }



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
                Logging.writeLog(peerID, "[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] has  the  optimistically  unchoked  neighbor [" + randomPeer + "]");
            }
            flag2.setFlag(!flag2.getFlag());
        }
    }







}

