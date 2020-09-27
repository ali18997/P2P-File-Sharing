package model;

import messages.Message;
import messages.MessagePayload;

/**
 * Author: @DilipKunderu
 */
public class NotInterested extends Message {
    public NotInterested() {
        super((byte) 3);
    }
}
