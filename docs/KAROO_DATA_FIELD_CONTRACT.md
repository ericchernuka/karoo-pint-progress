# Karoo data-field contract

This document is the implementation contract for Pint Progress. It is based on Karoo extension
SDK 1.1.9 and Android's `RemoteViews` and app-update requirements.

## Data types

Pint Progress exposes two data types in this order:

1. `Pints Fill`, graphical, type ID `pint-progress-fill`.
2. `Pints Count`, numeric, type ID `pint-progress-text`.

The released Pint Mug type ID `pint-progress` was removed after version 1.1.0. Existing Karoo pages
that use it require manual replacement with Pints Fill or Pints Count. There is no compatibility
alias. [ADR 0002](adr/0002-remove-pint-mug-field.md) records this decision.

## `ViewConfig` mapping

| Karoo input | Pint Progress behavior | Verification |
| --- | --- | --- |
| `gridSize` | Supplies a 60 × 60 grid fallback for a missing `viewSize` dimension | `CorePolicyBehaviorTest` |
| `viewSize` | Positive physical pixels take precedence and convert to Android dp | `CorePolicyBehaviorTest`, device size matrix |
| `textSize` | Upper-bounds the Pints Fill count; Karoo applies it to Pints Count | `CorePolicyBehaviorTest`, device text matrix |
| `alignment` | Selects a static physical left, center, or right Pints Fill layout; Karoo aligns Pints Count | resource validator, device alignment matrix |
| `boundariesEnabled` | Pints Fill remains edge to edge; Karoo owns Pints Count boundaries | resource validator, device boundary matrix |
| `preview` | Pints Fill cycles representative frames and Pints Count emits representative messages without ride calories | `PintDataFieldRuntimeTest`, page editor |

## Shared lifecycle rules

- Both fields use Karoo's cumulative Calories from Power stream and the global calories-per-beer
  setting.
- Updates are spaced at least one second apart. A quick source change is deferred instead of being
  emitted inside Karoo's one-Hz limit.
- Stream, preview, and pending graphical transition work stops when Karoo detaches the field.
- Idle, searching, unavailable, negative, and non-finite calorie states become unavailable
  progress.
- A calories-per-beer change creates a new steady baseline. It does not replay a completion for
  calories already recorded.

## Pints Fill

Pints Fill requests `showHeader = false` and renders one fresh `RemoteViews` for each update.

- Twenty generated steady assets cover 0% through 95% in 5% steps. Full-foam, draining, and
  unavailable assets cover the remaining states.
- Available progress shows the exact completed-pint count, including `0`, without a suffix.
  Unavailable progress shows an em dash.
- The fill image covers the complete field rectangle with `fitXY`. App-side boundary insets are not
  added.
- The count is vertically centered and uses Karoo's physical left, center, or right alignment.
  Karoo's `textSize` is the upper bound. Width, height, and font scale can reduce the count size so
  large values remain visible.
- The left layout mirrors only the beer artwork. This keeps highlights away from the left-aligned
  count without mirroring the text.
- Nonzero fill has a foam cap. The cap becomes deeper from 80%. Full foam and draining use the same
  shared transition states as the reducer.
- One observed increase of exactly one completed pint shows full foam for one second and draining
  for one second before the current steady state. First attachment, reset, target change,
  unavailable recovery, and jumps of two or more pints render directly.
- Same-pint calorie changes update the final steady frame without cancelling an active completion
  transition.
- The picker preview loops at one Hz through 50% with `0`, 80% with `0`, full foam with `1`, and
  draining with `1`.
- Light mode uses the shared beer and foam palette. Dark mode uses its qualified field-fill palette.
  The count halo is transparent in light mode and dark in night mode.

## Pints Count

Pints Count publishes one numeric value through `startStream`. Karoo owns its icon, header, sizing,
alignment, boundaries, and formatting.

- Live values are floored to 0.1-pint increments from the 5% progress buckets. This yields `0.00`
  at 5%, `0.10` at 10%, `0.90` at 95%, and `1.00` at one completed pint after Karoo applies its
  Variability Index formatter.
- SDK 1.1.9 has no custom numeric precision API. The Variability Index formatter is the available
  dimensionless two-decimal formatter and remains host-dependent.
- In page-editor preview mode, `startView` emits `0.5`, `0.9`, `1`, and `1.1` through
  `ShowCustomStreamState` at one Hz.
- The page editor can keep a host-owned numeric placeholder. Record its value with the exact device
  and KOS version. It is not extension output and is separate from emitted preview messages.
- The field never creates `RemoteViews`.

## Calories-per-beer setting

Karoo SDK 1.1.9 has no public custom field-control schema or stable field-instance identifier. Pint
Progress therefore provides one global setting in its launcher activity:

- 150 kcal by default;
- 80–400 kcal in 5 kcal steps;
- app-private Android preferences, with no permission or external storage;
- a live Flow into each visible field.

## Installation and update rules

Android accepts an APK as an update only when its package name and signing certificate match the
installed app and its `versionCode` increases. Pint Progress therefore requires the stable package
name `io.ericchernuka.pintprogress`, a monotonically increasing `versionCode`, and one private
project-owned signing key. Signing material must not enter this repository.

## Required device matrix

Before release, test the candidate on Karoo with:

- preview and active-ride modes;
- large, medium, short, narrow, and small Pints Fill tiles;
- exact Pints Fill counts `0`, `1`, `99`, `100`, and `999`;
- left, center, and right alignment;
- boundaries off and on;
- light and dark themes;
- unavailable, normal fill, 80% foam, full foam, and drain;
- Pints Count preview messages and live `0.00`, `0.10`, `0.90`, `1.00`, and a three-digit total;
- graphical and numeric preview detach;
- default, minimum, maximum, and mid-ride calories-per-beer changes;
- installation over the previous signed build;
- the removed Pint Mug tile and manual replacement path.

Record each candidate run with the [release evidence template](RELEASE_EVIDENCE_TEMPLATE.md).

## Device verification method

Install the candidate and create named narrow and roomy page-editor layouts. Record `viewSize`,
`gridSize`, `textSize`, alignment, `boundariesEnabled`, device model, and KOS version. Add Pints Fill
and capture at least five seconds. Both layouts must show this repeating sequence:

| Seconds | Fill frame | Completed count |
| ---: | --- | ---: |
| 0 | 50% | `0` |
| 1 | 80% | `0` |
| 2 | full foam | `1` |
| 3 | draining | `1` |
| 4 | 50% | `0` |

Add Pints Count and capture at least five seconds. Record the emitted `0.5`, `0.9`, `1`, `1.1`,
`0.5` messages separately when observable. Also record the host placeholder as a separate item.

For detach evidence, start debug logging before opening the page editor:

```bash
adb logcat -c
adb logcat -v time PintProgressField:D '*:S'
```

Leaving or removing the preview must produce `cancellation label=fill-preview` for Pints Fill or
`cancellation label=numeric-preview` for Pints Count. Also verify that no later frame appears and
that reopening the preview starts cleanly. Keep debug callback evidence separate from the signed
candidate evidence.

Use the controlled [Karoo calorie source](agents/karoo-calorie-source.md) for live threshold,
multi-pint, target-change, and dropout checks. The JVM suite cannot prove Binder delivery,
`RemoteViews` application, or physical fit in Karoo's host process.

## Primary references

- https://support.hammerhead.io/hc/en-us/community/posts/32649390375835-New-Extension-Capabilities-in-Karoo-Release-1-535-2029
- https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-view-config/index.html
- https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.internal/-view-emitter/index.html
- https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-show-custom-stream-state/index.html
- https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.models/-update-graphic-config/index.html
- https://developer.android.com/reference/android/widget/RemoteViews
- https://developer.android.com/studio/publish/versioning
- https://developer.android.com/studio/publish/app-signing
