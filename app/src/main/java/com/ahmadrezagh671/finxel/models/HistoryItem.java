package com.ahmadrezagh671.finxel.models;

/**
 * Represents a history entry for a backed-up CSV file, containing the file name,
 * associated bank name, and the timestamp of the backup.
 */
public class HistoryItem {

    private String fileName;
    private String bankName;
    private long timestamp;

    public HistoryItem(String fileName, String bankName, long timestamp) {
        this.fileName = fileName;
        this.bankName = bankName;
        this.timestamp = timestamp;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}