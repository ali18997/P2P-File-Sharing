import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Client {
    public Socket requestSocket;           //socket connect to the server
    public ObjectOutputStream out;         //stream write to the socket
    public ObjectInputStream in;          //stream read from the socket
    //public int clientPort;
    //public int serverPort;
    private Boolean requestFlag = false;
    private int PieceSize;
    private FlagObservable flag;
    private FlagObservable flag2;
    private int peerID;
    private int otherPeerID;
    private String otherPeerHostName;
    private int otherPeerPort;

    private HashMap<String, BitField> bitFields;
    private HashMap<String, byte[]> files;
    private HashMap<String, BitField> serverBitFields = new HashMap<String, BitField>();
    private HashMap<String, byte[]> requestBitFields;
    private HashMap<String, byte[]> haveBitFields = new HashMap<String, byte[]>();
    private HashMap<Integer, Integer> connectedPeersRates;
    private HashMap<Integer, Boolean> interestedPeers;
    private Boolean sendToServer = false;
    private Boolean requestFromServer = false;

    public Client(int otherPeerID, String otherPeerHostName, int otherPeerPort, int peerID, HashMap<String, byte[]> requestBitFields, HashMap<String, BitField> bitFields, HashMap<String, byte[]> files, int PieceSize, FlagObservable flag, FlagObservable flag2, HashMap<Integer, Integer> connectedPeersRates, HashMap<Integer, Boolean> interestedPeers) throws IOException {
        this.requestBitFields = requestBitFields;
        this.bitFields = bitFields;
        this.files = files;
        this.PieceSize = PieceSize;
        this.flag = flag;
        this.flag2 = flag2;
        this.peerID = peerID;
        this.otherPeerID = otherPeerID;
        this.connectedPeersRates = connectedPeersRates;
        this.interestedPeers = interestedPeers;
        this.otherPeerHostName = otherPeerHostName;
        this.otherPeerPort = otherPeerPort;

        FlagObserver observer = new FlagObserver();
        FlagObserver2 observer2 = new FlagObserver2();
        flag.addObserver(observer);
        flag2.addObserver(observer2);
        requestSocket = new Socket(this.otherPeerHostName, otherPeerPort);

        out = new ObjectOutputStream(requestSocket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(requestSocket.getInputStream());
        prepareBitFields();
        new MessageReceiving().start();
    }

    private void sendHave(int i, BitField bitFieldClient) {

        byte[] pieceIndex = ByteBuffer.allocate(4).putInt(i).array();
        Have haveMsg = new Have(bitFieldClient.FileName, pieceIndex, new BitField(bitFieldClient.FileName, bitFieldClient.FileSize, bitFieldClient.PieceSize, new byte[bitFieldClient.getBitField().length]));
        for (int j = 0; j < haveMsg.getBitField().bitField.length; j++) {
            haveMsg.getBitField().bitField[j] = 0;
        }
        try {
            PayloadMessage pieceHave = new PayloadMessage(MessageConversion.messageToBytes(haveMsg));
            ActualMessage haveMessage = new ActualMessage(1, 4, pieceHave);
            sendMessage(MessageConversion.messageToBytes(haveMessage));
            haveBitFields.get(bitFieldClient.FileName)[i] = 1;
        } catch (IOException e) {
            System.out.println("Client Error 1 " + e.toString());
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
                BitField bitFieldClient = ((BitField) mapElement.getValue());

                if (serverBitFields.containsKey(name)) {
                    BitField bitFieldServer = serverBitFields.get(name);
                    int length = bitFieldServer.bitField.length;
                    for (int i = 0; i < length; i++) {
                        if (haveBitFields.containsKey(name)) {
                            if (bitFieldServer.bitField[i] == 0 && bitFieldClient.bitField[i] == 1 && haveBitFields.get(name)[i] == 0) {
                                sendHave(i, bitFieldClient);
                            }
                        }
                    }
                }
                else {
                    BitField temp = new BitField(name, bitFieldClient.getFileSize(), bitFieldClient.getPieceSize(), new byte[bitFieldClient.getBitField().length]);
                    byte[] temp2 = new byte[bitFieldClient.getBitField().length];
                    for (int i = 0; i < temp.getBitField().length; i++) {
                        temp.getBitField()[i] = 0;
                        temp2[i] = 0;
                    }
                    serverBitFields.put(name, temp);
                    haveBitFields.put(name, temp2);
                    BitField bitFieldServer = serverBitFields.get(name);
                    int length = bitFieldServer.bitField.length;
                    for (int i = 0; i < length; i++) {
                        if (bitFieldServer.bitField[i] == 0 && bitFieldClient.bitField[i] == 1 && haveBitFields.get(name)[i] == 0) {
                            sendHave(i, bitFieldClient);
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
                if (sendToServer == false && interestedPeers.get(otherPeerID) == true) {
                    //UNCHOKE
                    ActualMessage unchokeMessage = new ActualMessage(1, 1, null);
                    try {
                        sendToServer = true;
                        sendMessage(MessageConversion.messageToBytes(unchokeMessage));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (sendToServer == true && interestedPeers.get(otherPeerID) == false) {
                    //CHOKE
                    ActualMessage chokeMessage = new ActualMessage(1, 0, null);
                    try {
                        sendToServer = false;
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

    public void sendMessage(byte[] msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }

    public String getMessage() throws IOException, ClassNotFoundException {
        String MESSAGE = (String)in.readObject();
        return MESSAGE;
    }

    public void closeConnection() throws IOException {
        in.close();
        out.close();
        requestSocket.close();
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
        if(requestFromServer) {
            outerloop:
            for (Map.Entry mapElement : serverBitFields.entrySet()) {
                String name = (String) mapElement.getKey();
                BitField bitFieldServer = ((BitField) mapElement.getValue());

                if (bitFields.containsKey(name)) {
                    BitField bitFieldClient = bitFields.get(name);
                    int length = bitFieldClient.bitField.length;
                    for (int i = 0; i < length; i++) {
                        if (bitFieldClient.bitField[i] == 0 && bitFieldServer.bitField[i] == 1 && requestBitFields.get(name)[i] == 0) {
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
                    prepareToReceiveFile(bitFieldServer);

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

    public class MessageReceiving extends Thread {
        public void run() {

                    while (true) {
                        try {
                            Random rand = new Random();
                            TimeUnit.MILLISECONDS.sleep(rand.nextInt(1));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Object receivedMsg = null;
                        try {
                            receivedMsg = MessageConversion.bytesToMessage((byte[]) in.readObject());
                            if (receivedMsg instanceof HandshakeMessage) {
                                HandshakeMessage handshakeMessage = (HandshakeMessage) receivedMsg;
                                if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ")) {
                                    System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] makes a connection to Peer [" + otherPeerID + "]");
                                    HandshakeMessage handshakeMessageBack = new HandshakeMessage(peerID);
                                    try {
                                        sendMessage(MessageConversion.messageToBytes(handshakeMessageBack));
                                        connectedPeersRates.put(otherPeerID, 0);
                                    } catch (IOException e) {
                                        System.out.println("Client Error 4 " + e.toString());
                                    }
                                    for (Map.Entry mapElement : bitFields.entrySet()) {
                                        String name = (String)mapElement.getKey();
                                        BitField bitField = ((BitField)mapElement.getValue());
                                        ActualMessage bitFieldMessage = null;
                                        try {
                                            bitFieldMessage = new ActualMessage(1, 5, new PayloadMessage(MessageConversion.messageToBytes(bitField)));
                                        } catch (IOException e) {
                                            System.out.println("Client Error 41 " + e.toString());
                                        }
                                        try {
                                            sendMessage(MessageConversion.messageToBytes(bitFieldMessage));
                                        } catch (IOException e) {
                                            System.out.println("Client Error 5 " + e.toString());
                                        }
                                    }

                                }
                            }
                            else if (receivedMsg instanceof ActualMessage) {
                                ActualMessage actualMessage = (ActualMessage) receivedMsg;
                                if (actualMessage.getMessageType() == 0) {
                                    //CHOKE
                                    System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] is choked by [" + otherPeerID + "]");
                                    requestFromServer = false;
                                    redundantRequests();
                                }
                                else if (actualMessage.getMessageType() == 1) {
                                    //UNCHOKE
                                    System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] is unchoked by [" + otherPeerID + "]");
                                    try {
                                        requestFromServer = true;
                                        requestPiece();
                                    } catch (IOException e) {
                                        System.out.println("Client Error 6 " + e.toString());
                                    }
                                }
                                else if (actualMessage.getMessageType() == 2) {
                                    //Interested
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
                                    Have msg = null;
                                    try {
                                        msg = (Have) MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());
                                        String name = msg.getFileName();

                                        int pieceNum = ByteBuffer.wrap(msg.getPieceIndex()).getInt();

                                        if (!serverBitFields.containsKey(name)){
                                            serverBitFields.put(name, msg.getBitField());
                                        }

                                        serverBitFields.get(name).getBitField()[pieceNum] = 1;

                                        System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] received the \"have\" message from [" + otherPeerID + "] for the piece [" + pieceNum + "]");

                                        if(requestFromServer) {
                                            try {
                                                requestPiece();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        else {
                                            if(bitFields.containsKey(name)){
                                                if(bitFields.get(name).getBitField()[pieceNum] == 0) {
                                                    ActualMessage interestMessage = new ActualMessage(1, 2, null);
                                                    try {
                                                        sendMessage(MessageConversion.messageToBytes(interestMessage));
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                            else{
                                                ActualMessage interestMessage = new ActualMessage(1, 2, null);
                                                try {
                                                    sendMessage(MessageConversion.messageToBytes(interestMessage));
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }

                                        }
                                    } catch (IOException e) {
                                        System.out.println("Client Error 8 " + e.toString());
                                    } catch (ClassNotFoundException e) {
                                        System.out.println("Client Error 9 " + e.toString());
                                    }


                                }
                                else if (actualMessage.getMessageType() == 5) {
                                    //System.out.println("Peer " + peerID + " received Bitfield Message from " + otherPeerID);

                                    BitField bitField = null;
                                    try {
                                        bitField = (BitField)MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());
                                    } catch (IOException e) {
                                        System.out.println("Client Error 11 " + e.toString());
                                    } catch (ClassNotFoundException e) {
                                        System.out.println("Client Error 12 " + e.toString());
                                    }

                                    serverBitFields.put(bitField.getFileName(), bitField);

                                    if (requestFlag == false) {
                                        requestFlag = true;
                                        Boolean flag = true;
                                        outerloop:
                                        for (Map.Entry mapElement : serverBitFields.entrySet()) {
                                            String name = (String) mapElement.getKey();
                                            BitField bitFieldServer = ((BitField) mapElement.getValue());

                                            if (bitFields.containsKey(name)) {
                                                BitField bitFieldClient = bitFields.get(name);
                                                int length = bitFieldClient.bitField.length;
                                                for (int i = 0; i < length; i++) {
                                                    if (bitFieldClient.bitField[i] == 0 && bitFieldServer.bitField[i] == 1 && requestBitFields.get(name)[i] == 0) {
                                                        ActualMessage interestMessage = new ActualMessage(1, 2, null);
                                                        try {
                                                            sendMessage(MessageConversion.messageToBytes(interestMessage));
                                                        } catch (IOException e) {
                                                            System.out.println("Client Error 13 " + e.toString());
                                                        }
                                                        flag = false;
                                                        break outerloop;
                                                    }
                                                }
                                            } else {
                                                ActualMessage interestMessage = new ActualMessage(1, 2, null);
                                                try {
                                                    sendMessage(MessageConversion.messageToBytes(interestMessage));
                                                } catch (IOException e) {
                                                    System.out.println("Client Error 14 " + e.toString());
                                                }
                                                flag = false;
                                                prepareToReceiveFile(bitFieldServer);
                                                break outerloop;
                                            }
                                        }
                                        if (flag) {
                                            ActualMessage notInterestMessage = new ActualMessage(1, 3, null);
                                            try {
                                                sendMessage(MessageConversion.messageToBytes(notInterestMessage));
                                            } catch (IOException e) {
                                                System.out.println("Client Error 15 " + e.toString());
                                            }
                                            requestFlag = false;
                                        }
                                    }

                                }
                                else if (actualMessage.getMessageType() == 6) {
                                    //REQUEST

                                    Request msg = null;
                                    try {
                                        msg = (Request) MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());
                                    } catch (IOException e) {
                                        System.out.println("Client Error 16 " + e.toString());
                                    } catch (ClassNotFoundException e) {
                                        System.out.println("Client Error 17 " + e.toString());
                                    }
                                    String name = msg.getFileName();


                                    if(!files.containsKey(name)){
                                        try {
                                            readActualFile(name);
                                        } catch (IOException e) {
                                            System.out.println("Client Error 18 " + e.toString());
                                        }
                                    }

                                    int pieceNum = ByteBuffer.wrap(msg.getPieceIndex()).getInt();

                                    //System.out.println("Peer " + peerID + " received Request Message from " + otherPeerID + " for file: " + name + " piece: " + pieceNum);
                                    if(sendToServer) {
                                        if (bitFields.get(name).bitField[pieceNum] == 1) {
                                            byte[] piece = Arrays.copyOfRange(files.get(name), pieceNum * bitFields.get(name).PieceSize, pieceNum * bitFields.get(name).PieceSize + bitFields.get(name).PieceSize);
                                            Piece pieceMsg = new Piece(name, msg.getPieceIndex(), piece);
                                            ActualMessage interestMessage = null;
                                            try {
                                                interestMessage = new ActualMessage(1, 7, new PayloadMessage(MessageConversion.messageToBytes(pieceMsg)));
                                            } catch (IOException e) {
                                                System.out.println("Client Error 19 " + e.toString());
                                            }
                                            try {
                                                sendMessage(MessageConversion.messageToBytes(interestMessage));
                                            } catch (IOException e) {
                                                System.out.println("Client Error 20 " + e.toString());
                                            }

                                            if (!serverBitFields.containsKey(name)) {
                                                BitField temp = bitFields.get(name);
                                                BitField temp2 = new BitField(temp.FileName, temp.FileSize, temp.PieceSize, new byte[temp.getBitField().length]);
                                                for (int i = 0; i < temp.getBitField().length; i++) {
                                                    temp2.getBitField()[i] = 0;
                                                }
                                                serverBitFields.put(name, temp2);
                                            }
                                            serverBitFields.get(name).bitField[pieceNum] = 1;

                                            byte[] temp = serverBitFields.get(name).bitField;
                                            Boolean tempFlag = true;
                                            for (int i = 0; i < serverBitFields.get(name).bitField.length-1; i++){
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

                                    Piece a = null;
                                    try {
                                        a = (Piece) MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());
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


                                            try {
                                                System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] has downloaded the complete file.");
                                                Files.write(Path.of(System.getProperty("user.dir") + "/peerFolder/" + peerID + "/" + fname), files.get(fname));
                                            } catch (IOException e) {
                                                System.out.println("Client Error 23 " + e.toString());
                                            }
                                        }


                                        try {
                                            requestPiece();
                                        } catch (IOException e) {
                                            System.out.println("Client Error 24 " + e.toString());
                                        }
                                    } catch (IOException e) {
                                        System.out.println("Client Error 21 " + e.toString());
                                    } catch (ClassNotFoundException e) {
                                        System.out.println("Client Error 22 " + e.toString());
                                    }

                                }
                            }
                        } catch (IOException e) {
                            System.out.println("Client Error 2 " + e.toString() + " " + receivedMsg + "Client " + peerID) ;
                        } catch (ClassNotFoundException e) {
                            System.out.println("Client Error 3 " + e.toString());
                        }

                    }
        }
    }


}
