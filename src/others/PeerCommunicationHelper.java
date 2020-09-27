package others;

import FileProcessor.FileManagerExecutor;
import messages.Message;
import messages.MessageHandler;
import messages.MessageUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.BitSet;

public class PeerCommunicationHelper {
	
	public static Message sendBitSetMsg(BufferedOutputStream out) throws Exception{
		MessageHandler messageHandler = new MessageHandler((byte)5,Peer.getPeerInstance().getBitSet().toByteArray());
		Message message = messageHandler.buildMessage();
		System.out.println(message.getMessage_length() + " " + message.getMessage_type() + " " + message.getMessagePayload());
		byte[] messageToSend = MessageUtil.concatenateByteArrays(MessageUtil
				.concatenateByte(message.getMessage_length(), message.getMessage_type()),message.getMessagePayload());
		out.write(messageToSend);
		out.flush();

		return message;
	}
	
	public static Message sendInterestedMsg(BufferedOutputStream out) throws Exception{
		MessageHandler messageHandler = new MessageHandler((byte)2);
		Message message = messageHandler.buildMessage();
		byte[] messageToSend = MessageUtil.concatenateByte(message.getMessage_length(), message.getMessage_type());
		out.write(messageToSend);
		out.flush();
		return message;
	}
	
	public static Message sendNotInterestedMsg(BufferedOutputStream out) throws Exception{
		MessageHandler messageHandler = new MessageHandler((byte)3);
		Message message = messageHandler.buildMessage();
		byte[] messageToSend = MessageUtil.concatenateByte(message.getMessage_length(), message.getMessage_type());
		out.write(messageToSend);
		out.flush();
		return message;
	}
	
	public static Message sendChokeMsg(BufferedOutputStream out) throws Exception{
		MessageHandler messageHandler = new MessageHandler((byte)0);
		Message message = messageHandler.buildMessage();
		byte[] messageToSend = MessageUtil.concatenateByte(message.getMessage_length(), message.getMessage_type());
		out.write(messageToSend);
		out.flush();
		return message;
	}
	
	public static Message sendUnChokeMsg(BufferedOutputStream out) throws Exception{
		MessageHandler messageHandler = new MessageHandler((byte)1);
		Message message = messageHandler.buildMessage();
		byte[] messageToSend = MessageUtil.concatenateByte(message.getMessage_length(), message.getMessage_type());
		out.write(messageToSend);
		out.flush();
		return message;
	}
	
	public static Message sendRequestMsg(BufferedOutputStream out, RemotePeerInfo remote) throws Exception{
		MessageHandler messageHandler = new MessageHandler((byte)6,getPieceIndex(remote));
		Message message = messageHandler.buildMessage();
		byte[] messageToSend = MessageUtil.concatenateByte(message.getMessage_length(), message.getMessage_type());
		out.write(messageToSend);
		out.flush();
		return message;
	}

	public static Message sendHaveMsg(BufferedOutputStream out, int recentReceivedPieceIndex) throws Exception{
		MessageHandler messageHandler = new MessageHandler((byte)6,MessageUtil.intToByteArray(recentReceivedPieceIndex));
		Message message = messageHandler.buildMessage();
		byte[] messageToSend = MessageUtil.concatenateByte(message.getMessage_length(), message.getMessage_type());
		out.write(messageToSend);
		out.flush();
		return message;
	}
	
	public static Message sendPieceMsg(BufferedOutputStream out, int pieceIndex) throws Exception{
		File piecePart = FileManagerExecutor.getFilePart(pieceIndex);
		byte[] payload = Files.readAllBytes(piecePart.toPath());
		MessageHandler messageHandler = new MessageHandler((byte)7,payload );
		Message message = messageHandler.buildMessage();
		byte[] messageToSend = MessageUtil.concatenateByte(message.getMessage_length(), message.getMessage_type());
		out.write(messageToSend);
		out.flush();
		return message;
	}
	
    public static byte[] getActualMessage(BufferedInputStream in) {
        byte[] lengthByte = new byte[4];
        int read = -1;
        byte[] data = null;
        try {
            read = in.read(lengthByte);
            if (read != 4) {
                System.out.println("Message length is not proper!!!");
            }
            int dataLength = MessageUtil.byteArrayToInt(lengthByte);
            //read msg type
            byte[] msgType = new byte[1];
            in.read(msgType);
            if (msgType[0] == (byte)5) {
                int actualDataLength = dataLength - 1;
                data = new byte[actualDataLength];
                data = MessageUtil.readBytes(in, data, actualDataLength);
            } else {
                System.out.println("Wrong message type sent");
            }

        } catch (IOException e) {
            System.out.println("Could not read length of actual message");
            e.printStackTrace();
        }
        return data;
    }
    
    public static byte getMessageType(BufferedInputStream in) throws IOException{
    	byte[] lengthBytePlusMsgType = new byte[5];
    	in.read(lengthBytePlusMsgType);
    	return lengthBytePlusMsgType[4];
    }
    
    public static boolean isInterseted(BitSet b1, BitSet b2){
    	for(int i=0; i<b2.length();i++){
    		if(b1.get(i)!=b2.get(i)){
    			return false;
    		}
    	}
		return true;
    }
    
    public static byte[] getPieceIndex(RemotePeerInfo remote){
    	BitSet b1 = remote.getBitfield();
    	BitSet b2 = Peer.getPeerInstance().getBitSet();
    	int pieceIndex = compare(b1,b2);
    	return MessageUtil.intToByteArray(pieceIndex);
    }
    
   public static int compare(BitSet lhs, BitSet rhs) {
	    if (lhs.equals(rhs)) return 0;
	    BitSet xor = (BitSet)lhs.clone();
	    xor.xor(rhs);
	    int firstDifferent = xor.length()-1;
	    if(firstDifferent==-1)
	            return 0;

	    return rhs.get(firstDifferent) ? 1 : -1;
	}
   public static void computeDownloadRate(){
	   
   }

}
