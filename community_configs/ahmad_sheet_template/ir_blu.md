# ir_blu.json

## The bank

**Blu (بلو)** — Iran
Sender IDs: `09999987641`, `+989999987641`, `Blu`
Website: https://blubank.com

## What a message looks like

A typical Blu transaction message looks like this:

```
بلو
واریز پول
رضا عزیز، 21,000,000 ریال به حساب شما نشست.
موجودی: 21,400,000 ریال
۱۵:۳۵
۱۴۰۴.۰۱.۲۰
```

The format is a fixed set of lines:
1. The brand name `بلو`, on its own line, on every message from this sender.
2. A short message-type line, `واریز پول` ("money deposited") or `برداشت پول` ("money withdrawn").
3. A sentence naming the amount and `ریال` (Iran's currency).
4. Balance `موجودی:` followed by the balance after this transaction, also in `ریال`.
5. A time line (`HH:MM`), written with **Persian digits** (`۰-۹`).
6. A date line (`YYYY.MM.DD`, Jalali calendar), also in Persian digits.

## Messages that are NOT transactions

Blu sends other notices through the same sender that this config has to recognize and ignore, most commonly feature/promotional announcements, for example:

```
بلو
گُلدن‌تایم 
هر روز از ساعت ۸ تا ۲۴ حتی در روزهای تعطیل در اپلیکیشن بلو طلا معامله کنید. 
https://example.ir/gold/
```

This is an ad for Blu's gold-trading hours, not a transaction. It doesn't mention an amount, a balance, or use the withdrawal/deposit wording at all.

## `information`

```json
"information": {
  "name": "Blu",
  "provider": ["09999987641", "+989999987641", "Blu"],
  "color": "#2F7BDC",
  "location": true,
  "app_version": "C01"
}
```

- `name` is `"Blu"`, the config's display name/ID inside Finxel.
- `provider` lists three possible sender identifiers Blu's messages might arrive under, `09999987641`, `+989999987641` (the same number with and without the country code), and the plain text `Blu`. Finxel matches a sender literally, so all the forms you've actually seen on your device should be listed.
- `color` is `#2F7BDC`, the accent color used by this config.
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

- **`amount_number`** a `REGEX_FIND_SMS_BODY` field (with `DOTALL` so the match can span multiple lines) capturing the number right before `ریال`, after `برداشت`/`واریز` appears earlier in the message, with commas stripped.
- **`amount_sign`** the same regex, but capturing the `برداشت`/`واریز` word itself instead of the number, then replaced with `-` or `+`.
- **`amount`** a `COMBINE_FIELDS` joining `amount_sign` and `amount_number` into one signed number string, e.g. `-1000000`.
- **`10000`** a constant `TEXT` field holding `"10000"`, used purely as a divisor.
- **`amount_divided_by_10000`** `amount` divided by `10000`, scaling the raw Rial amount down to the sheet template's units.
- **`balance_number`** a `REGEX_FIND_SMS_BODY` field capturing the number after `موجودی:`, with commas stripped.
- **`balance_divided_by_10000`** `balance_number` divided by `10000`.
- **`balance_before_transaction`** `balance_divided_by_10000` minus `amount_divided_by_10000`, the balance as it was right before this transaction, this is the field `compare_with_last_message` uses.

### `fields`

- **`date_day`** a `REGEX_FIND_SMS_BODY` field capturing just the two-digit day (`DD`) out of the `YYYY.MM.DD` line, anchored to the line that comes right after the `HH:MM` time line. Persian digits (`۰-۹`) are converted to regular digits first via `replacements_before`.
- **`date_h_m`** the same regex, but capturing the `HH:MM` group instead of the day, also with Persian-digit conversion.
- **`date_year_month`** a separate `REGEX_FIND_SMS_BODY` field capturing the `YYYY.MM` portion of the same date line, with the `.` replaced by `-`, this becomes the sheet's `Date` value.
- **`space`** a constant `TEXT` field holding a single space, used purely as a joining character.
- **`time`** a `COMBINE_FIELDS` joining `date_day`, `space`, and `date_h_m` into the final `dd hh:mm` string (e.g. `20 15:35`), matching the sheet template's `Time` format.
- **`description`** a `NOTIFICATION_ENTRY_DATA` field, pulling whatever you typed into the `description_entry` notification prompt (see `notification_entry` below), empty if you didn't answer one.
- **`bank_name`** a constant `TEXT` field holding `"Blu"`, used as the `Bank` value written to the sheet.
- **`layout_text`** a constant `TEXT` field holding the hint shown at the top of the confirmation sheet (`"Enter your Blu Bank transaction details."`).
- **`locationLatitude`** / **`locationLongitude`** `L_LATITUDE`/`L_LONGITUDE` fields, Finxel's built-in fields for the device's captured GPS coordinates (only populated if Location Service is on, see [location-service.md](../../docs/location-service.md)).
- **`comma`**, **`locationStartText`**, **`locationEndText`** small constant `TEXT` fields, just the literal pieces (`,`, the `=HYPERLINK("https://maps.google.com/maps?q=loc:`/`", "Location")` wrapper) needed to build a spreadsheet hyperlink formula.
- **`locationLink`** a `COMBINE_FIELDS` joining all the pieces above into one `=HYPERLINK(...)` formula string, with a `replacements_after` rule that swaps the whole thing for the plain text `"No Location"` if latitude/longitude ended up empty (i.e. no location was captured).

## `skips`

- Any message containing `رمز پویا` is skipped (dynamic password/one-time-code notices).
- Any message that does **not** contain `بلو` is skipped, this shouldn't normally trigger since the sender is dedicated to Blu, but guards against anything unrelated reaching this config.
- Any message that does **not** contain `پول` ("money") is skipped, meant to filter out non-transaction notices like the gold-trading ad above.

## `notification_entry`

```json
"notification_entry": [
  { "name": "description_entry", "hint": "Description" }
]
```

This defines one notification-reply prompt, `description_entry`, shown with the hint text `"Description"`. If you turn on the **Notification Entry** feature in Settings, Finxel will show this prompt right on the notification when a Blu message arrives, letting you type a quick note (e.g. what the purchase was for) without opening the app. Whatever you type is picked up by the `description` field in `fields` (a `NOTIFICATION_ENTRY_DATA` field referencing this same `description_entry` key), then flows into `layout` (the `ET_Description` box) and `result` (the sheet's `Description` column). If you don't answer the prompt, or don't have Notification Entry turned on, `description` just comes through empty and you can fill it in yourself on the confirmation sheet. See [notification-entry.md](../../docs/notification-entry.md) for the full format.

## `compare_with_last_message`

```json
"compare_with_last_message": [
  {
    "name": "Balance Check",
    "field": "balance_before_transaction",
    "last_message_field": "balance_divided_by_10000",
    "error_text": "Balance Mismatch: "
  }
]
```

This compares the current message's `balance_before_transaction` (what the balance should have been right before this transaction) against the **previous** message's `balance_divided_by_10000` (the balance it actually reported after its own transaction). If they don't match, you get a **Balance Mismatch** alert with the difference shown, telling you a transaction was likely missed or mis-parsed between the two messages.

## `layout`

The confirmation sheet shown when you tap a message has:

- A `TV` label showing the layout hint text (`"Enter your Blu Bank transaction details."`).
- A horizontal row with an `ET_Number` pre-filled from `amount_divided_by_10000` (editable) next to an `ET` pre-filled from `time`.
- An `ET` for `description`, pre-filled from the `NOTIFICATION_ENTRY_DATA`-based `description` field (empty unless Notification Entry is set up and answered).
- Three horizontal rows (`ActionLayout1/2/3`), each pairing an `ET_Number` (pre-filled from `amount_divided_by_10000` for the first row, blank for the second and third) with a `SPINNER` populated from the `ACTION` list, so you can split the transaction across up to three categories/people.

## `result`

One row is written to the sheet per confirmed message, pulling:

- **Price** ← `ET_amount` (the amount you confirmed on the layout)
- **Bank** ← the constant `"Blu"`
- **Date** ← the `date_year_month` field (extracted from the `YYYY.MM.DD` line, dash-separated)
- **Time** ← `ET_Time` (combining the extracted day and `HH:MM`)
- **Description** ← `ET_Description`
- **Action1/Action2/Action3** ← each pair's `ET_Amount_ActionN` and `Spinner_ActionN`, letting you split this transaction's value across up to three categories/people (see the `Data` sheet's `V`/`Action` columns in the [template README](README.md))
- **Location Link** ← the `locationLink` field, a clickable Google Maps link built from `L_LATITUDE`/`L_LONGITUDE`, or `"No Location"` if none was captured

## Quirks and things to know

- This config doesn't support the "you got charged" (`شارژ شدی`) top-up messages Blu sends, so those transactions won't show up automatically, if you use Blu's phone/internet top-up feature, you'll need to add those entries into the sheet by hand.
- Persian digits (`۰۱۲۳...۹`) are converted to normal digits before the date/time fields are parsed, since Blu writes its date/time lines with Persian numerals.
- The date fields only extract the **day** and the **year-month** separately (`date_day` and `date_year_month`), then combine them into `time` and `Date` respectively, rather than parsing the whole date as one block.
- This config needs both **Location Service** and **Notification Entry** turned on in Finxel's Settings to use all of its features, without Location Service on, the `Location Link` column will always show `"No Location"`; without Notification Entry on, you won't get the description prompt on the notification itself and will need to fill in `Description` manually on the confirmation sheet.

## Example SMS (15, sample/example data, no real personal information)

```
بلو
واریز پول
رضا عزیز، 21,000,000 ریال به حساب شما نشست.
موجودی: 21,400,000 ریال
۱۵:۳۵
۱۴۰۴.۰۱.۲۰
```

```
بلو
برداشت پول
رضا عزیز، 1,000,000 ریال از حساب شما پرید.
موجودی: 400,000 ریال
۱۸:۳۸
۱۴۰۴.۰۱.۲۰
```

```
بلو
واریز پول
رضا عزیز، 300,000 ریال به حساب شما نشست.
موجودی: 1,400,000 ریال
۱۲:۰۵
۱۴۰۴.۰۱.۱۵
```

```
بلو
برداشت پول
رضا عزیز، 300,000 ریال از حساب شما پرید.
موجودی: 1,100,000 ریال
۰۹:۲۴
۱۴۰۴.۰۱.۱۵
```

```
بلو
برداشت پول
رضا عزیز، 300,000 ریال از حساب شما پرید.
موجودی: 1,400,000 ریال
۱۴:۳۷
۱۴۰۴.۰۱.۱۱
```

```
بلو
گُلدن‌تایم 
هر روز از ساعت ۸ تا ۲۴ حتی در روزهای تعطیل در اپلیکیشن بلو طلا معامله کنید. 
https://example.ir/gold/
```

```
بلو
شارژ شدی
 رضا عزیز، 1,100,000 ریال بابت خرید شارژ از حساب شما پرید.
موجودی: 1,700,000 ریال
۱۰:۱۶
۱۴۰۴.۰۱.۰۷
```

```
بلو
برداشت پول
رضا عزیز، 2,500,000 ریال از حساب شما پرید.
موجودی: 2,800,000 ریال
۰۱:۵۱
۱۴۰۴.۰۱.۰۵
```

```
بلو
برداشت پول
رضا عزیز، 6,000,000 ریال از حساب شما پرید.
موجودی: 5,300,000 ریال
۲۲:۳۲
۱۴۰۴.۰۱.۰۴
```

```
بلو
برداشت پول
رضا عزیز، 500,000 ریال از حساب شما پرید.
موجودی: 11,300,000 ریال
۲۲:۲۲
۱۴۰۴.۰۱.۰۳
```

```
بلو
واریز پول
 رضا عزیز، 1,000,000 ریال به حساب شما نشست.
 موجودی: 11,800,000 ریال
۲۰:۰۹
۱۴۰۴.۰۱.۰۱
```

```
بلو
برداشت پول
رضا عزیز، 600,000 ریال از حساب شما پرید.
موجودی: 10,800,000 ریال
۱۹:۴۰
۱۴۰۴.۰۱.۰۱
```

```
بلو
واریز پول
رضا عزیز، 2,000,000 ریال به حساب شما نشست.
موجودی: 11,400,000 ریال
۲۱:۰۲
۱۴۰۳.۱۲.۲۹
```

```
بلو
برداشت پول
رضا عزیز، 1,000,000 ریال از حساب شما پرید.
موجودی: 9,400,000 ریال
۱۴:۱۰
۱۴۰۳.۱۲.۲۹
```

```
بلو
برداشت پول
رضا عزیز، 200,000 ریال از حساب شما پرید.
موجودی: 10,400,000 ریال
۲۳:۰۹
۱۴۰۳.۱۲.۲۷
```

**Author:** Ahmadrezagh671
