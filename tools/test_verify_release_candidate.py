import hashlib
import json
import os
import subprocess
import sys
import importlib.util
from pathlib import Path
import tempfile
import unittest
import zipfile

spec = importlib.util.spec_from_file_location('candidate', Path(__file__).with_name('verify-release-candidate.py'))
candidate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(candidate)


class CandidateTest(unittest.TestCase):
    def setUp(self):
        self.identity = dict(commit='a' * 40, package='io.ericchernuka.pintprogress',
                             version_name='1.2.3', version_code=123, apk_sha256='b' * 64,
                             signer_sha256='c' * 64, run_id=42, run_attempt=2)
        self.approval = dict(identity=self.identity.copy(), artifact_id=71, artifact_sha256='d' * 64,
                             decision='APPROVED', approver='maintainer', evidence='https://example.com/evidence',
                             checks=[dict(name=n, result='PASS', evidence='https://example.com/check')
                                     for n in candidate.required_checks()])
        self.run = dict(id=42, run_attempt=2, head_sha='a' * 40, event='workflow_dispatch',
                        status='completed', conclusion='success', path='.github/workflows/release.yml')
        self.artifact = dict(id=71, name='pint-candidate-42-2', expired=False, digest='sha256:' + 'd' * 64,
                             workflow_run=dict(id=42, head_sha='a' * 40))
        self.jobs = dict(jobs=[dict(name='prepare', conclusion='success'), dict(name='publish', conclusion='skipped')])
        self.expected = self.identity.copy()
        self.expected.update(artifact_id=71)

    def check(self):
        candidate.verify(self.identity, self.approval, self.identity, self.run,
                         self.artifact, self.jobs, self.expected, 'd' * 64)

    def test_identity_and_provenance(self):
        self.check()
        for field in self.identity:
            old = self.approval['identity'][field]
            self.approval['identity'][field] = 9 if isinstance(old, int) else 'wrong'
            with self.subTest(field=field), self.assertRaises(ValueError): self.check()
            self.approval['identity'][field] = old
        for target, field, bad in [(self.run, 'run_attempt', 1), (self.run, 'event', 'push'),
                                   (self.run, 'path', 'other.yml'), (self.run, 'conclusion', 'failure'),
                                   (self.artifact, 'id', 72), (self.artifact, 'expired', True),
                                   (self.artifact, 'digest', 'sha256:' + 'e' * 64),
                                   (self.artifact, 'name', 'pint-candidate-42-1'),
                                   (self.approval, 'artifact_id', 72),
                                   (self.approval, 'artifact_sha256', 'e' * 64)]:
            old = target[field]; target[field] = bad
            with self.subTest(field=field), self.assertRaises(ValueError): self.check()
            target[field] = old
        self.jobs['jobs'][0]['conclusion'] = 'skipped'
        with self.assertRaises(ValueError): self.check()

    def test_approval_is_complete_and_waivers_are_explicit(self):
        for field in ['approver', 'evidence', 'decision']:
            old = self.approval[field]; self.approval[field] = ''
            with self.subTest(field=field), self.assertRaises(ValueError): self.check()
            self.approval[field] = old
        row = self.approval['checks'][0]
        for result in ['FAIL', 'PENDING', 'WAIVED']:
            row['result'] = result
            with self.subTest(result=result), self.assertRaises(ValueError): self.check()
        self.approval['decision'] = 'APPROVED WITH WAIVERS'
        with self.assertRaises(ValueError): self.check()
        row.update(reason='Device unavailable', approver='maintainer')
        self.check()
        row['result'] = 'FAIL'
        with self.assertRaises(ValueError): self.check()
        self.approval['checks'].pop()
        with self.assertRaises(ValueError): self.check()

    def test_measured_apk_and_tag_identity_cannot_change(self):
        for field in self.identity:
            measured = self.identity.copy(); measured[field] = 'wrong'
            with self.subTest(field=field), self.assertRaises(ValueError):
                candidate.verify(self.identity, self.approval, measured, self.run,
                                 self.artifact, self.jobs, self.expected, 'd' * 64)
        for publish_run in [100, 200]:
            self.expected['publish_run'] = publish_run
            self.check()

    def test_archive_rejects_changed_bytes_and_extra_paths(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); archive = root / 'candidate.zip'
            with zipfile.ZipFile(archive, 'w') as output:
                output.writestr('candidate.json', '{}')
                output.writestr('pint-release.apk', b'fixture')
                output.writestr('pint-release.apk.sha256', hashlib.sha256(b'fixture').hexdigest() + '  pint-release.apk\n')
            digest = hashlib.sha256(archive.read_bytes()).hexdigest()
            candidate.unpack(archive, root / 'out', digest)
            with self.assertRaises(ValueError): candidate.unpack(archive, root / 'bad', '0' * 64)
            with zipfile.ZipFile(archive, 'a') as output: output.writestr('../escape', 'no')
            with self.assertRaises(ValueError):
                candidate.unpack(archive, root / 'bad', hashlib.sha256(archive.read_bytes()).hexdigest())

    def test_cli_record_unpack_verify_and_metadata_rejection(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            built = root / 'built'; built.mkdir()
            apk = b'candidate fixture'
            env = os.environ | dict(CANDIDATE_COMMIT=self.identity['commit'], VERSION_NAME='1.2.3',
                                    VERSION_CODE='123', PINT_SIGNER_SHA256=self.identity['signer_sha256'],
                                    PREPARE_RUN_ID='42', PREPARE_ATTEMPT='2', ARTIFACT_ID='71',
                                    APK_PACKAGE=self.identity['package'], APK_VERSION_NAME='1.2.3',
                                    APK_VERSION_CODE='123', APK_SHA256=hashlib.sha256(apk).hexdigest())
            script = str(Path(__file__).with_name('verify-release-candidate.py'))
            def cli(mode, path, values=env):
                return subprocess.run([sys.executable, script, mode, str(path)], env=values,
                                      capture_output=True, text=True)
            self.assertEqual(0, cli('record', built).returncode)
            (built / 'pint-release.apk').write_bytes(apk)
            (built / 'pint-release.apk.sha256').write_text(env['APK_SHA256'] + '  pint-release.apk\n')
            with zipfile.ZipFile(root / 'artifact.zip', 'w') as archive:
                for path in built.iterdir(): archive.write(path, path.name)
            archive_digest = hashlib.sha256((root / 'artifact.zip').read_bytes()).hexdigest()
            self.approval['identity'] = json.loads((built / 'candidate.json').read_text())
            self.approval['artifact_sha256'] = archive_digest
            self.artifact['digest'] = 'sha256:' + archive_digest
            env['APPROVAL_JSON'] = json.dumps(self.approval)
            for name, value in [('run', self.run), ('jobs', self.jobs), ('artifact', self.artifact)]:
                (root / (name + '.json')).write_text(json.dumps(value))
            self.assertEqual(0, cli('unpack', root).returncode)
            self.assertEqual(0, cli('verify', root).returncode)
            self.assertTrue((root / 'approval.json').is_file())
            for key in ['APK_VERSION_CODE', 'APK_SHA256']:
                self.assertNotEqual(0, cli('verify', root, env | {key: '999'}).returncode)
            self.assertNotEqual(0, cli('verify', root, env | {'APPROVAL_JSON': '{}'}).returncode)


if __name__ == '__main__':
    unittest.main()
