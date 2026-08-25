# Karoo graphical data-field contract

This document is the implementation contract for Pint Progress. It is based on Karoo extension
SDK 1.1.9 and Android's `RemoteViews` and app-update requirements.

## `ViewConfig` mapping

| Karoo input | Official meaning | Pint Progress behavior | Verification |
| --- | --- | --- | --- |
| `gridSize` | Column span × row span on a 60 × 60 grid | Used only as a fallback for a missing `viewSize` dimension | `PintFieldSizeTest` |
| `viewSize` | Current configured view size in physical pixels | Takes precedence, converts to dp, and bounds the complete layout | `PintFieldSizeTest`, on-device size matrix |
| `textSize` | Standard numeric field size for this grid size in sp | Caps counter text; width, height, count length, and font scale may reduce it | `PintFieldTypographyTest` |
| `alignment` | User-selected in-ride alignment | Positions the complete count-and-mug group, or the mug itself in mug-only views | `PintFieldGravityTest`, on-device alignment matrix |
| `boundariesEnabled` | Whether the user enabled field boundaries | Adds a larger inset before fitting or rendering content | `PintFieldChromeTest`, `PintFieldLayoutTest` |
| `preview` | Page editing rather than an active ride | Renders one representative mug, does not subscribe to calories, and still fits `viewSize` | `PintFieldLayoutTest`, on-device page editor |

## Rendering rules

1. Karoo's reported `viewSize` is authoritative when positive. Grid-derived size is a defensive
   fallback for a missing dimension, not a second competing layout signal.
2. Mug-only content uses the full available height, preserves the vector drawable's aspect ratio,
   and leaves its width content-sized so Karoo's alignment setting can move it.
3. Before the first completed mug, and while data is unavailable, the live field uses the adaptive
   mug-only treatment. It must not keep an invisible counter's fixed-size count layout.
4. Preview cycles through the production 50%, 80%, and full-with-bubbles frames at one Hz, using
   the same adaptive sizing rule. The loop is cancelled when Karoo detaches the preview.
5. Once a completed count exists, the largest count-and-mug treatment that fits both dimensions is
   used. Counter size follows Karoo up to the mug-height and complete-group width budgets.
6. Every `RemoteViews` update is constructed from a fresh instance and resets every property it
   relies on. Only Android-supported `RemoteViews` layouts and widgets are used.
7. Actual view updates are spaced at least one second apart, and cancellation stops collection and
   pending animation frames when Karoo detaches the field.
8. `showHeader = true` asks Karoo to render the standard data-type icon and compact `Pints` label.
   Pint Progress does not duplicate that chrome inside its `RemoteViews` or ask Karoo to render its
   numeric value through `formatDataTypeId`.
9. The full-with-bubbles frame may extend its foam crown slightly above the mug rim. The crown stays
   within the vector viewport and mug width, while every normal fill state remains inside the glass.

## Text-only field

`Pints (Text)` is a second graphical data type, selected independently from `Pints` in Karoo's
data-field picker. The choice is per tile and per ride profile, not a global appearance preference.
Both types consume the same calorie stream and calories-per-beer setting.

- Karoo still owns the standard data-type icon and `Pints (Text)` header; the field body is text
  only.
- The live value is floored to 0.1-pint increments from the existing 5% progress buckets. This
  yields `0.0` at 5%, `0.1` at 10%, `0.9` at 95%, and `1.0` only at a full pint.
- The text value uses `viewSize`, `textSize`, alignment, and boundaries just like the mug field.
  Its width is constrained before Android receives the `RemoteViews` update.
- Unavailable data renders `—`. Preview loops through `0.5`, `0.8`, and `1.0` at one Hz in a
  dedicated picker treatment that fits the host-measured preview body.
- Completion frames retain the one-Hz lifecycle cap but render the stable text value, not mug
  bubbles or drain artwork.

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
- text-only `0.0`, `0.1`, `0.9`, `1.0`, and a three-digit completed total in narrow and roomy tiles;
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
