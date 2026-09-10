package com.ahmadrezagh671.finxel.fragments;

import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ahmadrezagh671.finxel.R;
import com.ahmadrezagh671.finxel.activities.MainActivity;
import com.ahmadrezagh671.finxel.adapters.RVConfigAdapter;
import com.ahmadrezagh671.finxel.models.configModel.ConfigModel;
import com.ahmadrezagh671.finxel.popups.PopupConfigChipHold;
import com.ahmadrezagh671.finxel.popups.PopupHomeFunctions;
import com.ahmadrezagh671.finxel.popups.PopupSettingsFunctions;
import com.ahmadrezagh671.finxel.utilities.AppSettings;
import com.ahmadrezagh671.finxel.utilities.NotificationReplyManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Map;

/**
 * Displays app settings including configuration management,
 * feature toggles for location and notifications, and app version info.
 */
public class FragmentSettings extends Fragment {

    private static final String TAG = "FragmentSettings";
    private MainActivity mainActivity;

    ImageButton ibMenu;
    TextView tvConfigCount;
    TextView tvConfigSubtitle;
    TextView tvConfigEmpty;
    RecyclerView rvConfigList;
    Button btnRefreshConfigs;
    Button btnAddConfig;
    Button btnResetSettings;
    TextView tvAppVersion,tvAboutUs;

    private SwitchMaterial swLayoutConfirmPreventClose,swLocationService,swNotificationInput;

    private final ActivityResultLauncher<String[]> corePermissionsForLocationLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean smsGranted = result.getOrDefault(Manifest.permission.RECEIVE_SMS, false);
                Boolean locationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);

                if (smsGranted && locationGranted) {
                    checkAndRequestBackgroundLocation();
                } else {
                    AppSettings.updateLocationServiceStatus(requireContext(),false);
                    swLocationService.setChecked(false);
                }
            });

    private final ActivityResultLauncher<String> backgroundPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    AppSettings.updateLocationServiceStatus(requireContext(),true);
                } else {
                    AppSettings.updateLocationServiceStatus(requireContext(),false);
                    swLocationService.setChecked(false);
                }
            });

    private final ActivityResultLauncher<String[]> corePermissionsForNotificationLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean smsGranted = result.getOrDefault(Manifest.permission.RECEIVE_SMS, false);
                Boolean notificationGranted = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationGranted = result.getOrDefault(Manifest.permission.POST_NOTIFICATIONS, false);
                }

                if (smsGranted && notificationGranted) {
                    AppSettings.updateNotificationEntryStatus(requireContext(),true);
                } else {
                    AppSettings.updateNotificationEntryStatus(requireContext(),false);
                    swNotificationInput.setChecked(false);
                }
            });

    public FragmentSettings() {
        // Required empty public constructor
    }

    public static FragmentSettings newInstance() {
        FragmentSettings fragment = new FragmentSettings();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = (MainActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        tvConfigCount = view.findViewById(R.id.tvConfigCount);
        tvConfigSubtitle = view.findViewById(R.id.tvConfigSubtitle);
        tvConfigEmpty = view.findViewById(R.id.tvConfigEmpty);
        rvConfigList = view.findViewById(R.id.rvConfigList);
        btnRefreshConfigs = view.findViewById(R.id.btnRefreshConfigs);
        btnAddConfig = view.findViewById(R.id.btnAddConfig);
        btnResetSettings = view.findViewById(R.id.btnResetSettings);
        tvAppVersion = view.findViewById(R.id.tvAppVersion);
        swLayoutConfirmPreventClose = view.findViewById(R.id.swLayoutConfirmPreventClose);
        swLocationService = view.findViewById(R.id.swLocationService);
        swNotificationInput = view.findViewById(R.id.swNotificationInput);
        ibMenu = view.findViewById(R.id.ibMenu);

        tvAboutUs = view.findViewById(R.id.tvAboutUs);
        tvAboutUs.setText(HtmlCompat.fromHtml(
                getString(R.string.about_us),
                HtmlCompat.FROM_HTML_MODE_LEGACY
        ));
        tvAboutUs.setMovementMethod(LinkMovementMethod.getInstance());

        swLayoutConfirmPreventClose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                AppSettings.updateLayoutConfirmPreventCloseStatus(requireContext(),isChecked);
            }
        });

        swLocationService.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    openXiaomiSettingsIfNecessary();
                    checkBatteryOptimization();
                    requestCorePermissionsForLocation();
                    AppSettings.updateLocationServiceConfig(requireContext(),mainActivity.getConfigs());
                } else {
                    AppSettings.updateLocationServiceStatus(requireContext(),false);
                }
            }
        });

        swNotificationInput.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    openXiaomiSettingsIfNecessary();
                    checkBatteryOptimization();
                    requestCorePermissionsForNotification();
                    NotificationReplyManager.createChannel(requireContext());
                    AppSettings.updateNotificationEntryConfig(requireContext(),mainActivity.getConfigs());
                }else {
                    AppSettings.updateNotificationEntryStatus(requireContext(),false);
                }
            }
        });

        ibMenu.setOnClickListener(this::menuClick);

        return view;
    }

    private void menuClick(View view) {
        PopupSettingsFunctions.show(view, new PopupSettingsFunctions.OnSettingsFunctionsClickListener() {
            @Override
            public void onExitClicked() {
                mainActivity.finish();
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadSavedSettings();
        bindSettingsListeners();
        loadConfigSummary();
        loadAppVersion();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden && tvConfigCount != null) {
            loadConfigSummary();
        }
    }

    /**
     * Requests the core permissions needed for location services,
     * including background location on Android 10+.
     */
    private void requestCorePermissionsForLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            corePermissionsForLocationLauncher.launch(new String[]{
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            });
        }else {
            corePermissionsForLocationLauncher.launch(new String[]{
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
        }
    }

    /**
     * Requests the core permissions needed for notification entry functionality.
     */
    private void requestCorePermissionsForNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            corePermissionsForNotificationLauncher.launch(new String[]{
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.POST_NOTIFICATIONS
            });
        }else {
            corePermissionsForNotificationLauncher.launch(new String[]{
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CONTACTS
            });
        }
    }

    /**
     * Checks if battery optimization is enabled and prompts the user
     * to disable it if necessary for reliable background operation.
     */
    private void checkBatteryOptimization(){
        PowerManager powerManager = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
        String packageName = requireContext().getPackageName();

        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.disable_battery_optimization))
                    .setMessage(getString(R.string.battery_optimization_message))
                    .setPositiveButton(getString(R.string.go_to_settings), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                Intent batteryIntent = new Intent();
                                batteryIntent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                                startActivity(batteryIntent);
                            } catch (Exception e) {
                                Log.e(TAG, "onClick checkBatteryOptimization BatteryFix: " + e.getMessage());
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }
    }
    /**
     * Checks and requests background location permission on Android 10+,
     * prompting the user to allow "Allow all the time" if needed.
     */
    private void checkAndRequestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.allow_background_location))
                        .setMessage(getString(R.string.background_location_message))
                        .setPositiveButton(getString(R.string.go_to_settings), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                            }
                        })
                        .setNegativeButton(getString(R.string.not_now), null)
                        .show();

            } else {
                AppSettings.updateLocationServiceStatus(requireContext(),true);
            }
        } else {
            // Devices below Android 10 automatically get background access with fine location
            AppSettings.updateLocationServiceStatus(requireContext(),true);
        }
    }

    /**
     * Opens Xiaomi autostart settings if the device is a Xiaomi,
     * since Xiaomi restricts apps from running in the background.
     */
    private void openXiaomiSettingsIfNecessary() {
        String manufacturer = android.os.Build.MANUFACTURER;
        if ("xiaomi".equalsIgnoreCase(manufacturer)) {

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.enable_autostart))
                    .setMessage(getString(R.string.xiaomi_autostart_message))
                    .setPositiveButton(getString(R.string.go_to_settings), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                Intent autostartIntent = new Intent();
                                autostartIntent.setComponent(new ComponentName(
                                        "com.miui.securitycenter",
                                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                                ));
                                startActivity(autostartIntent);
                            } catch (Exception e) {
                                // Fallback if Xiaomi changes the component path in a future OS update
                                Log.e(TAG, "onClick openXiaomiSettingsIfNecessary XiaomiFix: " + e.getMessage());
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.not_now), null)
                    .show();
        }
    }



    /**
     * Loads the current settings values from AppSettings into the UI toggles.
     */
    private void loadSavedSettings() {
        swLayoutConfirmPreventClose.setChecked(AppSettings.getLayoutConfirmPreventCloseStatus(requireContext()));
        swLocationService.setChecked(AppSettings.getLocationServiceStatus(requireContext()));
        swNotificationInput.setChecked(AppSettings.getNotificationEntryStatus(requireContext()));
    }

    /**
     * Resets all settings to their defaults and reloads the UI.
     */
    private void resetSavedSettings() {
        AppSettings.resetSettings(requireContext());

        loadSavedSettings();

        Toast.makeText(getContext(), getString(R.string.settings_reset), Toast.LENGTH_SHORT).show();
    }

    /**
     * Binds click listeners to the settings buttons.
     */
    private void bindSettingsListeners() {
        btnRefreshConfigs.setOnClickListener(v -> refreshConfigs());

        btnAddConfig.setOnClickListener(v -> addConfig());

        btnResetSettings.setOnClickListener(v -> resetSavedSettings());
    }

    /**
     * Refreshes the configuration list and shows a confirmation toast.
     */
    public void refreshConfigs() {
        if (getContext() != null){
            loadConfigSummary();
            if (!this.isHidden()){
                Toast.makeText(getContext(), getString(R.string.configs_refreshed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Opens the import configuration dialog to add a new config.
     */
    private void addConfig() {
        mainActivity.showImportConfigDialog();
    }

    /**
     * Loads and displays the configuration summary including count and adapter.
     */
    private void loadConfigSummary() {
        if (getContext() == null || mainActivity == null) {
            return;
        }

        Map<String, ConfigModel> configs = mainActivity.getConfigs();
        int configCount = configs == null ? 0 : configs.size();

        if (configCount == 1) {
            tvConfigCount.setText(getString(R.string.configs_count_one));
        } else {
            tvConfigCount.setText(getString(R.string.configs_count_many, configCount));
        }
        tvConfigSubtitle.setText(configCount == 0
                ? getString(R.string.add_config_from_clipboard)
                : getString(R.string.saved_configs_storage));
        tvConfigEmpty.setVisibility(configCount == 0 ? VISIBLE : View.GONE);

        if (configCount > 0){
            rvConfigList.setLayoutManager(new LinearLayoutManager(getContext()));
            rvConfigList.setAdapter(new RVConfigAdapter(configs, new RVConfigAdapter.OnItemClick() {
                @Override
                public void itemClicked(View view, String key, ConfigModel config, int position) {
                    PopupConfigChipHold.show(view, new PopupConfigChipHold.OnConfigMenuClickListener() {
                        @Override
                        public void onEditConfigSelected() {
                            mainActivity.editConfig(key);
                        }

                        @Override
                        public void onDeleteConfigSelected() {
                            mainActivity.deleteConfig(key);
                        }

                        @Override
                        public void onShareConfigSelected() {
                            mainActivity.shareConfig(key);
                        }
                    });
                }
            }));
        } else {
            rvConfigList.setAdapter(null);
        }
    }

    /**
     * Loads the app version name and displays it in the version text view.
     */
    private void loadAppVersion() {
        try {
            PackageInfo packageInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            tvAppVersion.setText(getString(R.string.app_version, packageInfo.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            tvAppVersion.setText(getString(R.string.app_version_error));
        }
    }

}