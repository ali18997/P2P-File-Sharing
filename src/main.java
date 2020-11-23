public class main {
    public static void main(String args[]) throws Exception {

        Peer a = new Peer(1001, 4000);
        //Peer b = new Peer(1002, 5000);
        //Peer c = new Peer(1003,7000);
        Peer d = new Peer(1004,8000);
        //Peer e = new Peer(1005,9000);

        a.connectToPeer(1004, "localhost", 8000);
        //b.connectToPeer(1003, "localhost", 7000);
        //d.connectToPeer(1003, "localhost", 7000);
        //d.connectToPeer(1005, "localhost", 9000);




    }
}
