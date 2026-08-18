# Permissions

Finxel needs access to a few sensitive Android permissions in order to read and react to SMS messages. This page explains exactly what each permission is used for and how/when Finxel asks for it, so you can decide what to grant with confidence.

All processing happens **on your device**. Permissions are used only to make the features below work. Your SMS content, extracted data, configs, and sheets are never transmitted off your phone. (Finxel does use Firebase Analytics for anonymous app-usage stats, unrelated to permission-gated data. See the [Privacy](../README.md#privacy) section.)

## Permission overview

| Permission | Used for | Requested when |
|---|---|---|
| `READ_SMS` | Reading the content of your SMS inbox so it can be matched against your configs | App first launch |
| `RECEIVE_SMS` | Reacting to a message the moment it arrives (needed for real-time features) | App first launch |
| `READ_CONTACTS` | Matching a message sender against your saved contact names, in addition to raw phone numbers | App first launch |
| `POST_NOTIFICATIONS` | Showing the small "processing" notification, and the Notification Entry reply prompts | When you enable **Location Service** or **Notification Entry** in Settings |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Getting a GPS/network location fix to tag a matching SMS with | When you enable **Location Service** in Settings |
| `ACCESS_BACKGROUND_LOCATION` | Allowing the location fix to be captured even while Finxel isn't open on screen | When you enable **Location Service** in Settings (Android 10+) |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_LOCATION` | Running the short-lived background service that captures location/notification data right after a message arrives | Automatic, tied to Location Service / Notification Entry |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Asking Android not to kill Finxel in the background, so location/notification features stay reliable | When you enable **Location Service** or **Notification Entry** in Settings |

## Step 1. Core permissions (asked on first launch)

The moment you open Finxel for the first time, it requests:

- `READ_SMS`
- `RECEIVE_SMS`
- `READ_CONTACTS`

These are the minimum permissions needed for the **Home** and **Sheet** tabs to work at all. Without `READ_SMS`, Finxel has nothing to read.

**If you tap Allow:** you're taken straight to the Home tab.

**If you tap Deny (any of them):** Finxel opens the **Settings** tab instead, and you'll see a message that read permission isn't granted whenever you try to open Home.

### Granting them later

If you denied these initially, you can grant them from Android's system settings:

1. Open your phone's **Settings** app.
2. Go to **Apps** (or **Apps & notifications**) → **Finxel**.
3. Tap **Permissions**.
4. Enable **SMS** and **Contacts**.
5. Reopen Finxel. The Home tab should now work.

## Step 2. Feature permissions (asked from the Settings tab)

Two optional features in **Settings** request additional permissions only when you turn them on:

### Location Service toggle

Turning this switch on triggers a request for:

- `RECEIVE_SMS`, `READ_SMS`, `READ_CONTACTS` (re-confirmed)
- `ACCESS_FINE_LOCATION`
- `POST_NOTIFICATIONS` (Android 13+ only)

If you grant fine location, Finxel then (on Android 10+) shows a dialog asking you to open system settings and switch the location permission to **"Allow all the time."** This step is required because location needs to be captured in the background, right after a message arrives, while Finxel isn't open. If you don't complete this step, the toggle turns itself back off.

See **[location-service.md](location-service.md)** for how this data is used.

### Notification Entry toggle

Turning this switch on triggers a request for:

- `RECEIVE_SMS`, `READ_SMS`, `READ_CONTACTS` (re-confirmed)
- `POST_NOTIFICATIONS` (Android 13+ only)

This lets Finxel show a notification with a reply box right after a matching message arrives, so you can type in extra details without opening the app.

See **[notification-entry.md](notification-entry.md)** for details.

### If you deny a feature permission

If any required permission for a toggle isn't granted, Finxel automatically switches that toggle back **off**. The feature simply won't run until you try again and grant everything it asks for.

## Step 3. Device-specific background reliability prompts

Turning on **Location Service** or **Notification Entry** may also show one or both of the following dialogs. These aren't Android permissions in the technical sense, but they're just as important for the features to work reliably:

### Disable battery optimization

Some Android versions aggressively stop background apps to save battery, which can prevent Finxel from reacting to messages when the screen is off. If Finxel detects that battery optimization is still active for it, it shows a dialog:

- Tap **Go to Settings**, then choose to allow Finxel to run without battery restrictions (wording varies by manufacturer, e.g. "Don't optimize" / "Unrestricted").
- Tap **Cancel** to skip this, the feature may work less reliably in the background.

You can also manage this manually any time:
**Settings → Apps → Finxel → Battery → Unrestricted** (path varies by phone brand).

### Enable autostart (Xiaomi devices only)

MIUI (Xiaomi's Android skin) has an extra "Autostart" restriction that can stop background apps entirely, on top of standard battery optimization. If Finxel detects it's running on a Xiaomi device, it shows a dialog prompting you to open the Autostart manager and enable Finxel there.

If the automatic shortcut doesn't work on your MIUI version, you can find this manually under:
**Settings → Apps → Manage apps → Finxel → Autostart** (or **Security app → Permissions → Autostart**).

## Summary: how to grant permissions manually at any time

1. Open your phone's **Settings** app.
2. **Apps** → **Finxel** → **Permissions**.
3. Toggle on whichever permissions the feature you want requires (see the table above).
4. If using Location Service, also make sure **Location** is set to **"Allow all the time"**, not just "while using the app."
5. Reopen Finxel and re-enable the corresponding toggle in **Settings** if it had turned itself off.
