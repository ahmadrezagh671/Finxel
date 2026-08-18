package com.ahmadrezagh671.finxel.popups;

import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

import com.ahmadrezagh671.finxel.R;

/**
 * Shows a popup menu for sheet actions such as copying,
 * clearing, or viewing the history of a sheet.
 */
public class PopupSheetFunctions {
    private static final int ID_COPY_SHEET = 101;
    private static final int ID_CLEAR_SHEET = 102;
    private static final int ID_CHECK_HISTORY = 103;

    public interface OnSheetFunctionsClickListener {
        void onCopySheetSelected();
        void onClearSheetSelected();
        void onSheetHistorySelected();
    }
    public static void show(View anchorView,String sheetName, OnSheetFunctionsClickListener listener) {
        PopupMenu popup = new PopupMenu(anchorView.getContext(), anchorView);

        popup.getMenu().add(Menu.NONE, ID_COPY_SHEET, 1, anchorView.getContext().getString(R.string.copy_sheet, sheetName));
        popup.getMenu().add(Menu.NONE, ID_CLEAR_SHEET, 2, anchorView.getContext().getString(R.string.clear_sheet, sheetName));
        popup.getMenu().add(Menu.NONE, ID_CHECK_HISTORY, 3, anchorView.getContext().getString(R.string.history));

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case ID_COPY_SHEET:
                    listener.onCopySheetSelected();
                    return true;
                case ID_CLEAR_SHEET:
                    listener.onClearSheetSelected();
                    return true;
                case ID_CHECK_HISTORY:
                    listener.onSheetHistorySelected();
                    return true;
                default:
                    return false;
            }
        });

        popup.show();
    }
}
