package others;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;

public class OutgoingRequestsHandler implements Runnable {
    private RemotePeerInfo remotePeerInfo;
    Socket socket;
    BufferedOutputStream out;
    BufferedInputStream in;

    OutgoingRequestsHandler(RemotePeerInfo remote) {
        this.remotePeerInfo = remote;
    }

    @Override
    public void run() {
        System.out.println("Thread from outgoing pool spawned");
        PeerCommunication peerCommunication = new PeerCommunication(this.remotePeerInfo);
//        Peer.peer.log.TCPConnection(this.remotePeerInfo.get_peerID(), true);
        try {
			peerCommunication.startMessageExchange();
		} catch (Exception e) {
			throw new RuntimeException("Error starting message exchange in outgoing request handler for " + this.remotePeerInfo.get_peerID(), e);
		}

//        try{
//            this.socket = new Socket(InetAddress.getByName(this.remotePeerInfo.get_hostName()), this.remotePeerInfo.get_portNo());
//            this.out = new BufferedOutputStream(this.socket.getOutputStream());
//            out.flush();
//            this.in = new BufferedInputStream(this.socket.getInputStream());
//
//            out.write(String.valueOf(this.remotePeerInfo.get_peerID()).getBytes());
//            out.flush();
//            int input = in.read(new byte[1024]);
//            System.out.println(String.valueOf(input));
//
//            in.close();
//            out.close();
//        } catch (IOException e) {
//            throw new RuntimeException("Could not open client socket", e);
//        }
    }
}
