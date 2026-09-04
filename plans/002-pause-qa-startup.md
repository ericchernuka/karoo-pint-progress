# Plan 002: Start fresh QA output paused and correct calibration setup

> Follow the steps in order. Confirm each expected result. Stop on the conditions below. Update only this plan's status row in plans/README.md when execution ends. This file is a plan; its presence is not permission to execute it.
>
> Drift check: `git diff --stat da70ab23de201215f2a391cdccda29affb67fd51..HEAD -- tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputState.kt tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputStore.kt tools/karoo-calorie-source/src/test/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputStateTest.kt docs/agents/karoo-calorie-source.md`. Compare changed scope files with the excerpts before proceeding. Expected predecessor changes are named below; reconcile those and record the new baseline in this plan. Stop on other mismatches.

## Status

- Priority: P2
- Effort: S (S: hours; M: about one day, including checks)
- Risk: LOW
- Depends on: 001
- Category: bug
- Planned at: `da70ab23de201215f2a391cdccda29affb67fd51`, 2026-09-04
- Execution status: DONE (local)

## Why this matters

Fresh preferences enable output toward 171 Calories. Setup starts a ride before selecting 90 Calories, so the source can exceed the calibration target. The controller cannot reduce cumulative Calories. The setup also checks 0.50 without first setting the product target to 180.

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

    fun resume() = copy(isEmitting = true)
}
````

`tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputStore.kt:8`

````text
    fun read(): CalorieOutputState {
        return CalorieOutputState(
            targetCalories = preferences.getFloat(TARGET_VALUE, DEFAULT_TARGET).toDouble(),
            isEmitting = preferences.getBoolean(IS_EMITTING, true),
        )
````

`tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/PowerTargetController.kt:25`

````text
        var planned = maxOf(currentCalories, plannedCalories ?: currentCalories)
        if (currentCalories >= targetCalories) {
            stableFeedbackTicks = 0
            return 0.0
        }
````

`docs/agents/karoo-calorie-source.md:29`

````text
## Pair it on Karoo

1. Open Karoo Settings and confirm `Pint QA Calorie Driver` is installed under Extensions.
2. Open Sensors and start a search. Select the extension-source filter, shown as a puzzle-piece icon.
3. Pair `Pint QA Calorie Driver` and confirm that its details show `CONNECTED`.
4. Start a ride and add the candidate Pint Progress field that needs verification.
5. Select `50%: 90` and confirm that Pint Count reaches and stays at `0.50`. Stop if it does not.

Stop if the candidate remains in `Searching...` or KOS Calories do not increase. A target visible
only in the driver app does not prove that Pint Progress received it.
````

`tools/karoo-calorie-source/src/test/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputStateTest.kt:7`

````text
    @Test
    fun `silence preserves the last value for resume`() {
        val selected = CalorieOutputState().select(CaloriePreset.NINETY_FIVE_PERCENT.calories)
        val silent = selected.silence()

        assertEquals(false, silent.isEmitting)
        assertEquals(171.0, silent.resume().targetCalories, 0.0)
        assertEquals(true, silent.resume().isEmitting)
    }

    @Test
    fun `initial output is 95 percent`() {
        assertEquals(171.0, CalorieOutputState().targetCalories, 0.0)
    }

    @Test
    fun `only the Karoo system package can control the extension`() {
        assertEquals(true, allowsKarooCaller(arrayOf("io.hammerhead.appstore")))
        assertEquals(false, allowsKarooCaller(arrayOf("example.other")))
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

- `tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputState.kt`
- `tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputStore.kt`
- `tools/karoo-calorie-source/src/test/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputStateTest.kt`
- `docs/agents/karoo-calorie-source.md`
- `plans/README.md`

All other files are out of scope, including vendored lib, generated assets, signing material, permissions, and production dependencies. Preserve existing uncommitted user changes. Preserve all six ViewConfig semantics, one-Hz pacing, cancellation, exact counts, physical alignment, and the two-field product contract. Do not add compatibility paths.

## Git workflow

Use branch `ec/002-pause-qa-startup` when execution is authorized. Work in an isolated checkout if other work is active. Keep one coherent change. Local history uses direct titles such as “Remove Pint Mug data field”; match that style if a commit is requested. Do not commit, push, create issues, dispatch workflows, install on a device, or publish without separate authorization.

## Steps

### Step 1: Add a regression assertion

Extend the existing initial-state JUnit test to assert isEmitting is false. Preserve selected target, silence, and resume assertions.

**Verify:** `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :calorie-source:testDebugUnitTest :calorie-source:lintDebug :calorie-source:assembleDebug` → the new paused-default assertion fails before the fix; stop if it fails for another reason.

### Step 2: Change only fresh defaults

Set the state default and getBoolean fallback to false. Keep select and resume as explicit ways to start output. Preserve stored user choices; do not add a preference migration. Update the runbook: pause existing installations before a fresh ride, verify candidate target 180, then select 90 and confirm 0.50. If Calories exceed 90, restart the test ride rather than lowering the target.

**Verify:** `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :calorie-source:testDebugUnitTest :calorie-source:lintDebug :calorie-source:assembleDebug` → exit 0; initial paused and explicit start behavior pass.

### Step 3: Verify the product remains separate

Run the full product gate and inspect the diff. Do not add access to candidate preferences or change power calibration.

**Verify:** `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :lib:testDebugUnitTest :pint:testReleaseUnitTest :pint:lintDebug :pint:assembleDebug :pint:assembleRelease :pint:jacocoBehaviorTestCoverageVerification` → exit 0.

## Test plan

Extend CalorieOutputStateTest using its JUnit assertEquals style. The Android SharedPreferences fallback is verified by the source diff and a fresh-install device check; do not claim a pure state test covers the adapter. Device check: no driver output before selection, then 90 Calories and candidate 0.50 at target 180. Device installation requires separate authorization.

## Done criteria

- [ ] Every step's verification command produced its stated result; retain any required before-fix failure as regression evidence.
- [ ] All applicable implementation checks in the commands table pass. Design-only plans need only their listed design checks; do not run builds to validate prose.
- [ ] `git diff --check` exits 0.
- [ ] `git status --short` and `git diff --name-only` show no new changes outside Scope.
- [ ] Actual outcomes and any pending remote/device validation are recorded in this plan and plans/README.md.
- [ ] No secrets or private signing values appear in the diff.

## STOP conditions

If the proposed fix needs resetting saved preferences, sharing app storage, or changing power conversion, stop. Hardware results remain pending without an authorized device run.

Stop if an unexpected scope file has changed since the recorded baseline, if a check fails twice after a bounded fix attempt, or if work needs an out-of-scope edit. Report the evidence rather than broadening the plan.

## Maintenance notes

Keep fresh model and preference fallback defaults aligned. Existing installations may retain enabled output, so the pause instruction is required.

## Execution results

The new paused-default assertion failed before the fix (10 tests, 1 failure). The QA tests, lint, and assembly passed after changing both fresh defaults. Selection and resume remain explicit starts. Saved choices remain intact. Product checks passed in plan 001 and will run again at the runtime milestone. Fresh-install and calibration device checks remain pending. git diff --check passed.
