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

1. Start from the intended `main` commit with a clean tree.
2. Regenerate and validate drawables.
3. Run the full verification command from `AGENTS.md`.
4. Run the device matrix in `KAROO_DATA_FIELD_CONTRACT.md`.
5. Complete the security checklist in `SECURITY.md`.
6. Choose a unique increasing `versionCode` and the intended `versionName`.
7. Build from the audited commit, sign outside the repository, and verify the final APK.
8. Complete a copy of the [release evidence template](RELEASE_EVIDENCE_TEMPLATE.md). Keep every
   failed and waived row visible. For a pre-release candidate, paste the completed record into the
   pull request description or a dedicated PR comment before moving the candidate to `main`.
   Upload photos, recordings, and logs as PR attachments and link them from the record. Do not
   commit candidate evidence or captures unless a maintainer explicitly requests it.

## Automated signed release

Push a tag such as `v1.0.0` after the candidate gate. The tag-only release workflow builds with
that version name and its monotonically increasing GitHub Actions run number, verifies the signing
certificate and checksum, and publishes the APK to a GitHub Release. Configure the four
`PINT_*` signing secrets in the repository before using this workflow. Verification builds never
receive these secrets.

Example unsigned candidate build:

```bash
./gradlew -PpintVersionCode=NNN -PpintVersionName=X.Y.Z :pint:assembleRelease
```

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
