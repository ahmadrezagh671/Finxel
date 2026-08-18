# Writing a Configuration

A **configuration** ("config") is a single JSON file that tells Finxel:

1. Which SMS senders to watch (`information`).
2. Which messages to ignore (`skips`).
3. What data to pull out of each message (`fields`).
4. How to lay that data out as a sheet (`result`, `layout`).

Once imported (from Clipboard, from Files, or via GitHub see [getting-started.md](getting-started.md)), a config is saved to the app's internal storage and appears as a chip on the Home tab.

This page documents the full config format. If something isn't valid, Finxel will refuse to import it and show **"Invalid configuration format."** If a required top-level section is missing entirely, importing may fail with a JSON parsing error. See the checklist at the end of this page.

## Top-level structure

```json
{
    "information": { ... },
    "skips": [ ... ],
    "fields": [ ... ],
    "fields_before_click": [ ... ],
    "notification_entry": [ ... ],
    "compare_with_last_message": [ ... ],
    "spinner_list": [ ... ],
    "layout": [ ... ],
    "result": [ ... ]
}
```

| Section | Required? | Purpose                                                                                 |
|---|---|-----------------------------------------------------------------------------------------|
| `information` | **Required** | Name, senders, color, app_version, and whether this config uses location tagging        |
| `skips` | Optional | Rules for ignoring irrelevant messages from the same sender                             |
| `fields` | **Required** (can be an empty array `[]`) | The data extraction rules. The heart of the config                                      |
| `fields_before_click` | Optional | Fields computed before you open/confirm a message (e.g. a running balance)              |
| `notification_entry` | Optional | Extra manual-input fields collected via a reply notification                            |
| `compare_with_last_message` | Optional | Compares a numeric field from the current message with a numeric field from the previous message |
| `spinner_list` | Optional | Named lists of choices, reused by dropdown cells/layout components                      |
| `layout` | **Required** (can be an empty array `[]`) | The input form shown when you tap a message                                             |
| `result` | **Required** | The 2D grid describing the columns/rows of your sheet                                   |

> **Important:** `fields`, `layout`, and `result` must all be present in the JSON, even as empty arrays or the config will fail to parse when Finxel loads it. `skips`, `fields_before_click`, `notification_entry`, `compare_with_last_message`, and `spinner_list` can all be omitted entirely, if `skips` is left out, no messages are ever skipped for that config, they're all treated as valid transactions.

---

## `information` (required)

```json
"information": {
    "name": "MyBank",
    "provider": ["1234", "BankSMS"],
    "color": "#2196F3",
    "location": false,
    "app_version":"C01"
}
```

| Key | Required | Type | Description                                                                                                                                                                                                              |
|---|---|--|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `name` | Yes | string | The config's display name and its unique ID. Two configs can't share a name; Finxel will reject the import if one already exists.                                                                                        |
| `provider` | Yes | array of strings | The list of SMS sender IDs/numbers this config should react to. A sender is matched literally against the message's originating address.                                                                                 |
| `color` | Yes | string (hex color, e.g. `"#FF5722"`) | The accent color used for this config's chip and sheet.                                                                                                                                                                  |
| `location` | Yes | boolean | If `true`, this config's senders are automatically registered with the **Location Service** feature (when it's enabled in Settings) so matching messages get GPS-tagged. See [location-service.md](location-service.md). |
| `app_version` | Yes | string | The app's configuration version (e.g. `C01`). Use **only** the last three characters of the full app version. For example, if the app version is `0.4.8.C01`, set `app_version` to `C01`. |

---

## `skips` (optional)

Rules that let a message be ignored (and shown with an error/skip reason) instead of being processed as a transaction, useful for promotional texts, OTP codes, or any message from the same sender that isn't a real transaction.

If you don't need to filter anything out, you can leave `skips` out of the config entirely (or set it to `[]`). Every message from this config's `provider` list will then be treated as a valid transaction.

```json
"skips": [
    { "type": "CONTAINS", "action": "OTP", "error_text": "This is a one-time password, not a transaction" },
    { "type": "NOT_CONTAINS", "action": "transfer", "error_text": "This is not a transaction message" }
]
```

| Key | Required | Description |
|---|---|---|
| `type` | Yes | `"CONTAINS"` or `"NOT_CONTAINS"` whether the message body must contain or must *not* contain `action` to be skipped |
| `action` | Yes | The text to check for |
| `error_text` | Yes | Message shown on the card when this rule causes a skip |

Rules are checked in order; the first matching rule wins.

---

## `fields` (required, can be empty)

The list of values Finxel extracts from each message. Each entry becomes available (by `name`) to the `result` grid, to `layout`, and to other fields via `COMBINE_FIELDS`/`MATH_OPERATION`.

```json
{
    "name": "amount",
    "type": "REGEX_FIND_SMS_BODY",
    "regex": "(?:USD|\\$)\\s?([0-9,.]+)",
    "group": 1
}
```

### Common properties

| Key | Required | Description |
|---|---|---|
| `name` | Yes | Unique identifier for this field, referenced elsewhere in the config |
| `type` | Yes | One of the field types below |
| `regex` | Only for `REGEX_FIND_SMS_BODY` | The regular expression to run against the message body |
| `group` | Optional (default `0`) | The regex capture group to use as the result |
| `flags` | Optional | Array of regex flags (see below) |
| `format` | Only for date types | A Java `SimpleDateFormat` pattern, e.g. `"MMM dd, yyyy hh:mm a"` |
| `text` | Only for `TEXT` | The literal text to use |
| `key` | Only for `NOTIFICATION_ENTRY_DATA` | The name of the `notification_entry` item to pull the reply from |
| `operator` | Only for `MATH_OPERATION` | `"+"`, `"-"`, `"*"`, or `"/"` |
| `fields` | Only for `COMBINE_FIELDS` / `MATH_OPERATION` | Array of other field names to combine/operate on, in order |
| `replacements_before` | Optional, Only for `REGEX_FIND_SMS_BODY` | Find/replace pairs applied to the raw text *before* extraction |
| `replacements_after` | Optional, Only for `REGEX_FIND_SMS_BODY` and `COMBINE_FIELDS` | Find/replace pairs applied to the extracted value *after* extraction |

### Field types

| Type | What it does |
|---|---|
| `REGEX_FIND_SMS_BODY` | Runs `regex` against the SMS body and returns the matched `group` |
| `SMS_SENT_DATE` | The date/time the SMS was **sent**, formatted with `format` |
| `SMS_DATE` | The date/time the SMS was **received** on the device, formatted with `format` |
| `NOW_DATE` | The current date/time at extraction time, formatted with `format` |
| `SMS_NUMBER` | The sender's phone number / address |
| `COMBINE_FIELDS` | Concatenates the values of the fields listed in `fields`, in order, then applies `replacements_after` |
| `TEXT` | A fixed literal value (`text`) useful for constants or labels |
| `MATH_OPERATION` | Applies `operator` across the numeric values of the fields listed in `fields`, left to right |
| `L_LATITUDE` / `L_LONGITUDE` | The latitude/longitude previously captured by the **Location Service** for this message (empty if none was captured) |
| `NOTIFICATION_ENTRY_DATA` | The reply text the user typed for the `notification_entry` item named in `key` |

### Regex flags (optional array on a field)

`CASE_INSENSITIVE`, `MULTILINE`, `DOTALL`, `UNICODE_CASE`, `COMMENTS`, `LITERAL`, `UNICODE_CHARACTER_CLASS`, `CANON_EQ` these map directly to Java's `Pattern` flags.

### Replacements

```json
"replacements_after": [
    { "find": ",", "replace": "" }
]
```
Each entry needs `find` and `replace`. Multiple entries are applied in order. `find` is treated as a literal string (not a regex), so special characters don't need escaping.

### `fields_before_click`

Same format as `fields`, but these are computed **before** you tap into a message (for example, a balance shown directly on the card). Any field referenced by `name` elsewhere is looked up across both lists.

You'll typically reach for `fields_before_click` instead of `fields` for any value you want to compare against the previous message with `compare_with_last_message`, since that comparison can only read from `fields_before_click`. A running account balance is the classic example: you want to see it right on the card, *and* you want Finxel to flag it if it doesn't line up with the balance from the last message.

```json
"fields_before_click": [
    {
        "name": "balance",
        "type": "REGEX_FIND_SMS_BODY",
        "regex": "<your-regex>",
        "group": 1,
        "replacements_after": [
          { "find": ",", "replace": "" }
        ]
    }
],
"compare_with_last_message": [
    {
        "name": "balance_check",
        "field": "balance",
        "last_message_field": "balance",
        "error_text": "Balance Mismatch: "
    }
]
```

Here, `balance` is extracted and shown on the card *before* you open the message, and because it lives in `fields_before_click`, `compare_with_last_message` can use it to check this message's balance against the last one, catching a missed transaction or a parsing mistake before it ends up on your sheet.

---

## `compare_with_last_message` (optional)

Flags a numeric discrepancy between a field on the current message and a field on the previous (non-skipped) message from the same config, handy for spotting a balance that doesn't add up. `compare_with_last_message` is strictly for **numeric** fields, both the current and previous values must resolve to numbers, since the comparison works by subtracting one from the other.

```json
"compare_with_last_message": [
    {
        "name": "balance_check",
        "field": "balance",
        "last_message_field": "balance",
        "error_text": "Balance Mismatch: "
    }
]
```

| Key | Required | Description |
|---|---|---|
| `name` | Yes | Label for this comparison, shown with the alert |
| `field` | Yes | Name of the field on the **current** message to compare |
| `last_message_field` | Yes | Name of the field on the **previous** message to compare against |
| `error_text` | Yes | Text shown when the two values differ |

Both fields must resolve to valid numbers, or the comparison will fail. See [errors.md](errors.md) for what happens when that goes wrong (Error 101).

When the two values don't match, Finxel appends the actual **difference between them** right after your `error_text` so write `error_text` as a lead-in phrase, ending with a space or colon, rather than a full sentence. For example, `"error_text": "Balance Mismatch: "` shows up on the card as something like `Balance Mismatch: 25.00`.

That resulting difference is also **tappable**  a simple click on it in the message card copies it straight to your clipboard, so you can quickly paste it wherever you're investigating the mismatch.

---

## `notification_entry` (optional)

Defines extra data you'll be asked to type in via a reply notification when a matching message arrives (only used if the **Notification Entry** feature is turned on in Settings). See [notification-entry.md](notification-entry.md) for the full behavior.

```json
"notification_entry": [
    { "name": "purpose", "hint": "What was this for?" }
]
```

| Key | Required | Description |
|---|---|---|
| `name` | Yes | Identifier used to reference this entry from a `NOTIFICATION_ENTRY_DATA` field's `key` |
| `hint` | Yes | The placeholder text/question shown in the reply box |

---

## `spinner_list` (optional)

Named, reusable lists of options for dropdown-style layout components or result cells.

```json
"spinner_list": [
    { "name": "categories", "value": ["Food", "Transport", "Bills", "Other"] }
]
```

Reference a list elsewhere with `"items": "categories"`.

---

## `layout` (required, can be empty)

Describes the input form shown when you tap into a message. Text fields, spinners, and horizontal containers that can nest other components via `inside`.

```json
"layout": [
    {
        "id": "purpose",
        "type": "ET",
        "hint": "Purpose"
    },
    {
        "id": "category",
        "type": "SPINNER",
        "items": "categories",
        "selected": 0
    }
]
```

| Key | Required | Description                                                                                                                                |
|---|---|--------------------------------------------------------------------------------------------------------------------------------------------|
| `id` | Yes | Unique identifier for this component                                                                                                       |
| `type` | Yes | Component type, one of `TV`, `ET`, `ET_Number`, `SPINNER`, or `H_LinearLayout` (see below)                                                 |
| `hint` | Optional | Placeholder/label text (used by `ET`, `ET_Number`, and `SPINNER`)                                                                          |
| `value` | Optional | The **name of a `fields` entry**, its extracted value is looked up and used to pre-fill this component (used by `TV` and `ET`/`ET_Number`) |
| `items` | Optional | Name of a `spinner_list` entry to populate a `SPINNER`'s dropdown                                                                          |
| `selected` | Optional | Default selected index into `items`, for `SPINNER`                                                                                         |
| `inside` | Optional | Array of nested layout components (same structure), for `H_LinearLayout`                                                                   |

### Layout component types

| Type | What it renders | Notes                                                                                                                                                                                                                                                 |
|---|---|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `TV` | A plain text label (`MaterialTextView`) | Not editable text, `value` must name an existing `fields` entry, not a literal string.                                                                                                                                                                |
| `ET` | A single-line text input (`TextInputLayout` + `TextInputEditText`) | `hint` shows as the floating label. If `value` is set, the field pre-fills.                                                                                                                                                                           |
| `ET_Number` | Same as `ET`, but the keyboard/input type is restricted to signed decimal numbers | Same `hint`/`value` behavior as `ET`.                                                                                                                                                                                                                 |
| `SPINNER` | A dropdown (exposed dropdown menu: `TextInputLayout` + `MaterialAutoCompleteTextView`) | Options come from `items`, which must match a `spinner_list` entry's `name`. `selected` is the index into that list used as the initial choice; if omitted or out of range, the field starts empty. `hint` defaults to "Select an option" if not set. |
| `H_LinearLayout` | A horizontal row container | Doesn't display anything itself, lay out its children via `inside`. Children are spaced evenly (equal weight) with a small gap between them (none after the last child).                                                                              |

### Examples

A text input pre-filled from an extracted field:
```json
{
    "id": "note",
    "type": "ET",
    "hint": "Note",
    "value": "amount"
}
```

A numeric-only input with no pre-fill:
```json
{
    "id": "manual_amount",
    "type": "ET_Number",
    "hint": "Enter amount"
}
```

A read-only label showing an extracted value:
```json
{
    "id": "sender_label",
    "type": "TV",
    "value": "amount"
}
```

A dropdown using a `spinner_list` named `categories`, defaulting to the second option:
```json
{
    "id": "category",
    "type": "SPINNER",
    "hint": "Category",
    "items": "categories",
    "selected": 1
}
```

Two inputs placed side by side in one row:
```json
{
    "id": "row1",
    "type": "H_LinearLayout",
    "inside": [
        {
            "id": "amount_input",
            "type": "ET_Number",
            "hint": "Amount"
        },
        {
            "id": "category_input",
            "type": "SPINNER",
            "hint": "Category",
            "items": "categories"
        }
    ]
}
```

## `result` (required)

`result` is a **2D array of cells** an array of rows, and each row is an array of cell objects. This is what turns the values you extracted into an actual sheet. Every row in `result` is a *row template*, and every cell in that row is a *column definition* that says which value goes into that column and how it should look.

```json
"result": [
    [
        { "name": "Date", "value": "date", "type": "ET" },
        { "name": "Amount", "value": "amount", "type": "ET_Number", "f_color": "#D32F2F" },
        { "name": "Category", "value": "category", "type": "SPINNER", "items": "categories" }
    ]
]
```

### Cell properties

| Key | Required | Type | Description                                                                                                                                                                                                                                                   |
|---|---|---|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `name` | Yes | string | The cell/column label. This is what shows up in the "type here to edit" dialog title when you later tap the cell in the sheet, e.g. `"Type here to edit Amount"`.                                                                                             |
| `value` | Yes | string | The **key** used to look up this cell's content. See "Where `value` is looked up from" below. Can be `""` (an empty string) if you just want a blank/placeholder cell that isn't tied to any field or layout id, it will render as an empty cell in the sheet. |
| `type` | Yes | string | `"ET"`, `"ET_Number"`, or `"SPINNER"`. This controls what happens when you **tap an existing cell in the sheet later** to edit it. It is not just cosmetic.                                                                                                   |
| `b_color` | Optional | string (hex color) | Background color of the cell, e.g. `"#FFF3E0"`.                                                                                                                                                                                                               |
| `f_color` | Optional | string (hex color) | Text color of the cell, e.g. `"#D32F2F"`. If omitted, the cell uses the app's default text color.                                                                                                                                                             |
| `size` | Optional | number | Relative column width weight, defaults to `1`. A cell with `"size": 2` renders twice as wide as a cell with `"size": 1`. Only affects the header row's width, Finxel reads `size` from the **first** row of `result` when drawing column headers.             |
| `items` | Optional | string | Name of a `spinner_list` entry. Only meaningful when `type` is `"SPINNER"` it supplies the dropdown options shown when you tap the cell to edit it.                                                                                                          |

### Cell `type` in detail

`type` isn't just about how the cell looks, it decides which input widget Finxel shows you when you **tap a cell that's already in the sheet** to change its value:

| `type` | Editor shown when tapping the cell |
|---|---|
| `ET` | A plain text input |
| `ET_Number` | A numeric-only input (signed, decimal keyboard) |
| `SPINNER` | A dropdown populated from the `spinner_list` named in `items` |

So pick `ET_Number` for numeric columns (amounts, balances) and `SPINNER` for columns you want to be restricted to a fixed set of choices (categories, accounts, tags) even after the row has already been saved.

### Where `value` is looked up from

When a message is confirmed, Finxel builds one big map of `name -> value` before it fills in `result`. That map is assembled from, in order:

1. **`fields_before_click`** and **`fields`** every extracted/computed field, keyed by its `name`.
2. **`layout`** after you fill in and submit the confirmation sheet, every layout component's typed/selected value is added to the same map, keyed by its `id`. This includes `ET`, `ET_Number`, and `SPINNER` components (whatever you typed or picked), and also `TV` components (their pre-filled text).

Because both live in the same map, a `result` cell's `value` can reference **either a `fields`/`fields_before_click` name or a `layout` component's `id`**, Finxel doesn't care which list it came from, it just looks the key up. This is how you get manually-entered data (typed into the confirmation sheet form) into your sheet alongside automatically-extracted data.

```json
"fields": [
    { "name": "amount", "type": "REGEX_FIND_SMS_BODY", "regex": "Deposit:\\s*USD\\s?([0-9,.]+)", "group": 1 }
],
"layout": [
    { "id": "note", "type": "ET", "hint": "Note (optional)" }
],
"result": [
    [
        { "name": "Amount", "value": "amount", "type": "ET_Number" },
        { "name": "Note", "value": "note", "type": "ET" }
    ]
]
```
Here `"value": "amount"` pulls from `fields`, while `"value": "note"` pulls from what the user typed into the `note` layout component.

If `value` names something that doesn't exist in either `fields`/`fields_before_click` or `layout`, the cell will come out empty when the row is written.

### More than one row per message

`result` can contain **more than one row**, and every row you add is written as **its own row in the sheet, for every single message**. In other words, `result` isn't "one row = one column layout", it's "one row per template, and each incoming message writes out one sheet row for every template row you defined."

This is useful when a single SMS actually represents more than one logical line. For example, a bank transfer message that should log both the "transfer out" row and a "fee" row, or a purchase message you want to log once in a summary format and once with full raw details.

```json
"result": [
    [
        { "name": "Date", "value": "date", "type": "ET" },
        { "name": "Type", "value": "type_transfer", "type": "ET" },
        { "name": "Amount", "value": "amount", "type": "ET_Number" }
    ],
    [
        { "name": "Date", "value": "date", "type": "ET" },
        { "name": "Type", "value": "type_fee", "type": "ET" },
        { "name": "Amount", "value": "fee", "type": "ET_Number" }
    ]
]
```
With the config above, confirming **one** SMS appends **two** rows to the sheet: one for the transfer amount and one for the fee, both sharing the same `date` field but pulling their `Amount` from different fields (`amount` and `fee`).

The **first row** of `result` is special: besides being a data-row template, it also defines the sheet's **column headers** (the "A, B, C…" header strip uses each first-row cell's `size` for column widths). Every row after that should use the **same number of cells/columns** as the first row so the columns line up. Finxel doesn't enforce this, but a mismatched row count will make your sheet look misaligned.

`result` must contain at least one row for the sheet to show anything meaningful, and every row's cells must be objects (not raw strings).

---

## Full example: putting it all together

Say your bank, DemoBank, sends two kinds of SMS: purchase notifications and OTP codes. You want to skip the OTPs, log purchases, and track exactly where you made them by turning your device's GPS coordinates into a clickable Google Maps link. You also want to ensure no messages are missed by automatically calculating your pre-transaction balance (adding the spent amount back to your current balance) and comparing it against your last recorded message. Finally, you want to let yourself pick a spending category and add a note when you confirm each message, keeping the purchase amounts highlighted in red.

Example messages you might receive:

```
Your card ending 4321 was charged USD 42.50 at COFFEE HOUSE on Jan 05, 2026 09:14 AM. Available balance: USD 1,203.75
```
```
Your card ending 4321 was charged USD 15.00 at BOOKSTORE on Jan 06, 2026 10:30 AM. Available balance: USD 1,188.75
```
```
Your card ending 4321 was charged USD 80.00 at GROCERY MART on Jan 08, 2026 06:15 PM. Available balance: USD 1,108.75
```
```
Your OTP for login is 583910. Do not share this code with anyone.
```

Configuration that handles both:

```json
{
    "information": {
        "name": "DemoBank",
        "provider": ["DemoBank", "12345"],
        "color": "#2196F3",
        "location": true,
        "app_version":"C01"
    },
    "skips": [
        { "type": "CONTAINS", "action": "OTP", "error_text": "This is a one-time password, not a transaction" }
    ],
    "fields_before_click": [
    {
        "name": "balance",
        "type": "REGEX_FIND_SMS_BODY",
        "regex": "Available balance:\\s?USD\\s?([0-9,.]+)",
        "group": 1,
        "replacements_after": [
            { "find": ",", "replace": "" }
        ]
    },
    {
        "name": "amount",
        "type": "REGEX_FIND_SMS_BODY",
        "regex": "charged USD\\s?([0-9,.]+)",
        "group": 1,
        "replacements_after": [
            { "find": ",", "replace": "" }
        ]
    },
    {
        "name": "balance_before_transaction",
        "type": "MATH_OPERATION",
        "operator": "+",
        "fields": ["balance", "amount"]
    }
    ],
    "fields": [
    {
        "name": "merchant",
        "type": "REGEX_FIND_SMS_BODY",
        "regex": "at (.+?) on",
        "group": 1
    },
    {
        "name": "date",
        "type": "SMS_SENT_DATE",
        "format": "MMM dd, yyyy hh:mm a"
    },
    {
        "name": "locationLatitude",
        "type": "L_LATITUDE"
    },
    {
        "name": "locationLongitude",
        "type": "L_LONGITUDE"
    },
    {
        "name": "comma",
        "type": "TEXT",
        "text": ","
    },
    {
        "name": "locationStartText",
        "type": "TEXT",
        "text": "=HYPERLINK(\"https://maps.google.com/maps?q=loc:"
    },
    {
        "name": "locationEndText",
        "type": "TEXT",
        "text": "\", \"Location\")"
    },
    {
        "name": "locationLink",
        "type": "COMBINE_FIELDS",
        "fields": [
            "locationStartText",
            "locationLatitude",
            "comma",
            "locationLongitude",
            "locationEndText"
        ],
        "replacements_after": [
            {
                "find": "=HYPERLINK(\"https://maps.google.com/maps?q=loc:,\", \"Location\")",
                "replace": "No Location"
            }
        ]
    },
    {
        "name": "description",
        "type": "NOTIFICATION_ENTRY_DATA",
        "key": "purpose"
    }
    ],
    "notification_entry": [
        { "name": "purpose", "hint": "What was this purchase for?" }
    ],
    "compare_with_last_message": [
        {
            "name": "balance_check",
            "field": "balance_before_transaction",
            "last_message_field": "balance",
            "error_text": "Balance Mismatch: "
        }
    ],
    "spinner_list": [
        { "name": "categories", "value": ["Food", "Transport", "Bills", "Shopping", "Other"] }
    ],
    "layout": [
        {
            "id": "merchant_label",
            "type": "TV",
            "value": "merchant"
        },
        {
            "id": "row1",
            "type": "H_LinearLayout",
            "inside": [
                {
                    "id": "category",
                    "type": "SPINNER",
                    "hint": "Category",
                    "items": "categories",
                    "selected": 0
                },
                {
                    "id": "note",
                    "type": "ET",
                    "hint": "Note (optional)",
                    "value": "description"
                }
            ]
        }
    ],
    "result": [
        [
            { "name": "Date", "value": "date", "type": "ET", "size": 1.8 },
            { "name": "Merchant", "value": "merchant", "type": "ET", "size": 1.5 },
            { "name": "Amount", "value": "amount", "type": "ET_Number", "f_color": "#D32F2F" },
            { "name": "Balance", "value": "balance", "type": "ET_Number" },
            { "name": "Category", "value": "category", "type": "SPINNER", "items": "categories" },
            { "name": "Note", "value": "note", "type": "ET" },
            { "name": "Location", "value": "locationLink", "type": "ET", "size": 3 }
        ]
    ]
}
```

Walking through what happens when the purchase message arrives:
* `skips` checks the message for `"OTP"`, it doesn't match, so it's treated as a transaction.
* `fields_before_click` extracts both `balance` and `amount` so they are immediately available. It then runs a `MATH_OPERATION` to calculate `balance_before_transaction` (balance + amount).
* `compare_with_last_message` takes that computed `balance_before_transaction` and checks it against the previous message's `balance` to ensure no transactions were missed.
* Tapping the message runs `fields` to extract the `merchant` and `date`. It also pulls the device's current `L_LATITUDE` and `L_LONGITUDE`, wrapping them into a clickable Google Maps hyperlink (`locationLink`).
* The confirmation sheet (`layout`) shows the merchant as a label, plus a category dropdown and a note field side by side.
* On submit, `result` writes one row to the sheet, pulling data from the extracted fields, the calculated math operations, and the manual inputs you provided.

---

## Validation checklist before importing

- [ ] `information` has `name`, `provider` (array), `color`, `location`, and `app_version`.
- [ ] `fields`, `layout`, and `result` are all present (empty arrays are fine). `skips` may be omitted entirely if you don't need to filter anything out.
- [ ] Every entry in `fields` has at least `name` and `type`.
- [ ] `result` is a 2D array, an array of rows, each row an array of cell objects, each with `name`, `value` (can be `""`), and `type` (`ET`, `ET_Number`, or `SPINNER`).
- [ ] No other saved config already uses the same `information.name`.
- [ ] If you use `compare_with_last_message`, `COMBINE_FIELDS`, or `MATH_OPERATION`, double-check that every field name you reference actually exists in `fields`/`fields_before_click`. See [errors.md](errors.md) for what happens if it doesn't.

## Editing an existing configuration

You don't have to re-import a config to change it. From **Settings → Saved configuration files**, tap a config's menu and choose **Edit Config** to open it in Finxel's built-in JSON editor. Note that a config's **name cannot be changed** after it's created; delete and re-import it under a new name if you need to rename it.
