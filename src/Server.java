import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;

public class Server {

    public BitField bitfield;
    public byte[] file;

    private HashMap<String, BitField> bitFields = new HashMap<String, BitField>();
    private HashMap<String, byte[]> files = new HashMap<String, byte[]>();

    public Server(int port) {
        new Handler(port).start();
    }

    public void setFile(HashMap<String, BitField> bitFields, HashMap<String, byte[]> files){
        this.bitFields = bitFields;
        this.files = files;
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
                        new ServerFurther.Handler(listener.accept(),clientNum, sPort, bitFields, files).start();
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
