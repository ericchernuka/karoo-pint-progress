# Architecture

## Principles

- Model events and state explicitly. Derive display, layout, typography, and assets from inputs.
- Keep deterministic policy in `core`, free of Android timing and IPC.
- Keep Android and Karoo adapters small enough to validate by compilation and device tests.
- Emit immutable `RemoteViews` snapshots. Never depend on prior host-side view state.

## Runtime flow

```text
Karoo calorie stream ----+--> PintFrameScheduler --> PintFrame --> fillDisplayFor --> RemoteViews --> ViewEmitter
                         |
private calorie setting -+
                         |
                         +--> numericStateFrom -----------------------------------------------> StreamEmitter
```

`PintProgressExtension` owns the Karoo service connection and exposes one data type per field
variation: `PintsFillDataType` for the graphical fill field and `PintsCountDataType` for the native
count field. They share the calorie source, lifecycle, one-Hz update cap, and global calorie target.
The fill path asks `PintFrameScheduler` which frame to show and when, waits, and renders generated
field-fill assets. The count path publishes a numeric stream and lets Karoo own its viewport and
formatting. Both paths cancel their work on detach.

## Ownership map

| Concern | Owner |
| --- | --- |
| Calorie target bounds and slider mapping | `core/BeerCaloriesPolicy.kt` |
| Calories to completed pints and 5% bucket | `core/PintProgressReducer.kt` |
| Stream-state coalescing and completion transition | `core/PintFrameScheduler.kt` |
| One-Hz update cap for both fields | `core/OneHzPacer.kt` |
| Frame to fill asset and count, preview frames | `core/PintFieldDisplay.kt` |
| Fill asset enum | generated `core/PintAsset.kt` |
| Calorie stream to decimal pint total | `core/PintTextStreamState.kt` |
| Viewport conversion and fill text fitting | `core/PintFieldSizing.kt` |
| Karoo alignment layout selection | `PintsFillDataType.kt`, static `res/layout/pint_progress_fill_*_view.xml` |
| Android `RemoteViews` serialization | `PintsFillDataType.kt` |
| Native count stream | `PintsCountDataType.kt` |
| Runtime wiring and Karoo subscription | `PintDataFieldRuntime.kt` |
| Private preference adapter and UI | `BeerCaloriesStore.kt`, `PintSettingsActivity.kt` |
| Caller authorization | `core/KarooCallerPolicy.kt`, `PintProgressExtension.kt` |
| Shared icon and fill assets | `tools/generate-drawables.mjs` |

## State and transitions

The calorie stream is cumulative for the active ride. Progress has two derived values:

- `completed`: whole calorie targets reached
- `fillBucket`: one of 20 states for the next pint

Only an observed single completion crossing animates `FULL_BUBBLES -> DRAINING -> steady`. Initial
attachment, reset, skipped thresholds, unavailable data, and target changes render a steady state.
A target change updates the baseline immediately so historical calories never trigger a false
celebration. The field-fill renderer maps the shared frames to generated assets.

## Settings scope

The public Karoo SDK has no custom per-field control schema or stable field-instance identifier.
Calories per beer is therefore one global app-private preference. Each visible field observes it.
Do not imply that this is a Karoo `ViewConfig` value.

## Dependency rule

Prefer Android platform APIs and the existing coroutine dependency. A new dependency needs a clear
benefit, updated Gradle verification metadata, license review, and a security review of transitive
artifacts.
