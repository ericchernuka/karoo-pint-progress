import fs from "node:fs";
import path from "node:path";

const destination = path.resolve("pint/src/main/res/drawable");
const kotlinDestination = path.resolve(
  "pint/src/main/kotlin/io/ericchernuka/pintprogress/PintAssetDrawables.kt",
);
const assetDestination = path.resolve(
  "pint/src/main/kotlin/io/ericchernuka/pintprogress/core/PintAsset.kt",
);
fs.mkdirSync(destination, { recursive: true });

const semanticAssets = [
  ...Array.from({ length: 20 }, (_, bucket) => `pint_${String(bucket * 5).padStart(2, "0")}`),
  "pint_full_bubbles",
  "pint_draining",
  "pint_unavailable",
];
const fillAssets = [
  ...Array.from({ length: 20 }, (_, bucket) => `pint_fill_${String(bucket * 5).padStart(2, "0")}`),
  "pint_fill_full_foam",
  "pint_fill_draining",
  "pint_fill_unavailable",
];

const color = (name) => `@color/pint_${name}`;
const shape = (color, path) => ({ color, path });
const outline = (path, stroke = color("foreground"), lineCap) => ({
  fill: "#00000000", stroke, strokeWidth: "4", lineCap, lineJoin: "round", path,
});

const vectorSizes = {
  regular: { width: "66dp", height: "89dp" },
  compact: { width: "48dp", height: "65dp" },
  icon: { width: "32dp", height: "43dp" },
  extensionIcon: { width: "83dp", height: "112dp" },
};

const xml = (paths, size) => `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="${size.width}"
    android:height="${size.height}"
    android:viewportWidth="83"
    android:viewportHeight="112">
    <group android:translateX="-13">
${paths.map((entry) => `        <path\n            android:fillColor="${entry.fill ?? entry.color}"\n            android:pathData="${entry.path}"${entry.alpha ? `\n            android:fillAlpha="${entry.alpha}"` : ""}${entry.stroke ? `\n            android:strokeColor="${entry.stroke}"\n            android:strokeWidth="${entry.strokeWidth}"` : ""}${entry.lineCap ? `\n            android:strokeLineCap="${entry.lineCap}"` : ""}${entry.lineJoin ? `\n            android:strokeLineJoin="${entry.lineJoin}"` : ""} />`).join("\n")}
    </group>
</vector>
`;

const fillXml = (paths) => `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="100dp"
    android:height="100dp"
    android:viewportWidth="100"
    android:viewportHeight="100">
${paths.map((entry) => `    <path
        android:fillColor="${entry.color}"
        android:pathData="${entry.path}"${entry.alpha ? `
        android:fillAlpha="${entry.alpha}"` : ""} />`).join("\n")}
</vector>
`;

// The glass uses a straight-sided silhouette filled over the handle so Android vector
// antialiasing cannot reveal the hidden handle joins at large Karoo view sizes
const mugBodyPath = "M27,8 L58,8 C65,8 69,13 69,20 L69,92 C69,100 65,104 58,104 L28,104 C20,104 16,100 16,92 L16,20 C16,13 20,8 27,8 Z";

const mugSurface = shape(color("surface"), mugBodyPath);
const mugOutline = outline(mugBodyPath);
const mugHandleFill = shape(
  color("surface"),
  "M67,28 L80,28 C89,28 94,34 94,44 L94,73 C94,83 89,89 80,89 L67,89 L67,78 L79,78 C83,78 85,76 85,72 L85,45 C85,41 83,39 79,39 L67,39 Z",
);

// Open subpaths omit the two handle/body joins, and their endpoints extend behind mugSurface
// to keep the visible connection independent of VectorDrawable scaling and antialiasing
const mugHandleOutline = outline(
  "M67,28 L80,28 C89,28 94,34 94,44 L94,73 C94,83 89,89 80,89 L67,89 M67,78 L79,78 C83,78 85,76 85,72 L85,45 C85,41 83,39 79,39 L67,39",
  color("foreground"),
  "round",
);

const mugHandle = [mugHandleFill, mugHandleOutline];

const innerMug = shape(
  color("surface"),
  "M27,13 L58,13 C62,13 64,16 64,21 L64,91 C64,96 61,99 57,99 L29,99 C24,99 21,96 21,91 L21,21 C21,16 23,13 27,13 Z",
);

const fillTop = (percent) => 99 - (percent / 100) * 86;
// Keep liquid inside the straight section of the inner glass. Foam occupies the rounded cap for
// near-full states, so neither layer needs a clip path that could render differently on Karoo
const contentTop = (percent) => Math.max(fillTop(percent), 21);

const fillPath = (percent) => {
  if (percent === 0) return null;
  const top = contentTop(percent);
  return `M21,${top.toFixed(2)} L64,${top.toFixed(2)} L64,91 C64,96 61,99 57,99 L29,99 C24,99 21,96 21,91 L21,${top.toFixed(2)} Z`;
};

const foamPath = (top, height) => {
  const crest = top - height;
  if (crest < 18) {
    return `M21,${(top + 2).toFixed(2)} L64,${(top + 2).toFixed(2)} L64,21 C64,16 62,13 58,13 C53,13 49,16 44,15 C38,13 32,14 27,13 C23,13 21,16 21,21 Z`;
  }
  return `M21,${(top + 2).toFixed(2)} L64,${(top + 2).toFixed(2)} L64,${(crest + 3).toFixed(2)} C58,${(crest + 1).toFixed(2)} 52,${(crest + 4).toFixed(2)} 44,${(crest + 3).toFixed(2)} C35,${(crest - 1).toFixed(2)} 27,${(crest - 1).toFixed(2)} 21,${(crest + 3).toFixed(2)} Z`;
};

// Keep the completed-pint foam crown inside the mug envelope and behind the outline so the glass
// lip remains clear
const fullFoamPath = () =>
  "M21,23 L64,23 L64,21 C64,16 62,13 58,13 " +
  "C58,9 56,6 53,6 C50,6 48,8 45,7 " +
  "C43,5 41,4 38,4 C35,4 33,6 31,7 " +
  "C28,8 26,7 24,10 C22,12 21,16 21,21 Z";

const roundedPill = (x, top, bottom) => {
  if (bottom - top < 6) return null;
  const right = x + 4;
  return `M${x + 2},${top.toFixed(2)} C${right},${top.toFixed(2)} ${right},${(top + 2).toFixed(2)} ${right},${(top + 2).toFixed(2)} L${right},${(bottom - 2).toFixed(2)} C${right},${bottom.toFixed(2)} ${x},${bottom.toFixed(2)} ${x},${(bottom - 2).toFixed(2)} L${x},${(top + 2).toFixed(2)} C${x},${(top + 2).toFixed(2)} ${x},${top.toFixed(2)} ${x + 2},${top.toFixed(2)} Z`;
};

const mugMarks = (percent) => {
  if (percent === 0) return [];
  const start = Math.max(fillTop(percent) + 8, 33);
  return [
    roundedPill(30, start + 5, 87),
    roundedPill(41, start, 92),
    roundedPill(52, start + 11, 83),
  ].filter(Boolean).map((path) => ({ color: color("beer_highlight"), alpha: "0.9", path }));
};

const bubbles = [
  { color: color("bubble"), alpha: "0.9", path: "M28,7 A2.5,2.5 0,1 0,28.01,7" },
  { color: color("bubble"), alpha: "0.85", path: "M44,3 A2,2 0,1 0,44.01,3" },
  { color: color("bubble"), alpha: "0.75", path: "M59,8 A3,3 0,1 0,59.01,8" },
  { color: color("bubble"), alpha: "0.8", path: "M74,4 A1.5,1.5 0,1 0,74.01,4" },
];

const mugPaths = (percent, foam = null, extra = []) => {
  const fill = fillPath(percent);
  return [
    ...mugHandle,
    mugSurface,
    innerMug,
    ...(fill ? [{ color: color("amber"), path: fill }] : []),
    ...(foam ? [{ color: color("foam"), path: foam }] : []),
    ...mugMarks(percent),
    ...extra,
    mugOutline,
  ];
};

const write = (name, paths, size) => fs.writeFileSync(path.join(destination, `${name}.xml`), xml(paths, size));

const writeMugVariants = (name, paths) => {
  write(name, paths, vectorSizes.regular);
  write(`${name}_compact`, paths, vectorSizes.compact);
  write(`${name}_icon`, paths, vectorSizes.icon);
};

const kotlinName = (resourceName) =>
  (resourceName.match(/^pint_\d/) ? resourceName : resourceName.slice("pint_".length)).toUpperCase();
const kotlinMappings = (suffix) => semanticAssets
  .map((name) => `        PintAsset.${kotlinName(name)} -> R.drawable.${name}${suffix}`)
  .join("\n");
const fillKotlinMappings = fillAssets
  .map((name) => `    PintFillAsset.${kotlinName(name)} -> R.drawable.${name}`)
  .join("\n");

const generatedKotlin = `// Generated by tools/generate-drawables.mjs. Do not edit.\n// Regenerate with: node tools/generate-drawables.mjs\npackage io.ericchernuka.pintprogress\n\nimport io.ericchernuka.pintprogress.core.PintAsset\nimport io.ericchernuka.pintprogress.core.PintFieldLayout\nimport io.ericchernuka.pintprogress.core.PintFillAsset\n\n/** Compile-time drawable mappings generated alongside the drawable vectors. */\ninternal fun PintAsset.drawableRes(layout: PintFieldLayout): Int = when (layout) {\n    PintFieldLayout.PICKER -> when (this) {\n${kotlinMappings("_compact")}\n    }\n    PintFieldLayout.REGULAR -> when (this) {\n${kotlinMappings("")}\n    }\n    PintFieldLayout.COMPACT -> when (this) {\n${kotlinMappings("_compact")}\n    }\n    PintFieldLayout.ICON_ONLY -> when (this) {\n${kotlinMappings("_icon")}\n    }\n}\n\ninternal fun PintFillAsset.drawableRes(): Int = when (this) {\n${fillKotlinMappings}\n}\n`;

fs.writeFileSync(kotlinDestination, generatedKotlin);
fs.writeFileSync(assetDestination, `// Generated by tools/generate-drawables.mjs. Do not edit.\npackage io.ericchernuka.pintprogress.core\n\n/** Static drawable frames generated with the vector assets. */\nenum class PintAsset {\n    ${semanticAssets.map(kotlinName).join(",\n    ")}\n}\n\nenum class PintFillAsset {\n    ${fillAssets.map(kotlinName).join(",\n    ")}\n}\n\nfun displayFor(frame: PintFrame): Pair<PintAsset, String> = when (frame) {\n    PintFrame.Unavailable -> PintAsset.UNAVAILABLE to ""\n    is PintFrame.Steady -> PintAsset.entries[frame.progress.fillBucket] to frame.progress.completed.mugText()\n    is PintFrame.FullBubbles -> PintAsset.FULL_BUBBLES to frame.completed.mugText()\n    is PintFrame.Draining -> PintAsset.DRAINING to frame.completed.mugText()\n}\n\nfun fillDisplayFor(frame: PintFrame): Pair<PintFillAsset, String> = when (frame) {\n    PintFrame.Unavailable -> PintFillAsset.FILL_UNAVAILABLE to "—"\n    is PintFrame.Steady -> PintFillAsset.entries[frame.progress.fillBucket] to frame.progress.completed.toString()\n    is PintFrame.FullBubbles -> PintFillAsset.FILL_FULL_FOAM to frame.completed.toString()\n    is PintFrame.Draining -> PintFillAsset.FILL_DRAINING to frame.completed.toString()\n}\n\nprivate fun Int.mugText() = if (this == 0) "" else toString()\n`);

for (let bucket = 0; bucket < 20; bucket += 1) {
  const percent = bucket * 5;
  const foamHeight = 5 + ((percent - 80) / 5) * 1.5;
  const foam = percent >= 80 ? foamPath(contentTop(percent), foamHeight) : null;
  writeMugVariants(`pint_${String(percent).padStart(2, "0")}`, mugPaths(percent, foam));
}

writeMugVariants("pint_full_bubbles", mugPaths(100, fullFoamPath(), bubbles));
writeMugVariants("pint_draining", mugPaths(55, foamPath(contentTop(55), 5), bubbles.slice(2)));

writeMugVariants("pint_unavailable", [
  { ...mugHandleFill, fill: color("unavailable_surface") },
  { ...mugHandleOutline, stroke: color("unavailable_foreground") },
  { ...mugSurface, color: color("unavailable_surface") },
  { ...innerMug, color: color("unavailable_surface") },
  { color: color("unavailable_foreground"), path: "M38,42 L42,42 L42,68 L38,68 Z M38,78 L42,78 L42,84 L38,84 Z" },
  { color: color("unavailable_foreground"), path: "M50,42 L54,42 L54,68 L50,68 Z M50,78 L54,78 L54,84 L50,84 Z" },
  { ...mugOutline, stroke: color("unavailable_foreground") },
]);

write("ic_pint", mugPaths(80, foamPath(contentTop(80), 5)), vectorSizes.extensionIcon);

const fieldBubbleSpecs = [
  [9, 91, "circle"],
  [18, 82, "pill"],
  [8, 69, "pill"],
  [20, 55, "circle"],
  [91, 88, "pill"],
  [81, 76, "circle"],
  [92, 61, "circle"],
  [80, 46, "pill"],
];

const circlePath = (x, y, radius) =>
  `M${x - radius},${y} ` +
  `C${x - radius},${y - radius} ${x + radius},${y - radius} ${x + radius},${y} ` +
  `C${x + radius},${y + radius} ${x - radius},${y + radius} ${x - radius},${y} Z`;

const fieldFoamPath = (top, depth) => {
  const upper = Math.max(0, top - depth / 2);
  const middle = Math.min(100, top + depth / 2);
  return `M0,${upper} L100,${upper} L100,${middle} ` +
    `C90,${middle - 1} 82,${middle + 1} 72,${middle} ` +
    `C61,${middle + 1} 52,${middle - 1} 42,${middle} ` +
    `C31,${middle - 1} 20,${middle + 1} 0,${middle} Z`;
};

const verticalCapsule = (x, top, bottom, capDepth) => {
  if (bottom - top < capDepth * 2) return null;
  const middle = x + 2;
  const right = x + 4;
  return `M${middle},${top.toFixed(2)} ` +
    `C${right},${top.toFixed(2)} ${right},${(top + capDepth).toFixed(2)} ${right},${(top + capDepth).toFixed(2)} ` +
    `L${right},${(bottom - capDepth).toFixed(2)} ` +
    `C${right},${bottom.toFixed(2)} ${x},${bottom.toFixed(2)} ${x},${(bottom - capDepth).toFixed(2)} ` +
    `L${x},${(top + capDepth).toFixed(2)} ` +
    `C${x},${top.toFixed(2)} ${x},${top.toFixed(2)} ${middle},${top.toFixed(2)} Z`;
};

const fieldHighlights = (top) => {
  const start = Math.max(top + 9, 24);
  return [
    verticalCapsule(14, start, 92, 6),
    verticalCapsule(21, start + 10, 84, 5),
  ].filter(Boolean).map((path, index) => ({
    color: color("beer_highlight"),
    alpha: index === 0 ? "0.42" : "0.28",
    path,
  }));
};

const fieldBubbles = (top) => fieldBubbleSpecs
  .filter(([, y]) => y > top + 4)
  .map(([x, y, bubbleShape], index) => ({
    color: color("bubble"),
    alpha: index % 2 === 0 ? "0.72" : "0.5",
    path: bubbleShape === "pill"
      ? roundedPill(x, y - 4, y + 4)
      : circlePath(x, y, index % 3 === 0 ? 2.5 : 1.9),
  }));

const fieldFoamPockets = (percent, top, foamDepth) => {
  if (percent < 80) return [];
  const center = percent === 100 ? Math.min(8, foamDepth / 2) : top;
  return [
    circlePath(12, center, 2.5),
    roundedPill(85, center - 3, center + 3),
    circlePath(20, center + 2, 2),
  ].map((path) => ({ color: color("fill_amber"), alpha: "0.86", path }));
};

const fieldFillPaths = (percent, foamDepth) => {
  const top = 100 - percent;
  const paths = [{ color: color("surface"), path: "M0,0 L100,0 L100,100 L0,100 Z" }];
  if (percent > 0) {
    paths.push({ color: color("fill_amber"), path: `M0,${top} L100,${top} L100,100 L0,100 Z` });
    paths.push(...fieldHighlights(top));
    paths.push(...fieldBubbles(top));
    paths.push({ color: color("fill_foam"), path: fieldFoamPath(top, foamDepth) });
    paths.push(...fieldFoamPockets(percent, top, foamDepth));
  }
  return paths;
};

for (let bucket = 0; bucket < 20; bucket += 1) {
  const percent = bucket * 5;
  const foamDepth = percent >= 80 ? 8 : 4;
  fs.writeFileSync(
    path.join(destination, `pint_fill_${String(percent).padStart(2, "0")}.xml`),
    fillXml(fieldFillPaths(percent, foamDepth)),
  );
}

fs.writeFileSync(
  path.join(destination, "pint_fill_full_foam.xml"),
  fillXml(fieldFillPaths(100, 20)),
);
fs.writeFileSync(
  path.join(destination, "pint_fill_draining.xml"),
  fillXml(fieldFillPaths(55, 4)),
);
fs.writeFileSync(
  path.join(destination, "pint_fill_unavailable.xml"),
  fillXml([{ color: color("unavailable_surface"), path: "M0,0 L100,0 L100,100 L0,100 Z" }]),
);
