# Third-Party Notices

Finxel's own source code is licensed under the MIT License (see [LICENSE](LICENSE)). Finxel also depends on the third-party open-source library listed below, which is **not** covered by that MIT license and remains under its own original license terms.

## Sora Editor

- **Project:** [Rosemoe/sora-editor](https://github.com/Rosemoe/sora-editor)
- **Used for:** the in-app JSON editor used to create and edit configuration files (**Settings → Saved configuration files → Edit Config**)
- **Version used:** `0.24.6` (`io.github.rosemoe:editor`, via `io.github.rosemoe:editor-bom`)
- **License:** GNU Lesser General Public License v2.1 (LGPL-2.1)
- **Modifications:** none — Sora Editor is used unmodified, as a compiled library dependency pulled in via Gradle. No changes were made to its source code.

Sora Editor's own copyright notice, as included in its repository:

```
sora-editor - the awesome code editor for Android
https://github.com/Rosemoe/sora-editor
Copyright (C) 2020-2024 Rosemoe

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA
```

The full LGPL-2.1 license text is available at:
https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html

> This notice is provided for transparency and attribution. It is not legal advice — if you plan to redistribute, fork, or build on Finxel, review the LGPL-2.1 terms for the Sora Editor dependency yourself to confirm what applies to your use case.

## Visual Studio Code (JSON grammar and dark theme)

Finxel's config editor uses two files taken from Microsoft's open-source **[microsoft/vscode](https://github.com/microsoft/vscode)** repository ("Code - OSS"), used to provide JSON syntax highlighting and a dark color theme inside the editor:

- `app/src/main/assets/textmate/json/JSON.tmLanguage.json` — the JSON TextMate grammar, converted from [microsoft/vscode-JSON.tmLanguage](https://github.com/microsoft/vscode-JSON.tmLanguage) (see the file's own `information_for_contributors` header)
- `app/src/main/assets/textmate/dark_theme.json` — a dark color theme based on VS Code's built-in Dark Modern theme

Note that this refers only to the `microsoft/vscode` **source repository**, which is MIT licensed. It is separate from the compiled "Visual Studio Code" application distributed by Microsoft, which ships under its own proprietary product license — Finxel does not use or redistribute that product, only these two MIT-licensed source files.

- **License:** MIT License
- **Copyright:** Copyright (c) 2015 - present Microsoft Corporation
- **Modifications:** none to the JSON grammar file itself. The theme file has been adapted for use as a code-editor color scheme inside Finxel (colors/values only; no functional code).
- **Full license text:** https://github.com/microsoft/vscode/blob/main/LICENSE.txt

```
MIT License

Copyright (c) 2015 - present Microsoft Corporation

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

> `languages.json` is a small glue file written by Finxel's own developer to point the editor at the grammar file above — it isn't third-party content and doesn't need separate attribution.

## Firebase Analytics

- **Project:** [Firebase](https://firebase.google.com/) / [Google Play services](https://developers.google.com/android/guides/overview)
- **Used for:** collecting anonymous, aggregated app-usage analytics (e.g. screen views, session counts, crash/performance signals) to help understand how Finxel is used and to catch bugs. Analytics data does **not** include SMS content, configuration files, extracted records, sheets, or any other financial/personal data Finxel processes, those never leave the device.
- **Version used:** via `com.google.firebase:firebase-bom` (`34.16.0`) / `firebase-analytics`, and the `com.google.gms.google-services` Gradle plugin
- **License:** Firebase SDKs are distributed under the Apache License 2.0; the `google-services` plugin and Play services libraries are governed by [Google's Play Services / Firebase Terms of Service](https://firebase.google.com/terms).
- **Data handling:** Google's Firebase Privacy and Security policy applies to the anonymous usage data collected: https://firebase.google.com/support/privacy
- **Modifications:** none — used as an unmodified compiled dependency via Gradle.

> This notice is provided for transparency. If you fork Finxel and rebuild it with your own `google-services.json`, analytics data will be collected under your own Firebase project, not the original author's.