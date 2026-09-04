# Architecture

## Principles

- Model events and state explicitly. Derive display, layout, typography, and assets from inputs.
- Keep deterministic policy in `core`, free of Android timing and IPC.
- Keep Android and Karoo adapters small enough to validate by compilation and device tests.
- Emit immutable `RemoteViews` snapshots. Never depend on prior host-side view state.

## Runtime flow

```text
Karoo calorie stream ----+--> PintViewReducer --> RenderPlan --> fillDisplayFor
                         |                                         |
private calorie setting -+                                         v
                         |                               PintRemoteViews --> ViewEmitter
                         |
                         +--> numericStateFrom ---------------------------> StreamEmitter
```

`PintProgressExtension` owns the Karoo service connection and exposes two `PintProgressDataType`
instances: the graphical fill field and the native count field. They share the calorie source,
lifecycle, one-Hz update cap, and global calorie target. The fill path uses the reducer, scheduler,
and generated field-fill assets. The count path publishes a numeric stream and lets Karoo own its
viewport and formatting. Both paths cancel their work on detach.

## Ownership map

| Concern | Owner |
| --- | --- |
| Calorie target bounds and slider mapping | `core/BeerCaloriesPolicy.kt` |
| Calories to completed pints and 5% bucket | `core/PintProgressReducer.kt` |
| Stream-state coalescing and animation plan | `core/PintViewReducer.kt` |
| Frame to fill asset and count | generated `core/PintAsset.kt` |
| Calorie stream to decimal pint total | `core/PintTextStreamState.kt` |
| Viewport conversion and fill text fitting | `core/PintFieldSizing.kt` |
| Karoo alignment layout selection | `PintRemoteViews.kt`, static `res/layout/pint_progress_fill_*_view.xml` |
| Android `RemoteViews` serialization | `PintRemoteViews.kt` |
| One-Hz pacing and Karoo subscription | `PintProgressDataType.kt` |
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
