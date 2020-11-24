import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Client {
    public Socket requestSocket;           //socket connect to the server
    public ObjectOutputStream out;         //stream write to the socket
    public ObjectInputStream in;          //stream read from the socket
    private Boolean requestFlag = false;
    private int PieceSize;
    private FlagObservable flag;
    private FlagObservable flag2;
    private int peerID;
    private int otherPeerID;
    private String otherPeerHostName;

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

        FlagObserver observer = new FlagObserver();
        FlagObserver2 observer2 = new FlagObserver2();
        flag.addObserver(observer);
        flag2.addObserver(observer2);
        requestSocket = new Socket(this.otherPeerHostName, otherPeerPort);

        out = new ObjectOutputStream(requestSocket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(requestSocket.getInputStream());
        CommonMethods.prepareBitFields(peerID, PieceSize, bitFields);
        new MessageReceiving().start();
    }

    public void handShake() throws IOException {
        HandshakeMessage handshakeMessage = new HandshakeMessage(otherPeerID);
        CommonMethods.sendMessage(MessageConversion.messageToBytes(handshakeMessage), out);
    }


    public class FlagObserver implements Observer {

        public FlagObserver() {}

        public void update(Observable obj, Object arg) {
            CommonMethods.haveDetect(bitFields, serverBitFields, haveBitFields, out);
        }
    }

    public class FlagObserver2 implements Observer {

        public FlagObserver2() {}

        public void update(Observable obj, Object arg) {
            sendToServer = CommonMethods.updateNeighbours(otherPeerID, interestedPeers, sendToServer, out);
        }
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

    public class MessageReceiving extends Thread {
        public void run() {

                    while (true) {
                        Object receivedMsg = null;
                        try {
                            receivedMsg = MessageConversion.bytesToMessage((byte[]) in.readObject());
                            if (receivedMsg instanceof HandshakeMessage) {
                                HandshakeMessage handshakeMessage = (HandshakeMessage) receivedMsg;
                                if (handshakeMessage.getHeader().equals("P2PFILESHARINGPROJ")) {
                                    System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] makes a connection to Peer [" + otherPeerID + "]");
                                    HandshakeMessage handshakeMessageBack = new HandshakeMessage(peerID);
                                    try {
                                        CommonMethods.sendMessage(MessageConversion.messageToBytes(handshakeMessageBack), out);
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
                                            CommonMethods.sendMessage(MessageConversion.messageToBytes(bitFieldMessage), out);
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
                                    CommonMethods.redundantRequests(bitFields, haveBitFields);
                                }
                                else if (actualMessage.getMessageType() == 1) {
                                    //UNCHOKE
                                    System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] is unchoked by [" + otherPeerID + "]");
                                    try {
                                        requestFromServer = true;
                                        CommonMethods.requestPiece(requestFromServer, bitFields, serverBitFields, requestBitFields, files, out);
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
                                                CommonMethods.requestPiece(requestFromServer, bitFields, serverBitFields, requestBitFields, files, out);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        else {
                                            if(bitFields.containsKey(name)){
                                                if(bitFields.get(name).getBitField()[pieceNum] == 0) {
                                                    ActualMessage interestMessage = new ActualMessage(1, 2, null);
                                                    try {
                                                        CommonMethods.sendMessage(MessageConversion.messageToBytes(interestMessage), out);
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                            else{
                                                ActualMessage interestMessage = new ActualMessage(1, 2, null);
                                                try {
                                                    CommonMethods.sendMessage(MessageConversion.messageToBytes(interestMessage), out);
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
                                                            CommonMethods.sendMessage(MessageConversion.messageToBytes(interestMessage), out);
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
                                                    CommonMethods.sendMessage(MessageConversion.messageToBytes(interestMessage), out);
                                                } catch (IOException e) {
                                                    System.out.println("Client Error 14 " + e.toString());
                                                }
                                                flag = false;
                                                CommonMethods.prepareToReceiveFile(bitFieldServer, bitFields, files, requestBitFields);
                                                break outerloop;
                                            }
                                        }
                                        if (flag) {
                                            ActualMessage notInterestMessage = new ActualMessage(1, 3, null);
                                            try {
                                                CommonMethods.sendMessage(MessageConversion.messageToBytes(notInterestMessage), out);
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
                                            CommonMethods.readActualFile(name, files, peerID);
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
                                                CommonMethods.sendMessage(MessageConversion.messageToBytes(interestMessage), out);
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
                                            CommonMethods.requestPiece(requestFromServer, bitFields, serverBitFields, requestBitFields, files, out);
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
