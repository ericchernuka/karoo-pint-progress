import fs from "node:fs";
import path from "node:path";

const destination = path.resolve("pint/src/main/res/drawable");
fs.mkdirSync(destination, { recursive: true });

const xml = (paths) => `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="96dp"
    android:height="112dp"
    android:viewportWidth="96"
    android:viewportHeight="112">
${paths.map((entry) => `    <path\n        android:fillColor="${entry.fill ?? entry.color}"\n        android:pathData="${entry.path}"${entry.alpha ? `\n        android:fillAlpha="${entry.alpha}"` : ""}${entry.stroke ? `\n        android:strokeColor="${entry.stroke}"\n        android:strokeWidth="${entry.strokeWidth}"` : ""}${entry.lineCap ? `\n        android:strokeLineCap="${entry.lineCap}"` : ""}${entry.lineJoin ? `\n        android:strokeLineJoin="${entry.lineJoin}"` : ""} />`).join("\n")}
</vector>
`;

const mugOutline = {
  fill: "#00000000",
  stroke: "#F5F4F0",
  strokeWidth: "4",
  lineJoin: "round",
  path: "M27,8 L59,8 C67,8 71,13 71,21 L68,91 C68,99 64,104 57,104 L29,104 C22,104 18,99 18,91 L15,21 C15,13 19,8 27,8 Z",
};

const mugHandle = {
  fill: "#0D1117",
  stroke: "#F5F4F0",
  strokeWidth: "4",
  lineJoin: "round",
  path: "M70,28 L80,28 C89,28 94,34 94,44 L94,73 C94,83 89,89 80,89 L69,89 L69,78 L79,78 C83,78 85,76 85,72 L85,45 C85,41 83,39 79,39 L70,39 Z",
};

const innerMug = {
  color: "#0D1117",
  path: "M27,13 L59,13 C64,13 67,16 67,22 L64,90 C64,96 62,99 57,99 L29,99 C24,99 22,96 22,90 L19,22 C19,16 22,13 27,13 Z",
};

const fillTop = (percent) => 99 - (percent / 100) * 86;

const fillPath = (percent) => {
  if (percent === 0) return null;
  const top = fillTop(percent);
  return `M20,${top.toFixed(2)} L66,${top.toFixed(2)} L64,90 C64,96 62,99 57,99 L29,99 C24,99 22,96 22,90 L20,${top.toFixed(2)} Z`;
};

const foamPath = (top, height) => {
  const crest = top - height;
  return `M20,${(top + 2).toFixed(2)} L66,${(top + 2).toFixed(2)} L66,${(crest + 3).toFixed(2)} C59,${(crest + 1).toFixed(2)} 53,${(crest + 4).toFixed(2)} 45,${(crest + 3).toFixed(2)} C36,${(crest - 1).toFixed(2)} 27,${(crest - 1).toFixed(2)} 20,${(crest + 3).toFixed(2)} Z`;
};

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
  ].filter(Boolean).map((path) => ({ color: "#F5F4F0", alpha: "0.9", path }));
};

const bubbles = [
  { color: "#F5F4F0", alpha: "0.9", path: "M28,7 A2.5,2.5 0,1 0,28.01,7" },
  { color: "#F5F4F0", alpha: "0.85", path: "M44,3 A2,2 0,1 0,44.01,3" },
  { color: "#F5F4F0", alpha: "0.75", path: "M59,8 A3,3 0,1 0,59.01,8" },
  { color: "#F5F4F0", alpha: "0.8", path: "M74,4 A1.5,1.5 0,1 0,74.01,4" },
];

const write = (name, paths) => fs.writeFileSync(path.join(destination, `${name}.xml`), xml(paths));

for (let bucket = 0; bucket < 20; bucket += 1) {
  const percent = bucket * 5;
  const paths = [mugHandle, innerMug];
  const fill = fillPath(percent);
  if (fill) paths.push({ color: "#F9AB09", path: fill });
  if (percent >= 80) {
    const foamHeight = 5 + ((percent - 80) / 5) * 1.5;
    paths.push({ color: "#F5F4F0", path: foamPath(fillTop(percent), foamHeight) });
  }
  paths.push(...mugMarks(percent), mugOutline);
  write(`pint_${String(percent).padStart(2, "0")}`, paths);
}

write("pint_full_bubbles", [
  mugHandle,
  innerMug,
  { color: "#F9AB09", path: fillPath(100) },
  { color: "#F5F4F0", path: foamPath(fillTop(100), 9) },
  ...mugMarks(100),
  ...bubbles,
  mugOutline,
]);

write("pint_draining", [
  mugHandle,
  innerMug,
  { color: "#F9AB09", path: fillPath(55) },
  { color: "#F5F4F0", path: foamPath(fillTop(55), 5) },
  ...mugMarks(55),
  ...bubbles.slice(2),
  mugOutline,
]);

write("pint_unavailable", [
  { ...mugHandle, stroke: "#7B8794" },
  { ...innerMug, color: "#18212B" },
  { color: "#7B8794", path: "M38,42 L42,42 L42,68 L38,68 Z M38,78 L42,78 L42,84 L38,84 Z" },
  { color: "#7B8794", path: "M50,42 L54,42 L54,68 L50,68 Z M50,78 L54,78 L54,84 L50,84 Z" },
  { ...mugOutline, stroke: "#7B8794" },
]);

write("ic_pint", [
  mugHandle,
  innerMug,
  { color: "#F9AB09", path: fillPath(80) },
  { color: "#F5F4F0", path: foamPath(fillTop(80), 5) },
  ...mugMarks(80),
  mugOutline,
]);
