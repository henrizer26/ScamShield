package com.example.scamshield.models;

public class ScamModel {
    private int id;
    private String phoneNumber;
    private String message;
    private String scamType;
    private String dateTime;
    private boolean isReported;

    // Constructor
    public ScamModel(int id, String phoneNumber, String message, String scamType, String dateTime, boolean isReported) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.scamType = scamType;
        this.dateTime = dateTime;
        this.isReported = isReported;
    }

    // Getters
    public int getId() { return id; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getMessage() { return message; }
    public String getScamType() { return scamType; }
    public String getDateTime() { return dateTime; }
    public boolean isReported() { return isReported; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setMessage(String message) { this.message = message; }
    public void setScamType(String scamType) { this.scamType = scamType; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }
    public void setReported(boolean reported) { isReported = reported; }
}