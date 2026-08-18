# Location Service

Location Service is an optional Finxel feature that automatically tags a matching SMS with the device's location at (or right after) the moment it arrived. Useful for knowing *where* a card was swiped or a payment happened.

It's entirely opt-in and off by default.

## How it works, end to end

1. You turn on **Settings → Location Service**.
2. Finxel asks for the permissions it needs (see below) and marks every config whose `information.location` is `true` as "watched" for location.
3. When an SMS arrives, Finxel's background receiver checks whether the sender matches a watched config's `provider` list (by number, or by the sender's saved contact name).
4. If it matches, Finxel starts a short-lived foreground service that:
   - resolves the exact SMS row that just arrived,
   - requests a single current location fix from Google Play Services (high accuracy if fine location is granted, otherwise a coarser network-based fix),
   - waits up to **60 seconds** for a fix,
   - saves the latitude/longitude against that SMS in Finxel's local database.
5. On the **Home** tab, any message with a saved location shows a location chip you can tap to open it in your maps app (`geo:` link).

<img src="images/home.webp" alt="Home screen" width="300">

The whole process is silent. You'll only notice a brief, low-priority "Processing Incoming Message" notification while it works (required by Android for any foreground service), and it goes away in a few seconds.

## Enabling a config for location tagging

Location Service only reacts to senders belonging to configs that opt in. In your config's `information` object, set:

```json
"information": {
  "name": "MyBank",
  "provider": ["MyBank"],
  "color": "#2196F3",
  "location": true
}
```

If `location` is `false` (or omitted), messages from that config are never location-tagged, even while the feature is switched on globally in Settings.

Whenever you add, edit, or remove a config, or toggle the Settings switch, Finxel rebuilds the list of "location-enabled" senders automatically. You don't need to do anything else.

## Reading the captured location in a config

Once a location has been captured for a message, you can pull it into your sheet with two dedicated field types:

```json
{ "name": "latitude", "type": "L_LATITUDE" },
{ "name": "longitude", "type": "L_LONGITUDE" }
```

Add cells for these fields in your `result` grid the same way as any other field (see [write-configuration.md](write-configuration.md)). If no location was captured yet for a given message (e.g. it hasn't finished, or the feature wasn't on when the message arrived), these fields resolve to an empty value.

## Permissions required

Turning the toggle on requests:

- `RECEIVE_SMS`, `READ_SMS`, `READ_CONTACTS`
- `ACCESS_FINE_LOCATION`
- `POST_NOTIFICATIONS` (Android 13+)

...and then, on Android 10+, a follow-up prompt asking you to switch location access to **"Allow all the time"** in system settings. This is required because the location fix happens in the background, while the app isn't open. If this step is skipped or denied, the toggle turns itself back off.

See **[permissions.md](permissions.md)** for the full permission breakdown, including the battery optimization and Xiaomi autostart prompts that also appear when you enable this feature (both matter a lot for reliability, a background-killed app can't capture a location fix).

## Notes and limitations

- Location is captured **once per matching message**, right when it arrives. Finxel doesn't track your location continuously or in the background outside of this trigger.
- If the device can't get a fix within 60 seconds (e.g. poor GPS signal, no data connection for network location), no location is saved for that message; the field will just come back empty.
- If READ_SMS or POST_NOTIFICATIONS permission is revoked later, the location capture for new messages will silently stop working until permissions are restored.
- Location data stays on your device, stored in Finxel's local database, and is only shown/exported through fields you explicitly add to your config's `result`.
