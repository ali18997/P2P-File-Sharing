public class main {
    public static void main(String args[]) throws Exception {

        Peer a = new Peer(4000);
        Peer b = new Peer(5000);

        a.connectToPeer(5000);
        a.handShakePeer(5000);

    }
}
