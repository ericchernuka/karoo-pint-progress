# Plan 008: Decide whether QA presets should follow the candidate target

> Follow the steps in order. Confirm each expected result. Stop on the conditions below. Update only this plan's status row in plans/README.md when execution ends. This file is a plan; its presence is not permission to execute it.
>
> Drift check: `git diff --stat da70ab23de201215f2a391cdccda29affb67fd51..HEAD -- plans/008-target-aware-qa-design.md plans/008-target-aware-qa-decision.md`. Compare changed scope files with the excerpts before proceeding. Expected predecessor changes are named below; reconcile those and record the new baseline in this plan. Stop on other mismatches.

## Status

- Priority: P3
- Effort: M (S: hours; M: about one day, including checks)
- Risk: LOW
- Depends on: 002
- Category: direction
- Planned at: `da70ab23de201215f2a391cdccda29affb67fd51`, 2026-09-04
- Execution status: DONE (design study; feature approval pending)

## Why this matters

QA presets hard-code a 180-Calorie target, while the product defaults to 150 and permits 80 to 400 in steps of five. A target selector could reduce setup mistakes, but adds UI and persisted state. This plan produces a design decision only; it does not authorize building the feature.

## Current state

`tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputState.kt:3`

````text
enum class CaloriePreset(val calories: Double) {
    HALF(90.0),
    EIGHTY_PERCENT(144.0),
    NINETY_FIVE_PERCENT(171.0),
    ONE_PINT(180.0),
    COUNT_99(17_820.0),
    COUNT_100(18_000.0),
    TWO_AND_A_HALF_PINTS(450.0),
}

data class CalorieOutputState(
    val targetCalories: Double = CaloriePreset.NINETY_FIVE_PERCENT.calories,
    val isEmitting: Boolean = true,
) {
    fun select(calories: Double) = CalorieOutputState(calories, isEmitting = true)

    fun silence() = copy(isEmitting = false)
````

`pint/src/main/kotlin/io/ericchernuka/pintprogress/core/BeerCaloriesPolicy.kt:6`

````text
object BeerCaloriesPolicy {
    const val DEFAULT = 150
    const val MIN = 80
    const val MAX = 400
    const val STEP = 5
    const val STEP_COUNT = (MAX - MIN) / STEP

    fun normalize(value: Int): Int =
        MIN + ((value.coerceIn(MIN, MAX) - MIN).toFloat() / STEP).roundToInt() * STEP

    fun fromSliderProgress(progress: Int): Int =
        MIN + progress.coerceIn(0, STEP_COUNT) * STEP

    fun toSliderProgress(value: Int): Int =
        (normalize(value) - MIN) / STEP
}
````

`tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieSourceActivity.kt:17`

````text

        mapOf(
            R.id.output_50 to CaloriePreset.HALF,
            R.id.output_80 to CaloriePreset.EIGHTY_PERCENT,
            R.id.output_95 to CaloriePreset.NINETY_FIVE_PERCENT,
            R.id.output_one_pint to CaloriePreset.ONE_PINT,
            R.id.output_99 to CaloriePreset.COUNT_99,
            R.id.output_100 to CaloriePreset.COUNT_100,
            R.id.output_multi_pint to CaloriePreset.TWO_AND_A_HALF_PINTS,
        ).forEach { (buttonId, preset) ->
            findViewById<Button>(buttonId).setOnClickListener {
                outputStore.write(outputStore.read().select(preset.calories))
                renderStatus()
            }
        }

        findViewById<Button>(R.id.output_silence).setOnClickListener {
            outputStore.write(outputStore.read().silence())
            renderStatus()
````

`docs/agents/karoo-calorie-source.md:60`

````text
The fixed values assume the candidate target is 180 Calories:

| Control | Calories | Product state |
| --- | ---: | --- |
| `50%` | 90 | half threshold |
| `80%` | 144 | high-fill threshold |
| `95%` | 171 | bubble and target-change input |
| `1 pint` | 180 | exact completed count 1 |
| `99 pints` | 17,820 | exact completed count 99 |
| `100 pints` | 18,000 | exact completed count 100 |
| `2.5 pints` | 450 | multi-pint destination |

The 90-Calorie check is the calibration gate for the current Karoo and user profile. Do not use
high-count captures when it settles at a different value. On the validated Karoo, 17,820 Calories
can take about six minutes before the correction interval. Capture only the candidate's confirmed
state, never the target shown in the driver app.
````

## Conventions and commands

Run commands from the repository root. This is a Kotlin Android app with a vendored Karoo SDK in lib, pure product decisions under pint/core, Android adapters, JUnit tests, and a separate QA application. Existing JUnit style uses `@Test` and `assertEquals(expected, actual)`; see tools/karoo-calorie-source/src/test/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputStateTest.kt. Workflow steps use existing pinned Actions and shell commands. Prefer native shell/Python stdlib checks over a new dependency.

| Purpose | Command | Expected |
| --- | --- | --- |
| Full product gate | `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :lib:testDebugUnitTest :pint:testReleaseUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification` | Exit 0, including 100% core instruction and branch coverage |
| QA gate | `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :calorie-source:testDebugUnitTest :calorie-source:lintDebug :calorie-source:assembleDebug` | Exit 0 |
| Static resources | `node tools/validate-drawables.mjs` | Drawable visual contracts passed |
| Patch hygiene | `git diff --check` | Exit 0 |
| Scope | `git status --short` and `git diff --name-only` | Only approved scope plus pre-existing user edits |

The audit ran resource validation and patch hygiene successfully. It did not run Gradle or hardware checks. These build commands come from repository configuration, not a claimed successful audit build. Use another valid ANDROID_HOME if this machine-specific SDK path is absent. Do not install tools or dependencies without approval.

## Scope

Only these files may change:

- `plans/008-target-aware-qa-design.md`
- `plans/008-target-aware-qa-decision.md`
- `plans/README.md`

All other files are out of scope, including vendored lib, generated assets, signing material, permissions, and production dependencies. Preserve existing uncommitted user changes. Preserve all six ViewConfig semantics, one-Hz pacing, cancellation, exact counts, physical alignment, and the two-field product contract. Do not add compatibility paths.

## Git workflow

Use branch `ec/008-target-aware-qa-design` when execution is authorized. Work in an isolated checkout if other work is active. Keep one coherent change. Local history uses direct titles such as “Remove Pint Mug data field”; match that style if a commit is requested. Do not commit, push, create issues, dispatch workflows, install on a device, or publish without separate authorization.

## Steps

### Step 1: Compare the existing procedure with a target selector

Read the source and runbook. Compare retaining fixed presets plus explicit setup against a QA-only target selector with derived percentages/counts. Check standard Android numeric/SeekBar controls before proposing a custom UI. The QA app cannot read private candidate preferences. Record how the operator confirms the same target in both apps.

**Verify:** `git diff --check` → exit 0; decision file contains Current workflow and Options sections.

### Step 2: Define numeric and lifecycle rules

In plans/008-target-aware-qa-decision.md specify mappings at 150 and 180 and endpoints 80/400, percentage rounding, and behavior when a target changes below accumulated Calories. Distinguish target calories from the power calibration constant of 940 J/Calorie. Start paused, retain explicit resume, and avoid automatically claiming candidate success. Include example 50% values 75 and 90 and 100-pint values 15000 and 18000.

**Verify:** `python3 -c "assert 150*.5 == 75 and 180*.5 == 90 and 150*100 == 15000 and 180*100 == 18000"` → exit 0; arithmetic examples confirmed.

### Step 3: Produce the decision for approval

Write Recommendation, Open decisions, Scope, Verification, and Stop conditions sections. Provide an exact future file list and JUnit behavior matrix if implementation is recommended. End with approval pending; do not edit source or create a production prototype.

**Verify:** `git diff --check` → exit 0; only plans files changed.

## Test plan

No application test run is required for this design-only plan. Future tests must cover target bounds, all seven presets, rounding, pause/resume, and target reduction requiring a fresh ride. The existing CalorieOutputStateTest is the JUnit exemplar. Future build gates are listed below.

## Done criteria

- [ ] Every step's verification command produced its stated result; retain any required before-fix failure as regression evidence.
- [ ] All applicable implementation checks in the commands table pass. Design-only plans need only their listed design checks; do not run builds to validate prose.
- [ ] `git diff --check` exits 0.
- [ ] `git status --short` and `git diff --name-only` show no new changes outside Scope.
- [ ] Actual outcomes and any pending remote/device validation are recorded in this plan and plans/README.md.
- [ ] No secrets or private signing values appear in the diff.

## STOP conditions

If product-private preference access, a new dependency, or automatic candidate configuration is proposed, stop. Do not implement before the design decision is approved.

Stop if an unexpected scope file has changed since the recorded baseline, if a check fails twice after a bounded fix attempt, or if work needs an out-of-scope edit. Report the evidence rather than broadening the plan.

## Maintenance notes

Keep the driver target separate from the measured power calibration. A fixed runbook may remain the best choice; record that verdict rather than forcing a feature.

## Execution results

Reviewed current preset/state/store/activity, product target policy and native settings controls, and corrected setup from plan 002. The decision retains fixed presets for now and defines a bounded future selector contract without implementing it. All 28 numeric table values and the four specified examples pass arithmetic checks. Required decision sections and git diff --check pass. Only this plan and its decision document enter the commit. Future feature and device checks remain pending approval.
