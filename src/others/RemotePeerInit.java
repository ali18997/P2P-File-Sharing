package others;
//import com.jcraft.jsch.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class RemotePeerInit {
//    private static final List<RemotePeerInfo> remotePeerInfoList = Peer.getPeerInstance().peersToConnectTo;
//    private static final String setCommand = "java p2p/src/peerProcess ";
//
//    public static void main(String[] args) {
//        if (!remotePeerInfoList.isEmpty()) {
//            for (RemotePeerInfo r : remotePeerInfoList) {
//                try {
//                    JSch jSch = new JSch();
//
//                    /**
//                     * path to private key needs to be inserted here; change in accordance
//                     * with whose laptop would act as the primary peer
//                     * (definitely not Dilip's Laptop)
//                     */
//                    jSch.addIdentity ("/home/dilip/.ssh/id_rsa", "");
//                    Session session = jSch.getSession ("dilip", r.get_hostName(), 22);
//
//                    /**
//                     * Some kind of properties are being set here,
//                     * not sure what those are or where exactly are they being updated
//                     */
//                    session.setConfig ("StrictHostKeyChecking", "no");
//
//                    session.connect();
//
//                    System.out.println("Session to peer# " + r.get_peerID() + " at " + r.get_hostName());
//
//                    Channel channel = session.openChannel("exec");
//                    System.out.println("remotePeerID"+r.get_peerID());
//                    ((ChannelExec) channel).setCommand(setCommand + r.get_peerID());
//
//                    channel.setInputStream(null);
//                    ((ChannelExec) channel).setErrStream(System.err);
//
//                    InputStream input = channel.getInputStream();
//                    channel.connect();
//
//                    System.out.println("Channel Connected to peer# " + r.get_peerID() + " at "
//                            + r.get_hostName() + " server with commands");
//
//                    new Thread(() -> {
//                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input));
//                        String line;
//
//                        try {
//                            while ((line = bufferedReader.readLine()) != null) {
//                                System.out.println(r.get_peerID() + ">:" + line);
//                            }
//                            bufferedReader.close();
//                        } catch (Exception ex) {
//                            System.out.println(r.get_peerID() + " Exception >:");
//                            ex.printStackTrace();
//                        }
//                        channel.disconnect();
//                        session.disconnect();
//                    }).start();
//
//                } catch (JSchException e) {
//                System.out.println(r.get_peerID() + " JSchException >:");
//                e.printStackTrace();
//                } catch (IOException ex) {
//                    System.out.println(r.get_peerID() + " Exception >:");
//                    ex.printStackTrace();
//                }
//            }
//        }
//    }
}
