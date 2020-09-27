package model;

import messages.Message;
import messages.MessagePayload;

/**
 * Author: @DilipKunderu
 */
public class Have extends Message {
	
    public Have(byte[] piece_index) {
        super((byte) 4,piece_index);
    }
}
