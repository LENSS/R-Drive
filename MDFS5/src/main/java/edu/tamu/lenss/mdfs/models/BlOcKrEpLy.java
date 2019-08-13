package edu.tamu.lenss.mdfs.models;

import java.util.List;

//This class object is only used in MDFSBlockRetrieverViaRsock.java class
public class BlOcKrEpLy {
    private String fileName;
    private long fileCreatedTime;  //note: fileCReatedTime is fileID
    private byte blockIdx;
    private List<Byte> fileFragIndex;
    private String source;
    private String destination;

    public BlOcKrEpLy(String name, long time, byte blockIndex, String source, String destination){
        this.fileName = name;
        this.fileCreatedTime = time;
        this.blockIdx = blockIndex;
        this.source = source;
        this.destination = destination;
    }

    public byte getBlockIdx() {
        return blockIdx;
    }

    public void setBlockIdx(byte blockIdx) {
        this.blockIdx = blockIdx;
    }

    public List<Byte> getBlockFragIndex() {
        return fileFragIndex;
    }

    public void setBlockFragIndex(List<Byte> fileFragIndex) {
        this.fileFragIndex = fileFragIndex;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileCreatedTime() {
        return fileCreatedTime;
    }


    public String getDestination() {
        return destination;
    }

    public String setDestination() {
        return destination;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
