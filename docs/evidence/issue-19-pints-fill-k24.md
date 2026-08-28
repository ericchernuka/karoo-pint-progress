# Issue 19 hardware evidence

This template-based record covers the Pints Fill checks in Issue 19 for commit
`a65ab03e020a94efdde9ba372c7348c6bf874eda`. It is a limited hardware record, not the completed
release record required by `docs/RELEASE_EVIDENCE_TEMPLATE.md`. The full release matrix must still
be copied and completed before this candidate moves to `main`.

The original candidate below was superseded after Issue 18 changed the Pints Fill artwork. Its
screenshots remain historical evidence only. They do not prove the updated artwork.

## Revalidation after the Issue 18 artwork change

| Field | Value |
| --- | --- |
| Product source commit | `e48c5ce8976b3457879141718e960bd692c10cf7` |
| Version | `1.0.4-e48c5ce`, versionCode `61` |
| APK SHA-256 | `41c90702e49c3f259e01165dc2ae2e57df9a602ec3363e67d9bebcb4ccebf86b` |
| Signing certificate SHA-256 fingerprint | `c2f7efec9475dd185d922f6a7ee0e43468ea8e73802b6f66c51c4368f08d8b58` |
| Upgrade result | PASS; `adb install -r` upgraded versionCode `60` to `61` without an uninstall |
| Automated gate | PASS; drawable validation and the full 191-task Gradle gate completed successfully |
| Dark left-aligned artwork | PASS; [mirrored texture and dark bubbles](issue-19/left-dark-mirrored-texture.png) |

The device capture confirms that the left-aligned count stays unchanged, the complete fill artwork
is mirrored, both long highlights move to the right, and dark-mode bubbles remain amber-brown. The
rest of the Issue 19 device matrix is pending for this build. Until that matrix runs again, the
release decision remains `NOT APPROVED`.

## Result definitions

- `PASS`: The check completed and the result has recorded evidence.
- `FAIL`: The check completed and the expected result was not met.
- `WAIVED`: An approver accepted a check that was not run or did not pass.

## Candidate identity

| Field | Value |
| --- | --- |
| Commit SHA (exact) | `a65ab03e020a94efdde9ba372c7348c6bf874eda` |
| Clean-tree confirmation | `git status --short` had no output before the signed build |
| Version name | `1.0.4-a65ab03` |
| `versionCode` | `59` |
| APK filename | `pint-release.apk` |
| APK SHA-256 | `684d0a9b0638606590686e5102f9e8cf84d31e994356c92d591f1eb38de44715` |
| Signing certificate SHA-256 fingerprint | `c2f7efec9475dd185d922f6a7ee0e43468ea8e73802b6f66c51c4368f08d8b58` |
| CI URL | [Verify job 98878997062](https://github.com/ericchernuka/karoo-pint-progress/actions/runs/33180138583/job/98878997062) |
| Test date | `2026-08-28` |
| Tester | Codex; Eric Chernuka confirmed the physical night-brightness result |
| Karoo model | Karoo 3, hardware identifier `k24`, serial `redacted` |
| Karoo software version | KOS `1.650.2509`; build `SKQ1.230210.001 dev-keys` |

## Automated gate

| Check | Result | Evidence | Notes |
| --- | --- | --- | --- |
| Drawable generation (`node tools/generate-drawables.mjs`) | PASS | Command completed with exit code 0 | The tree stayed clean. |
| Drawable validation (`node tools/validate-drawables.mjs`) | PASS | `Drawable visual contracts passed` | Generated resources and static contracts passed. |
| Full Gradle gate from `AGENTS.md` | PASS | `BUILD SUCCESSFUL`; 191 tasks | Ran `:lib:testDebugUnitTest`, `:pint:testReleaseUnitTest`, `:pint:lintDebug`, both assemblies, and JaCoCo behavior coverage. |
| Patch hygiene (`git diff --check`) | PASS | Command completed with no output | Checked before and after device work. |
| Security checklist from `docs/SECURITY.md` | PASS | Manifest, dependency, verification metadata, permission, loader, URL, and credential checks | No product source, dependency, permission, or signing material entered the candidate diff. |

## Issue 19 Karoo matrix

| Device check | Result | Evidence | Notes |
| --- | --- | --- | --- |
| Karoo 2 and Karoo 3 where available | PASS | `adb devices -l` and device properties showed one connected `k24` | Karoo 3 was available and tested. No Karoo 2 was available. |
| Light and dark page-editor modes | PASS | [Dark editor halo](issue-19/dark-editor-halo.png), [light size matrix](issue-19/light-size-matrix.png) | The count, fill, foam, and host chrome remained readable. |
| Light and dark active-ride modes | PASS | [Dark active unavailable state](issue-19/dark-active-unavailable.png), [light active ride](issue-19/left-boundaries-on.png) | The signed candidate rendered in both system themes during controlled rides. |
| Boundaries on and off | PASS | [Left with boundaries on](issue-19/left-boundaries-on.png), [center with boundaries off](issue-19/center-boundaries-off.png) | Device logs also recorded `boundaries=true` and `boundaries=false`. |
| Left, center, and right alignment | PASS | Device captures and `ViewConfig` logs | Logs recorded `LEFT`, `CENTER`, and `RIGHT`; the count and unavailable em dash moved with the host setting. |
| Narrow and roomy field sizes | PASS | [Light size matrix](issue-19/light-size-matrix.png) and device logs | Observed 30 x 15 at 238 x 148 px and 50 sp, 60 x 25 at 478 x 243 px and 96 sp, and 60 x 60 at 478 x 642 px and 96 sp. |
| Signed candidate exact values `0` and `1` | PASS | [Dark editor halo with 0](issue-19/dark-editor-halo.png), [full foam with 1](issue-19/full-foam.png) | The signed candidate preview emitted these values through the production `RemoteViews` path. |
| Signed candidate exact values `99` and `100` | FAIL | No safe signed-candidate calorie source was available | The candidate bytes did not receive these inputs. Release review stays blocked unless the check runs or an approver records a waiver. |
| Debug diagnostic exact values `0`, `1`, `99`, and `100` | PASS | [Exact 99](issue-19/exact-99.png), [exact 100](issue-19/exact-100.png), [full foam with 1](issue-19/full-foam.png) | A temporary debug-only calibration sequence used the production `RemoteViews` renderer. It proves physical fit, not candidate-input behavior. The calibration code was removed before the candidate was restored. |
| Signed candidate unavailable, 50%, 80%, full foam, and draining | PASS | [Dark unavailable state](issue-19/dark-active-unavailable.png), [full foam](issue-19/full-foam.png), [draining](issue-19/draining.png) | The signed preview and ride paths rendered these states without clipping. |
| Signed candidate 95% input | FAIL | No safe signed-candidate calorie source was available | [Diagnostic 95%](issue-19/diagnostic-95.png) proves physical rendering only. |
| Debug diagnostic 50%, 80%, and 95% inputs | PASS | [50%](issue-19/diagnostic-50.png), [80%](issue-19/diagnostic-80.png), [95%](issue-19/diagnostic-95.png) | These retained captures came from the temporary calibration APK. |
| Target control values | PASS | On-device settings checks | The device accepted 80, 240, 400, and reset 150 kcal values. The original 180-kcal setting was restored. |
| Signed candidate live target change behavior | FAIL | The Karoo calorie stream stayed unavailable during the controlled ride | Runtime tests pass, but this candidate behavior was not observed at the device boundary. |
| Signed candidate dropout recovery and multi-pint jump | FAIL | No safe signed-candidate data source produced these transitions | Runtime tests cover direct recovery and conflated jumps. The diagnostic APK proves only the resulting frame fit. |
| Dark halo over fill boundary and foam | PASS | [Dark editor halo](issue-19/dark-editor-halo.png) | The white count stayed readable across beer, foam, and empty surface. No backing plate was added. |
| Night brightness | PASS | [Brightness 15 capture](issue-19/night-brightness.png) and physical display check | Eric Chernuka confirmed that the white `0` was readable over the dark beer and foam at system brightness 15. |
| Foam separation and clipping | PASS | Dark, light, narrow, and roomy captures | Foam remained distinct from beer. No fill, foam, count, or halo was clipped. |
| Hidden header behavior | PASS | Pints Fill editor and active-ride captures | Pints Fill did not add a field header inside its `RemoteViews`. |
| Picker order | PASS | [Picker order](issue-19/picker-order.png), [expanded Pints Fill card](issue-19/pints-fill-picker-card.png) | The order was Pints, Pints Fill, Pints Count. |
| Shared icon | PASS | Expanded picker card, extension metadata, and resource validation | Pints Fill stays in the Pint Progress group and uses the same declared pint icon resource. |
| Preview detach diagnostic | PASS | Sanitized log excerpt below | Leaving the Pints Fill editor invoked `cancellation label=fill-preview`. Re-entry started a clean preview. |
| Live detach diagnostic | PASS | Sanitized log excerpt below | Ending the controlled ride invoked `cancellation label=fill-live` for all three visible Pints Fill fields. The runtime tests prove that the same job owns pending transition frames. |
| Literal field removal during live work | FAIL | Karoo does not expose profile field editing during an active ride | Ride teardown proved host detachment, but the literal field-removal action was not run while live work existed. |
| Install over previous signed build without uninstalling | PASS | `adb install -r` returned `Success` | Updated `1.0.4-debug.4478ca3` versionCode 58 to `1.0.4-a65ab03` versionCode 59 with the same certificate. |

## Upgrade evidence

| Field | Value |
| --- | --- |
| Previous installed version | `io.ericchernuka.pintprogress`, `1.0.4-debug.4478ca3`, versionCode 58 |
| Install-over result | PASS; `adb install -r pint-release.apk` returned `Success` |
| Package name | `io.ericchernuka.pintprogress` |
| Version comparison | `58 -> 59` |
| Signing identity match | PASS; both APKs used certificate `c2f7efec9475dd185d922f6a7ee0e43468ea8e73802b6f66c51c4368f08d8b58` |

## Debug diagnostic log excerpts

The detach diagnostic used the exact candidate source in the normal debug build, which enables
`BuildConfig.DEBUG` logging. It did not contain the temporary calibration stream. The separate
calibration APK added that stream to drive fixed visual states. These lines came from the detach
diagnostic and are callback evidence only. They are separate from signed-candidate observations.

```text
08-28 10:17:54.963 D/PintProgressField(8417): grid=(30, 15) viewPx=(238, 148) textSp=50 alignment=RIGHT boundaries=true preview=true style=FILL
08-28 10:17:58.466 D/PintProgressField(8417): cancellation label=fill-preview
08-28 10:19:21.357 D/PintProgressField(8417): cancellation label=fill-live
08-28 10:19:21.365 D/PintProgressField(8417): cancellation label=fill-live
08-28 10:19:21.373 D/PintProgressField(8417): cancellation label=fill-live
```

## Failures and waivers

| Item | Result | Reason, impact, and follow-up | Approver |
| --- | --- | --- | --- |
| Signed candidate `99` and `100` inputs | FAIL | Only debug calibration evidence exists. Run a controlled signed-candidate source or obtain an explicit waiver. | N/A |
| Signed candidate 95%, live target change, dropout recovery, and multi-pint jump | FAIL | Local tests pass, but the device did not receive these inputs from the signed candidate. | N/A |
| Literal live field removal | FAIL | Ride teardown invoked live cancellation, but literal profile field removal was not available during the ride. | N/A |

## Known limitations

Only a Karoo 3 was available. The device run covers `RemoteViews`, Karoo sizing, themes, host
settings, picker behavior, update signing, and detach callbacks. The temporary calibration run
covers physical fit for otherwise unsafe inputs. It does not replace signed-candidate transition
evidence.

## Release decision

| Field | Value |
| --- | --- |
| Decision | `NOT APPROVED` |
| Approver | N/A |
| Decision date | `2026-08-28` |
| Decision notes | The recorded signed-candidate gaps block Issue 19 completion and release approval. No backing plate was added. |
