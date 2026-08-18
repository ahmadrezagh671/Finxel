package com.ahmadrezagh671.finxel.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.ahmadrezagh671.finxel.models.NotificationEntryConfigList;
import com.ahmadrezagh671.finxel.utilities.AppSettings;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Receives incoming SMS broadcasts and determines whether any configured
 * SMS-triggered features should run for the message.
 *
 * When an SMS is received, the receiver:
 * - Reconstructs the complete message from all SMS parts.
 * - Checks whether the sender matches any configured provider lists
 *   (by phone number or saved contact name).
 * - Determines whether location capture, notification-entry actions,
 *   or both are required for this message.
 * - Starts {@link LocationCaptureService} with the appropriate extras
 *   to perform the requested work.
 *
 * The receiver performs only lightweight processing and immediately
 * delegates any longer-running tasks to the foreground service, allowing
 * {@code onReceive()} to return as quickly as possible and comply with
 * Android broadcast execution limits.
 */
public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";


    /**
     * Entry point for the SMS broadcast. Parses the incoming message, checks
     * the sender against configured allow lists, and starts LocationCaptureService.
     * @param context The broadcast context
     * @param intent The SMS_RECEIVED intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // check start info (sender, body, timestamp)
        if (intent == null || !Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }
        if (!AppSettings.getLocationServiceStatus(context) && !AppSettings.getNotificationEntryStatus(context)) {
            return; // Exit immediately. Zero battery impact.
        }

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) {
            return;
        }
        String sender = messages[0].getOriginatingAddress();
        long timestamp = System.currentTimeMillis();
        StringBuilder bodyBuilder = new StringBuilder();
        for (SmsMessage message : messages) {
            if (message.getMessageBody() != null) {
                bodyBuilder.append(message.getMessageBody());
            }
        }
        String body = bodyBuilder.toString();
        if (sender == null) {
            Log.w(TAG, "onReceive: originating address is null, ignoring");
            return;
        }

        Intent serviceIntent = new Intent(context, LocationCaptureService.class);

        boolean locationNeeded = processLocationService(context, sender);
        boolean notifEntryNeeded = processNotificationEntry(context, sender,serviceIntent);

        if (!locationNeeded && !notifEntryNeeded){
            Log.w(TAG, "onReceive: sender not on list, ignoring");
            return;
        }

        serviceIntent.putExtra(LocationCaptureService.EXTRA_GET_LOCATION, locationNeeded);

        serviceIntent.putExtra(LocationCaptureService.EXTRA_SENDER, sender);
        serviceIntent.putExtra(LocationCaptureService.EXTRA_BODY, body);
        serviceIntent.putExtra(LocationCaptureService.EXTRA_TIMESTAMP, timestamp);

        try {
            ContextCompat.startForegroundService(context, serviceIntent);
        } catch (SecurityException e) {
            // Most likely cause: this is a location-type foreground service and the app
            // does not hold ACCESS_BACKGROUND_LOCATION, so the platform refused to start
            // it while there's no visible Activity. See setup notes.
            Log.e(TAG, "onReceive: failed to start LocationCaptureService", e);
        }
    }


    /**
     * Checks whether the sender matches any notification entry provider config.
     * If matched, attaches the notification entry data to the service intent.
     * @param context The broadcast context
     * @param sender The originating phone number
     * @param serviceIntent The intent to forward to LocationCaptureService
     * @return true if a notification entry config matched this sender
     */
    private boolean processNotificationEntry(Context context, String sender, Intent serviceIntent) {
        if (!AppSettings.getNotificationEntryStatus(context)) {
            return false;
        }
        NotificationEntryConfigList notificationEntryConfigList;
        try {
            notificationEntryConfigList = new NotificationEntryConfigList(AppSettings.getNotificationEntryConfig(context));
        } catch (JSONException e) {
            Log.w(TAG, "processNotificationEntry: failed to parse config, ignoring: " + e.getMessage());
            return false;
        }

        for (NotificationEntryConfigList.NotificationEntryConfig notifConf : notificationEntryConfigList.configs) {
            if (isSenderInList(sender, notifConf.providers, context)) {
                serviceIntent.putExtra(LocationCaptureService.EXTRA_NOTIFICATION_ENTRY, notifConf.toJsonString());
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether the sender matches any location service provider config.
     * @param context The broadcast context
     * @param sender The originating phone number
     * @return true if a location service config matched this sender
     */
    private boolean processLocationService(Context context, String sender) {
        if (!AppSettings.getLocationServiceStatus(context)) {
            return false;
        }
        List<String> providerList = getProviderList(AppSettings.getLocationServiceConfig(context));
        return isSenderInList(sender,providerList,context);
    }

    /**
     * Checks if the sender matches any entry in the provider list, including
     * a fallback lookup by contact display name.
     * @param sender The originating phone number
     * @param providerList The list of allowed phone numbers
     * @param context Context for contact lookup
     * @return true if the sender is in the list or matches a contact name
     */
    private boolean isSenderInList(String sender,List<String> providerList,Context context) {
        boolean matched = providerList.contains(sender);
        if (!matched) {
            String contactName = getContactDisplayName(context, sender);
            if (contactName != null && !contactName.isEmpty()) {
                Log.d(TAG, "onReceive: sender " + sender + " not on list by number, "
                        + "trying saved contact name \"" + contactName + "\"");
                matched = providerList.contains(sender);
            }
        }
        if (!matched) {
            return false;
        }
        return true;
    }

    /**
     * Parses a JSON config string into a list of allowed provider phone numbers.
     * @param locationServiceConfig JSON array string of provider numbers
     * @return List of provider phone numbers
     */
    private List<String> getProviderList(String locationServiceConfig) {
        List<String> providerList = new ArrayList<>();
        if (locationServiceConfig != null && !locationServiceConfig.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(locationServiceConfig);
                for (int i = 0; i < jsonArray.length(); i++) {
                    providerList.add(jsonArray.getString(i));
                }
            }catch (JSONException e) {
                Log.e(TAG, "isContactInYourList: " + e.getMessage());
            }
        }
        return providerList;

    }

    /**
     * Looks up the display name saved in the device's contacts for the given
     * phone number, using ContactsContract.PhoneLookup.
     * Returns null if no permission, no match, or on any lookup error.
     * Requires: android.permission.READ_CONTACTS
     * @param context Context for content resolver access
     * @param phoneNumber The phone number to look up
     * @return The contact display name, or null if not found
     */
    private String getContactDisplayName(Context context, String phoneNumber) {
        Uri uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber));

        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};

        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex);
                }
            }
        } catch (SecurityException e) {
            // READ_CONTACTS not granted.
            Log.w(TAG, "getContactDisplayName: missing READ_CONTACTS permission", e);
        } catch (Exception e) {
            Log.e(TAG, "getContactDisplayName: lookup failed", e);
        }

        return null;
    }
}