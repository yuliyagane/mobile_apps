package com.example.recoverytracker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "records")
public class RecoveryRecord {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private long date;
    private int painLevel;
    private int activityLevel;
    private String medications;
    private String notes;

    public RecoveryRecord(long date, int painLevel, int activityLevel, String medications, String notes) {
        this.date = date;
        this.painLevel = painLevel;
        this.activityLevel = activityLevel;
        this.medications = medications;
        this.notes = notes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }
    public int getPainLevel() { return painLevel; }
    public void setPainLevel(int painLevel) { this.painLevel = painLevel; }
    public int getActivityLevel() { return activityLevel; }
    public void setActivityLevel(int activityLevel) { this.activityLevel = activityLevel; }
    public String getMedications() { return medications; }
    public void setMedications(String medications) { this.medications = medications; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}