import java.io.Serializable;

public class Request implements Serializable {
    String FileName;
    byte[] pieceIndex;

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
