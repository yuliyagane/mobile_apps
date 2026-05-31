package com.example.incidentmap;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {IncidentEntity.class},
        version = 1
)

public abstract class AppDatabase extends RoomDatabase{
    public abstract IncidentDao incidentDao();
}
