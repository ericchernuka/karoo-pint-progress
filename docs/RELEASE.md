# Release process

## Artifact classes

| Artifact | Purpose | Signing |
| --- | --- | --- |
| Local or CI debug APK | Development and Karoo smoke testing | Debug key, not distributable |
| Release APK from the standard build | Verification input | Intentionally unsigned |
| Published APK | User distribution | Stable project-owned private key |

GitHub Actions assigns each run a monotonically increasing `versionCode`, but its debug signing key
is ephemeral. Debug APKs from different runs may require uninstalling the prior build. Normal
verification pushes do not publish debug APKs; a manually dispatched verification run can produce a
short-lived artifact explicitly marked `UNSAFE-DEBUG`.

## Candidate gate

1. Choose the exact candidate commit, version name, and an integer `versionCode` greater than the
   previous published APK. Confirm the previous version from its package metadata and record it.
2. Dispatch `Release` in `prepare` mode on that candidate ref. The `commit` input must equal the
   dispatch SHA. Preparation checks the complete product and QA gate, builds and signs once, and
   stores `pint-candidate-<run ID>-<attempt>` for 30 days. It does not publish a release.
3. Download that exact artifact. Record its ID and archive SHA-256 from the Actions artifact API,
   plus `candidate.json`, the APK checksum, and the approved signing fingerprint.
4. Install and test that signed APK against the full device matrix in
   `KAROO_DATA_FIELD_CONTRACT.md`. Complete the security checklist in `SECURITY.md`. Do not rebuild
   or replace it after recording evidence. Keep debug diagnostic results separate.
5. Complete the [release evidence template](RELEASE_EVIDENCE_TEMPLATE.md) and its publication JSON.
   Keep every failed and waived row visible. Store the record and captures in the PR or release
   evidence location; do not commit completed evidence to this repository.
6. After approval, create the release tag `v<version name>` at the candidate commit through the
   authorized repository process. Dispatch `Release` in `publish` mode with the same commit and
   version inputs, the exact preparation run ID/attempt and artifact ID, and the approval JSON.

## Automated signed release

The workflow has two manual modes. Only `prepare` receives the four `PINT_*` signing secrets.
`publish` has Actions read access and release write access, downloads the exact artifact ID, and
runs no Gradle tasks or signing operations. It uses checker code from the default branch, so merge
the workflow and checker change before the first publication. Existing pinned Actions are reused.

Publication checks the successful preparation attempt and its `prepare` job, candidate commit,
artifact name and ID, API archive digest, actual archive bytes, package/version metadata, APK
checksum, approved signer, tag commit, and approval identity. It preserves the candidate version
code; the publication run number has no effect. A successful publication-mode run cannot serve as
preparation. A missing, expired, or mismatched artifact blocks publication. Prepare and test a new
candidate if retention expires; there is no rebuild fallback.

The approval JSON includes every automated and device row from the evidence template. `APPROVED`
requires all rows to be `PASS`. `APPROVED WITH WAIVERS` permits `PASS` and explicit `WAIVED` rows,
each with its reason and approver. Any `FAIL`, pending, missing, or duplicate row blocks publication.
The JSON checker validates a maintainer assertion; it cannot prove a physical device test occurred.
The workflow attaches the APK, basename checksum, candidate identity, approval JSON, and a Markdown
link to the full evidence record to the release. The checksum can be checked beside the downloaded
APK with `sha256sum --check pint-release.apk.sha256`.

Before first use, configure the approved public signer variable and signing secrets, then perform
an authorized remote preparation and publication check. Confirm downloaded and published APK
hashes agree. Local fixture tests do not establish GitHub permissions or device results. Tag
protection and maintainer access must prevent moving an approved tag during publication.

Unsigned local builds remain available for build verification:

```bash
./gradlew -PpintVersionCode=NNN -PpintVersionName=X.Y.Z :pint:assembleRelease
```

The workflow follows the official [artifact API](https://docs.github.com/en/rest/actions/artifacts),
[workflow run attempt API](https://docs.github.com/en/rest/actions/workflow-runs#get-a-workflow-run-attempt),
[GitHub CLI API](https://cli.github.com/manual/gh_api), and
[release creation](https://cli.github.com/manual/gh_release_create) contracts.

## Release evidence

For every published release, include a completed copy of the [release evidence
template](RELEASE_EVIDENCE_TEMPLATE.md) in the GitHub Release notes or attach it as a Markdown
evidence file. Record together:

- Git commit SHA;
- version name and code;
- APK SHA-256;
- signing certificate SHA-256 fingerprint;
- successful CI run;
- completed device matrix;
- release notes and known limitations.

The template is the record format. Keep failed and waived rows visible, with the reason and
approver. If the APK bytes, candidate commit, or signing identity changes after evidence
collection, the record is invalid and the relevant checks must run again.

Keep pre-release evidence with its pull request. Do not add completed candidate records, photos,
recordings, or logs to the source tree unless a maintainer explicitly requests repository storage.
Published-release evidence belongs in the GitHub Release notes or release attachments.

Useful verification commands:

```bash
apkanalyzer manifest version-code app.apk
apkanalyzer manifest version-name app.apk
apksigner verify --print-certs app.apk
sha256sum app.apk
```

## Publication rules

- Never publish the CI debug artifact as a release.
- Never publish an unsigned release APK.
- Never rebuild after evidence collection.
- Keep package name `io.ericchernuka.pintprogress` and the signing identity stable for upgrades.
- If using a Karoo delivery manifest, ensure its URL, `latestVersionCode`, APK, and checksum agree.

## Approved signer configuration

A maintainer must set the repository variable `PINT_SIGNER_SHA256` to the public 64-hex SHA-256
certificate fingerprint from a maintainer-approved prior release record. Do not derive the expected
fingerprint from the candidate under test. Missing or malformed configuration blocks publication.
The checker accepts one matching certificate and rejects extra signers. Certificate rotation needs
an explicit policy change. This setting does not change unsigned local builds.

The approved release fingerprint has not been supplied in this checkout. Configuration and an
authorized remote verification run remain pending; no private key is needed to set this public value.
