package com.ahmadrezagh671.finxel.db;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a single SMS record stored in the Room database,
 * with a unique ID and a JSON string for flexible key-value data storage.
 */
@Entity(tableName = "sms_records")
public class SmsRecord {

    private static final String TAG = "SmsRecord";
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "sms_id")
    private final String smsId;

    @ColumnInfo(name = "value_json")
    private String valueJson;

    /**
     * Constructs a new SmsRecord with the given SMS ID and JSON value string.
     *
     * @param smsId the unique identifier for the SMS record
     * @param valueJson the JSON string containing key-value data
     */
    public SmsRecord(@NonNull String smsId, String valueJson) {
        this.smsId = smsId;
        this.valueJson = valueJson;
    }

    /**
     * Returns the unique SMS identifier.
     *
     * @return the smsId
     */
    @NonNull
    public String getSmsId() {
        return smsId;
    }

    /**
     * Returns the JSON value string associated with this record.
     *
     * @return the valueJson
     */
    @NonNull
    public String getValueJson() {
        return valueJson;
    }

    /**
     * Adds or updates a key-value pair in the JSON data string.
     * If the existing valueJson is null, empty, or invalid JSON, it initializes a new JSON object.
     *
     * @param key the key to add or update
     * @param value the value to associate with the key
     * @return true if the operation succeeded, false if a JSON error occurred
     */
    public boolean addValueToJson(String key, Object value) {
        JSONObject jsonObject;

        // Check if valueJson is null, empty, or invalid, and initialize it
        if (this.valueJson == null || this.valueJson.trim().isEmpty()) {
            jsonObject = new JSONObject();
        } else {
            try {
                jsonObject = new JSONObject(this.valueJson);
            } catch (JSONException e) {
                // Fallback if the existing string isn't valid JSON
                jsonObject = new JSONObject();
            }
        }

        try {
            // Add or update the key-value pair
            jsonObject.put(key, value);
            // Update the string field
            this.valueJson = jsonObject.toString();
        } catch (JSONException e) {
            Log.e(TAG, "addValueToJson: "+ e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Retrieves a value from the JSON data string for the given key.
     * Returns null if the JSON is empty, the key does not exist, or a parse error occurs.
     *
     * @param key the key whose value should be retrieved
     * @return the value associated with the key, or null if not found or on error
     */
    public Object getValueFromJson(String key) {
        // If the JSON string is empty or null, there's nothing to retrieve
        if (this.valueJson == null || this.valueJson.trim().isEmpty()) {
            return null;
        }

        try {
            JSONObject jsonObject = new JSONObject(this.valueJson);

            // Check if the key exists before trying to get it
            if (jsonObject.has(key)) {
                return jsonObject.get(key);
            }
        } catch (JSONException e) {
            Log.e(TAG, "getValueFromJson: " + e.getMessage());
        }

        return null; // Return null if the key wasn't found or an error occurred
    }
}