
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ServerFurther {



    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for dealing with a single client's requests.
     */
    public static class Handler extends Thread {
        private Socket connection;
        private ObjectInputStream in;	//stream read from the socket
        private ObjectOutputStream out;    //stream write to the socket
        private int no;		//The index number of the client
        private int peerID;
        private int otherPeerID;
        private Boolean requestFlag = false;
        private int PieceSize;
        private FlagObservable flag;
        private FlagObservable flag2;

        private HashMap<String, BitField> bitFields;
        private HashMap<String, byte[]> files;
        private HashMap<String, BitField> clientBitFields = new HashMap<String, BitField>();
        private HashMap<String, byte[]> requestBitFields;
        private HashMap<Integer, Boolean> handshakes = new HashMap<Integer, Boolean>();
        private HashMap<String, byte[]> haveBitFields = new HashMap<String, byte[]>();
        private HashMap<Integer, Integer> connectedPeersRates;
        private HashMap<Integer, Boolean> interestedPeers;
        private Boolean sendToClient = false;
        private Boolean requestFromClient = false;

        public Handler(Socket connection, int no, int peerID, HashMap<String, byte[]> requestBitFields, HashMap<String, BitField> bitFields, HashMap<String, byte[]> files, int PieceSize, FlagObservable flag, FlagObservable flag2, HashMap<Integer, Integer> connectedPeersRates, HashMap<Integer, Boolean> interestedPeers) throws IOException {
            this.connection = connection;
            this.no = no;
            this.peerID = peerID;
            this.requestBitFields = requestBitFields;
            this.bitFields = bitFields;
            this.files = files;
            this.PieceSize = PieceSize;
            this.flag = flag;
            this.flag2 = flag2;
            this.connectedPeersRates = connectedPeersRates;
            this.interestedPeers = interestedPeers;

            FlagObserver observer = new FlagObserver();
            FlagObserver2 observer2 = new FlagObserver2();
            flag.addObserver(observer);
            flag2.addObserver(observer2);
            prepareBitFields();
        }

        private void sendHave(int i, BitField bitFieldServer) {

            byte[] pieceIndex = ByteBuffer.allocate(4).putInt(i).array();
            Have haveMsg = new Have(bitFieldServer.FileName, pieceIndex, new BitField(bitFieldServer.FileName, bitFieldServer.FileSize, bitFieldServer.PieceSize, new byte[bitFieldServer.getBitField().length]));
            for (int j = 0; j < haveMsg.getBitField().bitField.length; j++) {
                haveMsg.getBitField().bitField[j] = 0;
            }
            try {
                PayloadMessage pieceHave = new PayloadMessage(MessageConversion.messageToBytes(haveMsg));
                ActualMessage haveMessage = new ActualMessage(1, 4, pieceHave);
                sendMessage(MessageConversion.messageToBytes(haveMessage));
                haveBitFields.get(bitFieldServer.FileName)[i] = 1;
            } catch (IOException e) {
                System.out.println("Server Error 1 " + e.toString());
            }

        }

        private void redundantRequests(){
            for (Map.Entry mapElement : bitFields.entrySet()) {
                String name = (String)mapElement.getKey();
                byte[] bitField = ((BitField)mapElement.getValue()).getBitField();
                byte[] bitField1 = requestBitFields.get(name);
                for (int i = 0; i < bitField.length; i++){
                    if(bitField[i] == 0){
                        bitField1[i] = 0;
                    }
                }
            }
        }

        public class FlagObserver implements Observer {

            public FlagObserver() {}

            public void update(Observable obj, Object arg) {

                for (Map.Entry mapElement : bitFields.entrySet()) {
                    String name = (String) mapElement.getKey();
                    BitField bitFieldServer = ((BitField) mapElement.getValue());

                    if (clientBitFields.containsKey(name)) {
                        BitField bitFieldClient = clientBitFields.get(name);
                        int length = bitFieldClient.bitField.length;
                        for (int i = 0; i < length; i++) {
                            if (haveBitFields.containsKey(name)) {
                                if (bitFieldClient.bitField[i] == 0 && bitFieldServer.bitField[i] == 1 && haveBitFields.get(name)[i] == 0) {
                                    sendHave(i, bitFieldServer);
                                }
                            }
                        }
                    }
                    else {
                        BitField temp = new BitField(name, bitFieldServer.getFileSize(), bitFieldServer.getPieceSize(), new byte[bitFieldServer.getBitField().length]);
                        byte[] temp2 = new byte[bitFieldServer.getBitField().length];
                        for (int i = 0; i < temp.getBitField().length; i++) {
                            temp.getBitField()[i] = 0;
                            temp2[i] = 0;
                        }
                        clientBitFields.put(name, temp);
                        haveBitFields.put(name, temp2);
                        BitField bitFieldClient = clientBitFields.get(name);
                        int length = bitFieldClient.bitField.length;
                        for (int i = 0; i < length; i++) {
                            if (bitFieldClient.bitField[i] == 0 && bitFieldServer.bitField[i] == 1 && haveBitFields.get(name)[i] == 0) {
                                sendHave(i, bitFieldServer);
                            }
                        }
                    }
                }

            }
        }

        public class FlagObserver2 implements Observer {

            public FlagObserver2() {
            }

            public void update(Observable obj, Object arg) {
                if(interestedPeers.containsKey(otherPeerID)) {
                    if (sendToClient == false && interestedPeers.get(otherPeerID) == true) {
                        //UNCHOKE
                        ActualMessage unchokeMessage = new ActualMessage(1, 1, null);
                        try {
                            sendToClient = true;
                            sendMessage(MessageConversion.messageToBytes(unchokeMessage));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (sendToClient == true && interestedPeers.get(otherPeerID) == false) {
                        //CHOKE
                        ActualMessage chokeMessage = new ActualMessage(1, 0, null);
                        try {
                            sendToClient = false;
                            sendMessage(MessageConversion.messageToBytes(chokeMessage));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        public void readActualFile(String name) throws IOException {
            final File folder = new File(System.getProperty("user.dir") + "/peerFolder/" + peerID);
            for (final File fileEntry : folder.listFiles()) {
                if (name.equals(fileEntry.getName())) {
                    byte[] bytes = Files.readAllBytes(fileEntry.toPath());
                    files.put(name, bytes);
                }
            }
        }

        public void prepareBitFields() throws IOException {
            final File folder = new File(System.getProperty("user.dir") + "/peerFolder/" + peerID);
            if (!folder.exists()){folder.mkdir();}
            for (final File fileEntry : folder.listFiles()) {
                String fileName = fileEntry.getName();

                int fsize = (int) Files.size(fileEntry.toPath());
                byte[] bitFieldArr = new byte[(int) Math.ceil((double)fsize/PieceSize)];
                for (int i = 0; i < bitFieldArr.length; i++) {
                    bitFieldArr[i] = 1;
                }
                BitField bitField = new BitField(fileName, fsize, PieceSize, bitFieldArr);
                this.bitFields.put(fileName, bitField);
            }
        }


        public void prepareToReceiveFile(BitField bitField){
            BitField temp = new BitField(bitField.getFileName(), bitField.getFileSize(), bitField.getPieceSize(), new byte[bitField.getBitField().length]);
            byte[] temp3 = new byte[bitField.getBitField().length];
            for (int i = 0; i < temp.getBitField().length; i++) {
                temp.getBitField()[i] = 0;
                temp3[i] = 0;
            }
            byte[] temp2 = new byte[temp.getFileSize()];
            bitFields.put(bitField.getFileName(), temp);
            files.put(bitField.getFileName(), temp2);
            requestBitFields.put(bitField.getFileName(), temp3);
        }

        public void requestPiece() throws IOException {
            if(requestFromClient) {
                outerloop:
                for (Map.Entry mapElement : clientBitFields.entrySet()) {
                    String name = (String) mapElement.getKey();
                    BitField bitFieldClient = ((BitField) mapElement.getValue());

                    if (bitFields.containsKey(name)) {
                        BitField bitFieldServer = bitFields.get(name);
                        int length = bitFieldServer.bitField.length;
                        for (int i = 0; i < length; i++) {
                            if (bitFieldServer.bitField[i] == 0 && bitFieldClient.bitField[i] == 1 && requestBitFields.get(name)[i] == 0) {
                                requestBitFields.get(name)[i] = 1;
                                byte[] pieceIndex = ByteBuffer.allocate(4).putInt(i).array();
                                Request request = new Request(name, pieceIndex);
                                PayloadMessage pieceRequest = new PayloadMessage(MessageConversion.messageToBytes(request));
                                ActualMessage requestMessage = new ActualMessage(1, 6, pieceRequest);
                                sendMessage(MessageConversion.messageToBytes(requestMessage));
                                break outerloop;
                            }
                        }
                    } else {
                        prepareToReceiveFile(bitFieldClient);

                        byte[] pieceIndex = ByteBuffer.allocate(4).putInt(0).array();
                        Request request = new Request(name, pieceIndex);
                        PayloadMessage pieceRequest = new PayloadMessage(MessageConversion.messageToBytes(request));
                        ActualMessage requestMessage = new ActualMessage(1, 6, pieceRequest);
                        sendMessage(MessageConversion.messageToBytes(requestMessage));
                        break outerloop;
                    }
                }
            }
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
                        try {
                            Random rand = new Random();
                            TimeUnit.MILLISECONDS.sleep(rand.nextInt(1));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //checkNewPieces();
                        Object receivedMsg = MessageConversion.bytesToMessage((byte[]) in.readObject());
                        if (receivedMsg instanceof HandshakeMessage) {
                            HandshakeMessage handshakeMessage = (HandshakeMessage) receivedMsg;
                            otherPeerID = handshakeMessage.getPeerID();
                            if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ") && handshakes.get(otherPeerID) == null) {
                                HandshakeMessage handshakeMessageBack = new HandshakeMessage(peerID);
                                handshakes.put(otherPeerID, true);
                                sendMessage(MessageConversion.messageToBytes(handshakeMessageBack));
                            }
                            else if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ") && handshakes.get(otherPeerID) == true) {
                                System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] is connected from Peer [" + otherPeerID + "]");
                                connectedPeersRates.put(otherPeerID, 0);

                                for (Map.Entry mapElement : bitFields.entrySet()) {
                                    String name = (String)mapElement.getKey();
                                    BitField bitField = ((BitField)mapElement.getValue());
                                    ActualMessage bitFieldMessage = new ActualMessage(1, 5, new PayloadMessage(MessageConversion.messageToBytes(bitField)));
                                    sendMessage(MessageConversion.messageToBytes(bitFieldMessage));
                                }
                            }
                        }
                        else if (receivedMsg instanceof ActualMessage) {
                            ActualMessage actualMessage = (ActualMessage) receivedMsg;
                            if (actualMessage.getMessageType() == 0) {
                                //CHOKE
                                System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] is choked by [" + otherPeerID + "]");
                                requestFromClient = false;
                                redundantRequests();

                            }
                            else if (actualMessage.getMessageType() == 1) {
                                //UNCHOKE
                                System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] is unchoked by [" + otherPeerID + "]");
                                requestFromClient = true;
                                requestPiece();
                            }
                            else if (actualMessage.getMessageType() == 2) {
                                System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] received the \"interested\" message from [" + otherPeerID + "]");

                                interestedPeers.put(otherPeerID, false);

                            }
                            else if (actualMessage.getMessageType() == 3) {
                                System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] received the \"not interested\" message from [" + otherPeerID + "]");
                                //if (interestedPeers.containsKey(otherPeerID)){
                                //    interestedPeers.remove(otherPeerID);
                                //}
                            }
                            else if (actualMessage.getMessageType() == 4) {
                                //HAVE
                                Have msg = (Have) MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());
                                String name = msg.getFileName();

                                int pieceNum = ByteBuffer.wrap(msg.getPieceIndex()).getInt();

                                if (!clientBitFields.containsKey(name)){
                                    clientBitFields.put(name, msg.getBitField());
                                }

                                clientBitFields.get(name).getBitField()[pieceNum] = 1;

                                System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] received the \"have\" message from [" + otherPeerID + "] for the piece [" + pieceNum + "]");
                                if(requestFromClient) {
                                    requestPiece();
                                }
                                else {
                                    if(bitFields.containsKey(name)) {
                                        if (bitFields.get(name).getBitField()[pieceNum] == 0) {
                                            ActualMessage interestMessage = new ActualMessage(1, 2, null);
                                            sendMessage(MessageConversion.messageToBytes(interestMessage));
                                        }
                                    }
                                    else {
                                        ActualMessage interestMessage = new ActualMessage(1, 2, null);
                                        sendMessage(MessageConversion.messageToBytes(interestMessage));
                                    }
                                }
                            }
                            else if (actualMessage.getMessageType() == 5) {
                                //System.out.println("Peer " + peerID + " received Bitfield Message from " + otherPeerID);

                                BitField bitField = (BitField)MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());

                                clientBitFields.put(bitField.getFileName(), bitField);

                                if(requestFlag == false) {
                                    requestFlag = true;
                                    Boolean flag = true;
                                    outerloop:
                                    for (Map.Entry mapElement : clientBitFields.entrySet()) {
                                        String name = (String) mapElement.getKey();
                                        BitField bitFieldClient = ((BitField) mapElement.getValue());

                                        if (bitFields.containsKey(name)) {
                                            BitField bitFieldServer = bitFields.get(name);
                                            int length = bitFieldServer.bitField.length;
                                            for (int i = 0; i < length; i++) {
                                                if (bitFieldServer.bitField[i] == 0 && bitFieldClient.bitField[i] == 1 && requestBitFields.get(name)[i] == 0) {
                                                    ActualMessage interestMessage = new ActualMessage(1, 2, null);
                                                    sendMessage(MessageConversion.messageToBytes(interestMessage));
                                                    flag = false;
                                                    break outerloop;
                                                }
                                            }
                                        } else {
                                            ActualMessage interestMessage = new ActualMessage(1, 2, null);
                                            sendMessage(MessageConversion.messageToBytes(interestMessage));
                                            flag = false;
                                            prepareToReceiveFile(bitFieldClient);
                                            break outerloop;
                                        }
                                    }
                                    if (flag) {
                                        ActualMessage notInterestMessage = new ActualMessage(1, 3, null);
                                        sendMessage(MessageConversion.messageToBytes(notInterestMessage));
                                        requestFlag = false;
                                    }
                                }

                            }
                            else if (actualMessage.getMessageType() == 6) {
                                //REQUEST

                                Request msg = (Request) MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());
                                String name = msg.getFileName();

                                if(!files.containsKey(name)){
                                    readActualFile(name);
                                }

                                int pieceNum = ByteBuffer.wrap(msg.getPieceIndex()).getInt();

                                //System.out.println("Peer " + peerID + " received Request Message from " + otherPeerID + " for file: " + name + " piece: " + pieceNum);
                                if(sendToClient) {
                                    if (bitFields.get(name).bitField[pieceNum] == 1) {
                                        byte[] piece = Arrays.copyOfRange(files.get(name), pieceNum * bitFields.get(name).PieceSize, pieceNum * bitFields.get(name).PieceSize + bitFields.get(name).PieceSize);
                                        Piece pieceMsg = new Piece(name, msg.getPieceIndex(), piece);
                                        ActualMessage interestMessage = new ActualMessage(1, 7, new PayloadMessage(MessageConversion.messageToBytes(pieceMsg)));
                                        sendMessage(MessageConversion.messageToBytes(interestMessage));

                                        if (!clientBitFields.containsKey(name)) {
                                            BitField temp = bitFields.get(name);
                                            BitField temp2 = new BitField(temp.FileName, temp.FileSize, temp.PieceSize, new byte[temp.getBitField().length]);
                                            for (int i = 0; i < temp.getBitField().length; i++) {
                                                temp2.getBitField()[i] = 0;
                                            }
                                            clientBitFields.put(name, temp2);
                                        }
                                        clientBitFields.get(name).bitField[pieceNum] = 1;

                                        byte[] temp = clientBitFields.get(name).bitField;
                                        Boolean tempFlag = true;
                                        for (int i = 0; i < clientBitFields.get(name).bitField.length-1; i++){
                                            if(temp[i] == 0){
                                                tempFlag = false;
                                            }
                                        }
                                        if(tempFlag) {
                                            interestedPeers.remove(otherPeerID);
                                        }
                                    }
                                }
                                else {
                                    if(!interestedPeers.containsKey(otherPeerID)){
                                        interestedPeers.put(otherPeerID, false);
                                    }
                                }

                            }
                            else if (actualMessage.getMessageType() == 7) {
                                //PIECE
                                Piece a = (Piece) MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());
                                int pieceNum = ByteBuffer.wrap(a.getPieceIndex()).getInt();
                                String fname = a.getName();
                                int count = 0;
                                for (int i = 0; i < bitFields.get(fname).getBitField().length; i++){
                                    if(bitFields.get(fname).getBitField()[i] == 1) {
                                        count = count + 1;
                                    }
                                }
                                System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] has downloaded the piece [" + pieceNum + "] from [" + otherPeerID + "]. Now the number of pieces it has is [" + (count + 1) + "]");


                                byte[] piece = a.getPiece();
                                for (int i = 0; i < bitFields.get(fname).getPieceSize(); i++) {
                                    if(pieceNum*bitFields.get(fname).getPieceSize() + i < files.get(fname).length) {
                                        files.get(fname)[pieceNum * bitFields.get(fname).getPieceSize() + i] = piece[i];
                                    }
                                }
                                bitFields.get(fname).getBitField()[pieceNum] = 1;
                                connectedPeersRates.replace(otherPeerID, connectedPeersRates.get(otherPeerID) + bitFields.get(fname).getPieceSize());

                                flag.setFlag(!flag.getFlag());



                                byte[] temp = bitFields.get(fname).getBitField();
                                Boolean tempFlag = true;
                                for (int i = 0; i < bitFields.get(fname).getBitField().length-1; i++){
                                    if(temp[i] == 0){
                                        tempFlag = false;
                                    }
                                }
                                if(tempFlag) {
                                    System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] has downloaded the complete file.");


                                    Files.write(Path.of(System.getProperty("user.dir") + "/peerFolder/" + peerID + "/" + fname), files.get(fname));

                                }
                                requestPiece();
                            }
                        }
                    }
                }
                catch(ClassNotFoundException classnot){
                    System.out.println("Server Error 2 " + classnot.toString());
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
                System.out.println("Server Error 3 " + ioException.toString());
            }
        }

    }

}
