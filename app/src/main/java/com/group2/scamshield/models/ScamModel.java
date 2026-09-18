package com.group2.scamshield.models;

import java.io.Serializable;

/** One stored scam: a row of the {@code scams} table, passed between screens. */
public class ScamModel implements Serializable {
    private final int id;
    private final String phoneNumber;
    private final String message;
    private final String scamType;
    private final String dateTime;
    private final boolean isReported;

    public ScamModel(int id, String phoneNumber, String message, String scamType, String dateTime, boolean isReported) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.scamType = scamType;
        this.dateTime = dateTime;
        this.isReported = isReported;
    }

    public int getId() { return id; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getMessage() { return message; }
    public String getScamType() { return scamType; }
    public String getDateTime() { return dateTime; }
    public boolean isReported() { return isReported; }
}
