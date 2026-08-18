# ir_mellat.json

## The bank

**Bank Mellat** (بانک ملت) is one of Iran's largest commercial banks. This config reacts to messages sent from the sender ID `Bank Mellat`, no phone number is used, Bank Mellat's SMS come through this named sender directly.

Website: https://www.bankmellat.ir

## What a message looks like

A typical Bank Mellat transaction message looks like this:

```
حساب9111111112
برداشت10,000,000
مانده38,650,000
04/01/11-12:50
```

The format is four lines, with no space between each Persian label and the number that follows it:
1. Account (`حساب`) followed directly by the masked account number.
2. Withdrawal/Deposit (`برداشت`) or (`واریز`)  followed directly by the amount.
3. Remaining balance (`مانده`) followed directly by the balance after this transaction.
4. A date/time line in the form `YY/MM/DD-HH:MM`, using the short two-digit Iranian year (Bank Mellat's SMS messages don't include the century).

## Messages that are NOT transactions

Bank Mellat sends two other kinds of messages from the same sender that this config has to recognize and ignore:

- **Dynamic password (`رمز پویا`) messages** sent before certain purchases, showing the merchant and amount but no account/balance line, just a one-time code notice. These are not real transaction confirmations.
- **Subsidy (`یارانه`) notices**, these are technically real deposits, but they don't follow the normal transaction message structure, so this config skips them rather than risk misreading them. If you want to log a subsidy deposit, you'll need to add that entry into the sheet by hand.

## `information`

```json
"information": {
  "name": "Mellat",
  "provider": ["Bank Mellat"],
  "color": "#b82318",
  "location": true,
  "app_version": "C01"
}
```

- `name` is `"Mellat"`, the config's display name/ID inside Finxel.
- `provider` is `["Bank Mellat"]`, the exact sender ID Finxel watches for. This is the config's only trigger. If your carrier shows a different sender ID for Bank Mellat's texts, you'll need to add it here.
- `color` is `#b82318`, the accent color used by this config.
- `location` is `true`, so if you turn on **Location Service** in Finxel's Settings, messages from this config get GPS-tagged automatically.
- `app_version` is `"C01"`, the config version identifier. It is derived from the last three characters of the Finxel app version this config was built against.

## `spinner_list`

```json
"spinner_list": [
  { "name": "ACTION", "value": ["Entertainment", "Expenses", "Income", "Alex", "Dad", "Sis"] }
]
```

This defines one reusable dropdown list, named `ACTION`, used by the `Spinner_Action1/2/3` components in `layout` and the matching cells in `result`. The values (`Entertainment`, `Expenses`, `Income`, `Alex`, `Dad`, `Sis`) are just sample categories/people matching the [Ahmad Sheet Template](README.md)'s example data. **You will almost certainly want to change this list** to match your own spending categories and the actual people you split transactions with, edit the config (Settings → Saved configuration files → Edit Config) and replace the `value` array with your own list before using this seriously.

## `fields` and `fields_before_click`

`fields_before_click` contains values calculated when the message arrives; `fields` contains values calculated when the confirmation sheet is opened. See [Writing a Configuration](../../docs/write-configuration.md#fields-and-fields_before_click)

### `fields_before_click`

- **`10000`** a constant `TEXT` field holding `"10000"`, used purely as a divisor.
- **`amount`** a `REGEX_FIND_SMS_BODY` field matching `برداشت` or `واریز` immediately followed by a number, then replacing the Persian word with `-` or `+` respectively, so the result is a signed number string like `-10000000`.
- **`balanceAmount`** a `REGEX_FIND_SMS_BODY` field matching `مانده` followed by the balance number, with commas stripped.
- **`balance`** a `MATH_OPERATION` dividing `balanceAmount` by `10000`, this scales the raw Rial amount down to match the units used across the sheet template (so `10,000,000` becomes `1,000`).
- **`amount_divided_by_10000`** the same `/10000` scaling applied to `amount`, this is the number that actually ends up on the sheet and in the message card.
- **`balance_before_transaction`** a `MATH_OPERATION` subtracting `amount_divided_by_10000` from `balance`, giving you what the balance *was* right before this transaction happened. This is the field `compare_with_last_message` uses to cross-check against the previous message.

### `fields`

- **`time`** a `REGEX_FIND_SMS_BODY` field capturing the `DD HH:MM` portion of the date/time line, used for the `Time` value shown on the confirmation card and written to the sheet.
- **`date_without_century`** a `REGEX_FIND_SMS_BODY` field capturing the `YY/MM` portion of the date line (the part in `YY/MM/DD-HH:MM`), with `/` replaced by `-`.
- **`century`** a constant `TEXT` field holding `"14"`, since Bank Mellat's SMS use the short two-digit Iranian year and never send the century.
- **`date`** a `COMBINE_FIELDS` joining `century` and `date_without_century`, producing the final `Date` value (e.g. `14` + `04-01` → `1404-01`, written to the sheet's `Date` column, see `result` below).
- **`description`** a `NOTIFICATION_ENTRY_DATA` field, pulling whatever you typed into the `description_entry` notification prompt (see `notification_entry` below), empty if you didn't answer one.
- **`bank_name`** a constant `TEXT` field holding `"Mellat"`, used as the `Bank` value written to the sheet.
- **`layout_text`** a constant `TEXT` field holding the hint shown at the top of the confirmation sheet (`"Enter your Mellat Bank transaction details."`).
- **`locationLatitude`** / **`locationLongitude`** `L_LATITUDE`/`L_LONGITUDE` fields, Finxel's built-in fields for the device's captured GPS coordinates (only populated if Location Service is on, see [location-service.md](../../docs/location-service.md)).
- **`comma`**, **`locationStartText`**, **`locationEndText`** small constant `TEXT` fields, just the literal pieces (`,`, the `=HYPERLINK("https://maps.google.com/maps?q=loc:`/`", "Location")` wrapper) needed to build a spreadsheet hyperlink formula.
- **`locationLink`** a `COMBINE_FIELDS` joining all the pieces above into one `=HYPERLINK(...)` formula string, with a `replacements_after` rule that swaps the whole thing for the plain text `"No Location"` if latitude/longitude ended up empty (i.e. no location was captured).

## `skips`

- Any message containing `رمز پویا` is skipped (it's a one-time-code notice, not a transaction).
- Any message that does **not** contain `حساب` is skipped, real transaction messages always start with this word, so anything missing it isn't a standard transaction.
- Any message containing `یارانه` is skipped (subsidy notices).

## `notification_entry`

```json
"notification_entry": [
  { "name": "description_entry", "hint": "Description" }
]
```

This defines one notification-reply prompt, `description_entry`, shown with the hint text `"Description"`. If you turn on the **Notification Entry** feature in Settings, Finxel will show this prompt right on the notification when a Mellat message arrives, letting you type a quick note (e.g. what the purchase was for) without opening the app. Whatever you type is picked up by the `description` field in `fields` (a `NOTIFICATION_ENTRY_DATA` field referencing this same `description_entry` key), then flows into `layout` (the `ET_Description` box) and `result` (the sheet's `Description` column). If you don't answer the prompt, or don't have Notification Entry turned on, `description` just comes through empty and you can fill it in yourself on the confirmation sheet. See [notification-entry.md](../../docs/notification-entry.md) for the full format.

## `compare_with_last_message`

```json
"compare_with_last_message": [
  {
    "name": "Balance Check",
    "field": "balance_before_transaction",
    "last_message_field": "balance",
    "error_text": "Balance Mismatch: "
  }
]
```

This compares the current message's `balance_before_transaction` (this message's balance minus this message's own amount, i.e. what the balance should have been *before* this transaction) against the **previous** message's `balance` (the balance it actually reported *after* its own transaction). If those two numbers don't match, Finxel shows a **Balance Mismatch** alert with the difference appended, meaning a transaction was likely missed or mis-parsed somewhere between the two messages.

## `layout`

The confirmation sheet shown when you tap a message has:

- A `TV` label showing the layout hint text (`"Enter your Mellat Bank transaction details."`).
- A horizontal row with an `ET_Number` pre-filled from `amount_divided_by_10000` (editable) next to an `ET` pre-filled from `time`.
- An `ET` for `description`, pre-filled from the `NOTIFICATION_ENTRY_DATA`-based `description` field (empty unless Notification Entry is set up and answered).
- Three horizontal rows (`ActionLayout1/2/3`), each pairing an `ET_Number` (pre-filled from `amount_divided_by_10000` for the first row, blank for the second and third) with a `SPINNER` populated from the `ACTION` list, letting you split the transaction across up to three categories/people.

## `result`

One row is written to the sheet per confirmed message, pulling:

- **Price** ← `ET_amount` (the amount you confirmed on the layout)
- **Bank** ← the constant `"Mellat"`
- **Date** ← the combined `14` + `YY-MM` date field
- **Time** ← `ET_Time`
- **Description** ← `ET_Description`
- **Action1/Action2/Action3** ← each pair's `ET_Amount_ActionN` and `Spinner_ActionN`, letting you split this transaction's value across up to three categories/people (see the `Data` sheet's `V`/`Action` columns in the [template README](README.md))
- **Location Link** ← the `locationLink` field, a clickable Google Maps link built from `L_LATITUDE`/`L_LONGITUDE`, or `"No Location"` if none was captured

## Quirks and things to know

- The amount and balance regexes assume **no space** between the Persian label and the number, this matches Bank Mellat's real formatting exactly; if a future message format adds a space, the regex will need updating.
- This config isn't tied to a specific masked account number, so if you have more than one Bank Mellat account, messages from all of them will come through this one config together. The masked account number (e.g. `حساب9111111112`) is visible in the raw message but isn't currently extracted into its own field, add one yourself if you need to separate accounts.

## Example SMS (15, sample/example data, no real personal information)

```
حساب9111111112
برداشت10,000,000
مانده38,650,000
04/01/11-12:50
```

```
محرمانه!
خرید از
گیفت
مبلغ 10,000,000
رمز پویا 62222
```

```
محرمانه!
خرید از
گیفت
مبلغ 8,000,000
رمز پویا 52222
```

```
حساب9111111112
برداشت5,000,000
مانده48,650,000
04/01/11-12:47
```

```
محرمانه!
خرید از
همراه
مبلغ 5,000,000
رمز پویا 22222
```

```
حساب9111111112
برداشت300,000
مانده53,650,000
04/01/11-09:49
```

```
حساب9111111112
واریز6,500,000
مانده53,950,000
04/01/10-23:07
```

```
حساب9111111112
برداشت1,700,000
مانده47,450,000
04/01/10-22:58
```

```
حساب9111111112
برداشت5,000,000
مانده49,150,000
04/01/10-19:19
```

```
محرمانه!
خرید از
اپل استور   
مبلغ 5,000,000
رمز پویا 82222
```

```
حساب9111111112
واریز50,000,000
مانده54,150,000
04/01/10-18:57
```

```
حساب9111111112
برداشت5,500,000
مانده4,150,000
04/01/10-14:47
```

```
محرمانه!
خرید از
اپل استور   
مبلغ 5,500,000
رمز پویا 72222
```

```
حساب9111111112
برداشت350,000
مانده9,650,000
04/01/10-09:34
```

```
حساب9111111112
برداشت1,750,000
مانده10,000,000
04/01/09-18:15
```

**Author:** Ahmadrezagh671
