package com.example.recoverytracker;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(entities = {RecoveryRecord.class}, version = 1, exportSchema = false)
public abstract class RecoveryDatabase extends RoomDatabase {
    private static RecoveryDatabase instance;

    public abstract RecoveryDao recoveryDao();

    public static synchronized RecoveryDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            RecoveryDatabase.class,
                            "recovery_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}