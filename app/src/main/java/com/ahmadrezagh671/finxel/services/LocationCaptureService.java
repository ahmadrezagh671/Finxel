package com.ahmadrezagh671.finxel.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.ahmadrezagh671.finxel.R;
import com.ahmadrezagh671.finxel.db.AppDatabase;
import com.ahmadrezagh671.finxel.db.SmsRecord;
import com.ahmadrezagh671.finxel.models.NotificationEntryConfigList;
import com.ahmadrezagh671.finxel.models.configModel.NotificationEntry;
import com.ahmadrezagh671.finxel.utilities.ConfigManager;
import com.ahmadrezagh671.finxel.utilities.NotificationReplyManager;
import com.ahmadrezagh671.finxel.utilities.TextTruncator;
import com.ahmadrezagh671.finxel.utilities.Utilities;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Started by SmsReceiver immediately after a matching SMS is received.
 * Runs as a foreground service to ensure the work isn't delayed or
 * terminated by the system while processing the SMS.
 *
 * Depending on the provided extras, the service can:
 * - Resolve the SMS row ID.
 * - Capture and store the device's current location.
 * - Display a notification containing configured reply/input actions.
 *
 * All work is performed on a background thread, and the service stops
 * itself as soon as the requested tasks are complete. Its intended
 * lifetime is only a few seconds.
 *
 * Manifest requirement (Android 10+/API 29+):
 * If location capture is used, the service must declare
 * android:foregroundServiceType="location". When the app is not visible,
 * Android requires ACCESS_BACKGROUND_LOCATION to start a location
 * foreground service.
 */
public class LocationCaptureService extends Service {

    private static final String TAG = "LocationCaptureService";
    public static final String EXTRA_GET_LOCATION = "get_location";
    public static final String EXTRA_NOTIFICATION_ENTRY = "notifi_entry_data";
    public static final String EXTRA_SENDER = "sender";
    public static final String EXTRA_BODY = "body";
    public static final String EXTRA_TIMESTAMP = "timestamp";
    private static final String CHANNEL_ID = "sms_location_capture";
    private static final int NOTIFICATION_ID = 42;
    private static final long LOCATION_TIMEOUT_SECONDS = 60L;


    private static final int ENTRY_NOTIFICATION_TEXT_MAX_LINES = 6;
    private static final int ENTRY_NOTIFICATION_TEXT_MAX_CHARS = 300;

    private ExecutorService executor;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Must be called within a few seconds of the service starting, so it's the very
        // first thing here — before any lookup/network work.
        startForeground(NOTIFICATION_ID, buildNotification());

        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        String sender = intent.getStringExtra(EXTRA_SENDER);
        String body = intent.getStringExtra(EXTRA_BODY);
        long timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, -1);
        boolean getLocation = intent.getBooleanExtra(EXTRA_GET_LOCATION, false);
        String notificationEntry = intent.getStringExtra(EXTRA_NOTIFICATION_ENTRY);

        // onStartCommand runs on the main thread; do the actual work off it.
        executor.execute(() -> {
            try {
                handleCapture(sender, body, timestamp,getLocation,notificationEntry);
            } catch (Exception e) {
                Log.e(TAG, "onStartCommand: capture failed", e);
            } finally {
                stopSelf(startId);
            }
        });

        return START_NOT_STICKY;
    }

    private void handleCapture(String sender, String body, long timestamp, boolean getLocation, String notificationEntry) {
        if (sender == null || timestamp == -1) {
            Log.e(TAG, "handleCapture: missing extras");
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "handleCapture: READ_SMS not granted");
            return;
        }

        // get sms id
        long smsId = Utilities.resolveSmsId(this,sender, body, timestamp);
        if (smsId == -1) {
            Log.w(TAG, "handleCapture: could not resolve sms id for this message");
            return;
        }

        // get location
        if (getLocation){
            Location location = fetchLocation();
            if (location == null) {
                Log.w(TAG, "handleCapture: could not obtain a location fix");
            }else {
                saveLocation(smsId, location.getLatitude(), location.getLongitude());
                Log.d(TAG, "handleCapture: saved location for sms id " + smsId);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "handleCapture: POST_NOTIFICATIONS not granted");
            return;
        }

        // push notification entry
        if (notificationEntry != null && !notificationEntry.isEmpty()) {
            NotificationEntryConfigList.NotificationEntryConfig notificationEntryConfig = new NotificationEntryConfigList.NotificationEntryConfig(notificationEntry);
            String skipsError = ConfigManager.checkSkips(body, notificationEntryConfig.skips);

            if (skipsError != null){
                Log.w(TAG, "handleCapture notificationEntry: sms has error" + skipsError);
                return;
            }

            pushEntriesNotification(notificationEntryConfig.entries, TextTruncator.truncate(body, ENTRY_NOTIFICATION_TEXT_MAX_LINES, ENTRY_NOTIFICATION_TEXT_MAX_CHARS),smsId);
        }
    }

    private static final AtomicInteger idGenerator = new AtomicInteger((int) (System.currentTimeMillis() % 100000));
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private void pushEntriesNotification(List<NotificationEntry> entries, String text, long smsId) {
        if (entries == null || entries.isEmpty()) return;

        int notificationId = idGenerator.incrementAndGet(); // unique per batch
        NotificationReplyManager.showReplyNotification(
                getApplicationContext(), notificationId, new ArrayList<>(entries),text,smsId);
    }

    private Location fetchLocation() {
        boolean hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!hasFine && !hasCoarse) {
            Log.w(TAG, "fetchLocation: no location permission granted");
            return null;
        }

        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean locationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!locationEnabled){
            Log.w(TAG, "fetchLocation: location in not enabled");
            return null;
        }

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);

        CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                .setPriority(hasFine ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setDurationMillis(TimeUnit.SECONDS.toMillis(LOCATION_TIMEOUT_SECONDS))
                .build();

        try {
            // We're on the executor's background thread here, not the main thread, so
            // blocking on the Task is fine.
            return Tasks.await(client.getCurrentLocation(request, null), LOCATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException | SecurityException e) {
            Log.e(TAG, "fetchLocation: failed to get location", e);
            return null;
        }
    }

    /** Provided by the user, adapted to run inside the service's background thread. */
    private void saveLocation(long id, double lat, double lng) {
        Log.d(TAG, "saveLocation: " + id + ": " + lat + " " + lng);
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        SmsRecord smsRecord = db.smsRecordDao().getRecordById(String.valueOf(id));
        if (smsRecord == null) {
            smsRecord = new SmsRecord(String.valueOf(id), null);
        }
        smsRecord.addValueToJson("lat", lat);
        smsRecord.addValueToJson("lng", lng);
        db.smsRecordDao().insertOrUpdate(smsRecord);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Location capture", NotificationManager.IMPORTANCE_MIN);
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        // IMPORTANCE_MIN keeps this out of the user's way, but it must still exist:
        // a foreground service requires a visible (even if unobtrusive) notification.
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.processing_incoming_message_notification_title))
                .setSmallIcon(R.drawable.ic_launcher_transparent)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}