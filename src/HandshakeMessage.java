

public class HandshakeMessage {
    private String header;
    private String zeros;
    private int peerID;

    public HandshakeMessage(int peerID) {
        this.header = "P2PFILESHARINGPROJ";
        this.zeros = "0000000000";
        this.peerID = peerID;
    }
    public String getHeader() {
        return header;
    }
    public String getZeros() {
        return zeros;
    }
    public int getPeerID() {
        return peerID;
    }
}
