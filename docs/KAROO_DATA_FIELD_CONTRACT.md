# Karoo data-field contract

This document is the implementation contract for Pint Progress. It is based on Karoo extension
SDK 1.1.9 and Android's `RemoteViews` and app-update requirements.

## `ViewConfig` mapping

| Karoo input | Official meaning | Pint Progress behavior | Verification |
| --- | --- | --- | --- |
| `gridSize` | Column span × row span on a 60 × 60 grid | Used only as a fallback for a missing `viewSize` dimension | `CorePolicyBehaviorTest` |
| `viewSize` | Current configured view size in physical pixels | Takes precedence, converts to dp, and bounds the complete layout | `CorePolicyBehaviorTest`, on-device size matrix |
| `textSize` | Standard numeric field size for this grid size in sp | Supplies the graphical normal-scale reference; roomy Android 12+ fields can expand the complete count-and-mug group to 2×; Karoo applies it directly to the native count field | `CorePolicyBehaviorTest`, on-device text matrix |
| `alignment` | User-selected in-ride alignment | Selects a left, center, or right XML layout for the complete group | resource validator, `assembleDebug`, on-device alignment matrix |
| `boundariesEnabled` | Whether the user enabled field boundaries | Adds a larger inset before fitting or rendering content | `CorePolicyBehaviorTest` |
| `preview` | Page editing rather than an active ride | Cycles representative mug frames or native count messages without using ride calories | `PintDataFieldRuntimeTest`, on-device page editor |

## Rendering rules

1. Karoo's reported `viewSize` is authoritative when positive. Grid-derived size is a defensive
   fallback for a missing dimension, not a second competing layout signal.
2. Regular and compact mugs preserve their vector aspect ratio and use 66 × 89 dp and 48 × 65 dp
   as normal-scale maximums. For a count-bearing frame, the final count size supplies one physical scale for the
   counter and the mug. The `RemoteViews` applies scaled maximum bounds to the mug `ImageView` after
   the text-size, width, height, and font-scale caps are known. Compact is the minimum live
   count-and-mug treatment, so small and short fields scale the complete group instead of removing
   the count. The no-count adaptive mug remains independent of counter sizing.
3. Before the first completed mug, and while data is unavailable, the live field uses the adaptive
   mug-only treatment. It must not keep an invisible counter's fixed-size count layout.
4. The picker preview cycles through production 50%, 80%, and full-with-bubbles frames, then
   emits completed counts of `1`, `99`, and `100`. No-count frames keep the `PICKER` mug-only
   treatment. Count-bearing frames resolve `REGULAR` or `COMPACT` from the actual `ViewConfig`
   dimensions. The loop emits one frame per second and is cancelled on detach.
5. Once a completed count exists, the largest count-and-mug treatment that fits both dimensions is
   used. `COMPACT` is the floor when neither nominal treatment fits. Width fitting solves for the
   count, suffix, and scaled mug as one group. Height fitting preserves each treatment's nominal
   count-to-mug ratio and reserves visible-glyph clearance in constrained regular fields. The final
   normal scale cannot exceed Karoo's `textSize` or the nominal treatment. A roomy field can expand
   the complete group to 2× on Android 12 or later. Older hosts keep the normal scale because their
   `RemoteViews` API cannot assign larger image layout dimensions. Measured glyph bounds supply the
   optical offset; a baseline translation is not used as a substitute for size clearance.
6. Every `RemoteViews` update is constructed from a fresh instance and resets every property it
   relies on. Alignment is part of the selected XML layout because reflective calls such as
   `setGravity` are not a reliable cross-process contract on Karoo.
7. Graphical view and native numeric stream updates are spaced at least one second apart. Cancellation
   stops collection and pending animation frames when Karoo detaches the field.
8. `showHeader = true` asks Karoo to render the standard data-type icon and compact `Pints` label.
   Pint Progress does not duplicate that chrome inside its `RemoteViews` or ask Karoo to render its
   numeric value through `formatDataTypeId`.
9. The full-with-bubbles frame may extend its foam crown slightly above the mug rim. The crown stays
   within the vector viewport and mug width, while every normal fill state remains inside the glass.

## Pints Fill field

`Pints Fill` is a separate graphical data type with type ID `pint-progress-fill`. It appears between
`Pints` and `Pints Count`, uses the existing pint icon, shares the graphical runtime and global
calorie target, and requests `showHeader = false`.

- Twenty generated steady assets fill the field from bottom to top in 5% steps.
- Every nonzero fill has a foam cap. The cap becomes deeper from the 80% bucket.
- Available progress shows the exact completed-pint count, including `0`, without a suffix.
- Unavailable progress uses the neutral surface and an em dash.
- The initial picker preview repeats the 50% steady frame with `0` at one Hz without using ride
  calories. The complete transition and preview sequence are owned by issue 16.
- Each update uses a fresh `RemoteViews`, and detaching the field cancels its shared runtime job.

## Native count field

`Pints Count` is a standard numeric data type, selected independently from `Pints` in Karoo's
data-field picker. The choice is per tile and per ride profile, not a global appearance preference.
Both types consume the same calorie stream and calories-per-beer setting.

- The field publishes a single numeric value through `startStream`. Karoo owns the native numeric
  treatment, including its icon, header, sizing, alignment, and boundaries.
- In page-editor preview mode, `startView` cycles `0.5`, `0.9`, `1`, and `1.1` through
  `ShowCustomStreamState` at one Hz. The SDK defines this event as an alternate message in the
  standard stream container. The page editor may keep a host-owned numeric placeholder. Its value
  is host- and KOS-version-dependent; record the observed value with exact device and KOS
  evidence. The host placeholder is not part of the extension stream and is separate from the
  emitted messages. Detaching the preview cancels the loop.
- The live value is floored to 0.1-pint increments from the existing 5% progress buckets. This
  yields `0.00` at 5%, `0.10` at 10%, `0.90` at 95%, and `1.00` only at a full pint after Karoo
  applies its native Variability Index formatter.
- SDK 1.1.9 has no custom numeric precision API. Variability Index is the available dimensionless
  two-decimal formatter, but using it for pints is host-dependent and requires the device value
  matrix below for each Karoo software release.
- Idle, searching, and unavailable calorie states remain native stream states. Invalid calorie
  values become unavailable rather than entering the numeric formatter.
- Distinct stream values are conflated and emitted no more than once per second. Detaching the
  field cancels its calorie and setting collection.
- The count field does not create or update `RemoteViews`. This avoids a second, incomplete copy of
  Karoo's numeric measurement and baseline behavior.

## Calories-per-beer setting

Karoo extension SDK 1.1.9 exposes only the six `ViewConfig` inputs above. It does not provide a
public schema for an extension to add a custom slider to Karoo's data-field editor or a field-instance
identifier for per-tile preferences. Pint Progress therefore provides one global setting in its
launcher activity:

- 150 kcal by default;
- 80–400 kcal in 5 kcal steps;
- app-private Android preferences, with no permission or external storage;
- a live Flow into each visible field;
- a new steady transition baseline when changed, preventing a false full/drain animation.

The setting changes calorie-to-progress conversion only. It does not alter any `ViewConfig`
semantics, Karoo's cumulative calorie source, or the one-Hz update limit.

## Installation and update rules

Android treats an APK as an update only when its package name and signing certificate match the
installed app. A lower `versionCode` is a downgrade and is rejected. Pint Progress therefore needs:

- the stable package name `io.ericchernuka.pintprogress`;
- a monotonically increasing `versionCode` for each changed distributable build;
- one private, project-owned signing key for every distributable build;
- no keystore, passwords, or base64 key material committed to this repository.

Karoo's optional delivery manifest follows the same model: `latestVersionCode` is numeric and must
increase so Karoo can identify an update. A public release is incomplete until the APK, delivery
manifest, version metadata, commit, and SHA-256 all agree.

## Required device matrix

Local tests cannot reproduce Karoo's host process or page editor. Before moving a candidate to
`main`, test on Karoo with:

- preview and in-ride modes;
- large, medium, short, narrow, and small tiles; completed counts remain visible in every live
  graphical tile, with boundaries off and on;
- left, center, and right alignment;
- boundaries off and on;
- no completed mug, `1+`, `99+`, and `100+`;
- light and dark system themes;
- unavailable calories, normal fill, 80% foam, bubbles, and drain;
- count preview sequence, live `0.00`, `0.10`, `0.90`, `1.00`, and a three-digit total in narrow and roomy tiles;
- graphical picker preview sequence: no count, `1+`, `99+`, and `100+`; confirm the `99+` to `100+`
  transition remains readable in both narrow and roomy tiles;
- graphical and numeric preview detach; use the debug cancellation diagnostic and record direct
  callback evidence separately from visible post-detach and re-entry observations;
- default, minimum, maximum, and mid-ride calories-per-beer changes;
- install over the previous signed build without uninstalling.

Record each candidate run with the [release evidence template](RELEASE_EVIDENCE_TEMPLATE.md).

The accepted issue #11 reference run used versionCode 55, versionName
`1.0.4-debug.c1aef2b`, commit `c1aef2b1d2c9a768559a8b3dc4924fd7cc6b76e0`, and APK SHA-256
`cb3c68c09b1da1c7740c2f103e3678e72d6b60b530e843afb8694a30bfe53b5a` on a Hammerhead k24
running KOS 1.650.2509. With boundaries enabled, it verified 60 × 25 at 478 × 243 px and 96 sp;
30 × 15 at 238 × 148 px and 50 sp; and 60 × 60 at 478 × 642 px and 96 sp. Fixed `1+`, `99+`, and
`100+` states remained visible. The accepted 60 × 25 `100+` capture had clear lower glyph space and
balanced count, suffix, and mug alignment. These observations validate the method and regression
vectors. They do not replace the required matrix on the release candidate or define a stable host
sizing contract.

The final-source preview run used versionCode 57, versionName `1.0.4-debug.717d3fe`, commit
`717d3fee37132aae75fea8f7943e432c3958df66`, and APK SHA-256
`93485419943693d02500f707794a122ac5a779307605fb2dc1270545a8f7b010` on the same device and
KOS version. An eight-second page-editor recording captured the ordered no-count, `1+`, `99+`, and
`100+` graphical states without clipping. The host supplied preview configurations of 60 × 60 at
478 × 642 px and 96 sp, 60 × 25 at 478 × 243 px and 96 sp, and 30 × 15 at 238 × 148 px and 50 sp,
all right-aligned with boundaries enabled. Debug logs directly recorded
`cancellation label=graphical-preview` and `cancellation label=numeric-preview` after detachment.
The numeric page editor continued to show its host-owned `42` placeholder. The extension's emitted
numeric preview messages were therefore not visible in that host UI and remain separate automated
stream evidence.

## Device verification method

The pure layout decision and preview cadence are covered by `CorePolicyBehaviorTest` and
`PintDataFieldRuntimeTest`. These tests do not apply `RemoteViews` or prove physical fit in Karoo's
host container. On a supported Karoo, install the candidate and create two named page-editor
layouts: one narrow and one roomy. For each layout, record the actual `ViewConfig` values supplied
by Karoo, including `viewSize` dimensions, `gridSize`, `textSize`, alignment, and
`boundariesEnabled`. Open the page editor with `preview=true`, add **Pints**, and record a screen
capture for at least six seconds. The debug line from `PintProgressField` includes these values and
is suitable evidence when the host does not expose them in its UI. Do not use live calories to reach
the count states. The expected sequence in both named layouts is:

| Seconds | Mug frame | Count label |
| ---: | --- | --- |
| 0 | 50% fill | *(none)* |
| 1 | 80% fill | *(none)* |
| 2 | full with bubbles | *(none)* |
| 3 | 50% fill | `1+` |
| 4 | 50% fill | `99+` |
| 5 | 50% fill | `100+` |

Then add **Pints Count** to a page and capture at least five seconds. The extension emits the
standard-container messages `0.5`, `0.9`, `1`, `1.1`, then `0.5` at one Hz. Record these emitted
messages separately when observable. Record the page editor's host-owned numeric placeholder,
its exact device, and its KOS version in the evidence record. The placeholder value is not an
extension contract or output, and it is a separate evidence item from the emitted messages. No
production fallback is allowed.
The static resource validator already proves the `+` suffix contract, the hidden initial suffix
state, and the package-owned XML layout shape.

### Physical detach evidence

Run this procedure with a debug APK once for the graphical preview and once for the numeric
preview. A signed release build does not emit this diagnostic. Start log capture before opening the
page editor:

```bash
adb logcat -c
adb logcat -v time PintProgressField:D '*:S'
```

While the preview is visible, leave the page editor or remove the field. Direct evidence is a
timestamped `PintProgressField: cancellation label=graphical-preview` or
`PintProgressField: cancellation label=numeric-preview` line after detachment. Also record the
last visible preview frame, whether the screen shows any later frame, and the result after opening
the same preview again. The cancellation line directly proves that the registered callback ran and
called `job.cancel()`. No later visible frame and clean re-entry are observations of host behavior,
not proof of coroutine state beyond that callback. Keep this debug record separate from the signed
release-candidate evidence. Use the application cancellation line as callback evidence.

For live three-digit sizing, use a controlled ride or test power source to reach `99+` and `100+`,
then capture roomy, narrow, short, and small tiles with boundaries off and on. Record the
`ViewConfig` values, theme, count visibility, and photos with the candidate evidence. This check
cannot be reproduced by the JVM suite because Karoo owns the host process, native numeric treatment,
and physical tile allocation.

## Primary references

- https://support.hammerhead.io/hc/en-us/community/posts/32649390375835-New-Extension-Capabilities-in-Karoo-Release-1-535-2029
- https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-view-config/index.html
- https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.internal/-view-emitter/index.html
- https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-show-custom-stream-state/index.html
- https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-update-graphic-config/index.html
- https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-karoo-app-manifest/index.html
- https://developer.android.com/reference/android/widget/RemoteViews
- https://developer.android.com/reference/android/widget/ImageView
- https://developer.android.com/reference/android/widget/SeekBar
- https://developer.android.com/reference/android/content/SharedPreferences
- https://developer.android.com/studio/publish/versioning
- https://developer.android.com/studio/publish/app-signing
