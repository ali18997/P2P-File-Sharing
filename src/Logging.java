import java.io.*;

public class Logging {
    public static void writeLog(int peerID, String log) {

            try (FileWriter fw = new FileWriter("logs/log_peer_" + peerID + ".log", true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {
                out.println(log);
            } catch (IOException e) {
                System.out.println(e.toString());
            }


    }
}
