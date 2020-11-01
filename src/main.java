public class main {
    public static void main(String args[]) throws Exception {

        Peer a = new Peer(4000);
        Peer b = new Peer(5000);
        Peer c = new Peer(7000);

        c.connectToPeer(4000);
        c.connectToPeer(5000);

    }
}
