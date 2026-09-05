# Release process

## Release in three steps

1. In GitHub Actions, select **Release**, choose the branch to release, and enter a version such as
   `1.2.0` or `v1.2.0`. Run the workflow. Use a new version for each candidate.
2. Open the draft release linked in the run summary. Download its `pint-release.apk`, install it on
   Karoo, and complete the device checklist in the draft notes. Record failures and explicit waivers.
3. When satisfied, edit the draft and click **Publish release**. This is your approval of the attached
   APK. Keep its assets, tag, and target commit unchanged after testing. If code or bytes change,
   create and test a new candidate.

The workflow builds and signs once, checks the approved signer and package metadata, and attaches
that APK and its SHA-256 file to a draft. It never publishes automatically. No second workflow,
manual commit SHA, artifact IDs, approval JSON, or separate tag command is needed. The draft targets
exactly the commit selected when the run starts. Existing tags and releases are rejected.

Version codes are derived from the version: `major * 1000000 + minor * 1000 + patch`.
For example, `1.2.0` becomes `1002000`. Minor and patch must be at most 999, and the code must fit
Android's positive range through 2100000000. The workflow rejects a code that does not exceed the
previous published APK's code. If downloading or inspecting that APK fails, the build fails closed.

## One-time setup

Merge this workflow into the default branch so manual dispatch is available. Configure these
repository secrets with the existing project signing key:

- `PINT_KEY_ALIAS`
- `PINT_KEY_PASSWORD`
- `PINT_KEYSTORE_PASSWORD`
- `PINT_KEYSTORE_BASE64`

Set repository variable `PINT_SIGNER_SHA256` to the approved certificate's 64-character SHA-256
fingerprint. Use an independent prior release record, not the candidate under test. Missing or
invalid settings stop the run with an error. Private signing material belongs only in secrets.

Repository write access permits editing and publishing drafts. Restrict that access to trusted
maintainers. Device evidence and approval are manual checks; the workflow does not validate the
checklist or prevent a maintainer from replacing draft assets. Review the completed draft before
publication. Keep release tags protected and consider GitHub immutable releases for published assets.

The first remote run still needs validation: confirm the draft is private to authorized users,
its target commit is correct, and its signed APK upgrades the previous release. After publication,
compare the downloaded APK with the checksum recorded during testing. Local tests cannot establish
remote permissions, signing configuration, or physical device results.

## Evidence and verification

The workflow inserts the [release evidence template](RELEASE_EVIDENCE_TEMPLATE.md) into the draft
notes and records commit, version, signer, build URL, and APK checksum above it. Complete the device
rows and decision there. Link captures when useful. Keep failures and waivers visible, with reasons
and approvers. Completed evidence belongs in the release or PR, not in the source tree.

Signer rejection tests remain automated. Input tests cover version normalization, version-code
bounds, and missing or malformed signer settings. The old cross-run artifact and approval-JSON
checks are removed because this flow has no cross-run artifact transfer or parsed approval. The
same signed file is uploaded directly to the draft, and publication is a maintainer action.

Run the full verification and release checker commands in `AGENTS.md` for workflow changes.
Unsigned local builds and debug APKs are development artifacts, not distributable releases.
Never replace the stable signing key for an ordinary upgrade.

Useful checks beside the downloaded APK:

```bash
apkanalyzer manifest version-code pint-release.apk
apksigner verify --print-certs pint-release.apk
shasum -a 256 -c pint-release.apk.sha256
```

If using a Karoo delivery manifest, ensure its URL, version code, APK, and checksum agree.

This flow uses GitHub's native [draft release creation](https://cli.github.com/manual/gh_release_create)
and [manual publication](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository).
