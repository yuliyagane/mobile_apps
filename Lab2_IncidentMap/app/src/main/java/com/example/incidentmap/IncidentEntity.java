package com.example.incidentmap;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "incidents")
public class IncidentEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String type;
    public String title;
    public String description;
    public double latitude;
    public double longitude;
    public long createAt;

    public IncidentEntity(
            String type,
            String title,
            String description,
            double latitude,
            double longitude,
            long createAt
    ){
        this.type = type;
        this.title = title;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createAt = createAt;
    }
}
