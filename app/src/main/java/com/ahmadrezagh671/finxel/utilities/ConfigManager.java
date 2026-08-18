package com.ahmadrezagh671.finxel.utilities;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import com.ahmadrezagh671.finxel.models.configModel.Cell;
import com.ahmadrezagh671.finxel.models.configModel.CompareWithLastMessage;
import com.ahmadrezagh671.finxel.models.configModel.ConfigModel;
import com.ahmadrezagh671.finxel.models.configModel.Field;
import com.ahmadrezagh671.finxel.models.configModel.Information;
import com.ahmadrezagh671.finxel.models.configModel.LayoutComponent;
import com.ahmadrezagh671.finxel.models.configModel.NotificationEntry;
import com.ahmadrezagh671.finxel.models.configModel.Replacement;
import com.ahmadrezagh671.finxel.models.configModel.Skip;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Manages configuration files stored as JSON in the app's internal storage.
 * Provides save, load, validate, parse, and delete operations for user-defined configs.
 */
public class ConfigManager {
    private static final String TAG = "ConfigManager";

    /**
     * Saves a JSON config string to a file in the app's internal "configs" directory.
     * @param context Application context
     * @param fileName The name of the config file (without .json extension)
     * @param jsonString The raw JSON content to save
     * @return true if the file was saved successfully, false otherwise
     */
    public static boolean saveConfig(Context context, String fileName, String jsonString) {
        File configDir = new File(context.getFilesDir(), "configs");

        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, fileName);

        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(jsonString);
            return true; // Success
        } catch (IOException e) {
            e.printStackTrace();
            return false; // Failed to save
        }
    }

    /**
     * Returns the file object for a config file in the app's internal "configs" directory.
     * @param context Application context
     * @param fileName The name of the config file (without .json extension)
     * @return The File object for the config file, or null if it doesn't exist
     */
    public static File getConfigFile(Context context, String fileName){
        File configDir = new File(context.getFilesDir(), "configs");
        File configFile = new File(configDir, fileName + ".json");

        if (!configFile.exists()) {
            return null; // File not found
        }

        return configFile;
    }

    /**
     * Validates whether a JSON string matches the mandatory structure of the app config.
     * Checks for required fields: information (name, provider, color), fields (name, type),
     * and result (2D array matrix).
     * @param jsonString The raw string content of the configuration file
     * @return true if valid and safe to parse, false otherwise
     */
    public static boolean isValidConfig(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }

        try {
            // 1. Try to parse as base JSON object
            JSONObject mainObj = new JSONObject(jsonString);

            // 2. Validate "information" object and its mandatory children
            if (!mainObj.has("information")) return false;
            JSONObject infoObj = mainObj.getJSONObject("information");
            if (!infoObj.has("name") || !infoObj.has("provider") || !infoObj.has("color") || !infoObj.has("location") || !infoObj.has("app_version")) {
                return false;
            }
            // Ensure provider is a nested JSON array
            if (!(infoObj.get("provider") instanceof JSONArray)) {
                return false;
            }

            // 3. Validate "fields" array and its essential element components
            if (!mainObj.has("fields")) return false;
            JSONArray fieldsArray = mainObj.getJSONArray("fields");

            for (int i = 0; i < fieldsArray.length(); i++) {
                Object element = fieldsArray.get(i);
                if (!(element instanceof JSONObject)) {
                    return false; // Every item inside fields must be a json object
                }

                JSONObject fieldObj = (JSONObject) element;
                // Every single field MUST have at least a name and a type identifier
                if (!fieldObj.has("name") || !fieldObj.has("type")) {
                    return false;
                }
            }

            // 4. Validate "result" multi-dimensional array matrix
            if (!mainObj.has("result")) return false;
            Object resultObj = mainObj.get("result");
            if (!(resultObj instanceof JSONArray)) {
                return false;
            }

            // Optional structural layout check: ensure it has at least one valid combination row
            JSONArray resultMatrix = (JSONArray) resultObj;
            if (resultMatrix.length() > 0) {
                if (!(resultMatrix.get(0) instanceof JSONArray)) {
                    return false; // The base result array must contain sub-arrays
                }
            }

            // If it passes all structural checks, it's safe to process
            return true;

        } catch (JSONException e) {
            // String is not a valid JSON string structure or type mismatches occurred
            Log.e(TAG, "isValidConfig: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reads the content of a specific config file from internal storage.
     * @param context Application context
     * @param fileName The name of the config file (without .json extension)
     * @return The file content as a string, or null if the file doesn't exist or an error occurs
     */
    public static String loadConfig(Context context, String fileName) {
        File configDir = new File(context.getFilesDir(), "configs");
        File configFile = new File(configDir, fileName + ".json");

        if (!configFile.exists()) {
            return null; // File not found
        }

        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            return stringBuilder.toString(); // Return the JSON string
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads all config JSON files from internal storage and parses them into a map of ConfigModel objects.
     * @param context Application context
     * @return A map of config names to their parsed ConfigModel objects
     */
    public static Map<String, ConfigModel> loadAllConfigs(Context context) {
        Map<String, ConfigModel> configMap = new HashMap<>();
        File configDir = new File(context.getFilesDir(), "configs");

        if (configDir.exists() && configDir.isDirectory()) {
            File[] files = configDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".json")) {
                        String jsonString = readFileToString(file);
                        if (jsonString != null) {
                            try {
                                ConfigModel config = parseJsonToModel(context,jsonString);
                                configMap.put(config.information.name,config);
                            } catch (JSONException e) {
                                e.printStackTrace(); // Log invalid JSON file formats
                            }
                        }
                    }
                }
            }
        }
        return configMap;
    }

    /**
     * Parses a raw JSON string into a ConfigModel object by manually traversing
     * the information, fields, skips, result matrix, and layout sections.
     * @param jsonString The raw JSON string to parse
     * @return A fully populated ConfigModel object
     * @throws JSONException if the JSON structure is invalid or missing required fields
     */
    public static ConfigModel parseJsonToModel(Context context,String jsonString) throws JSONException {
        JSONObject mainObj = new JSONObject(jsonString);

        // --- 1. Parse Information ---
        JSONObject infoObj = mainObj.getJSONObject("information");

        // check app config version, show toast error if not match, and continue
        String appConfigVersion;
        if (infoObj.has("app_version")){
            appConfigVersion = infoObj.getString("app_version");
            if (!Utilities.getConfigVersion(context).equalsIgnoreCase(appConfigVersion)){
                Toast.makeText(context, "Config version does not match the app version.", Toast.LENGTH_SHORT).show();
            }
        }else {
            throw new JSONException("App version is not defined.");
        }

        String infoName = infoObj.getString("name");
        int infoColor = Color.parseColor(infoObj.getString("color"));
        boolean location = infoObj.has("location") ? infoObj.getBoolean("location") : false;

        JSONArray providerArray = infoObj.getJSONArray("provider");
        List<String> providers = new ArrayList<>();
        for (int i = 0; i < providerArray.length(); i++) {
            providers.add(providerArray.getString(i));
        }
        Information information = new Information(infoName,appConfigVersion, providers, infoColor,location);

        // --- 2. Parse spinnerListArray---
        JSONArray spinnerListArray = mainObj.has("spinner_list") ? mainObj.getJSONArray("spinner_list") : new JSONArray();

        // --- 3. Parse Skips Array ---
        List<Skip> skipsList = new ArrayList<>();
        if(mainObj.has("skips")){
            JSONArray skipsArray = mainObj.getJSONArray("skips");

            for (int i = 0; i < skipsArray.length(); i++) {
                JSONObject skipObj = skipsArray.getJSONObject(i);
                Skip skip = new Skip();

                // Required properties
                skip.type = skipObj.getString("type");
                skip.action = skipObj.getString("action");
                skip.errorText = skipObj.getString("error_text");

                skipsList.add(skip);
            }
        }

        // --- 4. Parse compare_with_last_message  ---
        List<CompareWithLastMessage> compareWithLastMessageList = new ArrayList<>();
        if (mainObj.has("compare_with_last_message")){
            JSONArray compareWithLastMessageArray = mainObj.getJSONArray("compare_with_last_message");

            for (int i = 0; i < compareWithLastMessageArray.length(); i++) {
                JSONObject fieldObj = compareWithLastMessageArray.getJSONObject(i);

                CompareWithLastMessage compareWithLastMessage = new CompareWithLastMessage(
                        fieldObj.getString("name"),
                        fieldObj.getString("field"),
                        fieldObj.getString("last_message_field"),
                        fieldObj.getString("error_text")
                );

                compareWithLastMessageList.add(compareWithLastMessage);
            }
        }
        // --- 5. Parse notification_entry  ---
        List<NotificationEntry> notificationEntryList = new ArrayList<>();
        if (mainObj.has("notification_entry")){
            JSONArray notificationEntryArray = mainObj.getJSONArray("notification_entry");
            for (int i = 0; i < notificationEntryArray.length(); i++) {
                JSONObject entryObj = notificationEntryArray.getJSONObject(i);

                NotificationEntry notificationEntry = new NotificationEntry(
                        entryObj.getString("name"),
                        entryObj.getString("hint")
                );

                notificationEntryList.add(notificationEntry);
            }
        }

        // --- 6. Parse before click Fields  ---
        List<Field> balanceFieldsList = new ArrayList<>();
        if (mainObj.has("fields_before_click")){
            JSONArray balanceFieldsArray = mainObj.getJSONArray("fields_before_click");

            for (int i = 0; i < balanceFieldsArray.length(); i++) {
                JSONObject fieldObj = balanceFieldsArray.getJSONObject(i);
                Field field = getFieldFromJsonObj(fieldObj);
                balanceFieldsList.add(field);
            }
        }

        // --- 7. Parse Fields Array ---
        JSONArray fieldsArray = mainObj.getJSONArray("fields");
        List<Field> fieldsList = new ArrayList<>();

        for (int i = 0; i < fieldsArray.length(); i++) {
            JSONObject fieldObj = fieldsArray.getJSONObject(i);
            Field field = getFieldFromJsonObj(fieldObj);
            fieldsList.add(field);
        }

        // --- 8. Parse Layout ---
        JSONArray layoutArray = mainObj.getJSONArray("layout");
        List<LayoutComponent> layoutComponentList = new ArrayList<>();

        for (int i = 0; i < layoutArray.length(); i++) {
            layoutComponentList.add(layoutObjToLayoutComponent(layoutArray.getJSONObject(i),spinnerListArray));
        }

        // --- 9. Parse Result Matrix ---
        JSONArray resultMatrixArray = mainObj.getJSONArray("result");
        List<List<Cell>> resultMatrix = new ArrayList<>();

        for (int i = 0; i < resultMatrixArray.length(); i++) {
            JSONArray rowArray = resultMatrixArray.getJSONArray(i);
            List<Cell> rowList = new ArrayList<>();
            for (int j = 0; j < rowArray.length(); j++) {
                JSONObject resultObj = rowArray.getJSONObject(j);
                Cell cell = new Cell();

                cell.name = resultObj.getString("name");
                cell.value = resultObj.getString("value");
                cell.type = resultObj.getString("type");

                if (resultObj.has("b_color")) cell.b_color = Color.parseColor(resultObj.getString("b_color"));
                if (resultObj.has("f_color")) cell.f_color = Color.parseColor(resultObj.getString("f_color"));
                if (resultObj.has("size")) cell.size = (float) resultObj.getDouble("size");

                // for SPINNER
                if (resultObj.has("items")) {
                    String listName = resultObj.getString("items");
                    for (int k = 0; k < spinnerListArray.length(); k++) {
                        JSONObject thisList = spinnerListArray.getJSONObject(k);
                        if (thisList.getString("name").equals(listName)){
                            JSONArray targetList = thisList.getJSONArray("value");
                            for (int l = 0; l < targetList.length(); l++) {
                                cell.items.add(targetList.getString(l));
                            }
                            break;
                        }
                    }
                }

                rowList.add(cell);
            }
            resultMatrix.add(rowList);
        }

        return new ConfigModel(information,skipsList, fieldsList, balanceFieldsList,notificationEntryList,compareWithLastMessageList,resultMatrix,layoutComponentList);
    }

    /**
     * Extracts a Field object from a JSON object, handling required and optional properties
     * including regex, group, format, text, key, operator, and replacement sub-arrays.
     * @param fieldObj The JSON object representing a field
     * @return A Field object populated with the JSON data
     * @throws JSONException if required fields are missing
     */
    private static Field getFieldFromJsonObj(JSONObject fieldObj) throws JSONException {
        Field field = new Field();

        // Required properties
        field.name = fieldObj.getString("name");
        field.type = fieldObj.getString("type");

        // Optional properties check
        if (fieldObj.has("regex")) field.regex = fieldObj.getString("regex");
        if (fieldObj.has("group")) field.group = fieldObj.getInt("group");
        if (fieldObj.has("format")) field.format = fieldObj.getString("format");
        if (fieldObj.has("text")) field.text = fieldObj.getString("text");
        if (fieldObj.has("key")) field.key = fieldObj.getString("key");
        if (fieldObj.has("operator")) field.operator = fieldObj.getString("operator");

        // Optional Sub-Array: replacements_after
        if (fieldObj.has("replacements_after")) {
            JSONArray repAfterArr = fieldObj.getJSONArray("replacements_after");
            for (int j = 0; j < repAfterArr.length(); j++) {
                JSONObject repObj = repAfterArr.getJSONObject(j);
                field.replacementsAfter.add(new Replacement(repObj.getString("find"), repObj.getString("replace")));
            }
        }

        // Optional Sub-Array: replacements_before
        if (fieldObj.has("replacements_before")) {
            JSONArray repBeforeArr = fieldObj.getJSONArray("replacements_before");
            for (int j = 0; j < repBeforeArr.length(); j++) {
                JSONObject repObj = repBeforeArr.getJSONObject(j);
                field.replacementsBefore.add(new Replacement(repObj.getString("find"), repObj.getString("replace")));
            }
        }

        // Optional Sub-Array: fields (for COMBINE_FIELDS types)
        if (fieldObj.has("fields")) {
            JSONArray fieldsSubArr = fieldObj.getJSONArray("fields");
            for (int j = 0; j < fieldsSubArr.length(); j++) {
                field.fields.add(fieldsSubArr.getString(j));
            }
        }

        // Optional Sub-Array: flags
        if (fieldObj.has("flags")) {
            JSONArray flagsArr = fieldObj.getJSONArray("flags");
            for (int j = 0; j < flagsArr.length(); j++) {
                field.flags.add(flagsArr.getString(j));
            }
        }

        return field;
    }


    /**
     * Converts a layout JSON object into a LayoutComponent, recursively processing
     * nested inside layouts and resolving spinner item references.
     * @param layoutObj The JSON object representing a layout component
     * @param spinnerListArray The shared spinner list array for resolving item references
     * @return A LayoutComponent populated with the JSON data
     * @throws JSONException if required fields are missing
     */
    private static LayoutComponent layoutObjToLayoutComponent(JSONObject layoutObj,JSONArray spinnerListArray) throws JSONException {
        LayoutComponent layoutComponent = new LayoutComponent();

        layoutComponent.id = layoutObj.getString("id");
        layoutComponent.type = layoutObj.getString("type");

        if (layoutObj.has("hint")) layoutComponent.hint = layoutObj.getString("hint");
        if (layoutObj.has("value")) layoutComponent.value = layoutObj.getString("value");
        if (layoutObj.has("items")){
            String listName = layoutObj.getString("items");
            for (int k = 0; k < spinnerListArray.length(); k++) {
                JSONObject thisList = spinnerListArray.getJSONObject(k);
                if (thisList.getString("name").equals(listName)){
                    JSONArray targetList = thisList.getJSONArray("value");
                    for (int l = 0; l < targetList.length(); l++) {
                        layoutComponent.items.add(targetList.getString(l));
                    }
                    break;
                }
            }
        }
        if (layoutObj.has("selected")) layoutComponent.selected = layoutObj.getInt("selected");
        // if it has LayoutComponents inside this LayoutComponent
        if (layoutObj.has("inside")){
            JSONArray layoutArray = layoutObj.getJSONArray("inside");
            List<LayoutComponent> layoutComponentList = new ArrayList<>();

            for (int j = 0; j < layoutArray.length(); j++) {
                layoutComponentList.add(layoutObjToLayoutComponent(layoutArray.getJSONObject(j),spinnerListArray));
            }
            layoutComponent.inside = layoutComponentList;
        }
        return layoutComponent;
    }

    /**
     * Reads a file's contents into a string.
     * @param file The file to read
     * @return The file content as a string, or null on error
     */
    private static String readFileToString(File file) {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the list of config names from a map of parsed ConfigModel objects.
     * @param configModels A map of config names to ConfigModel objects
     * @return A list of config names
     */
    public static List<String> getNameOfConfigs(Map<String, ConfigModel> configModels) {
        if (configModels == null) {
            return new ArrayList<>();
        }
        // Directly initialize the list with the map's keys
        return new ArrayList<>(configModels.keySet());
    }

    /**
     * Deletes a config JSON file from internal storage.
     * @param context Application context
     * @param fileName The name of the config file (without .json extension)
     * @return true if the file was deleted, false if it didn't exist
     */
    public static boolean deleteConfig(Context context, String fileName) {
        File configDir = new File(context.getFilesDir(), "configs");
        File configFile = new File(configDir, fileName+".json");

        if (configFile.exists()) {
            return configFile.delete();
        }
        return false;
    }

    /**
     * Checks a text body against a list of skip rules.
     * Returns the errorText of the first matching skip rule, or null if no rule matches.
     * @param textBody The SMS body text to check
     * @param skips The list of skip rules to apply
     * @return An error message if a skip rule matched, or null if no match
     */
    public static String checkSkips(String textBody, List<Skip> skips){
        for (Skip skip:skips) {
            switch (skip.type){
                case "CONTAINS":
                    if (textBody.contains(skip.action)){
                        return skip.errorText;
                    }
                    break;
                case "NOT_CONTAINS":
                    if (!textBody.contains(skip.action)){
                        return skip.errorText;
                    }
                    break;
            }
        }
        return null;
    }

}
