package com.ahmadrezagh671.finxel.services;

import android.Manifest;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationManagerCompat;

import com.ahmadrezagh671.finxel.db.AppDatabase;
import com.ahmadrezagh671.finxel.db.SmsRecord;
import com.ahmadrezagh671.finxel.models.configModel.NotificationEntry;
import com.ahmadrezagh671.finxel.utilities.NotificationReplyManager;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BroadcastReceiver that handles replies to notification entries.
 * Receives the user's reply text from a RemoteInput, saves it to the database,
 * and either shows the next pending notification or a completion notification.
 */
public class NotificationEntryReplyReceiver extends BroadcastReceiver {
    private static final String TAG = "NotifEntryReplyReceiver";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();



    /**
     * Handles the broadcast reply intent: extracts the reply text, saves it to the database,
     * and updates the notification chain accordingly.
     * @param context The broadcast context
     * @param intent The intent containing the reply data and notification extras
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!NotificationReplyManager.ACTION_REPLY.equals(intent.getAction())) return;

        Bundle results = RemoteInput.getResultsFromIntent(intent);
        CharSequence replyText = results != null
                ? results.getCharSequence(NotificationReplyManager.KEY_TEXT_REPLY)
                : null;

        int notifId = intent.getIntExtra(NotificationReplyManager.EXTRA_NOTIF_ID, -1);
        ArrayList<NotificationEntry> remaining =
                (ArrayList<NotificationEntry>) intent.getSerializableExtra(NotificationReplyManager.EXTRA_REMAINING);

        if (remaining == null || remaining.isEmpty() || notifId == -1) return;

        NotificationEntry current = remaining.remove(0);
        String reply = replyText != null ? replyText.toString() : "";

        long smsId = intent.getLongExtra(NotificationReplyManager.EXTRA_SMS_ID,-1);

        final PendingResult pendingResult = goAsync();

        EXECUTOR.execute(() -> {
            try {
                saveEntryToDB(smsId,current.name,reply,context);

                if (!remaining.isEmpty()) {
                    NotificationReplyManager.showReplyNotification(
                            context.getApplicationContext(),
                            notifId,
                            remaining,
                            intent.getStringExtra(NotificationReplyManager.EXTRA_TEXT),
                            smsId);
                } else {
                    NotificationReplyManager.showDoneNotification(
                            context.getApplicationContext(),
                            notifId
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to process reply", e);
                // Optionally: update notification to show failure instead of canceling
            } finally {
                // MUST always call finish(), even on error/exception,
                // or you leak the wakelock/priority the system gave you.
                pendingResult.finish();
            }
        });
    }

    /**
     * Saves a notification entry reply to the database, keyed by the notification entry name.
     * @param smsID The SMS record ID to associate the entry with
     * @param key The notification entry name used as the database key
     * @param data The reply text entered by the user
     * @param context Application context for database access
     */
    private void saveEntryToDB(long smsID,String key,String data,Context context) {
        String entryKey = AppDatabase.ENTRY_START_KEY_DB + key;
        AppDatabase db = AppDatabase.getDatabase(context.getApplicationContext());
        SmsRecord smsRecord = db.smsRecordDao().getRecordById(String.valueOf(smsID));
        if (smsRecord == null) {
            smsRecord = new SmsRecord(String.valueOf(smsID), null);
        }
        smsRecord.addValueToJson(entryKey, data);
        db.smsRecordDao().insertOrUpdate(smsRecord);
    }
}
