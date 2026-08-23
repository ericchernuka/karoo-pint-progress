# Test boundary policy

`./gradlew :pint:jacocoBehaviorTestCoverageVerification` enforces **100% instruction and branch coverage** for every deterministic product-behavior class in `io.ericchernuka.pintprogress.core`:

- calorie validation, rounding, 5% bucketing, and completed-beer counting;
- caller-package authorization for the Karoo Binder boundary;
- first-attach, reset, skipped-threshold, full, bubbles, drain, and steady-state transitions;
- stream-state conversion, visible-state coalescing, timed-frame plans, and preview behavior;
- drawable frame selection, labels, and counter visibility.

The following files are intentionally outside JaCoCo's JVM scope because each is a thin Android or Karoo IPC adapter, not a location for product decisions:

- `PintProgressExtension.kt` creates and binds the official `KarooSystemService` from an Android `Service`.
- `KarooFlows.kt` adapts the official Binder consumer to a finite callback Flow,
  including terminal callbacks and idempotent unregistering. Its lifecycle adapter
  is JVM-tested; Binder delivery and the concrete `KarooSystemService` remain
  device-tested.
- `PintProgressDataType.kt` applies standard Flow backpressure (`conflate`), globally spaces
  actual `ViewEmitter` calls one second apart with `SystemClock.elapsedRealtime()` and coroutine
  `delay`, executes the fully covered timed plan, and bridges it to a real `ViewEmitter`.
- `PintRemoteViews.kt` serializes a fully covered `PintDisplay` model into Android `RemoteViews` and maps assets to compile-time `R.drawable` IDs.

Those adapters are compile-verified by `:pint:assembleDebug`, which validates the manifest, service declaration, extension metadata, layouts, vector assets, and SDK calls. They still require an on-device Karoo smoke test because local JVM tests cannot bind the Karoo System service or receive its Binder-backed `ViewEmitter`.

This keeps the coverage gate honest: no deterministic product decision is excluded, while Flow
scheduling, Android timing, and Karoo IPC are not misrepresented as locally executable.
