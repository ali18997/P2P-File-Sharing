package model;

import messages.Message;
import messages.MessagePayload;

/**
 * Author: @DilipKunderu
 */
public class UnChoke extends Message {
    public UnChoke() {
        super((byte) 1);
    }
}
