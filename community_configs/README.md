# Community Configs

This folder is where the Finxel community shares **templates** and the bank/provider **configs** that go with them, all free for anyone to use.

This is also where the in-app **Add Config from GitHub** button (see [Getting Started](../docs/getting-started.md#5-add-your-first-configuration)) points to.

## What's a "template" here?

A template is *where your extracted data ends up*, for example a specific Google Sheets / Excel layout designed to receive Finxel's output, or a data format for feeding another app. Each template is made by someone in the community and shared as its own folder, containing the template itself plus one or more Finxel configs that are built to work with it.

## Folder structure

```
community_configs/
├── README.md                    ← this file
├── ahmad_sheet_template/
│   ├── README.md                ← explains the template, its creator, and lists its configs
│   ├── SHEET_TEMPLATE.xlsx      ← the actual template file
│   ├── ir_mellat.json           ← a bank config that outputs into this template
│   ├── ir_mellat.md             ← explains that config
│   ├── ir_blu.json
│   ├── ir_blu.md
│   ├── us_wells_fargo.json
│   └── us_wells_fargo.md
├── alex_sheet_template/
│   └── ...
└── x_app_data/
    └── ...
```

Every template folder is self-contained: its own `README.md`, its own template file (or whatever artifact the "template" is, doesn't have to be a spreadsheet), and its own set of config + explanation pairs.

## Naming rules

- **Config files** are named `<country-code>_<provider-name>.json`, with a matching `<country-code>_<provider-name>.md` next to it. Use lowercase ISO-style country codes, e.g. `ir_mellat.json` / `ir_mellat.md`, `us_wells_fargo.json` / `us_wells_fargo.md`.
- **Template folders** can be named however you like (e.g. `ahmad_sheet_template`), just keep it descriptive.
- **All `.md` files in this folder must be written in English**, this includes template READMEs and config explanation files, so everyone in the community can read them. This does **not** apply to text *inside* a config itself: `error_text`, labels, or any other in-app strings your config displays to users can be in whatever language makes sense for that provider's audience.

## What goes in each file

**Template folder's `README.md`:**
- What the template is and how it's meant to be used.
- Creator's name/details (optional, include as much or as little as you're comfortable sharing).
- A table listing every config in the folder, something like:

  | Config | Provider | Country | Author |
  |---|---|---|---|
  | `ir_mellat.json` | Bank Mellat | Iran | Ahmad |
  | `ir_blu.json` | Blu Bank | Iran | Ahmad |
  | `us_wells_fargo.json` | Wells Fargo | USA | Alex |

- If your template is a modified/upgraded version of someone else's template, **link to the original template's folder in this repo** in your README, so people can see what it's based on and find the source.

**Each config's `.md` file:**
- What the config does and which provider/message format it targets.
- Anything a user should know before importing it (quirks, edge cases, regions it does/doesn't cover).
- The config writer's name/details, if they want to include them.

**Each config's `.json` file:**
- A working Finxel configuration, see [write-configuration.md](../docs/write-configuration.md) for the full format.

## Before you contribute, read an existing example

Don't start from a blank page, open an existing template folder and one of its config `.md`/`.json` pairs first, and match that style and level of detail. It's the fastest way to get the format right and keeps things consistent across the folder.

## How to contribute

### Adding a new template

1. Create a new folder here with a descriptive name.
2. Add your template file and a `README.md` following the format above.
3. Add at least one working config that outputs into it.
4. Open a pull request.

### Adding a new config to an existing template

1. Make sure your config actually works with that template, test it on your own device first.
2. Add your `<country-code>_<provider>.json` and matching `.md` file into that template's folder.
3. Add a row for it in that template's README config table.
4. Open a pull request.

### If you used AI to help write a config or its explanation

Check it yourself before contributing. Read through the JSON and the `.md` file, run it against real messages, and confirm the extraction, regexes, and explanation are actually correct. Don't submit AI output you haven't personally verified.

### Fixing or improving someone else's template or config

If you think an existing template or config has a problem, you have two options:

- **Propose a fix:** open a pull request changing the existing files. The original creator needs to review and accept it before it's merged; it's their template/config.
- **Make your own edition:** if you'd rather not wait for approval, or your changes are a matter of preference rather than a bug, copy it and publish your own version instead, e.g. `ir_mellat_alex_edition.json` (and `.md`) in your own template folder, or as an addition alongside the original. No permission needed for this, since you're not modifying someone else's file, just adding your own.

### Building on someone else's template

If your template is an upgraded or modified version of another contributor's template, **specify a link to the original template's folder in your own `README.md`**, so people can trace it back and the original creator gets credit.

## What belongs here (and what doesn't)

Everything in `community_configs` should be **open and genuinely useful to anyone using Finxel**, not tied to one company or private use case.

If you're building a template or configs specifically for your own company, for internal use by your company's members only, you're welcome to do that, but it doesn't need to (and shouldn't) be shared here. Keep company-specific setups private and just distribute them internally however you'd like. This folder is for things you're making public and free for everyone, permanently, by contributing them here.

## A note on trust

Finxel doesn't validate or endorse community templates or configs beyond checking that submissions are well-formed. A config does not contain executable code or arbitrary network-access capabilities; it defines how Finxel matches, extracts, transforms, and displays message data. Still, review any config's `fields`/regex rules, and any template file, before importing or using it, especially from providers or creators you can't independently verify.
