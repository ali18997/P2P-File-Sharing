public class main {
    public static void main(String args[]) throws Exception {

        Peer a = new Peer(4000);
        Peer b = new Peer(5000);
        Peer c = new Peer(7000);
        Peer d = new Peer(8000);

        a.connectToPeer(7000);
        b.connectToPeer(7000);
        d.connectToPeer(7000);

    }
}
