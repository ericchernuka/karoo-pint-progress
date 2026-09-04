# Plan 009: Decide whether guided QA sequences reduce manual work

> Follow the steps in order. Confirm each expected result. Stop on the conditions below. Update only this plan's status row in plans/README.md when execution ends. This file is a plan; its presence is not permission to execute it.
>
> Drift check: `git diff --stat da70ab23de201215f2a391cdccda29affb67fd51..HEAD -- plans/009-guided-qa-design.md plans/009-guided-qa-decision.md`. Compare changed scope files with the excerpts before proceeding. Expected predecessor changes are named below; reconcile those and record the new baseline in this plan. Stop on other mismatches.

## Status

- Priority: P3
- Effort: M (S: hours; M: about one day, including checks)
- Risk: LOW
- Depends on: 002, 008
- Category: direction
- Planned at: `da70ab23de201215f2a391cdccda29affb67fd51`, 2026-09-04
- Execution status: DONE (design study; feature approval pending)

## Why this matters

The runbook repeats threshold, multi-pint, dropout, and target-change operations manually. A guided sequence could reduce repeated input, but the driver only observes native Calories, not every candidate display state. This plan is a design study, not feature implementation.

## Current state

`docs/agents/karoo-calorie-source.md:48`

````text
Use the current element references from the snapshot. Select a button with `click <ref> --settle`,
verify the `Driving toward N Calories` status, navigate back to the ride, and wait for the candidate
to reach the requested state before capture. KOS applies Calories after the related Power samples,
so a high target can take several minutes. If confirmed Calories settle below the target, the driver
waits 60 unchanged samples and applies a small correction automatically.
````

`docs/agents/karoo-calorie-source.md:72`

````text
The 90-Calorie check is the calibration gate for the current Karoo and user profile. Do not use
high-count captures when it settles at a different value. On the validated Karoo, 17,820 Calories
can take about six minutes before the correction interval. Capture only the candidate's confirmed
state, never the target shown in the driver app.

For a multi-pint jump, reach `50%: 90`, then select `2.5 pints: 450`. For a dropout, reach
`95%: 171`, select `Pause output`, wait until the candidate shows its unavailable state, then select
`Resume last value`. For a live target change, keep `95%: 171` active after the driver reaches the
target, then change the target in Pint Progress settings.
````

`tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/PowerTargetController.kt:9`

````text
    fun watts(currentCalories: Double?, targetCalories: Double): Double {
        currentCalories ?: return 0.0
        val previousCalories = lastCurrentCalories
        val feedbackChanged = previousCalories != currentCalories
        if (previousCalories?.let { currentCalories < it } == true) {
            activeTargetCalories = null
            plannedCalories = null
            stableFeedbackTicks = 0
        }
        lastCurrentCalories = currentCalories
        if (activeTargetCalories != targetCalories) {
            activeTargetCalories = targetCalories
            plannedCalories = currentCalories
            stableFeedbackTicks = 0
        }

        var planned = maxOf(currentCalories, plannedCalories ?: currentCalories)
        if (currentCalories >= targetCalories) {
            stableFeedbackTicks = 0
            return 0.0
        }
        if (planned >= targetCalories) {
            stableFeedbackTicks = if (feedbackChanged) 1 else stableFeedbackTicks + 1
            if (stableFeedbackTicks < CORRECTION_DELAY_TICKS) return 0.0

            planned = currentCalories
            plannedCalories = currentCalories
            stableFeedbackTicks = 0
        }

        val remaining = targetCalories - planned
        val watts = (remaining * JOULES_PER_CALORIE).coerceAtMost(MAXIMUM_WATTS)
        plannedCalories = planned + watts / JOULES_PER_CALORIE
        return watts
````

`tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieSourceActivity.kt:36`

````text
        }
        findViewById<Button>(R.id.output_resume).setOnClickListener {
            outputStore.write(outputStore.read().resume())
            renderStatus()
        }

        renderStatus()
    }

    private fun renderStatus() {
        val output = outputStore.read()
        status.text = if (!output.isEmitting) {
            getString(R.string.status_silent)
        } else {
            getString(R.string.status_emitting, output.targetCalories.toInt())
        }
    }
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

- `plans/009-guided-qa-design.md`
- `plans/009-guided-qa-decision.md`
- `plans/README.md`

All other files are out of scope, including vendored lib, generated assets, signing material, permissions, and production dependencies. Preserve existing uncommitted user changes. Preserve all six ViewConfig semantics, one-Hz pacing, cancellation, exact counts, physical alignment, and the two-field product contract. Do not add compatibility paths.

## Git workflow

Use branch `ec/009-guided-qa-design` when execution is authorized. Work in an isolated checkout if other work is active. Keep one coherent change. Local history uses direct titles such as “Remove Pint Mug data field”; match that style if a commit is requested. Do not commit, push, create issues, dispatch workflows, install on a device, or publish without separate authorization.

## Steps

### Step 1: Define one bounded sequence

Compare the current manual checklist with a QA-only 50% to one-pint sequence. Start from a fresh ride and an explicit operator action. Define what native Calories can prove and what still needs a candidate screenshot or operator confirmation. Use the target decision from plan 008 only if approved; otherwise retain the documented fixed target of 180.

**Verify:** `git diff --check` → exit 0; decision file records scope and observable limits.

### Step 2: Specify stop and recovery behavior

In plans/009-guided-qa-decision.md define paused, running, waiting-for-confirmation, completed, and failed behavior only if each is needed. State timeout, cancellation, overshoot, missing feedback, ride reset, process restart, and manual override rules. Reuse the existing power controller. Never advance based only on elapsed time or emit a lower cumulative target as recovery. Dropout and target-change scenarios can remain manual.

**Verify:** `git diff --check` → exit 0; every failure input has a defined paused or stopped result.

### Step 3: Write an approval-ready recommendation

Include Current workflow, Options, Recommendation, Open decisions, Scope, Verification, and Stop conditions. Specify whether a checklist is sufficient or a minimal runner is justified. If recommending implementation, list exact files and simulated-feedback tests. Mark implementation pending approval and device evidence pending an authorized run.

**Verify:** `git diff --check` → exit 0; only plans files changed.

## Test plan

Future tests should feed scripted Calories and cover completion, unavailable feedback, overshoot, reset, cancel, timeout, and manual override. Use existing PowerTargetControllerTest style and no real timers for policy tests. Keep physical KOS dropout and displayed candidate state as device checks.

## Done criteria

- [ ] Every step's verification command produced its stated result; retain any required before-fix failure as regression evidence.
- [ ] All applicable implementation checks in the commands table pass. Design-only plans need only their listed design checks; do not run builds to validate prose.
- [ ] `git diff --check` exits 0.
- [ ] `git status --short` and `git diff --name-only` show no new changes outside Scope.
- [ ] Actual outcomes and any pending remote/device validation are recorded in this plan and plans/README.md.
- [ ] No secrets or private signing values appear in the diff.

## STOP conditions

If the sequence needs a background service, permission, cross-app control, or automatic pass/waiver decisions, stop. This design must not change the candidate APK.

Stop if an unexpected scope file has changed since the recorded baseline, if a check fails twice after a bounded fix attempt, or if work needs an out-of-scope edit. Report the evidence rather than broadening the plan.

## Maintenance notes

A driver reaching its own target is not proof of the candidate display. Keep the operator confirmation boundary explicit.

## Execution results

Reviewed the driver feedback boundary, PowerTargetController and tests, activity actions, corrected manual runbook, and plan 008 decision. The study retains the manual sequence for now and specifies the limited future 90-to-180 guide, explicit confirmation, and all requested stop/recovery inputs. No timeout is invented without device latency evidence. Required decision sections and git diff --check pass. Only this plan and its decision document enter the commit. No runner was implemented; future design approval and physical KOS checks remain pending.
