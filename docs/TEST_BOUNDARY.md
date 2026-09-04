# Test boundary policy

## Commands

```bash
# Focused JVM suite
./gradlew :pint:testDebugUnitTest

# Controlled Karoo Calories source
./gradlew :calorie-source:testDebugUnitTest :calorie-source:assembleDebug

# Dokka HTML documentation
./gradlew :lib:dokkaGeneratePublicationHtml
python3 tools/verify-dokka-output.py

# Full repository gate
./gradlew :lib:testDebugUnitTest :pint:testReleaseUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification :calorie-source:testDebugUnitTest :calorie-source:lintDebug :calorie-source:assembleDebug

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
- fill drawable selection, exact completed counts, unavailable text, Pints Fill width and height
  fitting, and deterministic picker previews;
- count-field 0.1-pint flooring, preview messages, native stream-state propagation, and custom data-point identity.

The following files are outside the core JaCoCo ratio. Android and Karoo adapters are compile- and
device-verified, while the embedded runtime scheduling policy has focused JVM coverage:

- `PintProgressExtension.kt` creates and binds the official `KarooSystemService` from an Android `Service`.
- `PintProgressDataType.kt` wires `SystemClock.elapsedRealtime()` and coroutine `delay` into the
  pure `PintDataFieldRuntime` coordinator, keeps the initial numeric and graphic configuration,
  bridges coordinator outputs to the Karoo emitters, and adapts the official Binder consumer into a
  finite callback Flow. JVM tests cover deterministic Flow backpressure, one-second pacing, numeric
  and graphical routing, preview order, reducer-plan delays, direct target changes,
  unavailable-progress recovery, conflated multi-pint jumps, pending-transition cancellation,
  atomic refresh-before-completion and completion-before-refresh state operations,
  terminal callbacks, synchronous terminal callbacks, and idempotent cleanup. Coroutine state
  machines contain generated normal-completion instructions that prevent this adapter from joining
  the core ratio. These tests verify callback order and waits, not Binder or `RemoteViews` delivery.
- `PintRemoteViews.kt` serializes the covered field-fill asset and completed count into Android
  `RemoteViews`, maps assets to compile-time `R.drawable` IDs, and applies the three static physical
  alignment layouts. The resource validator checks edge-to-edge fill rendering, initial text state,
  field labels, extension metadata, generated mappings, and the shared icon. Core tests cover fill
  count fitting. RemoteViews application and physical fit remain device checks.
- `PintSettingsActivity.kt` and `BeerCaloriesStore.kt` adapt the covered target policy to Android's
  `SeekBar` and app-private preferences.

Those adapters are compile-verified by `:pint:assembleDebug`, which validates the manifest, service
declaration, extension metadata, layouts, vector assets, and SDK calls. They still require an
on-device Karoo smoke test because local JVM tests cannot bind the Karoo System service or receive
its Binder-backed `ViewEmitter`. The device methods for the Pints Fill preview, the host-owned
numeric placeholder, and preview detach evidence are documented in [the Karoo data-field contract](KAROO_DATA_FIELD_CONTRACT.md#device-verification-method).
Record the placeholder with exact device and KOS evidence, separate from the emitted-message row.

The separate QA calorie-source APK drives KOS native Calories through supported Power and Speed
sensor types without changing the candidate APK. Use it for threshold, count, multi-pint,
target-change, and dropout checks. Its build, pairing, calibration, evidence, and cleanup procedure
is in the [QA calorie-source runbook](agents/karoo-calorie-source.md). Evidence is valid only when
the paired source is connected and the candidate reaches the selected state.

Record the device-only checks with the [release evidence template](RELEASE_EVIDENCE_TEMPLATE.md).

This keeps the coverage gate honest: the `core` ratio remains at 100% instruction and branch
coverage, while Flow scheduling, Android timing, and Karoo IPC are not misrepresented as locally
executable.
