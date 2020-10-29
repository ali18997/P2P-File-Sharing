import java.io.IOException;
import java.net.ServerSocket;

public class Server {

    public BitField bitfield;
    public byte[] file;

    public Server(int port) {
        new Handler(port).start();
    }

    public void setFile(BitField bitfield, byte[] file){
        this.bitfield = bitfield;
        this.file = file;
    }

    public class Handler extends Thread {
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
                        new ServerFurther.Handler(listener.accept(),clientNum, sPort, bitfield, file).start();
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
