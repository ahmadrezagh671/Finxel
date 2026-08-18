package com.ahmadrezagh671.finxel.dialogs;

import static com.ahmadrezagh671.finxel.utilities.Utilities.dpToPx;

import android.app.Activity;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ahmadrezagh671.finxel.R;
import com.ahmadrezagh671.finxel.models.configModel.LayoutComponent;
import com.ahmadrezagh671.finxel.utilities.AppSettings;
import com.ahmadrezagh671.finxel.utilities.Utilities;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog helper that displays a BottomSheetDialog with a form
 * built from LayoutComponent definitions, allowing the user to
 * confirm and submit values for each field.
 */
public class BottomSheetDialogItemConfirmer {

    Activity activity;
    DialogResult dialogResult;
    Map<String, String> fieldsResult;
    List<LayoutComponent> layouts;

    LinearLayout mainContainer;
    BottomSheetDialog bottomSheetDialog;

    Map<String,View> viewsInLayout = new HashMap<>();
    Map<String,String> submittedText = new HashMap<>();

    final View bottomSheetLayout;

    Button BTNClose,BTNSubmit;

    public interface DialogResult {
        void close();
        void dismiss();
        void submit(Map<String, String> submittedText);
    }

    /**
     * Constructs the dialog with the given activity, field results,
     * layout definitions, and result callback.
     * @param activity The host activity
     * @param fieldsResult Current values for each field key
     * @param layouts The layout component definitions to build the UI from
     * @param dialogResult Callback for close, dismiss, and submit events
     */
    public BottomSheetDialogItemConfirmer(Activity activity, Map<String, String> fieldsResult, List<LayoutComponent> layouts,DialogResult dialogResult) {
        this.activity = activity;
        this.dialogResult = dialogResult;
        this.fieldsResult = fieldsResult;
        this.layouts = layouts;

        bottomSheetDialog = new BottomSheetDialog(activity);
        bottomSheetLayout = activity.getLayoutInflater().inflate(R.layout.bottom_sheet_dialog_item_confirmer, null);
        mainContainer = bottomSheetLayout.findViewById(R.id.main_container);
        BTNClose = bottomSheetLayout.findViewById(R.id.BTNClose);
        BTNSubmit = bottomSheetLayout.findViewById(R.id.BTNSubmit);

        BTNSubmit.setOnClickListener(this::onBtnSubmitClick);
        BTNClose.setOnClickListener(this::onBtnCloseClick);
    }

    /**
     * Builds and shows the bottom sheet dialog with the form UI.
     */
    public void show() {
        buildUi();

        bottomSheetDialog.setContentView(bottomSheetLayout);

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

    private void onBtnSubmitClick(View v) {
        submittedText = getValuesFromLayout();

        dialogResult.submit(submittedText);

        Utilities.hideKeyboard(bottomSheetLayout);

        bottomSheetDialog.dismiss();
    }

    /**
     * Collects values from all tracked views in the layout and returns them as a map.
     * @return A map of field IDs to their current text values
     */
    private Map<String, String> getValuesFromLayout() {
        Map<String, String> result = new HashMap<>();
        viewsInLayout.forEach((key, view) -> {
            if (view instanceof EditText) {
                EditText editText = (EditText) view;
                result.put(key,editText.getText().toString());
            } else if (view instanceof TextView) { // also works for MaterialAutoCompleteTextView
                TextView textView = (TextView) view;
                result.put(key,textView.getText().toString());
            } else if (view instanceof LinearLayout) {
                result.put(key,"LinearLayout");
            }
        });
        return result;
    }

    /**
     * Builds the UI by inflating layout components into the main container.
     */
    private void buildUi() {
        LinearLayout.LayoutParams standardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        // Use dpToPx for consistent sizing across different screen densities
        int verticalMargin = dpToPx(activity, 6);
        int horizontalMargin = dpToPx(activity, 12);

        standardParams.setMarginStart(horizontalMargin);
        standardParams.setMarginEnd(horizontalMargin);
        standardParams.topMargin = verticalMargin;
        standardParams.bottomMargin = verticalMargin;

        for (LayoutComponent layout : layouts) {
            View newComponent = getComponent(layout);
            if (newComponent != null) {
                mainContainer.addView(newComponent, standardParams);
            }
        }
    }

    /**
     * Creates a View for a given LayoutComponent based on its type.
     * @param layout The layout component definition
     * @return The constructed View, or null if the type is unsupported
     */
    private View getComponent(LayoutComponent layout) {
        View component = null;

        float itemsRadius = dpToPx(activity, 20);

        switch (layout.type) {
            case "TV":
                MaterialTextView textView = new MaterialTextView(activity);
                textView.setTag(layout.id);
                textView.setText(fieldsResult.get(layout.value));
                // Apply a modern Material typography style programmatically
                textView.setTextAppearance(activity, com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);

                viewsInLayout.put(layout.id, textView);
                component = textView;
                break;

            case "ET_Number":
            case "ET":
                // 1. Create the container (handles the floating hint and border)
                TextInputLayout textInputLayout = new TextInputLayout(activity);
                textInputLayout.setTag(layout.id + "_layout");
                textInputLayout.setHint(layout.hint);

                textInputLayout.setBoxCornerRadii(itemsRadius,itemsRadius,itemsRadius,itemsRadius);

                // 2. Create the actual input field
                TextInputEditText editText = new TextInputEditText(textInputLayout.getContext());
                editText.setTag(layout.id);
                editText.setInputType(layout.type.equals("ET") ?
                        InputType.TYPE_CLASS_TEXT :
                        InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);

                if (layout.value != null && !layout.value.isEmpty()) {
                    editText.setText(fieldsResult.get(layout.value));
                }

                // Add the EditText to the layout
                textInputLayout.addView(editText);

                viewsInLayout.put(layout.id, editText); // Keep tracking the actual input
                component = textInputLayout; // Return the wrapper to be added to the UI
                break;

            case "SPINNER":
                // Modern Spinners are "Exposed Dropdown Menus" in Material Design
                TextInputLayout dropdownLayout = new TextInputLayout(activity, null,
                        com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle);
                dropdownLayout.setHint(layout.hint != null ? layout.hint : activity.getString(R.string.select_an_option));

                dropdownLayout.setBoxCornerRadii(itemsRadius,itemsRadius,itemsRadius,itemsRadius);

                MaterialAutoCompleteTextView autoComplete = new MaterialAutoCompleteTextView(dropdownLayout.getContext());
                autoComplete.setTag(layout.id);

                if (layout.items != null) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                            android.R.layout.simple_dropdown_item_1line, layout.items);
                    autoComplete.setAdapter(adapter);

                    // Set initial selection safely
                    if (layout.selected >= 0 && layout.selected < layout.items.size()) {
                        autoComplete.setText(layout.items.get(layout.selected), false);
                    }else {
                        autoComplete.setText("");
                    }
                }

                dropdownLayout.addView(autoComplete);

                viewsInLayout.put(layout.id, autoComplete);
                component = dropdownLayout;
                break;

            case "H_LinearLayout":
                LinearLayout horizontalLayout = new LinearLayout(activity);
                horizontalLayout.setTag(layout.id);
                horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);

                if (layout.inside != null) {
                    LinearLayout.LayoutParams childParams = new LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                    );
                    int margin = dpToPx(activity, 12);
                    childParams.setMarginStart(0);
                    childParams.setMarginEnd(margin);

                    for (int i = 0; i < layout.inside.size(); i++) {
                        LayoutComponent insideLayout = layout.inside.get(i);
                        View newComponent = getComponent(insideLayout);
                        if (newComponent != null) {
                            // if last item remove margin end
                            if (i == (layout.inside.size() - 1)){
                                childParams = new LinearLayout.LayoutParams(
                                        0,
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        1f
                                );
                            }
                            horizontalLayout.addView(newComponent, childParams);
                        }
                    }
                }
                viewsInLayout.put(layout.id, horizontalLayout);
                component = horizontalLayout;
                break;
        }
        return component;
    }

}