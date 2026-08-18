package com.ahmadrezagh671.finxel.dialogs;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.ahmadrezagh671.finxel.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Dialog for adding a new configuration, offering options to import
 * from GitHub, clipboard, or files, and to view help documentation.
 */
public class DialogAddConfig {

    public interface DialogResult {
        void fromGithub();
        void fromClipboard();
        void fromFiles();

        void help();
    }

    Activity activity;
    DialogResult dialogResult;
    AlertDialog dialog;
    View dialogLayout;

    /**
     * Constructs the dialog with the given activity.
     * @param activity The host activity
     */
    public DialogAddConfig(Activity activity) {
        this.activity = activity;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        dialogLayout = LayoutInflater.from(activity).inflate(R.layout.dialog_add_config, null);
        builder.setView(dialogLayout);

        // Initialize Views
        Button btnAddConfigClipboard = dialogLayout.findViewById(R.id.btnAddConfigClipboard);
        Button btnAddConfigFiles = dialogLayout.findViewById(R.id.btnAddConfigFiles);
        Button btnAddConfigGithub = dialogLayout.findViewById(R.id.btnAddConfigGithub);
        TextView tvHelp = dialogLayout.findViewById(R.id.tvHelp);

        btnAddConfigFiles.setOnClickListener(v -> {
            if (this.dialogResult != null) {
                this.dialogResult.fromFiles();
            }
        });

        btnAddConfigClipboard.setOnClickListener(v -> {
            if (this.dialogResult != null) {
                this.dialogResult.fromClipboard();
            }
        });

        btnAddConfigGithub.setOnClickListener(v -> {
            if (this.dialogResult != null) {
                this.dialogResult.fromGithub();
            }
        });

        tvHelp.setOnClickListener(v -> {
            if (this.dialogResult != null) {
                dialogResult.help();
            }
        });

        dialog = builder.create();
    }

    /**
     * Displays the dialog with the given result callback.
     * @param dialogResult Callback for handling user selections
     */
    public void show(DialogResult dialogResult) {
        this.dialogResult = dialogResult;
        if (dialog != null && !activity.isFinishing()) {
            dialog.show();
        }
    }

    /**
     * Dismisses the dialog if it is currently showing.
     */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}