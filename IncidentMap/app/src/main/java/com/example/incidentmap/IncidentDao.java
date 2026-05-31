package com.example.incidentmap;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface IncidentDao {
    @Insert
    void insert(IncidentEntity incident);

    @Delete
    void delete(IncidentEntity incident);

    @Query("SELECT * FROM incidents")
    List<IncidentEntity> getAllIncidents();

    @Query("SELECT COUNT(*) FROM incidents")
    int getCount();
}