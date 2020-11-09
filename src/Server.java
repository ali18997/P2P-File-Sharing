import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;

public class Server {

    private int PieceSize;

    private HashMap<String, BitField> bitFields;
    private HashMap<String, byte[]> files;
    private HashMap<String, byte[]> requestBitFields;

    public Server(int port, HashMap<String, byte[]> requestBitFields, HashMap<String, BitField> bitFields, HashMap<String, byte[]> files, int PieceSize) {

        this.requestBitFields = requestBitFields;
        this.bitFields = bitFields;
        this.files = files;
        this.PieceSize = PieceSize;
        new Handler(port).start();
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
                        new ServerFurther.Handler(listener.accept(),clientNum, sPort, requestBitFields, bitFields, files, PieceSize).start();
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
