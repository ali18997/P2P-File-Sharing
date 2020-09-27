package others;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Author: @DilipKunderu
 */
public class Client implements Runnable {
    private ExecutorService outThreadPool;
    private Map<Integer, RemotePeerInfo> peersToConnectTo;
    private Thread runningThread;

    Client(Map<Integer, RemotePeerInfo> peersToConnectTo) {
        this.peersToConnectTo = peersToConnectTo;
        this.outThreadPool = Executors.newFixedThreadPool(this.peersToConnectTo.size());
    }

    @Override
    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        for (Map.Entry e : this.peersToConnectTo.entrySet()) {
            RemotePeerInfo remote = (RemotePeerInfo) e.getValue();
            try {
                this.outThreadPool.execute(
                        new OutgoingRequestsHandler(remote)
                );
            } catch (Exception ex) {
                throw new RuntimeException("Thread pool size exceeded", ex);
            }
        }
    }
}
