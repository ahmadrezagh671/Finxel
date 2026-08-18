package com.ahmadrezagh671.finxel.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import com.ahmadrezagh671.finxel.models.NotificationEntryConfigList;
import com.ahmadrezagh671.finxel.models.configModel.ConfigModel;

import org.json.JSONArray;
import java.util.ArrayList;
import java.util.Map;

/**
 * Utility for reading and writing app settings via SharedPreferences.
 * Manages location service status/config, notification entry status/config,
 * and layout confirm-prevent-close preferences.
 */
public class AppSettings {

    private static final String PREFS_NAME = "settings_prefs";
    private static final String KEY_LAYOUT_CONFIRM_PREVENT_CLOSE = "LCPC";
    private static final String KEY_LOCATION_SERVICE = "LS";
    private static final String KEY_LOCATION_SERVICE_CONFIG = "LSC";
    private static final String KEY_NOTIFICATION_ENTRY = "NE";
    private static final String KEY_NOTIFICATION_ENTRY_CONFIG = "NEC";


    /**
     * Clears all app settings from SharedPreferences.
     * @param context Application context
     */
    public static void resetSettings(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply();
    }

    /**
     * Saves the location service JSON config string to SharedPreferences.
     * @param context Application context
     * @param config JSON string of location service configuration
     */
    public static void updateLocationServiceConfig(Context context, String config) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LOCATION_SERVICE_CONFIG, config)
                .apply();
    }
    /**
     * Reads the location service JSON config string from SharedPreferences.
     * @param context Application context
     * @return The stored config string, or null if not set
     */
    public static String getLocationServiceConfig(Context context){
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LOCATION_SERVICE_CONFIG, null);
    }

    /**
     * Saves the location service enabled status to SharedPreferences.
     * @param context Application context
     * @param isChecked Whether the location service is enabled
     */
    public static void updateLocationServiceStatus(Context context, boolean isChecked) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LOCATION_SERVICE, isChecked)
                .apply();
    }
    /**
     * Reads the location service enabled status from SharedPreferences.
     * @param context Application context
     * @return true if the location service is enabled, false otherwise
     */
    public static boolean getLocationServiceStatus(Context context){
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LOCATION_SERVICE, false);
    }

    /**
     * Saves the layout confirm-prevent-close status to SharedPreferences.
     * @param context Application context
     * @param isChecked Whether the setting is enabled
     */
    public static void updateLayoutConfirmPreventCloseStatus(Context context, boolean isChecked) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LAYOUT_CONFIRM_PREVENT_CLOSE, isChecked)
                .apply();
    }
    /**
     * Reads the layout confirm-prevent-close status from SharedPreferences.
     * @param context Application context
     * @return true if the setting is enabled, false otherwise
     */
    public static boolean getLayoutConfirmPreventCloseStatus(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LAYOUT_CONFIRM_PREVENT_CLOSE, false);
    }

    /**
     * Converts a map of ConfigModel objects to a JSON array of provider names
     * and saves it as the location service config.
     * @param context Application context
     * @param configs Map of config names to ConfigModel objects
     */
    public static void updateLocationServiceConfig(Context context, Map<String, ConfigModel> configs) {
        ArrayList<String> allProviders = new ArrayList<>();
        if (configs != null){
            configs.forEach((key, configModel) -> {
                if (configModel.information.location){
                    allProviders.addAll(configModel.information.provider);
                }
            });
        }
        JSONArray jsonArray = new JSONArray(allProviders);
        AppSettings.updateLocationServiceConfig(context,jsonArray.toString());
    }

    /**
     * Reads the notification entry enabled status from SharedPreferences.
     * @param context Application context
     * @return true if notification entries are enabled, false otherwise
     */
    public static boolean getNotificationEntryStatus(Context context){
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_NOTIFICATION_ENTRY, false);
    }

    /**
     * Reads the notification entry JSON config string from SharedPreferences.
     * @param context Application context
     * @return The stored config string, or null if not set
     */
    public static String getNotificationEntryConfig(Context context){
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_NOTIFICATION_ENTRY_CONFIG, null);
    }

    /**
     * Saves the notification entry JSON config string to SharedPreferences.
     * @param context Application context
     * @param config JSON string of notification entry configuration
     */
    public static void updateNotificationEntryConfig(Context context, String config) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_NOTIFICATION_ENTRY_CONFIG, config)
                .apply();
    }

    /**
     * Saves the notification entry enabled status to SharedPreferences.
     * @param context Application context
     * @param isChecked Whether notification entries are enabled
     */
    public static void updateNotificationEntryStatus(Context context, boolean isChecked) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NOTIFICATION_ENTRY, isChecked)
                .apply();
    }

    /**
     * Converts a map of ConfigModel objects to a JSON notification entry config
     * and saves it to SharedPreferences.
     * @param context Application context
     * @param configs Map of config names to ConfigModel objects
     */
    public static void updateNotificationEntryConfig(Context context, Map<String, ConfigModel> configs) {
        NotificationEntryConfigList notificationEntryConfigList = new NotificationEntryConfigList(configs);
        AppSettings.updateNotificationEntryConfig(context,notificationEntryConfigList.toJsonString());
    }
}
