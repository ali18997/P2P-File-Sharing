package others;

public enum MessageType {
    choke ((byte) 0),
    unchoke ((byte) 1),
    interested((byte) 2),
    notinterested((byte) 3),
    have((byte) 4),
    bitfield((byte) 5),
    request((byte) 6),
    piece((byte) 7);

    private final byte value;

    private MessageType (byte value) {
        this.value = value;
    }

}
