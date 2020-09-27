package model;

import messages.Message;

/**
 * Author: @DilipKunderu
 */
public class Piece extends Message {

    public Piece(byte[] payloadWithPiece_index) {
        super((byte) 7,payloadWithPiece_index);
    }
}
