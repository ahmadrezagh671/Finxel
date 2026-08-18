package com.ahmadrezagh671.finxel.utilities;

import android.net.Uri;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Helper for opening a file picker and reading JSON content from the selected file.
 * Uses ActivityResultContracts.OpenDocument to let the user pick a JSON file,
 * then reads its contents via a callback interface.
 */
public class OpenJsonFileHelper {

    /**
     * Callback interface for JSON file reading results.
     */
    public interface JsonPickerCallback {
        void onJsonRead(String jsonContent);
        void onError(Exception e);
    }

    private final ActivityResultLauncher<String[]> filePickerLauncher;
    private final ComponentActivity activity;

    /**
     * Constructs a new OpenJsonFileHelper and registers the file picker launcher.
     * @param activity The host Activity for registering the activity result launcher
     * @param callback Callback to receive the JSON content or errors
     */
    public OpenJsonFileHelper(ComponentActivity activity, JsonPickerCallback callback) {
        this.activity = activity;
        this.filePickerLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        try {
                            String json = readJsonFromUri(uri);
                            callback.onJsonRead(json);
                        } catch (Exception e) {
                            callback.onError(e);
                        }
                    }
                }
        );
    }

    /**
     * Launches the system file picker filtered to JSON files.
     */
    public void openPicker() {
        // MIME type for JSON
        filePickerLauncher.launch(new String[]{"application/json"});
    }

    private String readJsonFromUri(Uri uri) throws Exception {
        try (InputStream is = activity.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }
}