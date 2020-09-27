package model;

import messages.Message;

/**
 * Author: @DilipKunderu
 */
public class Request extends Message {

   public Request(byte[] pieceIndex) {
        super((byte) 7,pieceIndex);
    }



}
