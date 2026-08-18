package com.ahmadrezagh671.finxel.dialogs;

import android.app.Activity;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.ahmadrezagh671.finxel.R;
import com.ahmadrezagh671.finxel.models.configModel.Cell;
import com.ahmadrezagh671.finxel.utilities.AppSettings;
import com.ahmadrezagh671.finxel.utilities.Utilities;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Dialog helper that displays a BottomSheetDialog with an input field
 * or spinner, allowing the user to edit a cell value.
 * Supports text input (ET/ET_Number) and spinner (SPINNER) types.
 */
public class BottomSheetDialogInputText {

    Activity activity;
    DialogResult dialogResult;
    BottomSheetDialog bottomSheetDialog;
    final View bottomSheetLayout;

    TextInputEditText etInput;

    TextInputLayout layoutTextInput;

    MaterialAutoCompleteTextView spinner;
    TextInputLayout layoutSpinnerInput;
    Button btnClose, btnSave;
    Cell currentCellConfig;
    String startText;

    public interface DialogResult {
        void close();
        void dismiss();
        void save(String value);
    }

    /**
     * Constructs the dialog with the given activity, initial text, cell config, and result callback.
     * @param activity The host activity
     * @param startText The initial text to display in the input
     * @param currentCellConfig The cell configuration determining input type and properties
     * @param dialogResult Callback for close, dismiss, and save events
     */
    public BottomSheetDialogInputText(Activity activity, String startText, Cell currentCellConfig, DialogResult dialogResult) {
        this.dialogResult = dialogResult;
        this.activity = activity;
        this.currentCellConfig = currentCellConfig;
        this.startText = startText;

        bottomSheetDialog = new BottomSheetDialog(activity);
        bottomSheetLayout = activity.getLayoutInflater().inflate(R.layout.bottom_sheet_dialog_input_text, null);

        btnClose = bottomSheetLayout.findViewById(R.id.btnClose);
        btnSave = bottomSheetLayout.findViewById(R.id.btnSave);
    }

    /**
     * Shows the bottom sheet dialog with the appropriate input widget
     * based on the cell configuration type.
     */
    public void show() {
        bottomSheetDialog.setContentView(bottomSheetLayout);
        bottomSheetDialog.setCanceledOnTouchOutside(true);
        bottomSheetDialog.setCancelable(true);

        btnClose.setOnClickListener(this::onBtnCloseClick);

        switch (currentCellConfig.type) {
            case "ET_Number":
            case "ET":
                etInput = bottomSheetLayout.findViewById(R.id.etInput);
                layoutTextInput = bottomSheetLayout.findViewById(R.id.layoutTextInput);
                layoutTextInput.setVisibility(View.VISIBLE);
                etInput.setText(startText);
                layoutTextInput.setHint(activity.getString(R.string.type_here_to_edit, currentCellConfig.name));
                etInput.setInputType(currentCellConfig.type.equals("ET") ?
                        InputType.TYPE_CLASS_TEXT :
                        InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                showKeyboard(etInput);
                btnSave.setOnClickListener(v -> onBtnSaveClick(etInput));
                break;
            case "SPINNER":
                spinner = bottomSheetLayout.findViewById(R.id.spinner);
                layoutSpinnerInput = bottomSheetLayout.findViewById(R.id.layoutSpinnerInput);
                layoutSpinnerInput.setVisibility(View.VISIBLE);
                spinner.setText(startText);
                layoutSpinnerInput.setHint(activity.getString(R.string.type_here_to_edit, currentCellConfig.name));
                if (currentCellConfig.items != null) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                            android.R.layout.simple_dropdown_item_1line, currentCellConfig.items);
                    spinner.setAdapter(adapter);
                }
                showKeyboard(spinner);
                btnSave.setOnClickListener(v -> onBtnSaveClick(spinner));
                break;
        }


        bottomSheetDialog.setOnShowListener(dialog -> {
            View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            View touchOutside = bottomSheetDialog.findViewById(com.google.android.material.R.id.touch_outside);

            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

                if (AppSettings.getLayoutConfirmPreventCloseStatus(activity)) {
                    bottomSheetDialog.setCanceledOnTouchOutside(false);
                    bottomSheetDialog.setCancelable(true);
                    behavior.setHideable(false);
                }

                behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheetView, int newState) {
                        if (newState == BottomSheetBehavior.STATE_DRAGGING
                                || newState == BottomSheetBehavior.STATE_HIDDEN) {
                            Utilities.hideKeyboard(bottomSheetLayout);
                        }
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheetView, float slideOffset) {}
                });
            }

            // Handle outside-tap ourselves so we control ordering
            if (touchOutside != null && !AppSettings.getLayoutConfirmPreventCloseStatus(activity)) {
                touchOutside.setOnClickListener(v -> {
                    Utilities.hideKeyboard(bottomSheetLayout);
                    bottomSheetDialog.dismiss();
                });
            }
        });


        bottomSheetDialog.setOnDismissListener(d -> {
            dialogResult.dismiss();
        });

        bottomSheetDialog.show();


    }

    private void onBtnCloseClick(View v) {
        dialogResult.close();

        Utilities.hideKeyboard(bottomSheetLayout);

        bottomSheetDialog.dismiss();
    }

    private void onBtnSaveClick(EditText targetTextHolder) {
        dialogResult.save(targetTextHolder.getText().toString());

        Utilities.hideKeyboard(bottomSheetLayout);

        bottomSheetDialog.dismiss();
    }

    public void showKeyboard(View view){
        view.requestFocus();
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 100);
    }
}
