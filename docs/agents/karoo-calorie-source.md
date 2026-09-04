# QA calorie source

Use this debug-only companion APK when a Karoo test needs controlled native Calories values. It
advertises one sensor named `Pint QA Calorie Driver`. The sensor emits Power and a fixed Speed once
per second, which keeps the test ride out of auto-pause. It budgets power at no more than 50 kW,
using the measured 940 joules per displayed Calorie, and reads KOS Calories as feedback. KOS does
not accept Calories as a sensor-source type, so the driver must use this supported Power path. Its
package is separate from Pint Progress, so it does not change the candidate APK.

Do not use this APK for normal rides or release distribution. Do not use a screenshot of its control
screen as product proof.

## Build and install

Keep the candidate commit and APK identity in the evidence record. Build the source from the same
repository checkout, then install both APKs without rebuilding or replacing the candidate:

```bash
ANDROID_HOME=/Users/ec/Library/Android/sdk ./gradlew \
  :calorie-source:testDebugUnitTest \
  :calorie-source:assembleDebug

adb install -r tools/karoo-calorie-source/build/outputs/apk/debug/calorie-source-debug.apk
```

The source package is `io.ericchernuka.pintprogress.caloriesource`. The extension ID is
`pintprogress-calorie-source`.

## Pair it on Karoo

1. Open Karoo Settings and confirm `Pint QA Calorie Driver` is installed under Extensions.
2. Open Sensors and start a search. Select the extension-source filter, shown as a puzzle-piece icon.
3. Pair `Pint QA Calorie Driver` and confirm that its details show `CONNECTED`.
4. Open the driver and select `Pause output` before starting a fresh ride. Fresh installations start
   paused; existing installations retain their saved output choice.
5. Set the candidate Pint Progress target to 180 Calories. Start a fresh ride and add the candidate
   field that needs verification.
6. Select `50%: 90` and confirm that Pint Count reaches and stays at `0.50`. Stop if it does not.

If ride Calories exceed 90 before calibration, restart the test ride. Lowering the driver target
cannot reduce cumulative Calories.

Stop if the candidate remains in `Searching...` or KOS Calories do not increase. A target visible
only in the driver app does not prove that Pint Progress received it.

## Drive exact values

Open the control app with `agent-device` and keep the returned session active:

```bash
agent-device open io.ericchernuka.pintprogress.caloriesource --foreground
```

Use the current element references from the snapshot. Select a button with `click <ref> --settle`,
verify the `Driving toward N Calories` status, navigate back to the ride, and wait for the candidate
to reach the requested state before capture. KOS applies Calories after the related Power samples,
so a high target can take several minutes. If confirmed Calories settle below the target, the driver
waits 60 unchanged samples and applies a small correction automatically.
Use `agent-device screenshot` when the accessibility snapshot is sparse. Close the session when the
capture set is complete:

```bash
agent-device close
```

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

For a multi-pint jump, reach `50%: 90`, then select `2.5 pints: 450`. For a dropout, reach
`95%: 171`, select `Pause output`, wait until the candidate shows its unavailable state, then select
`Resume last value`. For a live target change, keep `95%: 171` active after the driver reaches the
target, then change the target in Pint Progress settings.

## Evidence rules

Record these facts before and after the capture set:

- candidate commit, version name, version code, APK SHA-256, and signing-certificate SHA-256;
- installed candidate package identity from the Karoo;
- active Power-source pairing and connected state;
- the selected Calories target and the visible candidate result.

Upload proof images or video as GitHub pull-request attachments. Keep the written evidence in the PR
description or a PR comment. Do not commit proof media or an evidence report to this repository.

This harness does not prove host behavior that KOS does not expose. In particular, it cannot prove
literal live removal of a data field when the host has no supported edit action. Record that item as
blocked or obtain a requirement waiver. It also cannot replace a night-brightness capture on the
physical display.

## Remove it

Disconnect the fake sensor before normal use, then uninstall the companion APK:

```bash
adb uninstall io.ericchernuka.pintprogress.caloriesource
```

The candidate Pint Progress package and its stored settings are separate and remain installed.
