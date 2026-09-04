# Agent guide

Pint Progress is a Kotlin/Android graphical data field for Hammerhead Karoo. Prefer pure state and
derived presentation in `pint/.../core`; keep Android and Karoo classes as thin adapters.

## Project map

- `pint/`: application, resources, and tests
- `lib/`: vendored Karoo extension SDK, avoid edits
- `tools/`: generated drawable source, validation, and the debug-only Karoo QA calorie source
- `docs/`: implementation and operations guidance

## Common tasks

| Task | Command or entry point |
| --- | --- |
| Full verification | `./gradlew :lib:testDebugUnitTest :pint:testReleaseUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification :calorie-source:testDebugUnitTest :calorie-source:lintDebug :calorie-source:assembleDebug` |
| Unit tests | `./gradlew :pint:testDebugUnitTest` |
| Regenerate mug assets | `node tools/generate-drawables.mjs` |
| Validate drawable and resource contracts | `node tools/validate-drawables.mjs` |
| Drive exact Calories on Karoo | [`docs/agents/karoo-calorie-source.md`](docs/agents/karoo-calorie-source.md) |
| Install debug APK | `adb install -r pint/build/outputs/apk/debug/pint-debug.apk` |
| Uninstall | `adb uninstall io.ericchernuka.pintprogress` |
| Check patch hygiene | `git diff --check` |

## Documentation

Start at [`docs/README.md`](docs/README.md).

| Topic | Document |
| --- | --- |
| Design and code ownership | [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) |
| Karoo sizing and host contract | [`docs/KAROO_DATA_FIELD_CONTRACT.md`](docs/KAROO_DATA_FIELD_CONTRACT.md) |
| Testing scope and device matrix | [`docs/TEST_BOUNDARY.md`](docs/TEST_BOUNDARY.md) |
| Mug asset generation | [`docs/DRAWABLES.md`](docs/DRAWABLES.md) |
| Security invariants | [`docs/SECURITY.md`](docs/SECURITY.md) |
| Builds and releases | [`docs/RELEASE.md`](docs/RELEASE.md) |
| Build system comparison | [`docs/BUILD_SYSTEM_COMPARISON.md`](docs/BUILD_SYSTEM_COMPARISON.md) |
| Installation and runtime failures | [`docs/TROUBLESHOOTING.md`](docs/TROUBLESHOOTING.md) |

## Agent skills

### Issue tracker

Issues and specs are tracked in this repository's GitHub Issues. See
[`docs/agents/issue-tracker.md`](docs/agents/issue-tracker.md).

### Triage labels

Use the five canonical triage labels defined for this repository. See
[`docs/agents/triage-labels.md`](docs/agents/triage-labels.md).

### Domain docs

This is a single-context repository. See [`docs/agents/domain.md`](docs/agents/domain.md).

## Non-negotiable invariants

- Preserve all six `ViewConfig` semantics and the one-Hz update limit.
- Cancel stream, preview, and animation work when Karoo detaches the field.
- Never hand-edit generated mug drawables or Kotlin asset mappings. Edit
  `tools/generate-drawables.mjs`, then regenerate every output.
- Keep light and dark colors in their resource qualifiers.
- Do not add permissions, network or external storage access, analytics, WebViews, native code,
  dynamic loading, signing material, or caller-gate bypasses without explicit approval.
- Avoid `lib/` changes. If unavoidable, preserve headers, update `NOTICE`, and add focused tests.
- Keep deterministic decisions in `core` and maintain 100% instruction and branch coverage there.

## Simplification rules

- Prefer small, fixed Android resources over custom build-time generation. Add generation only when
  it reduces maintained source or prevents demonstrated drift.
- Before adding a helper, coordinator, generator, or adapter, compare it with direct platform or
  standard-library use. Keep the layer only when it removes duplication or owns a real boundary.
- Consolidate tests by behavior, not file ownership. Preserve every meaningful input, edge case,
  expected output, lifecycle event, and assertion.
- When coverage moves to a validator or another test layer, document the equivalent coverage that
  remains. Do not combine files or tests only to reduce a line count.

## Done checklist

1. Run drawable and resource validation when generated assets, layouts, strings, extension metadata,
   or generated mappings change.
2. Run the full verification command and `git diff --check`.
3. Run the applicable Karoo device matrix before release.
4. Confirm no secrets, signing files, permissions, or unreviewed dependencies entered the diff.
5. Treat debug APKs as development artifacts only. Follow `docs/RELEASE.md` for distribution.
