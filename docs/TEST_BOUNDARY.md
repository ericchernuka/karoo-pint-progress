# Test boundary policy

## Commands

```bash
# Focused JVM suite
./gradlew :pint:testDebugUnitTest

# Dokka HTML documentation
./gradlew :lib:dokkaGeneratePublicationHtml
python3 tools/verify-dokka-output.py

# Full repository gate
./gradlew :lib:testDebugUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification

# Generated drawable and static-resource contracts
node tools/generate-drawables.mjs
node tools/validate-drawables.mjs
```

## Coverage boundary

`./gradlew :pint:jacocoBehaviorTestCoverageVerification` enforces **100% instruction and branch coverage** for every deterministic product-behavior class in `io.ericchernuka.pintprogress.core`:

- calorie validation, configurable-target normalization, slider mapping, 5% bucketing, and completed-beer counting;
- caller-package authorization for the Karoo Binder boundary;
- first-attach, reset, skipped-threshold, full, bubbles, drain, and steady-state transitions;
- stream-state conversion, visible-state coalescing, timed-frame plans, and preview behavior;
- drawable frame selection and counter visibility;
- count-field 0.1-pint flooring, preview messages, native stream-state propagation, and custom data-point identity.

The following files are outside the core JaCoCo ratio. Android and Karoo adapters are compile- and
device-verified, while the embedded runtime scheduling policy has focused JVM coverage:

- `PintProgressExtension.kt` creates and binds the official `KarooSystemService` from an Android `Service`.
- `PintProgressDataType.kt` wires `SystemClock.elapsedRealtime()` and coroutine `delay` into the
  pure `PintDataFieldRuntime` coordinator, keeps the initial numeric and graphic configuration,
  bridges coordinator outputs to the Karoo emitters, and adapts the official Binder consumer into a
  finite callback Flow. JVM tests cover deterministic Flow backpressure, one-second pacing, numeric
  and graphical routing, preview order, reducer-plan delays, target-change baseline, cancellation,
  terminal callbacks, synchronous terminal callbacks, and idempotent cleanup. Coroutine state
  machines contain generated normal-completion instructions that prevent this adapter from joining
  the core ratio. These tests verify callback order and waits, not Binder or `RemoteViews` delivery.
- `PintRemoteViews.kt` serializes the covered asset and count display into Android `RemoteViews`,
  maps assets to compile-time `R.drawable` IDs, and selects a static alignment layout. The resource
  validator checks every static alignment wrapper, responsive mug bounds, initial visibility,
  field labels, extension metadata, and the representative generated drawable mapping.
- `PintSettingsActivity.kt` and `BeerCaloriesStore.kt` adapt the covered target policy to Android's
  `SeekBar` and app-private preferences.

Those adapters are compile-verified by `:pint:assembleDebug`, which validates the manifest, service declaration, extension metadata, layouts, vector assets, and SDK calls. They still require an on-device Karoo smoke test because local JVM tests cannot bind the Karoo System service or receive its Binder-backed `ViewEmitter`.

Record the device-only checks with the [release evidence template](RELEASE_EVIDENCE_TEMPLATE.md).

This keeps the coverage gate honest: the `core` ratio remains at 100% instruction and branch
coverage, while Flow scheduling, Android timing, and Karoo IPC are not misrepresented as locally
executable.
