package others;

import FileProcessor.FileManagerExecutor;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    private ExecutorService inThreadPool;
    private int serverPort;
    private ServerSocket serverSocket;
    private Thread runningThread;

    Server() {
        this.runningThread = null;
        this.serverPort = Peer.getPeerInstance().get_port();
        inThreadPool = Executors.newFixedThreadPool(Peer.getPeerInstance().peersToExpectConnectionsFrom.size());
    }

    @Override
    public void run() {
        System.out.println("Spawned the SERVER super thread");
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        try {
            serverSocketOpen();
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + this.serverPort, e);
        }

        int key = Peer.getPeerInstance().get_peerID();

        while (!peerProcess.isCompleted()) {
            Socket clientSocket;

            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                if (peerProcess.isCompleted()) {
                    System.out.println("Server stopped");
                    break;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }

            this.inThreadPool.execute(
                    new IncomingRequestsHandler(clientSocket, Peer.getPeerInstance().getPeersToExpectConnectionsFrom().get(++key))
            );
        }
        this.inThreadPool.shutdown();
        stop();
        //set if bitfield is all 1s
        merger ();
        System.out.println("Server stopped");
    }

    private void merger() {
        FileManagerExecutor.filesmerge();
    }

    private synchronized void stop() {
        peerProcess.setCompleted(true);
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to close server", e);
        }
    }

    private void serverSocketOpen() throws IOException {
        this.serverSocket = new ServerSocket(this.serverPort);
    }
}