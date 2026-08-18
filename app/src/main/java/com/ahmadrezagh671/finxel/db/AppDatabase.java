package com.ahmadrezagh671.finxel.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * The Room database for the app, providing access to the SMS records DAO.
 * Uses a singleton pattern to ensure a single database instance per application.
 */
@Database(entities = {SmsRecord.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public static final String ENTRY_START_KEY_DB = "ENTRY_";
    private static volatile AppDatabase INSTANCE;

    public abstract SmsRecordDao smsRecordDao();

    /**
     * Returns the singleton instance of the database, creating it if necessary.
     *
     * @param context the application context used to build the database
     * @return the singleton AppDatabase instance
     */
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "sms_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}