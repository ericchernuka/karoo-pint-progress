#!/usr/bin/env python3
"""Validate the Dokka HTML contract used by CI."""

from html.parser import HTMLParser
from pathlib import Path, PureWindowsPath
import re
import sys
from urllib.parse import unquote


HTML_ROOT = Path("lib/build/dokka/html")
SOURCE_ROOT = Path("lib/src/main/kotlin")
SOURCE_PREFIX = (
    "https://github.com/ericchernuka/karoo-pint-progress/"
    "blob/main/lib/src/main/kotlin/"
)
SAMPLES = (
    'class EmptyDataType(extension: String) : DataTypeImpl(extension, "empty-datatype")',
    'class EmptyDataType(extension: String) : DataTypeImpl(extension, "empty-visual-datatype")',
    'class EmptyExtension : KarooExtension("empty-extension", "5.0")',
    'println("karoo system connected")',
)
DECLARATIONS = (
    ("io/hammerhead/karooext/extension/KarooExtension.kt", "abstract class KarooExtension("),
    ("io/hammerhead/karooext/extension/DataTypeImpl.kt", "abstract class DataTypeImpl("),
    ("io/hammerhead/karooext/KarooSystemService.kt", "class KarooSystemService("),
)


def classes(attrs):
    return set(attrs.get("class", "").split())


class DokkaParser(HTMLParser):
    def __init__(self, file):
        super().__init__(convert_charrefs=True)
        self.stack = []
        self.samples = []
        self.source_elements = []
        self.file = file

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        inherited_sample = next(
            (entry["sample"] for entry in reversed(self.stack) if entry["sample"] is not None),
            None,
        )
        is_sample = tag == "div" and "sample-container" in classes(attrs)
        sample = len(self.samples) if is_sample else inherited_sample
        if is_sample:
            self.samples.append({"parts": [], "main": any(entry["main"] for entry in self.stack)})
            self.samples[sample]["main"] |= attrs.get("data-togglable") == ":lib/main"
        is_source = (
            "source-link" in classes(attrs)
            or attrs.get("data-element-type") == "source-link"
        )
        source_element = None
        if is_source:
            source_element = {"file": self.file, "anchors": []}
            self.source_elements.append(source_element)
        source_element = next(
            (entry["source_element"] for entry in reversed(self.stack) if entry["source_element"]),
            source_element,
        )
        if tag == "a" and source_element is not None:
            source_element["anchors"].append(attrs.get("href"))
        self.stack.append(
            {
                "tag": tag,
                "sample": sample,
                "source": is_source,
                "source_element": source_element,
                "main": attrs.get("data-togglable") == ":lib/main",
            }
        )

    def handle_endtag(self, tag):
        for index in range(len(self.stack) - 1, -1, -1):
            if self.stack[index]["tag"] == tag:
                del self.stack[index:]
                return

    def handle_data(self, data):
        sample = next(
            (entry["sample"] for entry in reversed(self.stack) if entry["sample"] is not None),
            None,
        )
        if sample is not None:
            self.samples[sample]["parts"].append(data)


def fail(message):
    raise SystemExit(f"Dokka contract: {message}")


def declaration_line(relative, marker):
    path = SOURCE_ROOT / relative
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except OSError as error:
        fail(f"cannot read {path}: {error}")
    for number, line in enumerate(lines, 1):
        if marker in line:
            return number
    fail(f"declaration marker {marker!r} is missing from {path}")


def parse_html_file(path):
    parser = DokkaParser(path)
    parser.feed(path.read_text(encoding="utf-8"))
    parser.close()
    if any(entry["source"] for entry in parser.stack):
        fail(f"unclosed source-link element in {path}")
    return parser


def source_path_and_line(href, line_counts):
    if href.count("#") != 1:
        fail(f"source link must contain one path/line separator: {href}")
    path_part, anchor = href.rsplit("#", 1)
    if not re.fullmatch(r"L[1-9][0-9]*", anchor):
        fail(f"source link has an invalid line anchor: {href}")
    try:
        relative = unquote(path_part[len(SOURCE_PREFIX) :], errors="strict")
    except ValueError as error:
        fail(f"source link path cannot be decoded: {href} ({error})")
    windows_path = PureWindowsPath(relative)
    if Path(relative).is_absolute() or windows_path.is_absolute() or windows_path.drive:
        fail(f"source link uses an absolute repository path: {href}")
    try:
        candidate = (SOURCE_ROOT / relative).resolve()
        candidate.relative_to(SOURCE_ROOT.resolve())
    except (OSError, ValueError) as error:
        fail(f"source link escapes {SOURCE_ROOT}: {href} ({error})")
    if not candidate.is_file():
        fail(f"source link target is not a regular file: {href}")
    try:
        line = int(anchor[1:])
        if candidate not in line_counts:
            line_counts[candidate] = len(candidate.read_text(encoding="utf-8").splitlines())
        line_count = line_counts[candidate]
    except (OSError, ValueError) as error:
        fail(f"cannot inspect source link target {href}: {error}")
    if line > line_count:
        fail(f"source link line {line} is outside {candidate} ({line_count} lines): {href}")
    return relative, line


def main():
    if not HTML_ROOT.is_dir():
        fail(f"missing generated output directory {HTML_ROOT}")
    html_files = sorted(HTML_ROOT.rglob("*.html"))
    if not html_files:
        fail(f"no HTML files found under {HTML_ROOT}")
    samples = []
    source_elements = []
    for path in html_files:
        parser = parse_html_file(path)
        samples.extend(parser.samples)
        source_elements.extend(parser.source_elements)
    sample_texts = [" ".join(sample["parts"]).split() for sample in samples]
    sample_texts = [" ".join(words) for words in sample_texts]
    main_samples = [text for text, sample in zip(sample_texts, samples) if sample["main"]]
    if len(samples) < 4:
        fail(f"expected at least four sample containers, found {len(samples)}")
    if len(main_samples) < 4:
        fail(f"expected at least four :lib/main sample containers, found {len(main_samples)}")
    for sample in SAMPLES:
        if not any(sample in text for text in main_samples):
            fail(f"sample body is missing from a :lib/main sample container: {sample}")

    source_links = set()
    for element in source_elements:
        anchors = element["anchors"]
        if len(anchors) != 1:
            fail(
                f"source-link element in {element['file']} has {len(anchors)} anchors; "
                "expected exactly one"
            )
        href = anchors[0]
        if not href:
            fail(f"source-link element in {element['file']} has no usable href")
        source_links.add(href)
    if not source_links:
        fail("no Dokka source-link elements with usable anchors found")
    line_counts = {}
    for href in sorted(source_links):
        if not href.startswith(SOURCE_PREFIX):
            fail(f"source link does not use the local repository prefix: {href}")
        source_path_and_line(href, line_counts)
    for relative, marker in DECLARATIONS:
        line = declaration_line(relative, marker)
        expected = f"{SOURCE_PREFIX}{relative}#L{line}"
        if expected not in source_links:
            fail(f"generated source link is missing: {expected}")
    print(
        f"Dokka contract passed: {len(html_files)} HTML files, "
        f"{len(samples)} sample containers, {len(source_links)} source links"
    )


if __name__ == "__main__":
    sys.exit(main())
