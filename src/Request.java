import java.io.Serializable;

public class Request implements Serializable {
    private String FileName;
    private byte[] pieceIndex;

    public Request (String FileName, byte[] pieceIndex) {
        this.FileName = FileName;
        this.pieceIndex = pieceIndex;
    }

    public byte[] getPieceIndex() {
        return pieceIndex;
    }

    public String getFileName() {
        return FileName;
    }
}
