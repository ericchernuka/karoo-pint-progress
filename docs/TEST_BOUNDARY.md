# Test boundary policy

## Commands

```bash
# Focused JVM suite
./gradlew :pint:testDebugUnitTest

# Dokka HTML documentation
./gradlew :lib:dokkaGeneratePublicationHtml
python3 tools/verify-dokka-output.py

# Full repository gate
./gradlew :lib:testDebugUnitTest :pint:testReleaseUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification

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
- mug and fill drawable selection, exact fill counts, unavailable text, counter visibility,
  small-viewport count sizing, and deterministic picker previews;
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
- `PintRemoteViews.kt` serializes the covered mug or fill asset and count display into Android
  `RemoteViews`, maps assets to compile-time `R.drawable` IDs, and applies each presentation's
  static layout contract.
  The resource validator checks every static alignment wrapper, responsive mug bounds, initial
  visibility, the `pint_count_suffix` static `+` contract, field labels, extension metadata, and
  the representative generated drawable mapping. Core tests cover the shared count-and-mug scale
  for the observed roomy `96sp` text input and the bounded 128 × 68 dp narrow input at `46sp`.
  RemoteViews application and physical fit remain device checks.
  Core tests also require Android 12+ roomy scaling and the normal-scale fallback used on older
  hosts where `RemoteViews` cannot assign larger image layout dimensions.
- `PintSettingsActivity.kt` and `BeerCaloriesStore.kt` adapt the covered target policy to Android's
  `SeekBar` and app-private preferences.

Those adapters are compile-verified by `:pint:assembleDebug`, which validates the manifest, service declaration, extension metadata, layouts, vector assets, and SDK calls. They still require an on-device Karoo smoke test because local JVM tests cannot bind the Karoo System service or receive its Binder-backed `ViewEmitter`. The device methods for the host-owned native count placeholder, the graphical no-count, `1+`, `99+`, `100+` sequence, and preview detach evidence are documented in [the Karoo data-field contract](KAROO_DATA_FIELD_CONTRACT.md#device-verification-method). Record the observed placeholder value with the exact device and KOS evidence, and keep that row separate from the emitted-message row.

Temporary local calibration fields were used to isolate fixed `1+`, `99+`, and `100+` states during
the physical typography investigation. They exercised the production `RemoteViews` path without
requiring accumulated ride calories. They are not part of the committed debug or release extension
catalog. The permanent picker sequence provides the deterministic count states for later checks.

Record the device-only checks with the [release evidence template](RELEASE_EVIDENCE_TEMPLATE.md).

This keeps the coverage gate honest: the `core` ratio remains at 100% instruction and branch
coverage, while Flow scheduling, Android timing, and Karoo IPC are not misrepresented as locally
executable.
