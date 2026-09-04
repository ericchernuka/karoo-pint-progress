# QA preset target decision

Status: design study complete; recommendation awaits maintainer approval. No feature implementation.

Basis: baseline `da70ab2`, with paused startup and explicit 180-Calorie setup from plan 002 (`2d3b69e`).

## Current workflow

The driver has seven fixed presets in `tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputState.kt:3`. All use a 180-Calorie product target. Its activity maps each button directly to a preset and writes private preferences. The product target defaults to 150 and permits 80–400 in steps of five (`pint/src/main/kotlin/io/ericchernuka/pintprogress/core/BeerCaloriesPolicy.kt:6`).

Plan 002 removes the immediate setup fault: fresh output starts paused, and the operator sets the product target to 180 before calibration. The driver cannot read the candidate's private settings. A selector would still require the operator to confirm that both targets match.

## Options

| Option | Value | Cost and limit |
| --- | --- | --- |
| Retain fixed presets and corrected setup | Smallest supported workflow; no new state or UI | Tests with another target need manual calculation or the documented live target-change procedure |
| Add a QA-only target selector | Reuses the seven preset ratios across candidate targets | Requires matching two independent settings, dynamic labels, rounding policy, and new state tests |

A future selector should use the Android SeekBar and buttons already used by the product settings screen. The source implementation of `PintSettingsActivity.kt` demonstrates those native controls. A new UI library is unnecessary. Do not make the QA app depend on the production application module to share a few constants.

## Recommendation

Retain the corrected fixed-target workflow for now. The repository demonstrates one calibrated 180-Calorie preset set, not repeated demand for arbitrary-target test runs. Reconsider a selector when a device-test record shows repeated preset recalculation or a need to run the complete matrix at multiple targets. This is a recommendation to defer a feature, not an assertion that the feature cannot help.

## Numeric contract if a selector is approved

Derive raw Calories by multiplying the selected product target by the existing preset ratios. Preserve fractional values internally until an explicit display/target rounding boundary.

| Preset | Ratio | Target 80 | Target 150 | Target 180 | Target 400 |
| --- | --- | ---: | ---: | ---: | ---: |
| Half | 0.5 | 40 | 75 | 90 | 200 |
| 80% | 0.8 | 64 | 120 | 144 | 320 |
| 95% | 0.95 | 76 | 142.5 | 171 | 380 |
| One pint | 1 | 80 | 150 | 180 | 400 |
| 99 pints | 99 | 7920 | 14850 | 17820 | 39600 |
| 100 pints | 100 | 8000 | 15000 | 18000 | 40000 |
| Multi-pint | 2.5 | 200 | 375 | 450 | 1000 |

For a whole-Calorie target surface, propose rounding up and showing the actual resulting Calories on the button. For example, 95% of 150 becomes a 143-Calorie target. Do not display this as an exact 95% measurement. Confirm KOS quantization on the intended device before adopting this rule; the current activity uses `toInt()` only for its existing integer status targets.

Changing the configured product target must pause output and require a new explicit preset selection. A derived target below cumulative ride Calories requires a fresh test ride; lowering the driver target cannot remove Calories. Missing feedback keeps power at zero through the existing controller. Selection or resume is explicit; no background automatic start is added.

The product threshold in Calories is separate from `PowerTargetController`'s measured 940 joules per Calorie. A selector must not change that calibration or the 50 kW cap.

## Open decisions

Approval is needed to build the selector, choose whether it retains its target across launches, and accept a tested fractional-target rounding policy. The recommendation above avoids those new product choices for the current release. Record a new approved decision before implementation.

## Scope

This study changes only `plans/008-target-aware-qa-decision.md` and its plan execution record. If implementation is later approved, limit the first change to these files:

- `tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputState.kt`
- `tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputStore.kt`
- `tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieSourceActivity.kt`
- `tools/karoo-calorie-source/src/main/res/layout/activity_calorie_source.xml`
- `tools/karoo-calorie-source/src/main/res/values/strings.xml`
- `tools/karoo-calorie-source/src/test/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputStateTest.kt`
- `docs/agents/karoo-calorie-source.md`

The product APK, permissions, SDK, controller calibration, and candidate preferences are out of scope.

## Verification

The arithmetic table can be reproduced with Python stdlib Decimal using ratios 0.5, 0.8, 0.95, 1, 99, 100, and 2.5. This is arithmetic verification, not device evidence. The study needs no app build.

Future JUnit tests should use `CalorieOutputStateTest` as the pattern: all seven ratios at 80, 150, 180, and 400; invalid/out-of-step target handling; the chosen rounding rule; paused defaults; explicit selection/resume; and changing target while active. The SharedPreferences adapter and visible labels need a device check. Run `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :calorie-source:testDebugUnitTest :calorie-source:lintDebug :calorie-source:assembleDebug`, plus the current full gate from AGENTS.md, after a future implementation.

## Stop conditions

Stop if the feature needs cross-app preference access, a permission, a new dependency, or candidate APK modification. Do not treat the driver target or its status screen as proof of the candidate state. Device installation and live test actions need separate authorization.
