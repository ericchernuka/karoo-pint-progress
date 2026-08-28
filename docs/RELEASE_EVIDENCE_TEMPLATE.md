# Release evidence record

Copy this file for one release candidate. Replace every placeholder with evidence from the
candidate. Keep failed and waived rows in the completed record. Do not put private keys, passwords,
tokens, keystores, or other signing secrets in this file.

## Result definitions

- `PASS`: The check completed and the expected result is supported by the linked or attached evidence.
- `FAIL`: The check completed and the expected result was not met.
- `WAIVED`: An approver explicitly accepted that the check was not run or did not pass. Record the
  reason and approver.

## Candidate identity

| Field | Value |
| --- | --- |
| Commit SHA (exact) | `<40-character commit SHA>` |
| Clean-tree confirmation | `<clean-tree command and output or stable evidence link>` |
| Version name | `<version name>` |
| `versionCode` | `<integer versionCode>` |
| APK filename | `<exact APK filename>` |
| APK SHA-256 | `<64-character APK SHA-256>` |
| Signing certificate SHA-256 fingerprint | `<public certificate SHA-256 fingerprint only>` |
| CI URL | `<permalink to the successful CI run>` |
| Test date | `<YYYY-MM-DD>` |
| Tester | `<name or handle>` |
| Karoo model | `<Karoo model>` |
| Karoo software version | `<Karoo software version>` |

## Automated gate

| Check | Result (`PASS`, `FAIL`, or `WAIVED`) | Evidence | Notes |
| --- | --- | --- | --- |
| Drawable generation (`node tools/generate-drawables.mjs`) | `<PASS, FAIL, or WAIVED>` | `<command output or stable link>` | `<notes>` |
| Drawable validation (`node tools/validate-drawables.mjs`) | `<PASS, FAIL, or WAIVED>` | `<command output or stable link>` | `<notes>` |
| Full Gradle gate from `AGENTS.md` | `<PASS, FAIL, or WAIVED>` | `<CI job or command output>` | `<notes>` |
| Patch hygiene (`git diff --check`) | `<PASS, FAIL, or WAIVED>` | `<command output or stable link>` | `<notes>` |
| Security checklist from `docs/SECURITY.md` | `<PASS, FAIL, or WAIVED>` | `<checklist review or stable link>` | `<notes>` |

## Karoo device matrix

Run every row against the candidate. The normative source is [the Required device matrix](KAROO_DATA_FIELD_CONTRACT.md#required-device-matrix).

The cancellation rows marked **debug diagnostic** use a debug APK and are not release-candidate
evidence. Record the matching visible behavior separately against the signed candidate.

| Device check | Result (`PASS`, `FAIL`, or `WAIVED`) | Evidence | Notes |
| --- | --- | --- | --- |
| Preview and in-ride modes | `<PASS, FAIL, or WAIVED>` | `<photo, recording, or test record>` | `<notes>` |
| Live graphical count and mug share a readable physical scale in large, medium, short, narrow, and small tiles, boundaries off and on; visible digit curves have clear edge space | `<PASS, FAIL, or WAIVED>` | `<unobscured photo, recording, ViewConfig values, or test record>` | `<count raster, suffix baseline, mug balance, and fit notes>` |
| Left, center, and right alignment | `<PASS, FAIL, or WAIVED>` | `<photo, recording, or test record>` | `<notes>` |
| Boundaries off and on | `<PASS, FAIL, or WAIVED>` | `<photo, recording, or test record>` | `<notes>` |
| No completed mug, `1+`, `99+`, and `100+` | `<PASS, FAIL, or WAIVED>` | `<photo, recording, or test record>` | `<notes>` |
| Light and dark system themes | `<PASS, FAIL, or WAIVED>` | `<photo, recording, or test record>` | `<notes>` |
| Unavailable calories, normal fill, 80% foam, bubbles, and drain | `<PASS, FAIL, or WAIVED>` | `<photo, recording, or test record>` | `<notes>` |
| Count preview sequence; live `0.00`, `0.10`, `0.90`, and `1.00`; and a three-digit total in narrow and roomy tiles | `<PASS, FAIL, or WAIVED>` | `<photo, recording, or test record>` | `<notes>` |
| Graphical picker preview in named narrow layout: no count, `1+`, `99+`, and `100+` | `<PASS, FAIL, or WAIVED>` | `<recording plus ViewConfig values>` | `<notes>` |
| Graphical picker preview in named roomy layout: no count, `1+`, `99+`, and `100+` | `<PASS, FAIL, or WAIVED>` | `<recording plus ViewConfig values>` | `<notes>` |
| Numeric page-editor host placeholder: record the observed value with exact device and KOS evidence | `<PASS, FAIL, or WAIVED>` | `<recording or photo plus device/KOS evidence>` | `<host-owned value; not extension output>` |
| Numeric preview extension messages: `0.5`, `0.9`, `1`, `1.1` at one Hz | `<PASS, FAIL, or WAIVED>` | `<recording or log evidence>` | `<emitted-message evidence only; separate from host placeholder row>` |
| Graphical preview detach **debug diagnostic**: `cancellation label=graphical-preview` | `<PASS, FAIL, or WAIVED>` | `<debug logcat>` | `<direct callback evidence>` |
| Numeric preview detach **debug diagnostic**: `cancellation label=numeric-preview` | `<PASS, FAIL, or WAIVED>` | `<debug logcat>` | `<direct callback evidence>` |
| Graphical preview detach **signed candidate**: no later frame and clean re-entry | `<PASS, FAIL, or WAIVED>` | `<recording>` | `<visible post-detach and re-entry observation>` |
| Numeric preview detach **signed candidate**: no later frame and clean re-entry | `<PASS, FAIL, or WAIVED>` | `<recording>` | `<visible post-detach and re-entry observation>` |
| Default, minimum, maximum, and mid-ride calories-per-beer changes | `<PASS, FAIL, or WAIVED>` | `<photo, recording, or test record>` | `<notes>` |
| Install over the previous signed build without uninstalling | `<PASS, FAIL, or WAIVED>` | `<install output or test record>` | `<notes>` |

## Upgrade evidence

| Field | Value |
| --- | --- |
| Previous installed version | `<package, version name, and versionCode>` |
| Install-over result | `<PASS, FAIL, or WAIVED, and evidence link>` |
| Package name | `<io.ericchernuka.pintprogress>` |
| Version comparison | `<previous versionCode> -> <candidate versionCode>` |
| Signing identity match | `<PASS, FAIL, or WAIVED; compare public certificate fingerprints>` |

## Failures and waivers

| Item | Result | Reason, impact, and follow-up | Approver |
| --- | --- | --- | --- |
| `<failed or waived check, or None>` | `<FAIL or WAIVED>` | `<reason and follow-up, or None>` | `<name or handle, or N/A>` |

## Known limitations

`<Known limitations for this candidate, or None>`

## Release decision

| Field | Value |
| --- | --- |
| Decision | `<APPROVED, NOT APPROVED, or APPROVED WITH WAIVERS>` |
| Approver | `<name or handle>` |
| Decision date | `<YYYY-MM-DD>` |
| Decision notes | `<notes>` |
