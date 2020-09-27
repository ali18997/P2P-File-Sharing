package messages;

import model.*;

public class MessageHandler {
    private byte message_type;
    private byte[] messagePayload;
   
    public MessageHandler(byte message_type) {
        this.message_type = message_type;
    }
    
    public MessageHandler(byte message_type,byte[] messagePayload) {
        this.message_type = message_type;
        this.messagePayload = messagePayload;
    }
    
    public Message buildMessage() throws Exception {
        Message message;
        switch (this.message_type){
            case (byte)0:{
               message = new Choke();
               break;
            }
            case (byte)1:{
                message = new UnChoke();
                break;
            }
            case (byte)2:{
                message = new Interested();
                break;
            }
            case (byte)3:{
                message = new NotInterested();
                break;
            }
            case (byte)4:{
                message = new Have(this.messagePayload);
                break;
            }
            case (byte)5:{
                message = new BitField(this.messagePayload);
                break;
            }
            case (byte)6:{
                message = new Request(this.messagePayload);
                break;
            }
            case (byte)7:{
                message = new Piece(this.messagePayload);
                break;
            }
            default:{
                throw new Exception("Not a valid message type: " + message_type );
            }
        }
        return message;
    }

}
