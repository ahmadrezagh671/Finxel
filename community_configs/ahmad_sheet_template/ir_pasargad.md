# ir_pasargad.json

## The bank

**Bank Pasargad** (بانک پاسارگاد) — Iran. This config reacts to messages sent from the sender ID `B.Pasargad`.

Website: https://www.bpi.ir

## What a message looks like

A typical Bank Pasargad transaction message looks like this:

```
1404.2000.33333333.3
+18,500,000 
01/20_04:32
مانده: 311,272,000
```

The format is four lines:
1. The account number (e.g. `1404.2000.33333333.3`).
2. A signed amount line, `+` for money in or `-` for money out, already signed by the bank itself.
3. A date/time line in the form `MM/DD_HH:MM`, note there's no year in this line.
4. Balance (`مانده:`) followed by a space and the balance after this transaction.

## Messages that are NOT transactions

The same sender also pushes **mobile-banking login notices** every time you sign in to the Bank Pasargad app, for example:

```
ورود به موبایل بانک در تاریخ 1401/1/15-13:10 بانک پاسارگاد
```

("Logged into mobile bank on [date] Bank Pasargad") these have no amount or balance line and must be filtered out so they don't get treated as transactions.

## `information`

```json
"information": {
  "name": "Pasargad",
  "provider": ["B.Pasargad"],
  "color": "#E8922F",
  "location": true,
  "app_version": "C01"
}
```

- `name` is `"Pasargad"`, the config's display name/ID inside Finxel.
- `provider` is `["B.Pasargad"]`, the exact sender ID Finxel watches for.
- `color` is `#E8922F`, the accent color used by this config.
- `location` is `true`, so if you turn on **Location Service** in Finxel's Settings, messages from this config get GPS-tagged automatically.
- `app_version` is `"C01"`, the config version identifier. It is derived from the last three characters of the Finxel app version this config was built against.

## `spinner_list`

```json
"spinner_list": [
  { "name": "ACTION", "value": ["Entertainment", "Expenses", "Income", "Alex", "Dad", "Sis"] }
]
```

One reusable dropdown list, named `ACTION`, used by the `Spinner_Action1/2/3` components in `layout` and the matching cells in `result`. These sample values (`Entertainment`, `Expenses`, `Income`, `Alex`, `Dad`, `Sis`) match the [Ahmad Sheet Template](README.md)'s example data. **You will almost certainly want to change this list** to your own categories/people, edit the config (Settings → Saved configuration files → Edit Config) and replace the `value` array before using this seriously.

## `fields` and `fields_before_click`

`fields_before_click` contains values calculated when the message arrives; `fields` contains values calculated when the confirmation sheet is opened. See [Writing a Configuration](../../docs/write-configuration.md#fields-and-fields_before_click)

### `fields_before_click`

- **`10000`** a constant `TEXT` field holding `"10000"`, used purely as a divisor.
- **`amount`** a `REGEX_FIND_SMS_BODY` field matching any standalone signed number (`+`/`-` followed by digits, not touching a decimal point) in the message, with commas stripped. Since the bank already signs the number itself, no sign-replacement is needed here.
- **`balanceAmount`** a `REGEX_FIND_SMS_BODY` field matching `مانده:` followed by the balance number, with commas stripped.
- **`balance`** `balanceAmount` divided by `10000`, scaling the raw Rial amount down to the sheet template's units.
- **`amount_divided_by_10000`** the same `/10000` scaling applied to `amount`, the number that ends up on the sheet and the message card.
- **`balance_before_transaction`** `balance` minus `amount_divided_by_10000`, the balance as it was right before this transaction, this is the field `compare_with_last_message` uses.

### `fields`

- **`time`** a `REGEX_FIND_SMS_BODY` field capturing the `DD_HH:MM` portion of the date line (the part right after `MM/`), with `_` replaced by a space, giving the final `dd hh:mm` value shown on the confirmation card and written to the sheet.
- **`date_without_year`** a `REGEX_FIND_SMS_BODY` field capturing just the `MM` (month) portion of the date line, right before the `/DD_` part.
- **`year`** a constant `TEXT` field holding `"1405"`, a hardcoded placeholder since Bank Pasargad's SMS never include a year (see Quirks below).
- **`dash`** a constant `TEXT` field holding `"-"`, used purely as a joining character.
- **`date`** a `COMBINE_FIELDS` joining `year`, `dash`, and `date_without_year` into the final `Date` value (e.g. `1405` + `-` + `01` → `1405-01`).
- **`description`** a `NOTIFICATION_ENTRY_DATA` field, pulling whatever you typed into the `description_entry` notification prompt (see `notification_entry` below), empty if you didn't answer one.
- **`bank_name`** a constant `TEXT` field holding `"Pasargad"`, used as the `Bank` value written to the sheet.
- **`layout_text`** a constant `TEXT` field holding the hint shown at the top of the confirmation sheet (`"Enter your Pasargad Bank transaction details."`).
- **`locationLatitude`** / **`locationLongitude`** `L_LATITUDE`/`L_LONGITUDE` fields, Finxel's built-in fields for the device's captured GPS coordinates (only populated if Location Service is on, see [location-service.md](../../docs/location-service.md)).
- **`comma`**, **`locationStartText`**, **`locationEndText`** small constant `TEXT` fields, just the literal pieces (`,`, the `=HYPERLINK("https://maps.google.com/maps?q=loc:`/`", "Location")` wrapper) needed to build a spreadsheet hyperlink formula.
- **`locationLink`** a `COMBINE_FIELDS` joining all the pieces above into one `=HYPERLINK(...)` formula string, with a `replacements_after` rule that swaps the whole thing for the plain text `"No Location"` if latitude/longitude ended up empty (i.e. no location was captured).

## `skips`

- Any message containing `موبایل` ("mobile") is skipped, this catches the login notices, which always mention "mobile bank" (`موبایل بانک`).
- Any message that does **not** contain `مانده:` is skipped, real transaction messages always include a balance line with this exact label (note the colon), so this is the main transaction/non-transaction filter.
- Any message containing `بانک` ("bank") is skipped, a second, broader net that also catches login notices and any other administrative message that mentions the bank's name.

## `notification_entry`

```json
"notification_entry": [
  { "name": "description_entry", "hint": "Description" }
]
```

This defines one notification-reply prompt, `description_entry`, shown with the hint text `"Description"`. If you turn on the **Notification Entry** feature in Settings, Finxel will show this prompt right on the notification when a Pasargad message arrives, letting you type a quick note (e.g. what the purchase was for) without opening the app. Whatever you type is picked up by the `description` field in `fields` (a `NOTIFICATION_ENTRY_DATA` field referencing this same `description_entry` key), then flows into `layout` (the `ET_Description` box) and `result` (the sheet's `Description` column). If you don't answer the prompt, or don't have Notification Entry turned on, `description` just comes through empty and you can fill it in yourself on the confirmation sheet. See [notification-entry.md](../../docs/notification-entry.md) for the full format.

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

This compares the current message's `balance_before_transaction` (what the balance should have been right before this transaction) against the **previous** message's `balance` (the balance it actually reported after its own transaction). If they don't match, you get a **Balance Mismatch** alert with the difference shown, telling you a transaction was likely missed or mis-parsed between the two messages.

## `layout`

The confirmation sheet shown when you tap a message has:

- A `TV` label showing the layout hint text (`"Enter your Pasargad Bank transaction details."`).
- A horizontal row with an `ET_Number` pre-filled from `amount_divided_by_10000` (editable) next to an `ET` pre-filled from `time`.
- An `ET` for `description`, pre-filled from the `NOTIFICATION_ENTRY_DATA`-based `description` field (empty unless Notification Entry is set up and answered).
- Three horizontal rows (`ActionLayout1/2/3`), each pairing an `ET_Number` (pre-filled from `amount_divided_by_10000` for the first row, blank for the second and third) with a `SPINNER` populated from the `ACTION` list, so you can split the transaction across up to three categories/people.

## `result`

One row is written to the sheet per confirmed message, pulling:

- **Price** ← `ET_amount` (the amount you confirmed on the layout)
- **Bank** ← the constant `"Pasargad"`
- **Date** ← the combined `year` (hardcoded `"1405"`) + `-` + `MM` date field
- **Time** ← `ET_Time`
- **Description** ← `ET_Description`
- **Action1/Action2/Action3** ← each pair's `ET_Amount_ActionN` and `Spinner_ActionN`, letting you split this transaction's value across up to three categories/people (see the `Data` sheet's `V`/`Action` columns in the [template README](README.md))
- **Location Link** ← the `locationLink` field, a clickable Google Maps link built from `L_LATITUDE`/`L_LONGITUDE`, or `"No Location"` if none was captured

## Quirks and things to know

- **The fixed `1405` year is a placeholder you must update.** Bank Pasargad's SMS never include a 4-digit year, only `MM/DD`, so this config hardcodes the current Iranian year as a `TEXT` field (`year`). As soon as the Iranian calendar rolls over to a new year, edit this config (Settings → Saved configuration files → Edit Config) and update that `TEXT` field's value, otherwise every message logged after the rollover will carry the wrong year in its `Date`.
- The amount regex matches **any** standalone signed number in the message, so it works whether the number appears before or after other text, but it also means a message with more than one signed number in it (which doesn't currently happen in real Bank Pasargad transaction messages) could extract the wrong one. Keep an eye on this if Bank Pasargad ever changes their message format.
- Because two of the three skip rules (`موبایل` and `بانک`) both catch login notices, they're redundant with each other on purpose. It's a safety net in case Bank Pasargad phrases a future non-transaction message slightly differently.
- Since login notices are skipped and never touch `balance`/`balance_before_transaction`, they don't disrupt the balance chain, only real transactions affect it.

## Example SMS (15, sample/example data, no real personal information)

```
1404.2000.33333333.3
+18,500,000 
01/20_04:32
مانده: 311,272,000
```

```
1404.2000.33333333.3
+17,000,000 
01/19_04:47
مانده: 292,772,000
```

```
1404.2000.33333333.3
+12,500,000 
01/18_04:41
مانده: 275,772,000
```

```
1404.2000.33333333.3
+11,000,000 
01/17_04:18
مانده: 263,272,000
```

```
1404.2000.33333333.3
+11,300,000 
01/16_13:42
مانده: 252,272,000
```

```
1404.2000.33333333.3
-28,000 
01/15_13:51
مانده: 240,972,000
```

```
1404.2000.33333333.3
-140,000,000 
01/15_13:51
مانده: 241,000,000
```

```
ورود به موبایل بانک در تاریخ 1401/1/15-13:10 بانک پاسارگاد
```

```
ورود به موبایل بانک در تاریخ 1401/1/15-12:01 بانک پاسارگاد
```

```
1404.2000.33333333.3
+27,500,000 
01/15_04:42
مانده: 381,000,000
```

```
ورود به موبایل بانک در تاریخ 1401/1/14-13:00 بانک پاسارگاد
```

```
1404.2000.33333333.3
+26,000,000 
01/14_04:23
مانده: 353,500,000
```

```
1404.2000.33333333.3
+8,500,000 
01/13_13:58
مانده: 327,500,000
```

```
1404.2000.33333333.3
+11,000,000 
01/12_04:42
مانده: 319,000,000
```

```
1404.2000.33333333.3
+8,000,000 
01/11_04:25
مانده: 308,000,000
```

**Author:** Ahmadrezagh671
