import java.io.IOException;
import java.net.ServerSocket;

public class Server {

    public Server(int port) {
        new Handler(port).start();
    }

    public static class Handler extends Thread {
        private int sPort;   //The server will be listening on this port number

        public Handler (int port) {
            sPort = port;
        }

        public void run() {
            System.out.println("The server is running.");
            ServerSocket listener = null;
            try {
                listener = new ServerSocket(sPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int clientNum = 1;
            try {
                while(true) {
                    try {
                        new ServerFurther.Handler(listener.accept(),clientNum, sPort).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Client "  + clientNum + " is connected!");
                    clientNum++;
                }
            } finally {
                try {
                    listener.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
