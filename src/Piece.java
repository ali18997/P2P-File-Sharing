import java.io.Serializable;

public class Piece implements Serializable {
    private byte[] pieceIndex;
    private byte[] piece;

    public Piece(byte[] pieceIndex, byte[] piece) {
        this.pieceIndex = pieceIndex;
        this.piece = piece;
    }

    public byte[] getPiece() {
        return piece;
    }

    public byte[] getPieceIndex() {
        return pieceIndex;
    }

}
