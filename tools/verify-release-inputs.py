"""Validate the release version and independently configured public signer."""
import os
import re
import sys


def validate(version, signer):
    version = version.removeprefix('v')
    if not re.fullmatch(r'(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)', version):
        raise ValueError('Enter a version such as 1.2.0 or v1.2.0.')
    major, minor, patch = map(int, version.split('.'))
    code = major * 1000000 + minor * 1000 + patch
    if minor > 999 or patch > 999 or not 0 < code <= 2100000000:
        raise ValueError('Version is outside the Android version-code range; minor and patch must be at most 999.')
    if not re.fullmatch(r'[0-9a-fA-F]{64}', signer):
        raise ValueError('Set repository variable PINT_SIGNER_SHA256 to the approved 64-character certificate SHA-256 fingerprint.')
    return version, code


if __name__ == '__main__':
    try:
        version, code = validate(os.environ['RELEASE_VERSION'], os.environ.get('PINT_SIGNER_SHA256', ''))
        with open(os.environ['GITHUB_ENV'], 'a') as output:
            output.write(f'VERSION_NAME={version}\nVERSION_CODE={code}\n')
    except (ValueError, KeyError) as error:
        sys.exit(str(error))
