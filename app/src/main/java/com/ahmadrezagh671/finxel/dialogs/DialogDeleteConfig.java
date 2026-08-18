package com.ahmadrezagh671.finxel.dialogs;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.ahmadrezagh671.finxel.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Dialog for confirming the deletion of a configuration.
 */
public class DialogDeleteConfig {

    public interface DialogResult {
        void cancel();
        void delete();
    }
    Activity activity;
    DialogResult dialogResult;
    AlertDialog dialog;
    View dialogLayout;

    /**
     * Constructs the dialog with the given activity.
     * @param activity The host activity
     */
    public DialogDeleteConfig(Activity activity) {
        this.activity = activity;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        dialogLayout = LayoutInflater.from(activity).inflate(R.layout.dialog_delete_config, null);
        builder.setView(dialogLayout);

        // Initialize Views
        Button btnDelete = dialogLayout.findViewById(R.id.btnDelete);
        Button btnCancel = dialogLayout.findViewById(R.id.btnCancel);


        btnDelete.setOnClickListener(v -> {
            if (this.dialogResult != null) {
                this.dialogResult.delete();
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (this.dialogResult != null) {
                this.dialogResult.cancel();
            }
        });

        dialog = builder.create();
    }

    /**
     * Displays the dialog with the given result callback.
     * @param dialogResult Callback for cancel and delete actions
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
