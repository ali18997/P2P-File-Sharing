
import java.io.Serializable;

public class BitField implements Serializable {
    String FileName;
    int FileSize;
    int PieceSize;
    byte[] bitField;

    public BitField(String FileName, int FileSize, int PieceSize, byte[] bitField) {
        this.FileName = FileName;
        this.FileSize = FileSize;
        this.PieceSize = PieceSize;
        this.bitField = bitField;
    }

    public int getFileSize() {
        return FileSize;
    }

    public int getPieceSize() {
        return PieceSize;
    }

    public String getFileName() {
        return FileName;
    }

    public byte[] getBitField() {
        return bitField;
    }


}
