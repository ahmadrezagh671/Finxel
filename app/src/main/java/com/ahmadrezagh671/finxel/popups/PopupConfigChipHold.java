package com.ahmadrezagh671.finxel.popups;

import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

import com.ahmadrezagh671.finxel.R;

/**
 * Shows a popup menu for configuration chip actions,
 * allowing the user to edit or delete a configuration.
 */
public class PopupConfigChipHold {

    private static final int ID_EDIT_CONFIG = 101;
    private static final int ID_DELETE_CONFIG = 102;
    private static final int ID_SHARE_CONFIG = 104;

    public interface OnConfigMenuClickListener {
        void onEditConfigSelected();
        void onDeleteConfigSelected();
        void onShareConfigSelected();
    }
    public static void show(View anchorView, OnConfigMenuClickListener listener) {
        PopupMenu popup = new PopupMenu(anchorView.getContext(), anchorView);

        popup.getMenu().add(Menu.NONE, ID_EDIT_CONFIG, 1, anchorView.getContext().getString(R.string.edit_config));
        popup.getMenu().add(Menu.NONE, ID_DELETE_CONFIG, 2, anchorView.getContext().getString(R.string.delete_config));
        popup.getMenu().add(Menu.NONE, ID_SHARE_CONFIG, 3, anchorView.getContext().getString(R.string.share_config));

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case ID_EDIT_CONFIG:
                    listener.onEditConfigSelected();
                    return true;
                case ID_DELETE_CONFIG:
                    listener.onDeleteConfigSelected();
                    return true;
                case ID_SHARE_CONFIG:
                    listener.onShareConfigSelected();
                    return true;
                default:
                    return false;
            }
        });

        popup.show();
    }

}
