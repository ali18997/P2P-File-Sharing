import messages.Handshake;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ServerFurther {



    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for dealing with a single client's requests.
     */
    public static class Handler extends Thread {
        private String message;    //message received from the client
        private String MESSAGE;    //uppercase message send to the client
        private Socket connection;
        private ObjectInputStream in;	//stream read from the socket
        private ObjectOutputStream out;    //stream write to the socket
        private int no;		//The index number of the client
        private int sPort;
        private int clientPort;

        private byte[] ownBitField;
        private byte[] othersBitField;
        private String gettingFileName;
        private int gettingFilePieceSize;
        private int index;
        private byte[] gettingFile;

        public BitField bitfield;
        public byte[] file;

        private HashMap<Integer, Boolean> handshakes = new HashMap<Integer, Boolean>();

        public Handler(Socket connection, int no, int sPort, BitField bitfield, byte[] file) {
            this.connection = connection;
            this.no = no;
            this.sPort = sPort;
            this.bitfield = bitfield;
            this.file = file;
        }

        public void run() {
            try{
                //initialize Input and Output streams
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());
                try{
                    while(true)
                    {
                        Object receivedMsg = MessageConversion.bytesToMessage((byte[]) in.readObject());
                        if (receivedMsg instanceof HandshakeMessage) {
                            HandshakeMessage handshakeMessage = (HandshakeMessage) receivedMsg;
                            clientPort = handshakeMessage.getPeerID();
                            if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ") && handshakes.get(clientPort) == null) {
                                System.out.println("Peer " + sPort + " received Successful Handshake from " + clientPort);
                                HandshakeMessage handshakeMessageBack = new HandshakeMessage(sPort);
                                handshakes.put(clientPort, true);
                                sendMessage(MessageConversion.messageToBytes(handshakeMessageBack));
                            }
                            else if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ") && handshakes.get(clientPort) == true) {
                                System.out.println("Peer " + sPort + " Completed Handshake from " + clientPort);

                                //LATER ON IMPLEMENT ONLY SENDING IF THERE ARE PIECES

                                ActualMessage bitFieldMessage = new ActualMessage(file.length, 5, new PayloadMessage(MessageConversion.messageToBytes(bitfield)));
                                sendMessage(MessageConversion.messageToBytes(bitFieldMessage));
                            }
                        }
                        else if (receivedMsg instanceof ActualMessage) {
                            ActualMessage actualMessage = (ActualMessage) receivedMsg;
                            if (actualMessage.getMessageType() == 0) {
                                //CHOKE
                            }
                            else if (actualMessage.getMessageType() == 1) {
                                //UNCHOKE
                                System.out.println("Peer " + sPort + " received UnChoke Message from " + clientPort);
                                if (this.ownBitField[this.index] == 0 && this.othersBitField[this.index] == 1) {
                                    byte[] pieceIndex = ByteBuffer.allocate(4).putInt(this.index).array();
                                    PayloadMessage piece = new PayloadMessage(pieceIndex);
                                    ActualMessage requestMessage = new ActualMessage(1, 6, piece);
                                    sendMessage(MessageConversion.messageToBytes(requestMessage));
                                }

                            }
                            else if (actualMessage.getMessageType() == 2) {
                                System.out.println("Peer " + sPort + " received interested Message from " + clientPort);
                                ActualMessage unchokeMessage = new ActualMessage(1, 1, null);
                                sendMessage(MessageConversion.messageToBytes(unchokeMessage));
                            }
                            else if (actualMessage.getMessageType() == 3) {
                                System.out.println("Peer " + sPort + " received not interested Message from " + clientPort);
                            }
                            else if (actualMessage.getMessageType() == 4) {
                                //HAVE
                            }
                            else if (actualMessage.getMessageType() == 5) {
                                System.out.println("Peer " + sPort + " received Bitfield Message from " + clientPort);
                                BitField bitField = (BitField)MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());

                                this.ownBitField = new byte[bitField.bitField.length];
                                this.othersBitField = bitField.bitField;
                                this.gettingFileName = bitField.FileName;
                                this.gettingFilePieceSize = bitField.PieceSize;
                                this.gettingFile = new byte[bitField.FileSize];
                                this.index = 0;

                                if (this.ownBitField[this.index] == 0 && this.othersBitField[this.index] == 1) {
                                    ActualMessage interestMessage = new ActualMessage(1, 2, null);
                                    sendMessage(MessageConversion.messageToBytes(interestMessage));
                                }

                            }
                            else if (actualMessage.getMessageType() == 6) {
                                //REQUEST
                                int pieceNum = ByteBuffer.wrap(actualMessage.getPayload().getMessage()).getInt();

                                System.out.println("Peer " + sPort + " received Request Message from " + clientPort + " for piece " + pieceNum);

                                if (bitfield.bitField[pieceNum] == 1) {
                                    byte[] piece = Arrays.copyOfRange(file, pieceNum*bitfield.PieceSize, pieceNum*bitfield.PieceSize + bitfield.PieceSize);
                                    Piece pieceMsg = new Piece(actualMessage.getPayload().getMessage() ,piece);
                                    ActualMessage interestMessage = new ActualMessage(1, 7, new PayloadMessage(MessageConversion.messageToBytes(pieceMsg)));
                                    sendMessage(MessageConversion.messageToBytes(interestMessage));
                                }
                            }
                            else if (actualMessage.getMessageType() == 7) {
                                //PIECE
                                Piece a = (Piece) MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());
                                int pieceNum = ByteBuffer.wrap(a.getPieceIndex()).getInt();
                                System.out.println("Peer " + sPort + " received Piece " + pieceNum + " from "  + clientPort);
                                byte[] piece = a.getPiece();
                                for (int i = 0; i < gettingFilePieceSize; i++) {
                                    this.gettingFile[pieceNum*this.gettingFilePieceSize + i] = piece[i];
                                }
                                this.ownBitField[pieceNum] = 1;

                                if (this.index < this.ownBitField.length-1) {
                                    this.index = this.index + 1;
                                    if (this.ownBitField[this.index] == 0 && this.othersBitField[this.index] == 1) {
                                        byte[] pieceIndex = ByteBuffer.allocate(4).putInt(this.index).array();
                                        PayloadMessage piece2 = new PayloadMessage(pieceIndex);
                                        ActualMessage requestMessage = new ActualMessage(1, 6, piece2);
                                        sendMessage(MessageConversion.messageToBytes(requestMessage));
                                    }
                                }
                                else {
                                    System.out.println("Peer " + sPort + " received complete file " + gettingFileName + " from "  + clientPort);

                                    Files.write(Path.of(System.getProperty("user.dir") + "/peerFolder/" + sPort + "/" + this.gettingFileName), this.gettingFile);
                                }




                            }
                        }
                    }
                }
                catch(ClassNotFoundException classnot){
                    System.err.println("Data received in unknown format");
                }
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client " + no);
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    connection.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with Client " + no);
                }
            }
        }

        //send a message to the output stream
        public void sendMessage(byte[] msg)
        {
            try{
                out.writeObject(msg);
                out.flush();
                //System.out.println("Send message: " + msg + " to Client " + no);
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }

    }

}
