import others.peerProcess;

import java.io.IOException;

public class main {
    public static void main(String args[]) throws IOException {
        peerProcess a = new peerProcess();
        a.start(new String[]{"1000"});
    }
}
