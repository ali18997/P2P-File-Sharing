package others;
import java.net.Socket;

public class IncomingRequestsHandler implements Runnable {
    /**
     * need to pass this socket to the peerCommunication method;
     * this is the serverSocket that accepts connections from peers with greater Peer IDs*/
    private Socket clientSocket;
    private RemotePeerInfo remotePeerInfo;

    IncomingRequestsHandler(Socket clientSocket, RemotePeerInfo remotePeerInfo) {
        this.clientSocket = clientSocket;
        this.remotePeerInfo = remotePeerInfo;
    }
    

    @Override
    public void run() {
        System.out.println("incoming request thread spawned for remote peer " + this.remotePeerInfo.get_peerID());
        PeerCommunication peerCommunication = new PeerCommunication(this.remotePeerInfo, this.clientSocket);
//        Peer.peer.log.TCPConnection(this.remotePeerInfo.get_peerID(), false);
        try {
			peerCommunication.startMessageExchange();
		} catch (Exception e) {
			throw new RuntimeException("Error starting message exchange in incoming request handler for " + this.remotePeerInfo.get_peerID(), e);
		}

//        try {
//            BufferedOutputStream output = new BufferedOutputStream(clientSocket.getOutputStream());
//            output.flush();
//            BufferedInputStream input = new BufferedInputStream(clientSocket.getInputStream());
//
//            byte[] in = new byte[4];
//            int k = input.read(in);
//            System.out.println("incoming peer message : " + k);
//            output.write(("Client" + Peer.getPeerInstance().get_peerID() + " is sending message to the peer "
//            + k).getBytes());
//            output.flush();
//
//            input.close();
//            output.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
