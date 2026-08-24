# Agent guide

Pint Progress is a Kotlin/Android graphical data field for Hammerhead Karoo. Prefer pure state and
derived presentation in `pint/.../core`; keep Android and Karoo classes as thin adapters.

## Project map

- `pint/`: application, resources, and tests
- `lib/`: vendored Karoo extension SDK, avoid edits
- `tools/`: generated drawable source and validation
- `docs/`: implementation and operations guidance

## Common tasks

| Task | Command or entry point |
| --- | --- |
| Full verification | `./gradlew :lib:testDebugUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification` |
| Unit tests | `./gradlew :pint:testDebugUnitTest` |
| Regenerate mug assets | `node tools/generate-drawables.mjs` |
| Validate mug geometry and contrast | `node tools/validate-drawables.mjs` |
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

## Non-negotiable invariants

- Preserve all six `ViewConfig` semantics and the one-Hz update limit.
- Cancel stream, preview, and animation work when Karoo detaches the field.
- Never hand-edit generated mug drawables. Edit `tools/generate-drawables.mjs`, then regenerate.
- Keep light and dark colors in their resource qualifiers.
- Do not add permissions, network or external storage access, analytics, WebViews, native code,
  dynamic loading, signing material, or caller-gate bypasses without explicit approval.
- Avoid `lib/` changes. If unavoidable, preserve headers, update `NOTICE`, and add focused tests.
- Keep deterministic decisions in `core` and maintain 100% instruction and branch coverage there.

## Done checklist

1. Run generated-asset validation when visuals change.
2. Run the full verification command and `git diff --check`.
3. Run the applicable Karoo device matrix before release.
4. Confirm no secrets, signing files, permissions, or unreviewed dependencies entered the diff.
5. Treat debug APKs as development artifacts only. Follow `docs/RELEASE.md` for distribution.
