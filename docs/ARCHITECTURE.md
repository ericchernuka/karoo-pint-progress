# Architecture

## Principles

- Model events and state explicitly. Derive display, layout, typography, and assets from inputs.
- Keep deterministic policy in `core`, free of Android timing and IPC.
- Keep Android and Karoo adapters small enough to validate by compilation and device tests.
- Emit immutable `RemoteViews` snapshots. Never depend on prior host-side view state.

## Runtime flow

```text
Karoo calorie stream ----+
                         +--> PintViewReducer --> RenderPlan --> PintPresentation
private calorie setting -+                                         |
                                                                   v
                                                         PintRemoteViews --> ViewEmitter
```

`PintProgressExtension` owns the Karoo service connection and exposes two `PintProgressDataType`
instances: the original mug field and the text-only field. They share the same stream, progress
reducer, lifecycle, update cap, and global calorie target. `PintProgressDataType` resolves the field
viewport, combines Karoo calories with the global setting, paces output, and cancels work on detach.
Preview mode bypasses the calorie stream and cycles through representative production frames.

## Ownership map

| Concern | Owner |
| --- | --- |
| Calorie target bounds and slider mapping | `core/BeerCaloriesPolicy.kt` |
| Calories to completed mugs and 5% bucket | `core/PintProgressReducer.kt` |
| Stream-state coalescing and animation plan | `core/PintViewReducer.kt` |
| Frame to asset and count | `core/PintPresentation.kt` |
| Calorie stream to decimal pint total | `core/PintTextStreamState.kt` |
| Viewport and treatment selection | `core/PintFieldSize.kt`, `core/PintFieldLayout.kt` |
| Text fitting and boundary inset | `core/PintFieldTypography.kt`, `core/PintFieldChrome.kt` |
| Karoo alignment mapping | `PintFieldGravity.kt` |
| Android `RemoteViews` serialization | `PintRemoteViews.kt` |
| One-Hz pacing and Karoo subscription | `PintProgressDataType.kt` |
| Private preference adapter and UI | `BeerCaloriesStore.kt`, `PintSettingsActivity.kt` |
| Caller authorization | `core/KarooCallerPolicy.kt`, `PintProgressExtension.kt` |
| Mug assets | `tools/generate-drawables.mjs` |

## State and transitions

The calorie stream is cumulative for the active ride. Progress has two derived values:

- `completed`: whole calorie targets reached
- `fillBucket`: one of 20 states for the next mug

Only an observed single completion crossing animates `FULL_BUBBLES -> DRAINING -> steady`. Initial
attachment, reset, skipped thresholds, unavailable data, and target changes render a steady state.
A target change updates the baseline immediately so historical calories never trigger a false
celebration.

## Settings scope

The public Karoo SDK has no custom per-field control schema or stable field-instance identifier.
Calories per beer is therefore one global app-private preference. Each visible field observes it.
Do not imply that this is a Karoo `ViewConfig` value.

## Dependency rule

Prefer Android platform APIs and the existing coroutine dependency. A new dependency needs a clear
benefit, updated Gradle verification metadata, license review, and a security review of transitive
artifacts.
