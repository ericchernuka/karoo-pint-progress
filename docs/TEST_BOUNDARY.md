# Test boundary policy

## Commands

```bash
# Focused JVM suite
./gradlew :pint:testDebugUnitTest

# Full repository gate
./gradlew :lib:testDebugUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification

# Generated visual contracts
node tools/generate-drawables.mjs
node tools/validate-drawables.mjs
```

## Coverage boundary

`./gradlew :pint:jacocoBehaviorTestCoverageVerification` enforces **100% instruction and branch coverage** for every deterministic product-behavior class in `io.ericchernuka.pintprogress.core`:

- calorie validation, configurable-target normalization, slider mapping, 5% bucketing, and completed-beer counting;
- caller-package authorization for the Karoo Binder boundary;
- first-attach, reset, skipped-threshold, full, bubbles, drain, and steady-state transitions;
- stream-state conversion, visible-state coalescing, timed-frame plans, and preview behavior;
- drawable frame selection, labels, and counter visibility.
- text-only 0.1-pint flooring, unavailable and preview values, and constrained decimal typography.

The following files are intentionally outside JaCoCo's JVM scope because each is a thin Android or Karoo IPC adapter, not a location for product decisions:

- `PintProgressExtension.kt` creates and binds the official `KarooSystemService` from an Android `Service`.
- `KarooFlows.kt` adapts the official Binder consumer into a finite callback Flow. Its normal,
  terminal, synchronous-terminal, cancellation, and idempotent-cleanup behavior is JVM-tested;
  Binder delivery and the concrete `KarooSystemService` remain device-tested.
- `PintProgressDataType.kt` applies standard Flow backpressure (`conflate`), globally spaces
  actual `ViewEmitter` calls one second apart with `SystemClock.elapsedRealtime()` and coroutine
  `delay`, combines app-private target changes with Karoo's calorie stream, executes the fully
  covered timed plan, and bridges it to a real `ViewEmitter`.
- `PintRemoteViews.kt` serializes a fully covered `PintDisplay` model into Android `RemoteViews` and maps assets to compile-time `R.drawable` IDs.
- `PintSettingsActivity.kt` and `BeerCaloriesStore.kt` adapt the covered target policy to Android's
  `SeekBar` and app-private preferences.

Those adapters are compile-verified by `:pint:assembleDebug`, which validates the manifest, service declaration, extension metadata, layouts, vector assets, and SDK calls. They still require an on-device Karoo smoke test because local JVM tests cannot bind the Karoo System service or receive its Binder-backed `ViewEmitter`.

This keeps the coverage gate honest: no deterministic product decision is excluded, while Flow
scheduling, Android timing, and Karoo IPC are not misrepresented as locally executable.
