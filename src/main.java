import others.peerProcess;

import java.io.IOException;

public class main {
    public static void main(String args[]) throws IOException {
        //peerProcess a = new peerProcess();
        //a.start(new String[]{"1000"});

        ActualMessage b = new ActualMessage(20, 5);
        System.out.println(b.getMessageLength());
        System.out.println(b.getMessageType());
        System.out.println(b.getPayload());
    }
}
