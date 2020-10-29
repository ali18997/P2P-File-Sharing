
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;


public class Peer {
    private int peerPort;   //The server will be listening on this port number
    private Server server;
    private HashMap<Integer, Client> clients = new HashMap<Integer, Client>();
    private BitField bitField;
    private byte[] file;

    public Peer(int port) throws IOException {
        peerPort = port;
        server = new Server(peerPort);
        final File folder = new File(System.getProperty("user.dir") + "/peerFolder/" + port);
        listFilesForFolder(folder);
    }

    public void connectToPeer(int port) throws IOException {
        Client temp = new Client(port, peerPort);
        temp.setFile(bitField, file);
        clients.put(port, temp);
    }

    public void handShakePeer(int port) throws IOException {
        HandshakeMessage handshakeMessage = new HandshakeMessage(peerPort);
        clients.get(port).sendMessage(MessageConversion.messageToBytes(handshakeMessage));
    }

    public void messageToPeer(int port, String msg) throws IOException {
        clients.get(port).sendMessage(msg.getBytes());
    }

    public void disconnectPeer(int port) throws IOException {
        clients.get(port).closeConnection();
    }

    public void listFilesForFolder(final File folder) throws IOException {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                System.out.println(fileEntry.getName());
                String fileName = fileEntry.getName();
                int PieceSize = 2;
                byte[] bytes = Files.readAllBytes(fileEntry.toPath());
                byte[] bitFieldArr = new byte[(int) Math.ceil((double)bytes.length/PieceSize)];

                for (int i = 0; i < bitFieldArr.length; i++) {
                    bitFieldArr[i] = 1;
                }
                BitField bitField = new BitField(fileName, bytes.length, PieceSize, bitFieldArr);
                this.bitField = bitField;
                this.file = bytes;


                byte[] bitFieldArr2 = new byte[bitFieldArr.length];

                for (int i = 0; i < bitFieldArr2.length; i++) {
                    bitFieldArr2[i] = 0;
                }

                byte[] fileBytes = new byte[bytes.length];

                for (int i = 0; i < bitFieldArr2.length; i++) {
                    if (bitFieldArr2[i] == 0 && bitFieldArr[i] == 1) {
                        byte[] a = Arrays.copyOfRange(bytes, i*PieceSize, i*PieceSize + PieceSize);
                        fileBytes[i*PieceSize] = a[0];
                        fileBytes[i*PieceSize+1] = a[1];
                        bitFieldArr2[i] = 1;
                    }
                }

                //Files.write(Paths.get(fileName), fileBytes);
            }
        }
    }






}

