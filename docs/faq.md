# FAQ

## General

### What does Finxel actually do?
It watches SMS messages from senders you specify, extracts the data you care about (amount, date, balance, etc.) using a configuration you provide, and turns the results into a spreadsheet-style sheet you can review and copy out, all on your device.

### Is Finxel free?
Yes. It's open source and free to use.

### Does Finxel come with support for my bank already built in?
Not automatically. Finxel is a generic engine, it needs a **configuration file** to know how to read a specific sender's message format. You can write your own (see [write-configuration.md](write-configuration.md)), import one, or check the [community_configs](https://github.com/ahmadrezagh671/Finxel/tree/main/community_configs) folder for shared configs.

### Where do I get the app?
From the project's GitHub **Releases** page, as an APK. It isn't on the Google Play Store. See [getting-started.md](getting-started.md).

### Does Finxel send my messages or data anywhere?
No, not your data. Everything, SMS content, extracted fields, configs, sheets, and history, is processed and stored locally on your device. There's no Finxel server involved, and none of that data is ever sent anywhere.

Finxel does use **Firebase Analytics** to collect anonymous, aggregated app-usage data (e.g. which screens are opened, session counts, crash/performance signals) to help understand how the app is used and to catch bugs. This is completely separate from your SMS content, configs, extracted records, or sheets. Analytics never sees any of that. See [THIRD-PARTY-NOTICES.md](../THIRD-PARTY-NOTICES.md) for the full details on this dependency.

### Is my data backed up if I lose my phone?
Finxel stores everything locally (configs, database, sheet history), so it isn't automatically backed up to the cloud beyond whatever your device's own Android backup settings do. This is exactly why you shouldn't treat the app's sheet as your real records, see the next question.

### Should I keep my financial records inside Finxel?
No. **Finxel's only job is to turn incoming SMS messages into structured, extracted data**. It's a converter, not a bookkeeping app or a storage app. Once a message has been processed into a row on the **Sheet** tab, use **Copy** to copy that data and paste it into wherever you actually keep your finances. Google Sheets, Excel, an accounting app, anything. After you've pasted it into your real records, feel free to **Clear** the sheet in Finxel. Keeping data only inside Finxel means it's one uninstall, factory reset, or lost phone away from being gone.

### Is there an iOS version?
No, Finxel is Android-only. Reading SMS content directly the way Finxel does isn't something iOS allows third-party apps to do.

### What's the minimum Android version supported?
Android 9.0 (API level 28) or newer.

## Permissions

### Why does Finxel need to read my SMS and contacts?
Reading SMS (`READ_SMS`/`RECEIVE_SMS`) is the whole point of the app. It's how Finxel finds and parses transaction messages. `READ_CONTACTS` is used only to also match a sender by their saved contact name, in case a bank's SMS shows up under a contact name rather than a raw number. See [permissions.md](permissions.md) for the complete list and reasoning.

### Why does it also want location and notification permissions?
Only if you turn on the optional **Location Service** or **Notification Entry** features in Settings. If you don't use those, you can safely leave those permissions off. The core reading/sheet features work without them.

### I denied a permission by accident. How do I grant it now?
Settings app → Apps → Finxel → Permissions, then toggle on what you need and reopen Finxel. Full steps in [permissions.md](permissions.md).

## Configurations

### What is a "config" really?
A single JSON file describing one provider/sender: which numbers to watch, what to ignore, what fields to extract from the message text (usually with regular expressions), and how to lay those fields out as a sheet. Full format in [write-configuration.md](write-configuration.md).

### Can two configs have the same name?
No, config names must be unique on your device. Delete the existing one first if you want to reuse a name.

### I imported a config and nothing shows up on the Home tab. Why?
A few common reasons:
- No SMS from that config's `provider` senders exist on your device yet.
- The sender ID in your `provider` list doesn't exactly match how the message shows up (try the exact number or exact sender name as it appears in your default messaging app).

### Can I edit a config after importing it?
Yes, Settings → Saved configuration files → tap a config's menu → **Edit Config**, which opens Finxel's built-in JSON editor. You can't rename a config this way (the name is fixed once created); delete and reimport it under a new name instead.

### I see "Invalid configuration format" when importing. What's wrong?
The JSON is missing one of the required top-level pieces Finxel checks for on import: `information` (with `name`, `provider`, `color`, `location`), `fields` (each with `name` and `type`), and `result` (a 2D array). Also make sure `layout` is present, even as an empty array. See the checklist at the bottom of [write-configuration.md](write-configuration.md).

## Location Service & Notification Entry

### I turned on Location Service but locations aren't showing up. What should I check?
1. Confirm the toggle is still on in Settings (it turns itself back off automatically if a required permission was denied).
2. Confirm location permission is set to **"Allow all the time,"** not just "while using the app."
3. Confirm the relevant config has `"location": true` under `information`.
4. Make sure battery optimization is disabled for Finxel, and (on Xiaomi devices) that Autostart is enabled. Both are covered in [permissions.md](permissions.md).
5. Remember it can take up to 60 seconds to get a fix; a very poor GPS/network signal can also cause it to fail for that message.

### The reply notification for Notification Entry never shows up. Why?
Check the same background-reliability items as above (battery optimization, Xiaomi Autostart, POST_NOTIFICATIONS permission), and confirm your config actually defines a `notification_entry` array. See [notification-entry.md](notification-entry.md).

### Why does the app keep asking me to disable battery optimization or enable autostart?
These aren't Android "permissions" exactly, but many phones (especially Xiaomi/MIUI, but also other brands) will kill background apps aggressively to save battery, which stops Location Service and Notification Entry from working reliably. Finxel prompts you to disable these restrictions specifically for itself so those two background features keep working.

## Errors & troubleshooting

### What does "Error 101" mean?
It shows up on a message card when a `compare_with_last_message` rule in your config can't be evaluated. Almost always because of a typo in a field name or a value that isn't a clean number. It's a sign that your configuration needs fixing. Full explanation and fix steps in [errors.md](errors.md).

### A message shows a red error tag with custom text instead of a number. Is that a bug?
No, that's your config's own `skips` rule working as intended, showing the `error_text` you defined for it. It just means this particular message was intentionally excluded from processing.

### Finxel force-closes or a config seems to break the app. What should I do?
Double-check the config's JSON for structural mistakes (missing commas/brackets, wrong types). An editor with JSON syntax highlighting (like the one built into Finxel, or any code editor on your desktop) will usually point out the exact issue. If a specific field type keeps failing, revisit its section in [write-configuration.md](write-configuration.md).

## Contributing / reporting issues

### I found a bug or want to request a feature. Where do I go?
Open an issue on [Finxel's GitHub Issues page](https://github.com/ahmadrezagh671/Finxel/issues). Include your Android version, device model, and (if relevant) a sanitized copy of the config you were using. Remove any personal data first. For quick questions or informal discussion first, you're also welcome in the [Telegram Group](https://t.me/FinxelCommunity).

### Can I share a config I made for other users?
Yes, sharing configs (and whole templates) for common providers is encouraged. Open a pull request adding your files to the [community_configs](https://github.com/ahmadrezagh671/Finxel/tree/main/community_configs) folder (linked from the in-app **Add Config from GitHub** option), see that folder's own `README.md` for how templates and configs are organized and how to submit one.
