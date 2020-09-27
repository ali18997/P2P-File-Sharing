package model;


import messages.Message;
import messages.MessagePayload;

/**
 * Author: @DilipKunderu
 */
public class Interested extends Message {
    public Interested() {
        super((byte) 2);
    }
}
