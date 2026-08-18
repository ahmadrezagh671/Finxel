package com.ahmadrezagh671.finxel.utilities;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;

import com.ahmadrezagh671.finxel.R;
import com.ahmadrezagh671.finxel.models.configModel.NotificationEntry;
import com.ahmadrezagh671.finxel.services.NotificationEntryReplyReceiver;

import java.util.ArrayList;

/**
 * Manages notification channels and reply notifications for notification entry configs.
 * Creates the notification channel and builds reply/completion notifications
 * with RemoteInput support for user responses.
 */
public class NotificationReplyManager {

    public static final String CHANNEL_ID = "entry_reply_channel";
    public static final String ACTION_REPLY = "com.ahmadrezagh671.finxel.ACTION_REPLY";
    public static final String KEY_TEXT_REPLY = "key_text_reply";
    public static final String EXTRA_NOTIF_ID = "extra_notif_id";
    public static final String EXTRA_REMAINING = "extra_remaining";
    public static final String EXTRA_TEXT = "extra_text";
    public static final String EXTRA_SMS_ID = "extra_sms_id" ;

    /**
     * Creates the notification channel for entry reply notifications.
     * Must be called before any notifications are posted.
     * @param context Application context
     */
    public static void createChannel(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        NotificationChannel channel =         new NotificationChannel(
                CHANNEL_ID, context.getString(R.string.entry_replies_channel), NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(channel);
    }

    /**
     * Shows a reply notification for the current notification entry, allowing the user
     * to respond via RemoteInput. Displays remaining entries as a notification chain.
     * @param context Application context
     * @param notificationId The notification ID for this batch
     * @param remaining The list of remaining notification entries to process
     * @param text The body text of the SMS message
     * @param smsId The database ID of the SMS record
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    public static void showReplyNotification(Context context, int notificationId, ArrayList<NotificationEntry> remaining, String text, long smsId) {
        if (remaining == null || remaining.isEmpty()) {
            NotificationManagerCompat.from(context).cancel(notificationId);
            return;
        }

        NotificationEntry current = remaining.get(0);

        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel(current.hint)
                .build();

        Intent intent = new Intent(context, NotificationEntryReplyReceiver.class);
        intent.setAction(ACTION_REPLY);
        intent.putExtra(EXTRA_NOTIF_ID, notificationId);
        intent.putExtra(EXTRA_REMAINING, remaining); // ArrayList<Serializable> works fine
        intent.putExtra(EXTRA_TEXT,text);
        intent.putExtra(EXTRA_SMS_ID,smsId);

        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId, // same request code per batch -> extras get replaced each time
                intent,
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_send, current.hint, replyPendingIntent)
                        .addRemoteInput(remoteInput)
                        .setAllowGeneratedReplies(false)
                        .build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_transparent)
                .setContentText(text)
                .setSubText(context.getString(R.string.data_entry))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .addAction(replyAction)
                .setAutoCancel(false);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    /**
     * Shows a completion notification indicating all entries have been saved.
     * @param context Application context
     * @param notifId The notification ID to cancel/update
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    public static void showDoneNotification(Context context, int notifId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_transparent)
                .setContentText(context.getString(R.string.entry_saved_successfully))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSilent(true)
                .setTimeoutAfter(3000);

        NotificationManagerCompat.from(context).notify(notifId, builder.build());
    }
}