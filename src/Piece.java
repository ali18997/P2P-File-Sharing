import java.io.Serializable;

public class Piece implements Serializable {
    private String name;
    private byte[] pieceIndex;
    private byte[] piece;

    public Piece(String name, byte[] pieceIndex, byte[] piece) {
        this.name = name;
        this.pieceIndex = pieceIndex;
        this.piece = piece;
    }

    public byte[] getPiece() {
        return piece;
    }

    public byte[] getPieceIndex() {
        return pieceIndex;
    }

    public String getName() {
        return name;
    }
}
