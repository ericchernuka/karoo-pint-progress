import assert from "node:assert/strict";
import fs from "node:fs";

const drawable = (name) => fs.readFileSync(`pint/src/main/res/drawable/${name}.xml`, "utf8");
const colors = (qualifier = "values") => {
  const xml = fs.readFileSync(`pint/src/main/res/${qualifier}/colors.xml`, "utf8");
  return Object.fromEntries(
    [...xml.matchAll(/<color name="([^"]+)">(#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8}))<\/color>/g)]
      .map(([, name, value]) => [name, value]),
  );
};

const channel = (value) => {
  const normalized = value / 255;
  return normalized <= 0.04045
    ? normalized / 12.92
    : ((normalized + 0.055) / 1.055) ** 2.4;
};
const luminance = (hex) => {
  const rgb = [1, 3, 5].map((offset) => Number.parseInt(hex.slice(offset, offset + 2), 16));
  return 0.2126 * channel(rgb[0]) + 0.7152 * channel(rgb[1]) + 0.0722 * channel(rgb[2]);
};
const contrast = (left, right) => {
  const [bright, dark] = [luminance(left), luminance(right)].sort((a, b) => b - a);
  return (bright + 0.05) / (dark + 0.05);
};

const pathForColor = (xml, colorName) => {
  const paths = [...xml.matchAll(/<path\s+([\s\S]*?)\s*\/>/g)].map(([, attributes]) => attributes);
  const path = paths.find((attributes) => attributes.includes(`android:fillColor="@color/${colorName}"`));
  assert.ok(path, `Expected a ${colorName} path`);
  return path.match(/android:pathData="([^"]+)"/)?.[1];
};

const bounds = (pathData) => {
  // Foam and amber paths only contain absolute M/L/C coordinates, so each numeric pair is a
  // rendered point or Bezier control point. Checking all pairs catches containment regressions
  // without coupling validation to path formatting or command order.
  const coordinates = pathData.match(/-?\d+(?:\.\d+)?/g)?.map(Number) ?? [];
  assert.equal(coordinates.length % 2, 0, "Path data must contain coordinate pairs");
  const points = Array.from({ length: coordinates.length / 2 }, (_, index) => ({
    x: coordinates[index * 2],
    y: coordinates[index * 2 + 1],
  }));
  return {
    minX: Math.min(...points.map(({ x }) => x)),
    maxX: Math.max(...points.map(({ x }) => x)),
    minY: Math.min(...points.map(({ y }) => y)),
    maxY: Math.max(...points.map(({ y }) => y)),
  };
};

const foamDrawables = fs.readdirSync("pint/src/main/res/drawable")
  .filter((name) => name.endsWith(".xml"))
  .filter((name) => drawable(name.slice(0, -4)).includes("@color/pint_foam"));

for (const fileName of foamDrawables) {
  const name = fileName.slice(0, -4);
  const xml = drawable(name);
  const foam = bounds(pathForColor(xml, "pint_foam"));
  const amber = bounds(pathForColor(xml, "pint_amber"));
  assert.ok(foam.minX >= 21 && foam.maxX <= 64 && foam.minY >= 13 && foam.maxY <= 99,
    `${name}: foam must remain inside the inner glass`);
  assert.ok(amber.minX >= 21 && amber.maxX <= 64 && amber.minY >= 21 && amber.maxY <= 99,
    `${name}: amber must remain inside the straight section of the inner glass`);
  assert.ok(foam.maxY >= amber.minY,
    `${name}: foam must overlap the amber body so the fill has no visual gap`);
}

// At 100%, amber reaches the straight section and foam fills the entire rounded cap. Check all
// layout variants because Karoo can select any of them based on field dimensions.
for (const name of ["pint_full_bubbles", "pint_full_bubbles_compact", "pint_full_bubbles_icon"]) {
  const xml = drawable(name);
  const foam = bounds(pathForColor(xml, "pint_foam"));
  const amber = bounds(pathForColor(xml, "pint_amber"));
  assert.equal(amber.minY, 21, `${name}: full amber must reach the cap boundary`);
  assert.deepEqual(foam, { minX: 21, maxX: 64, minY: 13, maxY: 23 },
    `${name}: full foam must fill the rounded cap and meet the amber body`);
}

const light = colors();
const night = colors("values-night");
assert.ok(
  contrast(light.pint_foam, light.pint_surface) >= 1.5,
  "Light foam must remain visibly distinct from the glass surface",
);
assert.ok(
  contrast(light.pint_foam, light.pint_amber) >= 2,
  "Light foam must remain visibly distinct from the beer body",
);
assert.ok(
  contrast(night.pint_foam, night.pint_surface) >= 10,
  "Night foam must remain visibly distinct from the glass surface",
);
assert.ok(
  contrast(night.pint_foam, night.pint_amber) >= 1.7,
  "Night foam must remain visibly distinct from the beer body",
);

console.log("Drawable visual contracts passed");
