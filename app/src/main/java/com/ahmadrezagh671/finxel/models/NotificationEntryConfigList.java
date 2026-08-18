package com.ahmadrezagh671.finxel.models;

import android.util.Log;

import com.ahmadrezagh671.finxel.models.configModel.ConfigModel;
import com.ahmadrezagh671.finxel.models.configModel.NotificationEntry;
import com.ahmadrezagh671.finxel.models.configModel.Skip;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Holds a list of notification entry configurations parsed from JSON,
 * each containing providers, skip rules, and notification entries.
 */
public class NotificationEntryConfigList {

    private static final String TAG = "NotificationEntryConfig";

    public static class NotificationEntryConfig {
        public List<String> providers;
        public List<Skip> skips;
        public List<NotificationEntry> entries;

        public NotificationEntryConfig(List<String> providers, List<Skip> skips, List<NotificationEntry> entries) {
            this.providers = providers;
            this.skips = skips;
            this.entries = entries;
        }

        public NotificationEntryConfig(ConfigModel configModel) {
            if (!configModel.notificationEntries.isEmpty()){
                this.providers = configModel.information.provider;
                this.skips = configModel.skips;
                this.entries = configModel.notificationEntries;
            }
        }

        private static JSONObject parseJson(String json) {
            try {
                return new JSONObject(json);
            } catch (JSONException e) {
                Log.e(TAG, "parseJson: " + e.getMessage());
                return new JSONObject();
            }
        }

        public NotificationEntryConfig(String json) {
            this(parseJson(json));
        }

        public NotificationEntryConfig(JSONObject jsonObject) {
            try {
                // providers
                JSONArray providersJsonArray = jsonObject.getJSONArray("providers");
                this.providers = new ArrayList<>();
                for (int i = 0; i < providersJsonArray.length(); i++) {
                    providers.add(providersJsonArray.getString(i));
                }

                // skips
                JSONArray skipsJsonArray = jsonObject.getJSONArray("skips");
                this.skips = new ArrayList<>();
                for (int i = 0; i < skipsJsonArray.length(); i++) {
                    JSONObject skipObj = skipsJsonArray.getJSONObject(i);
                    Skip skip = new Skip();
                    skip.type = skipObj.getString("type");
                    skip.action = skipObj.getString("action");
                    skip.errorText = skipObj.getString("error_text");
                    skips.add(skip);
                }

                // entries
                JSONArray entriesJsonArray = jsonObject.getJSONArray("entries");
                this.entries = new ArrayList<>();
                for (int i = 0; i < entriesJsonArray.length(); i++) {
                    JSONObject entryObj = entriesJsonArray.getJSONObject(i);
                    NotificationEntry entry = new NotificationEntry(entryObj.getString("name"),entryObj.getString("action"));
                    entries.add(entry);
                }

            } catch (JSONException e) {
                Log.e(TAG, "NotificationEntryConfig: " + e.getMessage());
            }
        }

        public JSONObject toJson() {
            JSONObject jsonObject = new JSONObject();
            try {
                // providers
                jsonObject.put("providers",new JSONArray(providers));

                // skips
                JSONArray skipJsonArray = new JSONArray();
                if (skips != null && !skips.isEmpty()) {
                    skips.forEach(skip -> {
                        JSONObject skipObj = new JSONObject();
                        try {
                            skipObj.put("type",skip.type);
                            skipObj.put("action",skip.action);
                            skipObj.put("error_text",skip.errorText);
                            skipJsonArray.put(skipObj);
                        } catch (JSONException e) {
                            Log.e(TAG, "NotificationEntryConfig toJson skips: " + e.getMessage());
                        }
                    });
                }
                jsonObject.put("skips",skipJsonArray);

                // entries
                JSONArray entriesJsonArray = new JSONArray();
                if (entries != null && !entries.isEmpty()) {
                    entries.forEach(entry -> {
                        JSONObject entryObj = new JSONObject();
                        try {
                            entryObj.put("name",entry.name);
                            entryObj.put("action",entry.hint);
                            entriesJsonArray.put(entryObj);
                        } catch (JSONException e) {
                            Log.e(TAG, "NotificationEntryConfig toJson skips: " + e.getMessage());
                        }
                    });
                }
                jsonObject.put("entries", entriesJsonArray);

            } catch (JSONException e) {
                Log.e(TAG, "NotificationEntryConfig toJson: " + e.getMessage());
            }

            return jsonObject;
        }
        public String toJsonString() {
            return toJson().toString();
        }
    }

    public List<NotificationEntryConfig> configs;

    public NotificationEntryConfigList(List<NotificationEntryConfig> configs) {
        this.configs = configs;
    }

    public NotificationEntryConfigList(Map<String, ConfigModel> configModel) {
        this.configs = new ArrayList<>();
        configModel.forEach((key , config) -> {
            configs.add(new NotificationEntryConfig(config));
        });
    }

    public NotificationEntryConfigList(String json) throws JSONException {
        this.configs = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(json);

        for (int i = 0; i < jsonArray.length(); i++) {
            configs.add(new NotificationEntryConfig(jsonArray.getJSONObject(i)));
        }
    }

    public NotificationEntryConfigList() {
        this.configs = new ArrayList<>();
    }

    public void addConfig(NotificationEntryConfig config) {
        configs.add(config);
    }

    public void addConfig(ConfigModel configModel) {
        configs.add(new NotificationEntryConfig(configModel));
    }

    public String toJsonString() {
        return toJson().toString();
    }

    public JSONArray toJson() {
        JSONArray jsonArray = new JSONArray();
        configs.forEach(config -> {
            jsonArray.put(config.toJson());
        });
        return jsonArray;
    }
}
