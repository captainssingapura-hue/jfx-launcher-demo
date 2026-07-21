# Windows registry work — `fxsuite-<env>://` protocol handlers

**Scope:** every registry change needed to let a web page launch the suite, per
environment. Nothing here requires administrator rights.

---

## 1. TL;DR — three values per environment

For an environment id `<env>` (e.g. `prod`, `uat`, `dev1`):

| # | Key | Value | Type | Data | Required |
|---|-----|-------|------|------|:--------:|
| 1 | `HKCU\Software\Classes\fxsuite-<env>` | `(Default)` | `REG_SZ` | `URL:FxSuite (<env>)` | convention |
| 2 | `HKCU\Software\Classes\fxsuite-<env>` | `URL Protocol` | `REG_SZ` | *(empty string)* | **yes** |
| 3 | `HKCU\Software\Classes\fxsuite-<env>\shell\open\command` | `(Default)` | `REG_SZ` | the launch command (§3) | **yes** |

The intermediate `shell` and `shell\open` keys are created implicitly and hold no values.

> **The one people miss:** the empty **`URL Protocol`** value. It is the flag that marks the
> key as a *URL scheme* rather than a file association. Its data is irrelevant — it only has
> to exist. Without it, Windows ignores the scheme entirely.

---

## 2. Verified live example

```
HKEY_CURRENT_USER\Software\Classes\fxsuite-prod
    (Default)       REG_SZ    URL:FxSuite (prod)
    URL Protocol    REG_SZ

HKEY_CURRENT_USER\Software\Classes\fxsuite-prod\shell
HKEY_CURRENT_USER\Software\Classes\fxsuite-prod\shell\open
HKEY_CURRENT_USER\Software\Classes\fxsuite-prod\shell\open\command
    (Default)       REG_SZ    "C:\Program Files\Java\jdk-25.0.2\bin\javaw.exe" -jar "Q:\…\prod\master-launcher.jar" --env=prod "%1"
```

---

## 3. The command value

```
"<absolute path to javaw.exe>" -jar "<absolute path to master-launcher.jar>" [--env=<id>] [--base=<url>] "%1"
```

- **`%1`** is substituted by Windows with the entire clicked URL
  (`fxsuite-prod://launch/hello?tok=…`). **Keep it quoted** — the URL can contain characters
  that would otherwise split into multiple arguments.
- **Use `javaw.exe`, not `java.exe`** — `java.exe` flashes a console window on every launch.
- **Absolute paths only.** There is no working directory guarantee when the shell invokes it.
- `--env` / `--base` are how *multiplexed* dev environments reuse one binary (§4).

---

## 4. One registration per environment

Only the scheme name and the arguments differ:

| Environment | Key | Command target | Extra args |
|---|---|---|---|
| Production | `…\Classes\fxsuite-prod` | `…\fxsuite\prod\master-launcher.jar` | `--env=prod` |
| UAT | `…\Classes\fxsuite-uat` | `…\fxsuite\uat\master-launcher.jar` | `--env=uat` |
| dev1 | `…\Classes\fxsuite-dev1` | `…\fxsuite\dev\master-launcher.jar` | `--env=dev1 [--base=…]` |
| devN | `…\Classes\fxsuite-devN` | **same** `…\fxsuite\dev\master-launcher.jar` | `--env=devN [--base=…]` |

Singleton environments point at their own dedicated install; every dev environment points at
the one shared dev install. Adding a dev environment is therefore *only* a new registration.

**Scheme naming:** lowercase; stick to letters, digits and `-` (valid URI scheme syntax). Pick
a prefix unlikely to collide with anything already registered on the machine.

---

## 5. Applying the changes

### Via the launcher (what we do)

```bash
java -jar dist/fxsuite/prod/master-launcher.jar --register   --env=prod
java -jar dist/fxsuite/dev/master-launcher.jar  --register   --env=dev1 --base=https://dev1.internal
java -jar dist/fxsuite/prod/master-launcher.jar --unregister --env=prod
java -jar dist/fxsuite/prod/master-launcher.jar --list        # all registered environments
java -jar dist/fxsuite/prod/master-launcher.jar --prune       # drop registrations whose jar is gone
```

### The `.reg` file it imports

```reg
Windows Registry Editor Version 5.00

[HKEY_CURRENT_USER\Software\Classes\fxsuite-prod]
@="URL:FxSuite (prod)"
"URL Protocol"=""

[HKEY_CURRENT_USER\Software\Classes\fxsuite-prod\shell\open\command]
@="\"C:\\Program Files\\Java\\jdk-25.0.2\\bin\\javaw.exe\" -jar \"Q:\\…\\master-launcher.jar\" --env=prod \"%1\""
```

**Escaping rules inside a `.reg` string:** `\` → `\\` and `"` → `\"`. The file must be saved
**UTF‑16LE with a BOM** for the `Version 5.00` header. Import with `reg import file.reg`.

> **Why a `.reg` file rather than `reg add /d …`?** The command value contains both spaces and
> embedded quotes. Passing that as a single argument from a spawned process cannot survive
> Windows argv quoting — it arrives at `reg.exe` mangled. (Typed by hand at a `cmd` prompt with
> careful escaping it can work; programmatically it is unreliable.) A `.reg` file has
> unambiguous escaping rules, so we generate one and import it.

### Removing

```bash
reg delete "HKCU\Software\Classes\fxsuite-prod" /f
```

---

## 6. Verifying

```bash
reg query "HKCU\Software\Classes\fxsuite-prod" /s          # the whole subtree
reg query "HKCU\Software\Classes\fxsuite-prod\shell\open\command" /ve
cmd /c start "" "fxsuite-prod://launch/hello"              # exercise the handler end-to-end
```

*(In Git Bash, escape the switches: `//s`, `//ve` — a single `/s` is mangled into a path.)*

Diagnostics for what the launcher did with the URL: `%LOCALAPPDATA%\fxsuite\<env>\launch.log`.

---

## 7. Hive choice

| Hive | Scope | Admin? |
|---|---|:--:|
| `HKCU\Software\Classes` | current user — **what we use** | no |
| `HKLM\Software\Classes` | all users on the machine | yes |
| `HKEY_CLASSES_ROOT` | *merged view* of the two (HKCU wins) | — |

**Never write to `HKCR` directly** — it's a view, not a real hive; writes land in HKLM and need
admin. Per-user registration under HKCU is what keeps this install free of IT involvement.

---

## 8. What happens on a click

1. The browser sees an unknown scheme and asks the shell to open the URL.
2. Windows looks up `HKCU\Software\Classes\fxsuite-<env>`, sees `URL Protocol`, and reads
   `shell\open\command`.
3. It substitutes `%1` with the full URL and starts the process directly (`CreateProcess`) —
   **no `cmd.exe`, no script** is involved.
4. The launcher parses and verifies the URL, then spawns the app.

---

## 9. Browser behaviour

- On first use per scheme, Chrome/Edge/Firefox show an **"Allow this site to open
  fxsuite-&lt;env&gt;?"** prompt. The user can tick "always allow" for that site. No registry
  entry of ours affects this — it is the browser's own per-origin permission.
- Each distinct scheme prompts separately, so several environments mean several one-time
  prompts.
- **Optional, admin-only:** Chrome/Edge can suppress the prompt for named origins via the
  `AutoLaunchProtocolsFromOrigins` policy under
  `HKLM\SOFTWARE\Policies\Google\Chrome` (or `…\Microsoft\Edge`). This needs GPO/admin, so it
  cuts against the no‑IT goal — treat it as a nice-to-have, not a requirement.

---

## 10. Troubleshooting

| Symptom | Cause |
|---|---|
| Click does nothing, no prompt | Scheme not registered, or `URL Protocol` value missing |
| Prompt appears, then nothing happens | Bad path in the command, or the jar was moved/deleted (`--prune` finds these) |
| Console window flashes | Command uses `java.exe` — switch to `javaw.exe` |
| Launcher reports a truncated / empty URL | `%1` not quoted in the command value |
| "serves environment X but the link was for Y" | The scheme's command has the wrong `--env`, or the wrong install was registered |
| Registration appears to succeed but nothing changes | Wrote to `HKCR`/`HKLM` without admin, or edited a different user's hive |

---

## 11. Security note

`HKCU` is **writable by the user (and by anything running as them)**. Malware could repoint
`fxsuite-<env>\shell\open\command` at its own executable, and the launcher would never run to
notice. This is a known, accepted limitation of the per-user model — see the trust-model
discussion in [`multi-env-simple-launcher.md`](multi-env-simple-launcher.md): the local
launcher and its registration are trusted, and protecting them is an endpoint-hygiene concern.
Machine-wide registration under `HKLM` (admin-only, therefore not user-writable) is the
hardening step if that threat ever becomes in-scope.
