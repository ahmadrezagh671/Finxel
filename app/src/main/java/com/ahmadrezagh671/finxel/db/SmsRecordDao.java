package com.ahmadrezagh671.finxel.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for the SMS records table, providing insert, query, and delete operations.
 */
@Dao
public interface SmsRecordDao {

    // OnConflictStrategy.REPLACE acts as an "upsert" (inserts new, or overwrites if smsId exists)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(SmsRecord record);

    @Query("SELECT * FROM sms_records")
    LiveData<List<SmsRecord>> getAllRecordsLive();

    @Query("SELECT * FROM sms_records WHERE sms_id = :id LIMIT 1")
    SmsRecord getRecordById(String id);

    @Delete
    void deleteRecord(SmsRecord record);
}