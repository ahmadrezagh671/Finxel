package com.ahmadrezagh671.finxel.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.ahmadrezagh671.finxel.R;
import com.ahmadrezagh671.finxel.db.AppDatabase;
import com.ahmadrezagh671.finxel.dialogs.DialogAddConfig;
import com.ahmadrezagh671.finxel.dialogs.DialogDeleteConfig;
import com.ahmadrezagh671.finxel.fragments.FragmentHome;
import com.ahmadrezagh671.finxel.fragments.FragmentSettings;
import com.ahmadrezagh671.finxel.fragments.FragmentSheet;
import com.ahmadrezagh671.finxel.models.configModel.ConfigModel;
import com.ahmadrezagh671.finxel.utilities.AppSettings;
import com.ahmadrezagh671.finxel.utilities.ConfigManager;
import com.ahmadrezagh671.finxel.utilities.OpenJsonFileHelper;
import com.ahmadrezagh671.finxel.utilities.Utilities;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import org.json.JSONException;

import java.io.File;
import java.util.Map;

/**
 * Main activity of the app that hosts the bottom navigation, manages fragment
 * switching, and handles config import, edit, and delete operations.
 */
public class MainActivity extends AppCompatActivity {

    FragmentHome fragmentHome;
    FragmentSettings fragmentSettings;
    public FragmentSheet fragmentSheet;

    public BottomNavigationView buttonNavView;
    FragmentManager fragmentManager;
    Fragment lastFragment;
    FrameLayout frameLayout;

    Map<String, ConfigModel> configs;
    public Map<String, ConfigModel> getConfigs() {
        return configs;
    }

    public AppDatabase db;
    DialogAddConfig dialogAddConfig;

    OpenJsonFileHelper openJsonFileHelper;

    /**
     * Called when the activity is being destroyed. Stops all fragments.
     */
    @Override
    protected void onDestroy() {
        fragmentHome.onDestroy();
        fragmentSettings.onDestroy();
        fragmentSheet.onDestroy();
        super.onDestroy();
    }

    /**
     * Handles permission request results for SMS and contact permissions.
     *
     * @param requestCode  the request code passed to requestPermissions
     * @param permissions  the requested permissions
     * @param grantResults the grant results for the permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            buttonNavView.setSelectedItemId(R.id.menuHome);
        }else {
            buttonNavView.setSelectedItemId(R.id.menuSettings);
        }
    }

    /**
     * Initializes the main activity UI, bottom navigation, fragment setup,
     * permission requests, and JSON file picker callback.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        configs = ConfigManager.loadAllConfigs(this);
        db = AppDatabase.getDatabase(this);

        buttonNavView = findViewById(R.id.buttonNavView);
        frameLayout = findViewById(R.id.frameLayout);

        // fix bottom padding bug
        buttonNavView.setOnApplyWindowInsetsListener(null);
        buttonNavView.setPadding(0,0,0,0);

        fragmentManager = getSupportFragmentManager();
        replaceOrCreateFragments();

        buttonNavView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.menuHome){
                    if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(MainActivity.this, R.string.read_sms_permission_not_granted, Toast.LENGTH_SHORT).show();
                    } else {
                        lastFragment = openFragment(fragmentManager,R.id.frameLayout,fragmentHome,lastFragment);
                    }
                } else if (id == R.id.menuSheet) {
                    lastFragment = openFragment(fragmentManager,R.id.frameLayout,fragmentSheet,lastFragment);
                } else if (id == R.id.menuSettings) {
                    lastFragment = openFragment(fragmentManager,R.id.frameLayout,fragmentSettings,lastFragment);
                }
                return true;
            }
        });


        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_SMS,Manifest.permission.RECEIVE_SMS,Manifest.permission.READ_CONTACTS}, 100);
        } else {
            buttonNavView.setSelectedItemId(R.id.menuHome);
        }

        openJsonFileHelper = new OpenJsonFileHelper(this, new OpenJsonFileHelper.JsonPickerCallback() {
            @Override
            public void onJsonRead(String jsonContent) {
                ConfigModel newConfig = insertConfigFromString(jsonContent);
                if (newConfig != null){
                    configs.put(newConfig.information.name,newConfig);
                    refreshServiceConfigs();
                    refreshConfigsFragments();
                    if (dialogAddConfig != null){
                        dialogAddConfig.dismiss();
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, "Error Reading File: " + e.getMessage().substring(0,100) + " ...", Toast.LENGTH_LONG).show();
            }
        });

    }



    /**
     * Restores existing fragments or creates new ones, hiding all current fragments first.
     */
    private void replaceOrCreateFragments() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            getSupportFragmentManager().beginTransaction().hide(fragment).commit();
            fragment.onDestroy();

            // for when light/dark mode changes and activity opens again
            switch (fragment.getClass().getSimpleName()){
                case "FragmentHome":
                    fragmentHome = (FragmentHome) fragment;
                    break;
                case "FragmentSettings":
                    fragmentSettings = (FragmentSettings) fragment;
                    break;
                case "FragmentSheet":
                    fragmentSheet = (FragmentSheet) fragment;
                    break;
            }
        }
        if (fragmentHome == null)
            fragmentHome = new FragmentHome();

        if (fragmentSettings == null)
            fragmentSettings = new FragmentSettings();

        if (fragmentSheet == null)
            fragmentSheet = new FragmentSheet();
    }


    /**
     * Opens a fragment in the given container, hiding the previous fragment.
     *
     * @param fragmentManager the FragmentManager to use for the transaction
     * @param fragmentContainerView_id the container view ID where the fragment should be placed
     * @param newfragment the fragment to open
     * @param lastFragment the previously shown fragment to hide
     * @return the newly opened fragment
     */
    public static Fragment openFragment(FragmentManager fragmentManager, int fragmentContainerView_id, Fragment newfragment, Fragment lastFragment){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (lastFragment != null)
            fragmentTransaction.hide(lastFragment);

        if (fragmentManager.getFragments().contains(newfragment))
            fragmentTransaction.show(newfragment);
        else
            fragmentTransaction.add(fragmentContainerView_id,newfragment);

        lastFragment = newfragment;
        fragmentTransaction.commit();

        return lastFragment;
    }

    /**
     * Refreshes all config-related fragments by reloading their data.
     */
    public void refreshConfigsFragments(){
        fragmentSheet.reload();
        fragmentHome.start();
        fragmentSettings.refreshConfigs();
    }

    /**
     * Shows the dialog for importing a new configuration.
     */
    public void showImportConfigDialog() {
        dialogAddConfig = new DialogAddConfig(this);
        dialogAddConfig.show(new DialogAddConfig.DialogResult() {
            @Override
            public void fromGithub() {
                Utilities.openUrl(MainActivity.this,"https://github.com/ahmadrezagh671/Finxel/tree/main/community_configs");
            }

            @Override
            public void fromClipboard() {
                ConfigModel newConfig = insertConfigFromClipboard();
                if (newConfig != null){
                    configs.put(newConfig.information.name,newConfig);
                    refreshServiceConfigs();
                    refreshConfigsFragments();
                    dialogAddConfig.dismiss();
                }
            }

            @Override
            public void fromFiles() {
                insertConfigFromFiles();
            }

            @Override
            public void help() {
                Utilities.openUrl(MainActivity.this,"https://github.com/ahmadrezagh671/Finxel/blob/main/docs/write-configuration.md");
            }
        });
    }

    /**
     * Opens the JSON file picker to select a config file for import.
     */
    private void insertConfigFromFiles() {
        openJsonFileHelper.openPicker();
    }


    private ConfigModel insertConfigFromClipboard(){
        String value = Utilities.getClipboardText(this);
        return insertConfigFromString(value);
    }

    /**
     * Parses and saves a configuration from a JSON string value.
     *
     * @param value the raw JSON string to parse and save
     * @return the parsed ConfigModel if successful, null otherwise
     */
    private ConfigModel insertConfigFromString(String value){
        if (value.trim().isEmpty()){
            Toast.makeText(this, R.string.no_config_found, Toast.LENGTH_SHORT).show();
            return null;
        }

        if (!ConfigManager.isValidConfig(value)) {
            Toast.makeText(this, R.string.invalid_config_format, Toast.LENGTH_SHORT).show();
            return null;
        }

        try {
            ConfigModel configModel = ConfigManager.parseJsonToModel(this,value);

            if (configs.containsKey(configModel.information.name)){
                Toast.makeText(this,R.string.config_name_already_exists, Toast.LENGTH_SHORT).show();
                return null;
            }

            boolean saved = ConfigManager.saveConfig(this,configModel.information.name + ".json", value);
            if (!saved){
                Toast.makeText(this,R.string.failed_to_save_config, Toast.LENGTH_SHORT).show();
                return null;
            }

            Toast.makeText(this, R.string.saved_successfully, Toast.LENGTH_SHORT).show();

            return configModel;
        } catch (JSONException e) {
            Toast.makeText(this,getString(R.string.error_parsing_json, e.getMessage()), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    ActivityResultLauncher<Intent> editConfigLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    configs = ConfigManager.loadAllConfigs(this);
                    refreshServiceConfigs();
                    refreshConfigsFragments();
                }
            }
    );

    /**
     * Shares a configuration with the given key.
     *
     * @param key the configuration key to share
     */
    public void shareConfig(String key) {
        File configFile = ConfigManager.getConfigFile(this,key);

        if (configFile == null)
            return;

        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                configFile
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/json");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, getString(R.string.share_json_config_file)));
    }

    /**
     * Launches the edit config activity for the given configuration key.
     *
     * @param key the configuration key to edit
     */
    public void editConfig(String key) {
        Intent intent = new Intent(this, EditConfigActivity.class);
        intent.putExtra(EditConfigActivity.EXTRA_CONFIG_KEY, key);
        editConfigLauncher.launch(intent);
    }

    /**
     * Shows a confirmation dialog to delete a configuration with the given key.
     *
     * @param key the configuration key to delete
     */
    public void deleteConfig(String key) {
        DialogDeleteConfig dialogDeleteConfig = new DialogDeleteConfig(this);
        dialogDeleteConfig.show(new DialogDeleteConfig.DialogResult() {
            @Override
            public void cancel() {
                dialogDeleteConfig.dismiss();
            }

            @Override
            public void delete() {
                boolean deleted = ConfigManager.deleteConfig(MainActivity.this, key);
                if (deleted) {
                    configs.remove(key);
                    refreshConfigsFragments();
                    Toast.makeText(MainActivity.this, R.string.config_deleted, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, R.string.failed_to_delete_config, Toast.LENGTH_SHORT).show();
                }
                dialogDeleteConfig.dismiss();
            }
        });
    }

    private void refreshServiceConfigs() {
        if (AppSettings.getLocationServiceStatus(this)){
            AppSettings.updateLocationServiceConfig(this,getConfigs());
        }
        if (AppSettings.getNotificationEntryStatus(this)){
            AppSettings.updateNotificationEntryConfig(this,getConfigs());
        }
    }


}