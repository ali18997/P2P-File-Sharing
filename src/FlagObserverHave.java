import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

public class FlagObserverHave implements Observer {

    private HashMap<String, BitField> bitFields;
    private HashMap<String, BitField> otherPeerBitFields;
    private HashMap<String, byte[]> haveBitFields;
    private ObjectOutputStream out;

    public FlagObserverHave(HashMap<String, BitField> bitFields, HashMap<String, BitField> otherPeerBitFields, HashMap<String, byte[]> haveBitFields, ObjectOutputStream out) {
        this.bitFields = bitFields;
        this.otherPeerBitFields = otherPeerBitFields;
        this.haveBitFields = haveBitFields;
        this.out = out;
    }

    public void update(Observable obj, Object arg) {
        CommonMethods.haveDetect(bitFields, otherPeerBitFields, haveBitFields, out);
    }
}