package com.ahmadrezagh671.finxel.popups;

import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

import com.ahmadrezagh671.finxel.R;

/**
 * Shows a popup menu on the settings screen with actions
 * for now just exit app
 */
public class PopupSettingsFunctions {
    private static final int ID_EXIT = 101;

    public interface OnSettingsFunctionsClickListener {
        void onExitClicked();
    }

    public static void show(View anchorView, OnSettingsFunctionsClickListener listener) {
        PopupMenu popup = new PopupMenu(anchorView.getContext(), anchorView);

        popup.getMenu().add(Menu.NONE, ID_EXIT, 1, anchorView.getContext().getString(R.string.exit));

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case ID_EXIT:
                    listener.onExitClicked();
                    return true;
                default:
                    return false;
            }
        });

        popup.show();
    }
}
