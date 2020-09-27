package FileProcessor;

import others.Constants;
import others.Peer;
import others.PeerCommunicationHelper;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class FileManagerExecutor  {
   static  Map<Integer,File> pieceMap;
   static  Map<Integer,File> fileSoFar = new TreeMap<>();

    public static void fileSplit(File inputFile, int pieceSize){
        pieceMap = new HashMap<>();
        FileInputStream inputStream;
        FileOutputStream partOfFile;
        File newFilePart;
        int fileSize;
        int bytesRead,count = 0;
        byte[] filePiece;
        try{
            inputStream = new FileInputStream(inputFile);
            fileSize = (int)inputFile.length();
            while(fileSize>0){
                filePiece = new byte[pieceSize];
                bytesRead = inputStream.read(filePiece);
                fileSize-=bytesRead;
                count++;
                newFilePart = new File( Constants.root + "/peer_" + Peer.getPeerInstance().get_peerID() + "/" + "Part" + Integer.toString(count));
                partOfFile = new FileOutputStream(newFilePart);
                partOfFile.write(filePiece);
                pieceMap.put(count,newFilePart);
                partOfFile.flush();
                partOfFile.close();
            }
            inputStream.close();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

    }
    
    public static File getFilePart(int filePartNumber){
    	return fileSoFar.get(filePartNumber);
    }
    
    public static void putFilePart(int filePartNumber, File filePart){
    	fileSoFar.put(filePartNumber,filePart);
    }

   /* public void sendFilePart(int filePart, Socket socket) {
        File fileToSend;
        FileInputStream fileInputStream;
        BufferedInputStream bufferedInputStream;
        OutputStream outputStream;
        fileToSend = pieceMap.get(filePart);
        byte[] byteFile = new byte[(int) fileToSend.length()];
        try {

            fileInputStream = new FileInputStream(fileToSend);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            bufferedInputStream.read(byteFile, 0, byteFile.length);
            outputStream = socket.getOutputStream();
            outputStream.write(byteFile, 0, byteFile.length);
            outputStream.flush();

        }catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }*/

    public static void acceptFilePart(int filePart,BufferedInputStream in) {
        FileOutputStream fileOutputStream;
        BufferedOutputStream bufferedOutputStream;
        File fileToWrite;
        try {
            fileToWrite = new File(Constants.root + "/peer_" + String.valueOf(Peer.getPeerInstance().get_peerID()) + "/"+"Part" + Integer.toString(filePart));
            fileOutputStream = new FileOutputStream(fileToWrite);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            bufferedOutputStream.write(PeerCommunicationHelper.getActualMessage(in));
            fileSoFar.put(filePart, fileToWrite);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();

        }catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public static  void filesmerge(){
        File mergeFile = new File(Constants.root + "/peer_" + String.valueOf(Peer.getPeerInstance().get_peerID()) + "/"+Constants.getFileName());    // change file name
        FileOutputStream fileOutputStream;
        FileInputStream fileInputStream;
        byte[] fileBytes;
        int bytesRead=0;
        try{
            fileOutputStream = new FileOutputStream(mergeFile,true);
            Set<Integer> keys = fileSoFar.keySet();

            for(Integer key : keys){
                fileInputStream = new FileInputStream(fileSoFar.get(key));
                fileBytes = new byte[(int)fileSoFar.get(key).length()];
                bytesRead = fileInputStream.read(fileBytes,0,(int) fileSoFar.get(key).length());
                fileOutputStream.write(fileBytes);
                fileOutputStream.flush();
                fileInputStream.close();
            }
            fileOutputStream.close();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}


