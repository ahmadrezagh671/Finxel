package com.ahmadrezagh671.finxel.utilities;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import com.ahmadrezagh671.finxel.models.MySMS;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for querying SMS messages from the device's SMS provider.
 *
 * Supports filtering messages by phone numbers or contact names. Contact
 * names are automatically resolved to their associated phone numbers before
 * querying, allowing callers to work with either form transparently.
 *
 * Requires:
 * - android.permission.READ_SMS to query the SMS provider.
 * - android.permission.READ_CONTACTS if contact name resolution is needed.
 */
public class SMSManager {
    private static final String TAG = "SMSManager";

    /**
     * Loads SMS messages whose sender address matches any of the supplied
     * phone numbers or contact names.
     *
     * Contact names are first resolved to all associated phone numbers before
     * executing the query. The resulting messages are returned in descending
     * chronological order (newest first).
     *
     * @param context Context used to access the SMS and Contacts content providers.
     * @param phoneNumbers A list containing phone numbers, contact names, or both.
     * @return A list of matching {@link MySMS} objects. Returns an empty list if
     * no senders can be resolved or no matching messages are found.
     *
     * Requires:
     * - android.permission.READ_SMS
     * - android.permission.READ_CONTACTS (only when contact names are supplied)
     */
    public static List<MySMS> loadSms(Context context, List<String> phoneNumbers) {
        // Expand any contact names in the list into their associated phone numbers
        List<String> resolvedNumbers = resolveToPhoneNumbers(context, phoneNumbers);

        if (resolvedNumbers.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. Create a string of question marks separated by commas (e.g., "?, ?, ?")
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < resolvedNumbers.size(); i++) {
            placeholders.append("?");
            if (i < resolvedNumbers.size() - 1) {
                placeholders.append(", ");
            }
        }

        // 2. Build the dynamic selection string: "address IN (?, ?, ?)"
        String selection = "address IN (" + placeholders.toString() + ")";

        // 3. Convert the resolved list to the required String array
        String[] selectionArgs = resolvedNumbers.toArray(new String[0]);

        // 4. Run the query safely
        Cursor cursor = context.getContentResolver().query(
                Uri.parse("content://sms/"),
                null,
                selection,
                selectionArgs,
                "date DESC"
        );
        return MySMS.getSmsList(cursor);
    }

    /**
     * Walks the input list and, for each entry:
     *  - if it already looks like a phone number, keeps it as-is
     *  - otherwise treats it as a contact name, looks up all phone numbers saved
     *    under that name, and adds those instead
     *  - if it's neither a phone-number pattern nor a known contact name, keeps
     *    the original value as a fallback
     * Duplicates are removed (LinkedHashSet preserves insertion order).
     *
     * Requires android.permission.READ_CONTACTS to resolve names.
     */
    private static List<String> resolveToPhoneNumbers(Context context, List<String> items) {
        Set<String> result = new LinkedHashSet<>();

        for (String item : items) {
            if (item == null || item.trim().isEmpty()) {
                continue;
            }

            if (isPhoneNumber(item)) {
                result.add(item);
            } else {
                List<String> numbersForName = getPhoneNumbersByContactName(context, item);
                if (!numbersForName.isEmpty()) {
                    result.addAll(numbersForName);
                } else {
                    // Not a recognized contact name and not phone-number shaped;
                    // keep it in case the address column stores it directly anyway.
                    result.add(item);
                }
            }
        }

        return new ArrayList<>(result);
    }

    /**
     * Heuristic: treat a string as a phone number if, after stripping spaces,
     * dashes, parentheses, and a leading '+', it's all digits.
     */
    private static boolean isPhoneNumber(String value) {
        String stripped = value.replaceAll("[\\s\\-()+]", "");
        return !stripped.isEmpty() && stripped.matches("\\d+");
    }

    /**
     * Looks up every phone number saved under a given contact display name.
     * Returns an empty list if no permission, no match, or on any lookup error.
     */
    private static List<String> getPhoneNumbersByContactName(Context context, String contactName) {
        List<String> numbers = new ArrayList<>();

        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " = ?";
        String[] selectionArgs = new String[]{contactName};

        try (Cursor cursor = context.getContentResolver()
                .query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                while (cursor.moveToNext()) {
                    if (numberIndex >= 0) {
                        numbers.add(cursor.getString(numberIndex));
                    }
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "getPhoneNumbersByContactName: missing READ_CONTACTS permission", e);
        } catch (Exception e) {
            Log.e(TAG, "getPhoneNumbersByContactName: lookup failed", e);
        }

        return numbers;
    }
}
