## Release checks

The workflow records the commit, version, signer, build, and checksum above. Test the attached APK.
Mark a box only after the check passes. For a failure or waiver, leave it unchecked and record the
result, reason, and approver below. Publishing this draft is maintainer approval.

Tester / date / Karoo model / software version: _fill in_

- [ ] Review the security checklist in `docs/SECURITY.md`.
- [ ] Confirm the attached APK checksum and installation over the previous signed version.

### Device checks

The device matrix in `docs/KAROO_DATA_FIELD_CONTRACT.md` defines these checks. Debug diagnostic
rows use a debug build; record the corresponding visible behavior against the signed APK separately.

- [ ] Preview and in-ride modes
- [ ] Pints Fill count remains readable in large, medium, short, narrow, and small tiles, boundaries off and on
- [ ] Left, center, and right alignment
- [ ] Boundaries off and on
- [ ] Completed counts `0`, `1`, `99`, and `100` remain visible over Pints Fill
- [ ] Light and dark system themes
- [ ] Unavailable calories, normal fill, 80% foam, full foam, and drain
- [ ] Count preview sequence; live `0.00`, `0.10`, `0.90`, and `1.00`; and a three-digit total in narrow and roomy tiles
- [ ] Pints Fill picker preview in named narrow layout: 50%/`0`, 80%/`0`, full foam/`1`, drain/`1`
- [ ] Pints Fill picker preview in named roomy layout: 50%/`0`, 80%/`0`, full foam/`1`, drain/`1`
- [ ] Numeric page-editor host placeholder: record the observed value with exact device and KOS evidence
- [ ] Numeric preview extension messages: `0.5`, `0.9`, `1`, `1.1` at one Hz
- [ ] Pints Fill preview detach **debug diagnostic**: `cancellation label=fill-preview`
- [ ] Numeric preview detach **debug diagnostic**: `cancellation label=numeric-preview`
- [ ] Pints Fill preview detach **signed candidate**: no later frame and clean re-entry
- [ ] Numeric preview detach **signed candidate**: no later frame and clean re-entry
- [ ] Default, minimum, maximum, and mid-ride calories-per-beer changes
- [ ] Install over the previous signed build without uninstalling
- [ ] Existing Pint Mug tile no longer resolves and can be replaced manually with Pints Fill or Pints Count

### Failures, waivers, and evidence

Record results and links here. State “None” if there are no failures or waivers.

### Approval

Approver / date: _fill in before publishing_
