import fs from "node:fs";
import path from "node:path";

const destination = path.resolve("pint/src/main/res/drawable");
fs.mkdirSync(destination, { recursive: true });

const xml = (paths) => `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="96dp"
    android:height="128dp"
    android:viewportWidth="96"
    android:viewportHeight="128">
${paths.map((entry) => `    <path\n        android:fillColor="${entry.fill ?? entry.color}"\n        android:pathData="${entry.path}"${entry.alpha ? `\n        android:fillAlpha="${entry.alpha}"` : ""}${entry.stroke ? `\n        android:strokeColor="${entry.stroke}"\n        android:strokeWidth="${entry.strokeWidth}"` : ""} />`).join("\n")}
</vector>
`;

const glassOutline = {
  fill: "#00000000",
  stroke: "#E8EDF3",
  strokeWidth: "3",
  path: "M18,12 L78,12 L70,112 L26,112 Z",
};

const innerGlass = {
  color: "#27313B",
  alpha: "0.36",
  path: "M22,16 L74,16 L67,108 L29,108 Z",
};

const fillPath = (percent) => {
  if (percent === 0) return null;
  const top = 108 - (percent / 100) * 92;
  const left = 29 - ((108 - top) / 92) * 7;
  const right = 67 + ((108 - top) / 92) * 7;
  return `M${left.toFixed(2)},${top.toFixed(2)} L${right.toFixed(2)},${top.toFixed(2)} L67,108 L29,108 Z`;
};

const foamPath = (top, height) => `M23,${(top - height).toFixed(2)} L73,${(top - height).toFixed(2)} L72,${(top + 2).toFixed(2)} L24,${(top + 2).toFixed(2)} Z`;
const bubbles = [
  { color: "#F6F8FA", alpha: "0.9", path: "M28,8 A2.5,2.5 0,1 0,28.01,8" },
  { color: "#F6F8FA", alpha: "0.85", path: "M44,3 A2,2 0,1 0,44.01,3" },
  { color: "#F6F8FA", alpha: "0.75", path: "M59,9 A3,3 0,1 0,59.01,9" },
  { color: "#F6F8FA", alpha: "0.8", path: "M71,5 A1.5,1.5 0,1 0,71.01,5" },
];

const write = (name, paths) => fs.writeFileSync(path.join(destination, `${name}.xml`), xml(paths));

for (let bucket = 0; bucket < 20; bucket += 1) {
  const percent = bucket * 5;
  const paths = [innerGlass];
  const fill = fillPath(percent);
  if (fill) paths.push({ color: "#E6A329", path: fill });
  if (percent >= 80) {
    const foamHeight = 2 + ((percent - 80) / 5) * 1.5;
    paths.push({ color: "#F6F8FA", path: foamPath(108 - (percent / 100) * 92, foamHeight) });
  }
  paths.push(glassOutline);
  write(`pint_${String(percent).padStart(2, "0")}`, paths);
}

write("pint_full_bubbles", [
  innerGlass,
  { color: "#E6A329", path: fillPath(100) },
  { color: "#F6F8FA", path: foamPath(16, 10) },
  ...bubbles,
  glassOutline,
]);

write("pint_draining", [
  innerGlass,
  { color: "#E6A329", path: fillPath(55) },
  { color: "#F6F8FA", path: foamPath(57.4, 4) },
  ...bubbles.slice(2),
  glassOutline,
]);

write("pint_unavailable", [
  innerGlass,
  { color: "#9CA3AF", path: "M31,42 L36,42 L36,69 L31,69 Z M31,78 L36,78 L36,83 L31,83 Z" },
  { color: "#9CA3AF", path: "M50,42 L55,42 L55,69 L50,69 Z M50,78 L55,78 L55,83 L50,83 Z" },
  glassOutline,
]);

write("ic_pint", [
  { color: "#E6A329", path: "M18,18 L78,18 L70,110 L26,110 Z" },
  { color: "#F6F8FA", path: "M18,18 L78,18 L76,28 L20,28 Z" },
  glassOutline,
]);
