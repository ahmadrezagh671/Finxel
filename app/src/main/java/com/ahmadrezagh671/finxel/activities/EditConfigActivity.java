package com.ahmadrezagh671.finxel.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ahmadrezagh671.finxel.R;
import com.ahmadrezagh671.finxel.dialogs.DialogDiscardChanges;
import com.ahmadrezagh671.finxel.models.configModel.ConfigModel;
import com.ahmadrezagh671.finxel.utilities.ConfigManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import org.eclipse.tm4e.core.registry.IThemeSource;
import org.json.JSONException;

import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver;
import io.github.rosemoe.sora.widget.CodeEditor;

/**
 * Activity for editing JSON configuration files using a code editor with syntax highlighting.
 */
public class EditConfigActivity extends AppCompatActivity {

    public static final String EXTRA_CONFIG_KEY = "config_key";

    private MaterialToolbar toolbar;
    private CodeEditor editor;
    private MaterialButton btnSave,btnCancel;
    private String configKey;
    private String originalContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_config);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        configKey = getIntent().getStringExtra(EXTRA_CONFIG_KEY);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        editor = findViewById(R.id.editor);
        editor.setTypefaceText(Typeface.MONOSPACE); // Use Monospace Typeface

        setupJsonLanguage();

        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        loadConfigContent();

        btnSave.setOnClickListener(v -> saveConfig());
        btnCancel.setOnClickListener(v -> onBackPressed());
    }

    /**
     * Loads the config content for the given key into the editor, or finishes if the key is invalid.
     */
    private void loadConfigContent() {
        if (configKey == null) {
            Toast.makeText(this, R.string.config_key_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String jsonContent = ConfigManager.loadConfig(this, configKey);
        if (jsonContent != null) {
            originalContent = jsonContent;
            editor.setText(jsonContent);
        } else {
            Toast.makeText(this, R.string.failed_to_load_config, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Validates and saves the current editor content as a JSON configuration file.
     */
    private void saveConfig() {
        String jsonContent = editor.getText() != null ? editor.getText().toString() : "";

        if (jsonContent.trim().isEmpty()) {
            Toast.makeText(this, R.string.config_content_is_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ConfigManager.isValidConfig(jsonContent)) {
            Toast.makeText(this, R.string.invalid_config_format, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            ConfigModel configModel = ConfigManager.parseJsonToModel(this,jsonContent);

            if (!configKey.equals(configModel.information.name)){
                Toast.makeText(this,R.string.config_names_cannot_be_changed, Toast.LENGTH_SHORT).show();
                return;
            }

            boolean saved = ConfigManager.saveConfig(this,configModel.information.name + ".json", jsonContent);
            if (!saved){
                Toast.makeText(this,R.string.failed_to_save_config, Toast.LENGTH_SHORT).show();
                return;
            }

            setResult(RESULT_OK);
            finish();
            Toast.makeText(this, R.string.config_saved_successfully, Toast.LENGTH_SHORT).show();

        } catch (JSONException e) {
            Toast.makeText(this,getString(R.string.error_parsing_json, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows a discard confirmation dialog if the user has unsaved changes.
     */
    @Override
    public void onBackPressed() {
        String currentContent = editor.getText() != null ? editor.getText().toString() : "";
        if (originalContent == null || originalContent.equals(currentContent)) {
            super.onBackPressed();
            return;
        }

        DialogDiscardChanges dialogDiscardChanges = new DialogDiscardChanges(this);
        dialogDiscardChanges.show(new DialogDiscardChanges.DialogResult() {
            @Override
            public void cancel() {
                dialogDiscardChanges.dismiss();
            }

            @Override
            public void discard() {
                setResult(RESULT_CANCELED);
                dialogDiscardChanges.dismiss();
                finish();
            }
        });
    }

    /**
     * Sets up the TextMate JSON language with a dark theme for the code editor.
     */
    private void setupJsonLanguage() {
        try {
            // 1. Tell TextMate where to find files (using your app's assets)
            FileProviderRegistry.getInstance().addFileProvider(
                    new AssetsFileResolver(getApplicationContext().getAssets())
            );

            // 2. Load the Dark Theme
            ThemeRegistry themeRegistry = ThemeRegistry.getInstance();
            String themePath = "textmate/dark_theme.json";

            ThemeModel themeModel = new ThemeModel(
                    IThemeSource.fromInputStream(
                            FileProviderRegistry.getInstance().tryGetInputStream(themePath), themePath, null
                    ), "myDarkTheme"
            );

            themeModel.setDark(true); // Flag the theme as dark mode
            themeRegistry.loadTheme(themeModel);
            themeRegistry.setTheme("myDarkTheme");

            // 3. Load the languages.json registry
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json");

            // 4. Apply the Theme and Language to the Editor
            editor.setColorScheme(TextMateColorScheme.create(themeRegistry));

            // "source.json" must exactly match the scopeName in your languages.json
            TextMateLanguage jsonLanguage = TextMateLanguage.create("source.json", true);
            editor.setEditorLanguage(jsonLanguage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}