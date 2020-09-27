package logger;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Logger {

    int peer_ID;

    protected final String logFile;

    public Logger(int peer_ID){
        this.peer_ID = peer_ID;
        this.logFile = "\\project\\log_peer_"+peer_ID+".log";
    }

    public void log(String s) throws IOException {
        log(logFile, s);
    }

    public void log(String f, String s) throws IOException {
        TimeZone EST = TimeZone.getTimeZone("EST");
        Date date = new Date();
        DateFormat format = new SimpleDateFormat("yyyy.mm.dd hh:mm:ss ");
        format.setTimeZone(EST);
        String currentTime = format.format(date);
        FileWriter aWriter = new FileWriter(f, true);
        aWriter.write(currentTime + ": Peer " + peer_ID + s + "\n");
        aWriter.flush();
        aWriter.close();
    }
}
