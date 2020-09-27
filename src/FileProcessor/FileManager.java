package FileProcessor;

import java.io.File;
import java.net.Socket;

public interface FileManager{
    void fileSplit(File inputFile, int pieceSize);
  //  void sendFilePart(int filePart, Socket socket);
    //void getFilePart(int filePart, Socket socket);
    //void filesmerge();
}