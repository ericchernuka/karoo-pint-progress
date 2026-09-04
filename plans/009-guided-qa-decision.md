# Guided QA sequence decision

Status: design study complete; recommendation awaits maintainer approval. No sequence runner implemented.

Basis: baseline `da70ab2`, corrected setup from plan 002, and plan 008's recommendation to retain the fixed 180-Calorie target.

## Current workflow

`docs/agents/karoo-calorie-source.md` defines manual threshold, multi-pint, dropout, and target-change operations. The operator selects a target and then checks the candidate field. The driver subscribes to native Calories in `CalorieSourceExtension.kt:33`; it has no observation of the candidate's displayed frame, numeric placeholder, alignment, or theme.

The existing controller budgets power, handles delayed feedback, stops at or above target, and retries a correction after 60 unchanged samples (`PowerTargetController.kt:9`). `PowerTargetControllerTest.kt` already checks these behaviors. Reaching driver feedback alone cannot establish a complete device-matrix pass.

## Options

| Option | Value | Cost and limit |
| --- | --- | --- |
| Retain the manual runbook | Uses the current working controls and explicit visual confirmation | Operator performs each step and records evidence |
| Add a bounded 50%-to-one-pint guide | Can prompt the next operation and reduce repeated navigation | Adds lifecycle/state rules but still requires candidate confirmation |
| Automate the whole device matrix | Could reduce some repeated input | Cannot verify many display states through existing APIs; exceeds this study's justified scope |

## Recommendation

Keep the manual runbook for this release. The repository provides no measured operator-time problem that warrants another controller now. If repeated sessions establish that navigation is the bottleneck, start with a two-stage guide for 90 then 180 Calories at a product target of 180. Keep dropout and live target changes manual until there is device evidence for their observable completion conditions.

This recommendation completes the study. It does not reject a future guide or approve one for implementation.

## Proposed bounded sequence

For any future prototype, a fresh ride and matching candidate target are prerequisites. The user explicitly starts the sequence. The guide drives to 90, pauses output, and asks the operator to confirm the candidate shows 0.50 and record evidence. Only explicit confirmation starts the 180-Calorie stage. At 180, it pauses again for confirmation of the completed-pint result. It does not mark the full foam/drain animation as passed from final Calories alone; that requires an observation or recording.

Use existing native buttons and status text. Reuse `PowerTargetController` for power calculation. A new guide owns only stage progression; it does not duplicate the power budget or change the one-second emission loop. A return to the app must not restart an interrupted guide automatically.

## State and recovery contract

| State | Meaning | Exit |
| --- | --- | --- |
| Paused | No sequence output; initial and restart state | Explicit Start after setup confirmation |
| Running | Drive the current stage through the existing controller | Target reached, cancellation, or a fault |
| Waiting for confirmation | Output paused; target reached but candidate not yet verified | Explicit confirmation or cancellation |
| Completed | Both stages confirmed by the operator | Explicit new session using a fresh ride |
| Failed | Output paused with a specific fault | Explicit reset after correcting the cause |

| Input | Required result |
| --- | --- |
| No Calories feedback at start or during a stage | Pause with an unavailable-feedback reason; no blind timed advance |
| Feedback exceeds the stage target | Fail the stage; require a fresh ride, never command a lower target to undo Calories |
| Feedback decreases during a stage | Treat as ride reset and pause; do not silently restart |
| User cancels or selects a manual preset | End the guide, then honor the explicit manual action |
| Process restart | Start paused; do not restore a running stage |
| Selected timeout expires | Fail and pause; no automatic next stage |
| Candidate confirmation is absent or rejected | Remain paused; no automatic pass or waiver |

The guide needs a timeout based on an authorized device baseline. Do not infer it from the 60-sample correction delay: that is one controller interval, not a guarantee of KOS latency. No universal timeout value was established in this source-only study.

## Open decisions

The maintainer must approve a guide and its timeout policy before implementation. Confirm whether pausing fake Speed triggers auto-pause on the intended KOS and how that affects resuming a stage. Establish what evidence the operator records at each confirmation. Plan 008 remains a recommendation; the proposed first guide retains its fixed 180-Calorie setup unless a later target-selector decision is approved.

## Scope

This study changes only `plans/009-guided-qa-decision.md` and its execution record. If approved later, the first implementation would be limited to:

- `tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieSourceActivity.kt`
- `tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieSourceExtension.kt`
- `tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputState.kt`
- `tools/karoo-calorie-source/src/main/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputStore.kt`
- `tools/karoo-calorie-source/src/main/res/layout/activity_calorie_source.xml`
- `tools/karoo-calorie-source/src/main/res/values/strings.xml`
- `tools/karoo-calorie-source/src/test/kotlin/io/ericchernuka/pintprogress/caloriesource/CalorieOutputStateTest.kt`
- `docs/agents/karoo-calorie-source.md`

A dedicated policy file is justified only if the approved stage logic no longer fits coherently in the current state file. Such a file requires an updated implementation plan first. Candidate code, SDK changes, power calibration, permissions, background services, and automatic evidence approval remain outside scope.

## Verification

This study requires a scope and whitespace check, not a build. A future policy check should feed scripted Calories and a controlled clock through the production stage logic. Cover 90-to-180 success with both confirmations; unavailable feedback; overshoot; reset; cancellation; timeout; manual override; and process restart. Assert paused output at every stop and no transition without confirmation. Use the current QA JUnit style; do not add a framework or real timer sleeps.

After any future implementation, run `ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew :calorie-source:testDebugUnitTest :calorie-source:lintDebug :calorie-source:assembleDebug` and the current full gate from AGENTS.md. A separately authorized Karoo check must verify resume after pause, displayed candidate states, and detach behavior. Simulated feedback is not device evidence.

## Stop conditions

Stop if the guide requires a new service, permission, cross-app controls, candidate APK changes, or automatic pass/waiver decisions. Stop if available feedback cannot distinguish a required state. Preserve the manual runbook as the accepted workflow until a new design is approved.
