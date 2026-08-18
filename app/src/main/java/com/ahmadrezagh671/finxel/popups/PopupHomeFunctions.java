package com.ahmadrezagh671.finxel.popups;

import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

import com.ahmadrezagh671.finxel.R;

/**
 * Shows a popup menu on the home screen with actions
 * such as opening settings, moving to top, finding last unchecked, or exiting.
 */
public class PopupHomeFunctions {

    private static final int ID_SETTINGS = 101;
    private static final int ID_MOVE_TOP = 102;
    private static final int ID_LAST_UNCHECKED = 103;
    private static final int ID_EXIT = 104;

    public interface OnHomeFunctionsClickListener {
        void onSettingsClicked();
        void onMoveTopClicked();
        void onLastUncheckedClicked();
        void onExitClicked();
    }

    public static void show(View anchorView, OnHomeFunctionsClickListener listener) {
        PopupMenu popup = new PopupMenu(anchorView.getContext(), anchorView);

        popup.getMenu().add(Menu.NONE, ID_SETTINGS, 1, anchorView.getContext().getString(R.string.settings));
        popup.getMenu().add(Menu.NONE, ID_MOVE_TOP, 2, anchorView.getContext().getString(R.string.move_top));
        popup.getMenu().add(Menu.NONE, ID_LAST_UNCHECKED, 3, anchorView.getContext().getString(R.string.last_unchecked));
        popup.getMenu().add(Menu.NONE, ID_EXIT, 4, anchorView.getContext().getString(R.string.exit));

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case ID_SETTINGS:
                    listener.onSettingsClicked();
                    return true;
                case ID_MOVE_TOP:
                    listener.onMoveTopClicked();
                    return true;
                case ID_LAST_UNCHECKED:
                    listener.onLastUncheckedClicked();
                    return true;
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
