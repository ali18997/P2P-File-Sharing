package messages;

import others.Constants;
import others.RemotePeerInfo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class Handshake {
	private String header;
	private String zero_bits;
	private int peer_ID;
	private RemotePeerInfo remotePeerInfo;

	public Handshake(int peer_ID, RemotePeerInfo remotePeerInfo) {
		this.header = Constants.HANDSHAKEHEADER;
		this.zero_bits = Constants.ZERO_BITS;
		this.peer_ID = peer_ID;
		this.remotePeerInfo = remotePeerInfo;
	}

	@Override
	public String toString() {
		return this.header + this.zero_bits + String.valueOf(this.peer_ID);
	}
	
	 public void sendHandshakeMsg(BufferedOutputStream out) throws IOException {
	            byte[] handshakeMsg = MessageUtil.concatenateByteArrays(MessageUtil
	                    .concatenateByteArrays(this.header.getBytes(),
	                            this.zero_bits.getBytes()), 
	                    MessageUtil.intToByteArray(this.peer_ID));
	           out.write(handshakeMsg);
	           out.flush();
	    }
	 
	 public boolean recieveHandshake(BufferedInputStream in) throws IOException{
		 byte[] b = new byte[32];
         in.read(b);
         byte[] copyOfRange = Arrays.copyOfRange(b, 28, 32);
         byte[] header = Arrays.copyOfRange(b, 0, 18);
         int peerId = MessageUtil.byteArrayToInt(copyOfRange);
         String s = new String(header);
             if (s.equals(Constants.HANDSHAKEHEADER) && (this.remotePeerInfo.get_peerID() == peerId)) {
            	 return true;
             } else {
            	 return false;
             }
	 }
	 
}
