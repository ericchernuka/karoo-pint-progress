"""Compare successful apksigner output with the independently approved certificate."""
import re
import sys
from pathlib import Path


def verify(output, expected):
    if not re.fullmatch(r'[0-9a-fA-F]{64}', expected):
        raise ValueError('PINT_SIGNER_SHA256 must contain the approved 64-hex fingerprint')
    digests = re.findall(r'^Signer #\d+ certificate SHA-256 digest: (.*)$', output, re.MULTILINE)
    if len(digests) != 1 or digests[0].lower() != expected.lower():
        raise ValueError('Expected exactly one approved signer certificate')
    return expected.lower()


if __name__ == '__main__':
    try:
        print(verify(Path(sys.argv[1]).read_text(), sys.argv[2]))
    except (ValueError, OSError, IndexError) as error:
        sys.exit(str(error))
