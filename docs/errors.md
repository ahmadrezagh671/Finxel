# Errors

Finxel shows most problems as a plain-language message right on the message card (for example, a skip rule's `error_text`, or "Invalid configuration format" when importing). Those are expected, everyday messages and aren't documented here.

This page covers Finxel's numbered **error codes**. Messages that mean something unexpected went wrong while processing a message, almost always because of a mistake in a configuration file rather than in the message itself.

Currently there is only one such code.

## Error 101

**Where you'll see it:** as a red "Error 101" tag on a message card in the **Home** tab, in place of the normal comparison alerts.

**What it means:** Finxel tried to run your config's `compare_with_last_message` rules on this message and something about that comparison failed unexpectedly.

Unlike a `skips` message (which is an intentional, expected outcome your config defined), Error 101 means Finxel hit a problem it wasn't expecting while comparing this message's data against the previous message's data. **This is not a normal or "safe" thing to see**. It means there's something wrong with your configuration that needs to be fixed, not something to ignore or work around.

### Why it happens

`compare_with_last_message` takes a field from the current message and a field from the previous message, converts both to numbers, and subtracts them. Error 101 is raised whenever that process breaks down. The most common causes are:

- **`compare_with_last_message` only works with `fields_before_click`.** It can't reference an entry from `fields`. Only fields defined under `fields_before_click` are available for comparison. If a `field` or `last_message_field` points at a name that only exists in `fields`, the comparison has nothing valid to read and Error 101 is raised. If you need to use `compare_with_last_message`, make sure the fields it needs are defined in `fields_before_click`, not just `fields`.
- **A field name typo.** The `field` or `last_message_field` value in `compare_with_last_message` doesn't exactly match a `name` defined in `fields_before_click`.
- **The referenced field didn't extract a value.** For example, a `REGEX_FIND_SMS_BODY` field whose regex didn't match this particular message, leaving nothing to compare.
- **The value isn't a valid number.** `compare_with_last_message` requires both values to parse as numbers. If the extracted text still contains a currency symbol, a thousands separator, or any non-numeric character (e.g. `"$1,250.00"` instead of `"1250.00"`), the comparison fails. Check your field's `replacements_after` rules to strip these out.
- **There's no valid "previous message" to compare against** in some rare edge cases, or the previous message's own field extraction failed for one of the reasons above.

### How to fix it

1. Open the config in question from **Settings → Saved configuration files → Edit Config**.
2. Check every entry under `compare_with_last_message` and confirm that `field` and `last_message_field` refer to fields defined in `fields_before_click`, not `fields`. And spell out real, existing field `name`s exactly.
3. Check that both referenced fields are designed to always produce a clean numeric string. Add or fix `replacements_after` rules to strip currency symbols, commas, spaces, or units.
4. Save your changes and pull down to refresh the Home tab. If the message still shows Error 101, double check the raw SMS text against your regex to make sure the field is actually matching.

See **[write-configuration.md](write-configuration.md#compare_with_last_message-optional)** for the full format of `compare_with_last_message`, and the **Field types** section for how to shape a clean numeric field with `replacements_after`.

---

There are currently no other numbered error codes in Finxel. If you run into a problem that doesn't match anything above, see the **[FAQ](faq.md)** or report it as an issue on [Finxel's GitHub Issues page](https://github.com/ahmadrezagh671/Finxel/issues).
