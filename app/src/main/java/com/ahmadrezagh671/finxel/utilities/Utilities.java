package com.ahmadrezagh671.finxel.utilities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.ahmadrezagh671.finxel.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;

import java.util.Arrays;

/**
 * General utility class providing clipboard operations, chip selection,
 * theme color helpers, density conversion, URL opening, and SMS lookup.
 */
public class Utilities {
    private static final String TAG = "Utilities";

    /**
     * Reads the current text from the system clipboard.
     * @param context Application context
     * @return The clipboard text, or an empty string if clipboard is empty
     */
    public static String getClipboardText(Context context) {
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clipData = clipboard.getPrimaryClip();

            if (clipData != null && clipData.getItemCount() > 0) {
                return String.valueOf(
                        clipData.getItemAt(0).coerceToText(context)
                );
            }
        }

        return "";
    }

    /**
     * Hides the soft keyboard from the current input field.
     */
    public static void hideKeyboard(View view) {
        if (view == null) return;

        // find the actually focused child (the EditText) and clear it
        View focused = view.findFocus();
        if (focused != null) {
            focused.clearFocus();
        }

        InputMethodManager imm =
                (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Copies text to the system clipboard and shows a toast confirmation.
     * @param context Application context
     * @param title The label for the clip
     * @param text The text to copy
     */
    public static void copyToClipboard(Context context, String title, String text) {
        // Get the Clipboard Manager
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        // Create a ClipData object containing the text
        ClipData clip = ClipData.newPlainText(title, text);

        // Set the clip as the primary clip
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);

            // Optional: It's good Android UX practice to let the user know it worked
            Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Returns the text of the currently checked chip in a ChipGroup.
     * @param chipGroup The ChipGroup to query
     * @return The selected chip's text, or null if no chip is selected
     */
    public static String getSelectedChipText(ChipGroup chipGroup) {
        // 1. Get the ID of the currently checked chip
        int checkedChipId = chipGroup.getCheckedChipId();

        // 2. If no chip is selected, getCheckedChipId() returns View.NO_ID (-1)
        if (checkedChipId != View.NO_ID) {
            // 3. Find the Chip view within the ChipGroup using the ID
            Chip selectedChip = chipGroup.findViewById(checkedChipId);

            if (selectedChip != null) {
                // 4. Return the text or the tag (since you set both to 'item')
                return selectedChip.getText().toString();
            }
        }

        // Return null (or an empty string "") if nothing is selected
        return null;
    }

    /**
     * Retrieves a ColorStateList for a given theme attribute from a view.
     * @param view The view to resolve the attribute from
     * @param attr The theme attribute to resolve
     * @return A ColorStateList for the attribute
     */
    public static ColorStateList getThemeAttrColor(View view,int attr) {
        return ColorStateList.valueOf(MaterialColors.getColor(view,attr));
    }

    /**
     * Converts density-independent pixels to actual screen pixels.
     * @param context Context for display metrics
     * @param dp The value in dp
     * @return The value in pixels
     */
    public static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    /**
     * Opens a URL in the default browser.
     * @param context Application context
     * @param url The URL to open
     */
    public static void openUrl(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(intent);
    }



    /**
     * Resolves an SMS row ID by polling the SMS content provider
     * for a matching sender, body, and timestamp. Retries up to 15 times
     * with increasing delays to account for race conditions with the default SMS app.
     * @param context Application context
     * @param sender The originating phone number
     * @param body The SMS body text
     * @param timestamp The message timestamp in milliseconds
     * @return The SMS row ID, or -1 if not found
     */
    public static long resolveSmsId(Context context,String sender, String body, long timestamp) {
        final int SMS_LOOKUP_RETRIES = 15;
        final long SMS_LOOKUP_DELAY_MS = 2000L;

        Log.d(TAG,"resolveSmsId: sender " + sender + " timestamp " + timestamp);
        ContentResolver resolver = context.getContentResolver();
        Uri uri = Telephony.Sms.Inbox.CONTENT_URI;
        String[] projection = {
                Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE
        };
        String selection = Telephony.Sms.DATE + " >= ?";
        String[] selectionArgs = { String.valueOf(timestamp - 10000) };
        String sortOrder = Telephony.Sms.DATE + " DESC";

        for (int attempt = 0; attempt < SMS_LOOKUP_RETRIES; attempt++) {
            Log.d(TAG,"resolveSmsId: attempt " + (attempt + 1));
            try (Cursor cursor = resolver.query(uri, projection, selection, selectionArgs, sortOrder)) {
                if (cursor != null) {
                    Log.d(TAG, "resolveSmsId: cursor count: " + cursor.getCount());
                    int idCol = cursor.getColumnIndexOrThrow(Telephony.Sms._ID);
                    int addressCol = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
                    int bodyCol = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY);

                    while (cursor.moveToNext()) {
                        String address = cursor.getString(addressCol);
                        String storedBody = cursor.getString(bodyCol);
                        if (address != null && address.equals(sender)
                                && storedBody != null && storedBody.equals(body)) {
                            return cursor.getLong(idCol);
                        }
                    }
                }else {
                    Log.e(TAG, "resolveSmsId: cursor is null, info: " + uri.toString() + " " + sender + " " + body + " " + timestamp + " " + selection + " " + sortOrder + " " + Arrays.toString(selectionArgs));
                }
            } catch (Exception e) {
                Log.e(TAG, "resolveSmsId: query failed", e);
            }

            try {
                Log.d(TAG, "resolveSmsId: sleeping");
                Thread.sleep(SMS_LOOKUP_DELAY_MS * ((attempt / 3)+1));
            } catch (InterruptedException e) {
                Log.e(TAG, "resolveSmsId: interrupted");
                Thread.currentThread().interrupt();
                return -1;
            }
        }
        return -1;
    }

    /** Returns versionCode (e.g. 48) */
    public static long getVersionCode(Context context) {
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);

            return info.getLongVersionCode(); // API 28+
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    /** Returns versionName (e.g. "0.4.8.C01") */
    public static String getVersionString(Context context) {
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);

            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    /** Returns config version (e.g. "C01") */
    public static String getConfigVersion(Context context) {
        String version = getVersionString(context);

        if (version == null || version.length() < 3) {
            return "";
        }

        return version.substring(version.length() - 3);
    }

}
