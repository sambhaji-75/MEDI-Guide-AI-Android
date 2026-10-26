package com.mediguide.ai;

public class MedicineModel {
    private String id, userId, medicineName, dosage, frequency;
    private int hour, minute;
    private long createdAt;

    public MedicineModel() {}

    public MedicineModel(String id, String userId, String medicineName, String dosage,
                         int hour, int minute, String frequency, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.hour = hour;
        this.minute = minute;
        this.frequency = frequency;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String v) { id = v; }
    public String getUserId() { return userId; }
    public void setUserId(String v) { userId = v; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String v) { medicineName = v; }
    public String getDosage() { return dosage; }
    public void setDosage(String v) { dosage = v; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String v) { frequency = v; }
    public int getHour() { return hour; }
    public void setHour(int v) { hour = v; }
    public int getMinute() { return minute; }
    public void setMinute(int v) { minute = v; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long v) { createdAt = v; }
}
