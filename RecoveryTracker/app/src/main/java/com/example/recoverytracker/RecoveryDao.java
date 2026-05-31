package com.example.recoverytracker;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface RecoveryDao {
    @Insert
    void insert(RecoveryRecord record);

    @Query("SELECT * FROM records ORDER BY date DESC")
    List<RecoveryRecord> getAllRecords();

    @Query("SELECT * FROM records ORDER BY date DESC LIMIT 5")
    List<RecoveryRecord> getLast5Records();

    @Query("SELECT COUNT(*) FROM records")
    int getCount();

    @Query("DELETE FROM records WHERE id = :id")
    void deleteById(int id);
}