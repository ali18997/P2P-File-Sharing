import java.io.Serializable;

public class Have implements Serializable {
    private String FileName;
    private byte[] pieceIndex;
    private BitField bitField;

    public Have (String FileName, byte[] pieceIndex, BitField bitField) {
        this.FileName = FileName;
        this.pieceIndex = pieceIndex;
        this.bitField = bitField;
    }

    public byte[] getPieceIndex() {
        return pieceIndex;
    }

    public String getFileName() {
        return FileName;
    }

    public BitField getBitField() {
        return bitField;
    }
}
