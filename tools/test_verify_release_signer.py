import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('signer', Path(__file__).with_name('verify-release-signer.py'))
signer = importlib.util.module_from_spec(spec)
spec.loader.exec_module(signer)


class SignerTest(unittest.TestCase):
    def test_single_approved_signer(self):
        digest = 'ab' * 32
        output = f'Signer #1 certificate SHA-256 digest: {digest}\nSigner #1 certificate SHA-1 digest: abc\n'
        self.assertEqual(digest, signer.verify(output, digest.upper()))
        for expected, report in [('', output), ('x' * 64, output), ('cd' * 32, output),
                                 (digest, ''), (digest, output + output),
                                 (digest, output.replace(digest, 'invalid')),
                                 (digest, f'Source Stamp Signer certificate SHA-256 digest: {digest}')]:
            with self.subTest(expected=expected, report=report):
                with self.assertRaises(ValueError):
                    signer.verify(report, expected)


if __name__ == '__main__':
    unittest.main()
