# Karoo data-field contract

This document is the implementation contract for Pint Progress. It is based on Karoo extension
SDK 1.1.9 and Android's `RemoteViews` and app-update requirements.

## `ViewConfig` mapping

| Karoo input | Official meaning | Pint Progress behavior | Verification |
| --- | --- | --- | --- |
| `gridSize` | Column span × row span on a 60 × 60 grid | Used only as a fallback for a missing `viewSize` dimension | `PintFieldSizeTest` |
| `viewSize` | Current configured view size in physical pixels | Takes precedence, converts to dp, and bounds the complete layout | `PintFieldSizeTest`, on-device size matrix |
| `textSize` | Standard numeric field size for this grid size in sp | Caps graphical mug counter text; Karoo applies it directly to the native count field | `PintFieldTypographyTest`, on-device text matrix |
| `alignment` | User-selected in-ride alignment | Selects a left, center, or right XML layout for the complete group | `PintRemoteViewsLayoutTest`, on-device alignment matrix |
| `boundariesEnabled` | Whether the user enabled field boundaries | Adds a larger inset before fitting or rendering content | `PintFieldChromeTest`, `PintFieldLayoutTest` |
| `preview` | Page editing rather than an active ride | Cycles representative mug frames or native count messages without using ride calories | `PintFieldLayoutTest`, `PintTextStreamStateTest`, on-device page editor |

## Rendering rules

1. Karoo's reported `viewSize` is authoritative when positive. Grid-derived size is a defensive
   fallback for a missing dimension, not a second competing layout signal.
2. Regular and compact mugs preserve their vector aspect ratio and use 66 × 89 dp and 48 × 65 dp
   as maximums. They can shrink when Karoo temporarily reduces the host container without restarting
   the field. Mug-only and preview treatments continue to fill available height.
3. Before the first completed mug, and while data is unavailable, the live field uses the adaptive
   mug-only treatment. It must not keep an invisible counter's fixed-size count layout.
4. Preview cycles through the production 50%, 80%, and full-with-bubbles frames at one Hz, using
   the same adaptive sizing rule. The loop is cancelled when Karoo detaches the preview.
5. Once a completed count exists, the largest count-and-mug treatment that fits both dimensions is
   used. Counter size follows Karoo up to the mug-height and complete-group width budgets.
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

## Native count field

`Pints Count` is a standard numeric data type, selected independently from `Pints` in Karoo's
data-field picker. The choice is per tile and per ride profile, not a global appearance preference.
Both types consume the same calorie stream and calories-per-beer setting.

- The field publishes a single numeric value through `startStream`. Karoo owns the native numeric
  treatment, including its icon, header, sizing, alignment, and boundaries.
- In page-editor preview mode, `startView` cycles `0.5`, `0.9`, `1`, and `1.1` through
  `ShowCustomStreamState` at one Hz. Detaching the preview cancels the loop.
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
- large, medium, short, and narrow tiles;
- left, center, and right alignment;
- boundaries off and on;
- no completed mug, `1+`, `99+`, and `100+`;
- light and dark system themes;
- unavailable calories, normal fill, 80% foam, bubbles, and drain;
- count preview sequence, live `0.00`, `0.10`, `0.90`, `1.00`, and a three-digit total in narrow and roomy tiles;
- default, minimum, maximum, and mid-ride calories-per-beer changes;
- install over the previous signed build without uninstalling.

## Primary references

- https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-view-config/index.html
- https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.internal/-view-emitter/index.html
- https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-update-graphic-config/index.html
- https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-karoo-app-manifest/index.html
- https://developer.android.com/reference/android/widget/RemoteViews
- https://developer.android.com/reference/android/widget/ImageView
- https://developer.android.com/reference/android/widget/SeekBar
- https://developer.android.com/reference/android/content/SharedPreferences
- https://developer.android.com/studio/publish/versioning
- https://developer.android.com/studio/publish/app-signing
