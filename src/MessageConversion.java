import java.io.*;

public class MessageConversion {
    public static byte[] messageToBytes(Object obj) throws IOException {
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream);
        objectOutStream.writeObject(obj);
        return byteOutStream.toByteArray();
    }
    public static Object bytesToMessage(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteInStream = new ByteArrayInputStream(data);
        ObjectInputStream objectInStream = new ObjectInputStream(byteInStream);
        return objectInStream.readObject();
    }
}
