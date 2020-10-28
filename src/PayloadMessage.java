import java.io.Serializable;

public class PayloadMessage implements Serializable {
    private byte[] message;

    public PayloadMessage(byte[] message){
        this.message = message;
    }
}
