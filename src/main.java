public class main {
    public static void main(String args[]) throws Exception {
        //peerProcess a = new peerProcess();
        //a.start(new String[]{"1000"});

        Peer a = new Peer(2000);
        Peer b = new Peer(3000);

        a.connectToPeer(3000);
        a.handShakePeer(3000);


    }
}
