"""Validate candidate bytes, GitHub provenance, and the maintainer's approval assertion."""
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import zipfile

IDENTITY_FIELDS = ('commit', 'package', 'version_name', 'version_code', 'apk_sha256',
                   'signer_sha256', 'run_id', 'run_attempt')


def require(condition, message):
    if not condition:
        raise ValueError(message)


def required_checks():
    template = Path(__file__).resolve().parent.parent / 'docs/RELEASE_EVIDENCE_TEMPLATE.md'
    checks = []
    active = False
    for line in template.read_text().splitlines():
        if line.startswith('## '):
            active = line in ('## Automated gate', '## Karoo device matrix')
        if active and line.startswith('| ') and '<PASS, FAIL, or WAIVED>' in line:
            checks.append(line.split('|')[1].strip())
    require(bool(checks), 'Evidence template has no required checks')
    return checks


def identity_valid(identity):
    require(re.fullmatch(r'[0-9a-f]{40}', identity['commit']), 'Invalid commit')
    require(identity['package'] == 'io.ericchernuka.pintprogress', 'Invalid package')
    require(re.fullmatch(r'[0-9A-Za-z][0-9A-Za-z.+-]{0,63}', identity['version_name']), 'Invalid version name')
    require(type(identity['version_code']) is int and 0 < identity['version_code'] <= 2100000000,
            'Invalid version code')
    for field in ('apk_sha256', 'signer_sha256'):
        require(re.fullmatch(r'[0-9a-f]{64}', identity[field]), 'Invalid ' + field)
    for field in ('run_id', 'run_attempt'):
        require(type(identity[field]) is int and identity[field] > 0, 'Invalid ' + field)


def verify(record, approval, measured, run, artifact, jobs, expected, archive_digest):
    identity_valid(record)
    for field in IDENTITY_FIELDS:
        require(record[field] == approval['identity'][field] == measured[field] == expected[field],
                'Candidate identity mismatch: ' + field)
    require(run['id'] == record['run_id'] and run['run_attempt'] == record['run_attempt']
            and run['head_sha'] == record['commit'] and run['event'] == 'workflow_dispatch'
            and run['status'] == 'completed' and run['conclusion'] == 'success'
            and run['path'] == '.github/workflows/release.yml', 'Not a successful candidate preparation run')
    prepare = [job for job in jobs['jobs'] if job['name'] == 'prepare']
    publish = [job for job in jobs['jobs'] if job['name'] == 'publish']
    require(len(prepare) == 1 and prepare[0]['conclusion'] == 'success'
            and all(job['conclusion'] == 'skipped' for job in publish), 'Run did not prepare a candidate')
    require(type(artifact['id']) is int and artifact['id'] > 0
            and artifact['id'] == approval['artifact_id'] == expected['artifact_id']
            and artifact['expired'] is False
            and artifact['name'] == f"pint-candidate-{record['run_id']}-{record['run_attempt']}"
            and artifact['workflow_run']['id'] == record['run_id']
            and artifact['workflow_run']['head_sha'] == record['commit'], 'Artifact provenance mismatch')
    require(re.fullmatch(r'[0-9a-f]{64}', archive_digest)
            and archive_digest == approval['artifact_sha256']
            and artifact['digest'] == 'sha256:' + archive_digest, 'Artifact archive digest mismatch')
    require(approval['decision'] in ('APPROVED', 'APPROVED WITH WAIVERS'), 'Candidate is not approved')
    require(isinstance(approval['approver'], str) and approval['approver'].strip(), 'Missing approver')
    require(isinstance(approval['evidence'], str) and approval['evidence'].startswith('https://'), 'Missing evidence link')
    rows = approval['checks']
    require(isinstance(rows, list) and len(rows) == len(required_checks())
            and {row['name'] for row in rows} == set(required_checks()), 'Missing or duplicate evidence checks')
    waived = False
    for row in rows:
        require(isinstance(row['evidence'], str) and row['evidence'].strip(), 'Missing check evidence')
        require(row['result'] in ('PASS', 'WAIVED'), 'Failed or pending check')
        if row['result'] == 'WAIVED':
            waived = True
            require(approval['decision'] == 'APPROVED WITH WAIVERS'
                    and isinstance(row.get('reason'), str) and row['reason'].strip()
                    and isinstance(row.get('approver'), str) and row['approver'].strip(), 'Incomplete waiver')
    require(approval['decision'] != 'APPROVED WITH WAIVERS' or waived, 'No explicit waivers')


def unpack(archive, destination, expected_digest):
    require(hashlib.sha256(archive.read_bytes()).hexdigest() == expected_digest, 'Archive checksum mismatch')
    with zipfile.ZipFile(archive) as source:
        names = source.namelist()
        require(len(names) == 3 and set(names) == {'candidate.json', 'pint-release.apk', 'pint-release.apk.sha256'},
                'Unexpected artifact contents')
        checksum = hashlib.sha256(source.read('pint-release.apk')).hexdigest() + '  pint-release.apk\n'
        require(source.read('pint-release.apk.sha256').decode() == checksum, 'Invalid APK checksum file')
        destination.mkdir(parents=True, exist_ok=False)
        for name in names:
            (destination / name).write_bytes(source.read(name))


def expected_identity():
    return dict(commit=os.environ['CANDIDATE_COMMIT'], package='io.ericchernuka.pintprogress',
                version_name=os.environ['VERSION_NAME'], version_code=int(os.environ['VERSION_CODE']),
                apk_sha256=os.environ['APK_SHA256'], signer_sha256=os.environ['PINT_SIGNER_SHA256'].lower(),
                run_id=int(os.environ['PREPARE_RUN_ID']), run_attempt=int(os.environ['PREPARE_ATTEMPT']))


def main():
    mode, directory = sys.argv[1], Path(sys.argv[2])
    if mode == 'unpack':
        approval = json.loads(os.environ['APPROVAL_JSON'])
        unpack(directory / 'artifact.zip', directory / 'candidate', approval['artifact_sha256'])
        return
    identity = expected_identity()
    identity_valid(identity)
    measured = identity | dict(package=os.environ['APK_PACKAGE'], version_name=os.environ['APK_VERSION_NAME'],
                               version_code=int(os.environ['APK_VERSION_CODE']))
    require(measured == identity, 'Measured APK metadata mismatch')
    if mode == 'record':
        (directory / 'candidate.json').write_text(json.dumps(identity, indent=2) + '\n')
        return
    require(mode == 'verify', 'Unknown command')
    record = json.loads((directory / 'candidate/candidate.json').read_text())
    approval = json.loads(os.environ['APPROVAL_JSON'])
    # The approved APK hash is independent of the measured downloaded bytes.
    expected = identity | dict(apk_sha256=approval['identity']['apk_sha256'], artifact_id=int(os.environ['ARTIFACT_ID']))
    verify(record, approval, measured, json.loads((directory / 'run.json').read_text()),
           json.loads((directory / 'artifact.json').read_text()), json.loads((directory / 'jobs.json').read_text()),
           expected, hashlib.sha256((directory / 'artifact.zip').read_bytes()).hexdigest())
    (directory / 'approval.json').write_text(json.dumps(approval, indent=2) + '\n')
    (directory / 'evidence.md').write_text('Release evidence: ' + approval['evidence'] + '\n\n'
                                         + 'Decision: ' + approval['decision'] + '\n\n'
                                         + 'Approver: ' + approval['approver'] + '\n')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, KeyError, TypeError, OSError, IndexError, zipfile.BadZipFile) as error:
        sys.exit('Candidate verification failed: ' + str(error))
