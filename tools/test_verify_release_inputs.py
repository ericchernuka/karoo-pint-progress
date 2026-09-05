import importlib.util
from pathlib import Path
import unittest
import os
import subprocess
import sys
import tempfile

spec = importlib.util.spec_from_file_location('release', Path(__file__).with_name('verify-release-inputs.py'))
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)

class ReleaseInputsTest(unittest.TestCase):
    def test_version_and_signer(self):
        self.assertEqual(('1.2.0', 1002000), release.validate('v1.2.0', 'a' * 64))
        self.assertEqual(('1.2.0', 1002000), release.validate('1.2.0', 'A' * 64))
        for version, signer in [('1.2.0', ''), ('1.2.0', 'z' * 64), ('1.2', 'a' * 64),
                                ('1.1000.0', 'a' * 64), ('2100.0.1', 'a' * 64),
                                ('0.0.0', 'a' * 64), ('1.02.0', 'a' * 64), ('$(id)', 'a' * 64)]:
            with self.subTest(version=version, signer=signer), self.assertRaises(ValueError):
                release.validate(version, signer)

    def test_workflow_environment(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / 'env'
            env = dict(os.environ, RELEASE_VERSION='v1.2.0', PINT_SIGNER_SHA256='a' * 64,
                       GITHUB_ENV=str(output))
            script = str(Path(__file__).with_name('verify-release-inputs.py'))
            subprocess.run([sys.executable, script], env=env, check=True)
            self.assertEqual('VERSION_NAME=1.2.0\nVERSION_CODE=1002000\n', output.read_text())
            output.unlink()
            env['PINT_SIGNER_SHA256'] = ''
            result = subprocess.run([sys.executable, script], env=env, capture_output=True, text=True)
            self.assertNotEqual(0, result.returncode)
            self.assertIn('Set repository variable PINT_SIGNER_SHA256', result.stderr)
            self.assertFalse(output.exists())

if __name__ == '__main__':
    unittest.main()
