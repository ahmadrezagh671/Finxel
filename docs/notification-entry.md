# Notification Entry

Notification Entry is an optional Finxel feature that pops up a small reply-notification right when a matching SMS arrives, asking you to fill in one or more extra pieces of information. Without having to open the app.

Think of it as a quick "what was this for?" prompt that shows up the moment a transaction message lands.

It's entirely opt-in and off by default.

## How it works, end to end

1. You turn on **Settings → Notification Entry**.
2. Any config that defines a `notification_entry` list is automatically registered.
3. When an SMS arrives, Finxel checks whether the sender matches a registered config's `provider` list.
4. If it matches, and the message doesn't get filtered out by that config's `skips` rules, Finxel starts a short background service that:
   - resolves the SMS,
   - shows a notification with a text-reply action, using the first entry you defined,
   - includes a short preview of the message body (truncated to a few lines) so you remember what it's about.
5. You tap the notification, type your answer in the reply box, and send it.
6. Finxel saves your reply against that SMS, then if your config defines more than one `notification_entry` item, immediately shows the **next** entry's reply notification, one at a time, until all of them are answered.
7. Once every entry has a reply, you get a short "your entry has been saved successfully" confirmation notification instead.

If the notification is dismissed or ignored, no reply is saved and the entries stay unanswered; you can still fill them in later by processing the message from the **Home** tab if your layout collects the same information.

## Defining entries in a config

Add a `notification_entry` array to your config, listing every question you want to be prompted for, in the order they should appear:

```json
"notification_entry": [
  { "name": "purpose", "hint": "What was this purchase for?" },
  { "name": "category", "hint": "Which category? (food, bills, other)" }
]
```

| Key | Required | Description                                                                           |
|---|---|---------------------------------------------------------------------------------------|
| `name` | Yes | A unique identifier for this entry, you'll reference it later to read the saved reply |
| `hint` | Yes | The question/placeholder text shown inside the reply notification                     |

If `notification_entry` is omitted, this config simply never triggers a reply notification, even while the feature is on globally.

## Reading a saved reply in your config

Use the `NOTIFICATION_ENTRY_DATA` field type, with `key` set to the entry's `name`, to pull the typed reply into your extraction pipeline and, from there, into your `result` sheet:

```json
{
  "name": "purpose_field",
  "type": "NOTIFICATION_ENTRY_DATA",
  "key": "purpose"
}
```

Add a cell referencing `purpose_field` in your `result` grid the same way as any other field, see [write-configuration.md](write-configuration.md).

If the reply hasn't been submitted yet for a given message, this field simply comes back empty until you answer the notification.

## Permissions required

Turning the toggle on requests:

- `RECEIVE_SMS`, `READ_SMS`, `READ_CONTACTS`
- `POST_NOTIFICATIONS` (Android 13+)

See **[permissions.md](permissions.md)** for the full permission breakdown, plus the battery optimization and Xiaomi autostart prompts shown when this feature is enabled. Both help make sure the reply notification actually appears promptly when a message arrives while the app is in the background.

## Notes and limitations

- Entries are asked **one at a time**, in the order they're listed in `notification_entry`. You can't skip ahead to a later one.
- Skip rules (`skips`) still apply: if a message matches a skip rule, no entry notification is shown for it at all.
- Replies are stored locally in Finxel's database, keyed to the specific SMS they were collected for.
- This feature is independent of Location Service. You can enable either, both, or neither, per your needs.
