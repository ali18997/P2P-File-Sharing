import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ActualMessageProcessor {
    private Boolean requestFromOtherPeer = false;
    private Boolean requestFlag = false;
    private HashMap<String, BitField> bitFields;
    private HashMap<String, byte[]> files;
    private HashMap<String, BitField> otherPeerBitFields = new HashMap<String, BitField>();
    private HashMap<String, byte[]> requestBitFields;
    private HashMap<String, byte[]> haveBitFields = new HashMap<String, byte[]>();
    private HashMap<Integer, Integer> connectedPeersRates;
    private HashMap<Integer, Boolean> interestedPeers;
    private int peerID;
    private int otherPeerID;
    public ObjectOutputStream out;
    private FlagObservable flagHave;
    private FlagObservable flagNeighbours;
    private Boolean sendToOtherPeer = false;
    private int PieceSize;

    public ActualMessageProcessor(int peerID, int otherPeerID, HashMap<String, BitField> bitFields,
                                  HashMap<String, byte[]> requestBitFields,
                                  HashMap<Integer, Integer> connectedPeersRates,
                                  HashMap<Integer, Boolean> interestedPeers,
                                  HashMap<String, byte[]> files, FlagObservable flagHave,
                                  FlagObservable flagNeighbours, int PieceSize,
                                  ObjectOutputStream out
                                  ) throws IOException {
        this.peerID = peerID;
        this.otherPeerID = otherPeerID;
        this.bitFields = bitFields;
        this.requestBitFields = requestBitFields;
        this.connectedPeersRates = connectedPeersRates;
        this.interestedPeers = interestedPeers;
        this.files = files;
        this.flagHave = flagHave;
        this.flagNeighbours = flagNeighbours;
        this.PieceSize = PieceSize;
        this.out = out;

        System.out.println("CURRENT PEER ID: " + peerID + " OTHER PEER ID: " + otherPeerID);

        FlagObserverHave observerHave = new FlagObserverHave(bitFields, otherPeerBitFields, haveBitFields, out);
        FlagObserverNeighbours observerNeighbours = new FlagObserverNeighbours();
        flagHave.addObserver(observerHave);
        flagNeighbours.addObserver(observerNeighbours);
        CommonMethods.prepareBitFields(peerID, PieceSize, bitFields);

    }

    public void process(ActualMessage actualMessage) throws IOException, ClassNotFoundException {
        if (actualMessage.getMessageType() == 0) {
            //CHOKE
            System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] is choked by [" + otherPeerID + "]");
            requestFromOtherPeer = false;
            CommonMethods.redundantRequests(bitFields, requestBitFields);

        } else if (actualMessage.getMessageType() == 1) {
            //UNCHOKE
            System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] is unchoked by [" + otherPeerID + "]");
            requestFromOtherPeer = true;
            CommonMethods.requestPiece(requestFromOtherPeer, bitFields, otherPeerBitFields, requestBitFields, files, out);
        } else if (actualMessage.getMessageType() == 2) {
            System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] received the \"interested\" message from [" + otherPeerID + "]");

            System.out.print("BEFORE: " + peerID + " " + otherPeerID + " " + interestedPeers);
            interestedPeers.put(otherPeerID, false);
            System.out.print("AFTER: " + peerID + " " + otherPeerID + " " + interestedPeers);

        } else if (actualMessage.getMessageType() == 3) {
            System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] received the \"not interested\" message from [" + otherPeerID + "]");
        } else if (actualMessage.getMessageType() == 4) {
            //HAVE
            Have msg = (Have) MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());
            String name = msg.getFileName();

            int pieceNum = ByteBuffer.wrap(msg.getPieceIndex()).getInt();

            if (!otherPeerBitFields.containsKey(name)) {
                otherPeerBitFields.put(name, msg.getBitField());
            }

            otherPeerBitFields.get(name).getBitField()[pieceNum] = 1;

            System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] received the \"have\" message from [" + otherPeerID + "] for the piece [" + pieceNum + "]");
            if (requestFromOtherPeer) {
                CommonMethods.requestPiece(requestFromOtherPeer, bitFields, otherPeerBitFields, requestBitFields, files, out);
            } else {
                if (bitFields.containsKey(name)) {
                    if (bitFields.get(name).getBitField()[pieceNum] == 0) {
                        ActualMessage interestMessage = new ActualMessage(1, 2, null);
                        CommonMethods.sendMessage(MessageConversion.messageToBytes(interestMessage), out);
                    }
                } else {
                    ActualMessage interestMessage = new ActualMessage(1, 2, null);
                    CommonMethods.sendMessage(MessageConversion.messageToBytes(interestMessage), out);
                }
            }
        } else if (actualMessage.getMessageType() == 5) {
            //System.out.println("Peer " + peerID + " received Bitfield Message from " + otherPeerID);

            BitField bitField = (BitField) MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());

            otherPeerBitFields.put(bitField.getFileName(), bitField);

            if (requestFlag == false) {
                requestFlag = true;
                Boolean flag = true;
                outerloop:
                for (Map.Entry mapElement : otherPeerBitFields.entrySet()) {
                    String name = (String) mapElement.getKey();
                    BitField bitFieldClient = ((BitField) mapElement.getValue());

                    if (bitFields.containsKey(name)) {
                        BitField bitFieldServer = bitFields.get(name);
                        int length = bitFieldServer.bitField.length;
                        for (int i = 0; i < length; i++) {
                            if (bitFieldServer.bitField[i] == 0 && bitFieldClient.bitField[i] == 1 && requestBitFields.get(name)[i] == 0) {
                                ActualMessage interestMessage = new ActualMessage(1, 2, null);
                                CommonMethods.sendMessage(MessageConversion.messageToBytes(interestMessage), out);
                                flag = false;
                                break outerloop;
                            }
                        }
                    } else {
                        ActualMessage interestMessage = new ActualMessage(1, 2, null);
                        CommonMethods.sendMessage(MessageConversion.messageToBytes(interestMessage), out);
                        flag = false;
                        CommonMethods.prepareToReceiveFile(bitFieldClient, bitFields, files, requestBitFields);
                        break outerloop;
                    }
                }
                if (flag) {
                    ActualMessage notInterestMessage = new ActualMessage(1, 3, null);
                    CommonMethods.sendMessage(MessageConversion.messageToBytes(notInterestMessage), out);
                    requestFlag = false;
                }
            }

        } else if (actualMessage.getMessageType() == 6) {
            //REQUEST

            Request msg = (Request) MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());
            String name = msg.getFileName();

            if (!files.containsKey(name)) {
                CommonMethods.readActualFile(name, files, peerID);
            }

            int pieceNum = ByteBuffer.wrap(msg.getPieceIndex()).getInt();

            //System.out.println("Peer " + peerID + " received Request Message from " + otherPeerID + " for file: " + name + " piece: " + pieceNum);
            if (sendToOtherPeer) {
                if (bitFields.get(name).bitField[pieceNum] == 1) {
                    byte[] piece = Arrays.copyOfRange(files.get(name), pieceNum * bitFields.get(name).PieceSize, pieceNum * bitFields.get(name).PieceSize + bitFields.get(name).PieceSize);
                    Piece pieceMsg = new Piece(name, msg.getPieceIndex(), piece);
                    ActualMessage interestMessage = new ActualMessage(1, 7, new PayloadMessage(MessageConversion.messageToBytes(pieceMsg)));
                    CommonMethods.sendMessage(MessageConversion.messageToBytes(interestMessage), out);

                    if (!otherPeerBitFields.containsKey(name)) {
                        BitField temp = bitFields.get(name);
                        BitField temp2 = new BitField(temp.FileName, temp.FileSize, temp.PieceSize, new byte[temp.getBitField().length]);
                        for (int i = 0; i < temp.getBitField().length; i++) {
                            temp2.getBitField()[i] = 0;
                        }
                        otherPeerBitFields.put(name, temp2);
                    }
                    otherPeerBitFields.get(name).bitField[pieceNum] = 1;

                    byte[] temp = otherPeerBitFields.get(name).bitField;
                    Boolean tempFlag = true;
                    for (int i = 0; i < otherPeerBitFields.get(name).bitField.length - 1; i++) {
                        if (temp[i] == 0) {
                            tempFlag = false;
                        }
                    }
                    if (tempFlag) {
                        interestedPeers.remove(otherPeerID);
                    }
                }
            } else {
                if (!interestedPeers.containsKey(otherPeerID)) {
                    interestedPeers.put(otherPeerID, false);
                }
            }

        } else if (actualMessage.getMessageType() == 7) {
            //PIECE
            Piece a = (Piece) MessageConversion.bytesToMessage(actualMessage.getPayload().getMessage());
            int pieceNum = ByteBuffer.wrap(a.getPieceIndex()).getInt();
            String fname = a.getName();
            int count = 0;
            for (int i = 0; i < bitFields.get(fname).getBitField().length; i++) {
                if (bitFields.get(fname).getBitField()[i] == 1) {
                    count = count + 1;
                }
            }
            System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] has downloaded the piece [" + pieceNum + "] from [" + otherPeerID + "]. Now the number of pieces it has is [" + (count + 1) + "]");


            byte[] piece = a.getPiece();
            for (int i = 0; i < bitFields.get(fname).getPieceSize(); i++) {
                if (pieceNum * bitFields.get(fname).getPieceSize() + i < files.get(fname).length) {
                    files.get(fname)[pieceNum * bitFields.get(fname).getPieceSize() + i] = piece[i];
                }
            }
            bitFields.get(fname).getBitField()[pieceNum] = 1;
            connectedPeersRates.replace(otherPeerID, connectedPeersRates.get(otherPeerID) + bitFields.get(fname).getPieceSize());

            flagHave.setFlag(!flagHave.getFlag());


            byte[] temp = bitFields.get(fname).getBitField();
            Boolean tempFlag = true;
            for (int i = 0; i < bitFields.get(fname).getBitField().length - 1; i++) {
                if (temp[i] == 0) {
                    tempFlag = false;
                }
            }
            if (tempFlag) {
                System.out.println("[" + java.time.LocalDateTime.now() + "]: Peer [" + peerID + "] has downloaded the complete file.");


                Files.write(Path.of(System.getProperty("user.dir") + "/peerFolder/" + peerID + "/" + fname), files.get(fname));

            }
            CommonMethods.requestPiece(requestFromOtherPeer, bitFields, otherPeerBitFields, requestBitFields, files, out);
        }
    }

    public class FlagObserverNeighbours implements Observer {

        public FlagObserverNeighbours() {
        }

        public void update(Observable obj, Object arg) {
            sendToOtherPeer = CommonMethods.updateNeighbours(otherPeerID, interestedPeers, sendToOtherPeer, out);
        }
    }
}
