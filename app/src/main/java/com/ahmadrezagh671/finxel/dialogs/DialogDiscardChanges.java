package com.ahmadrezagh671.finxel.dialogs;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.ahmadrezagh671.finxel.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Dialog for confirming whether to discard unsaved changes.
 */
public class DialogDiscardChanges {

    public interface DialogResult {
        void cancel();
        void discard();
    }

    Activity activity;
    DialogResult dialogResult;
    AlertDialog dialog;
    View dialogLayout;

    /**
     * Constructs the dialog with the given activity.
     * @param activity The host activity
     */
    public DialogDiscardChanges(Activity activity) {
        this.activity = activity;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        dialogLayout = LayoutInflater.from(activity).inflate(R.layout.dialog_discard_changes, null);
        builder.setView(dialogLayout);

        Button btnDiscard = dialogLayout.findViewById(R.id.btnDiscard);
        Button btnCancel = dialogLayout.findViewById(R.id.btnCancel);

        btnDiscard.setOnClickListener(v -> {
            if (this.dialogResult != null) {
                this.dialogResult.discard();
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
     * @param dialogResult Callback for cancel and discard actions
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