import assert from "node:assert/strict";
import fs from "node:fs";

const drawable = (name) => fs.readFileSync(`pint/src/main/res/drawable/${name}.xml`, "utf8");
const resource = (type, name) => fs.readFileSync(`pint/src/main/res/${type}/${name}.xml`, "utf8");
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
const attributesForColor = (xml, colorName) =>
  [...xml.matchAll(/<path\s+([\s\S]*?)\s*\/>/g)]
    .map(([, attributes]) => attributes)
    .filter((attributes) => attributes.includes(`android:fillColor="@color/${colorName}"`));
const pathsForColor = (xml, colorName) =>
  attributesForColor(xml, colorName)
    .map((attributes) => attributes.match(/android:pathData="([^"]+)"/)?.[1]);
const pathForColor = (xml, colorName) => {
  const path = pathsForColor(xml, colorName)[0];
  assert.ok(path, `Expected a ${colorName} path`);
  return path;
};

const bounds = (pathData) => {
  // Check every absolute M/L/C point to detect containment without depending on path formatting
  const coordinates = pathData.match(/-?\d+(?:\.\d+)?/g)?.map(Number) ?? [];
  assert.equal(coordinates.length % 2, 0, "Path data must contain coordinate pairs");
  const x = coordinates.filter((_, index) => index % 2 === 0);
  const y = coordinates.filter((_, index) => index % 2 === 1);
  return {
    minX: Math.min(...x),
    maxX: Math.max(...x),
    minY: Math.min(...y),
    maxY: Math.max(...y),
  };
};

const icon = drawable("ic_pint");
const iconFoam = bounds(pathForColor(icon, "pint_foam"));
const iconAmber = bounds(pathForColor(icon, "pint_amber"));
assert.ok(iconFoam.minX >= 21 && iconFoam.maxX <= 64 && iconFoam.minY >= 13 && iconFoam.maxY <= 99,
  "The shared icon foam must remain inside the inner glass");
assert.ok(iconAmber.minX >= 21 && iconAmber.maxX <= 64 && iconAmber.minY >= 21 && iconAmber.maxY <= 99,
  "The shared icon amber must remain inside the inner glass");
assert.ok(iconFoam.maxY >= iconAmber.minY,
  "The shared icon foam must overlap the amber body");

const light = colors();
const night = colors("values-night");
assert.equal(light.pint_fill_amber, light.pint_amber, "Light Pints Fill must keep the mug beer color");
assert.equal(light.pint_fill_foam, light.pint_foam, "Light Pints Fill must keep the mug foam color");
assert.equal(light.pint_fill_count_shadow, "#00000000", "Light Pints Fill halo must be invisible");
assert.equal(night.pint_fill_amber, "#A85F00", "Night Pints Fill must use restrained beer");
assert.equal(night.pint_fill_foam, "#E2D5B8", "Night Pints Fill must use restrained foam");
assert.equal(night.pint_fill_count_shadow, night.pint_surface, "Night Pints Fill halo must use the dark surface");
const assertContrast = (palette, against, minimum, mode, surface) => assert.ok(
  contrast(palette.pint_foam, palette[`pint_${against}`]) >= minimum,
  `${mode} foam must remain visibly distinct from the ${surface}`,
);
assertContrast(light, "surface", 1.5, "Light", "glass surface");
assertContrast(light, "amber", 2, "Light", "beer body");
assertContrast(night, "surface", 10, "Night", "glass surface");
assertContrast(night, "amber", 1.7, "Night", "beer body");

const element = (xml, id) => xml.match(new RegExp(`<[^>]+android:id="@\\+id/${id}"[^>]*>`))?.[0];
const attribute = (xml, id, name) => element(xml, id)?.match(new RegExp(`${name}="([^"]+)"`))?.[1];
for (const [alignment, gravity] of [
  ["left", "left|center_vertical"],
  ["center", "center"],
  ["right", "right|center_vertical"],
]) {
  const fill = resource("layout", `pint_progress_fill_${alignment}_view`);
  assert.match(fill, /android:id="@\+id\/pint_fill_root"/);
  assert.equal(attribute(fill, "pint_fill_root", "android:layout_width"), "match_parent");
  assert.equal(attribute(fill, "pint_fill_root", "android:layout_height"), "match_parent");
  assert.equal(attribute(fill, "pint_fill_image", "android:layout_width"), "match_parent");
  assert.equal(attribute(fill, "pint_fill_image", "android:layout_height"), "match_parent");
  assert.equal(attribute(fill, "pint_fill_image", "android:scaleType"), "fitXY");
  assert.equal(attribute(fill, "pint_fill_image", "android:scaleX"), undefined);
  assert.ok(
    fill.indexOf("@+id/pint_fill_image") < fill.indexOf("@+id/pint_fill_count"),
    `${alignment} Pints Fill count must render above every fill state`,
  );
  assert.equal(attribute(fill, "pint_fill_count", "android:layout_width"), "wrap_content");
  assert.equal(attribute(fill, "pint_fill_count", "android:layout_height"), "match_parent");
  assert.equal(attribute(fill, "pint_fill_count", "android:layout_gravity"), gravity);
  assert.equal(attribute(fill, "pint_fill_count", "android:gravity"), "center_vertical");
  assert.equal(attribute(fill, "pint_fill_count", "android:fontFamily"), "sans-serif-condensed");
  assert.equal(attribute(fill, "pint_fill_count", "android:includeFontPadding"), "false");
  assert.equal(attribute(fill, "pint_fill_count", "android:singleLine"), "true");
  assert.equal(attribute(fill, "pint_fill_count", "android:text"), "—");
  assert.equal(attribute(fill, "pint_fill_count", "android:textColor"), "@color/pint_foreground");
  assert.equal(attribute(fill, "pint_fill_count", "android:shadowColor"), "@color/pint_fill_count_shadow");
  assert.equal(attribute(fill, "pint_fill_count", "android:shadowRadius"), "3");
  assert.equal(attribute(fill, "pint_fill_count", "android:textScaleX"), "1");
  assert.equal(attribute(fill, "pint_fill_count", "android:textSize"), "1sp");
  assert.equal(attribute(fill, "pint_fill_count", "android:textStyle"), "normal");
  assert.equal(attribute(fill, "pint_fill_count", "android:visibility"), "visible");
  assert.doesNotMatch(fill, /pint_count_suffix/);
}

const dataType = fs.readFileSync(
  "pint/src/main/kotlin/io/ericchernuka/pintprogress/PintProgressDataType.kt",
  "utf8",
);
assert.match(dataType, /UpdateGraphicConfig\(showHeader = false\)/);
const remoteViews = fs.readFileSync(
  "pint/src/main/kotlin/io/ericchernuka/pintprogress/PintRemoteViews.kt",
  "utf8",
);
for (const [alignment, resourceName] of [
  ["LEFT", "left"],
  ["CENTER", "center"],
  ["RIGHT", "right"],
]) {
  assert.match(
    remoteViews,
    new RegExp(`ViewConfig\\.Alignment\\.${alignment} -> R\\.layout\\.pint_progress_fill_${resourceName}_view`),
  );
}

const renderFillStart = remoteViews.indexOf("fun renderFill(");
const renderFillEnd = remoteViews.indexOf("\n\n}", renderFillStart);
const renderFill = remoteViews.slice(renderFillStart, renderFillEnd);
assert.doesNotMatch(renderFill, /setViewPadding/, "Pints Fill must render edge to edge");

const mappings = fs.readFileSync("pint/src/main/kotlin/io/ericchernuka/pintprogress/PintAssetDrawables.kt", "utf8");
for (let bucket = 0; bucket < 20; bucket += 1) {
  const percent = String(bucket * 5).padStart(2, "0");
  const xml = drawable(`pint_fill_${percent}`);
  assert.match(mappings, new RegExp(`PintFillAsset\\.FILL_${percent} -> R\\.drawable\\.pint_fill_${percent}`));
  assert.match(xml, /android:fillColor="@color\/pint_surface"/);
  if (bucket === 0) {
    assert.doesNotMatch(xml, /android:fillColor="@color\/pint_fill_amber"/);
    assert.doesNotMatch(xml, /android:fillColor="@color\/pint_fill_foam"/);
  } else {
    assert.match(xml, /android:fillColor="@color\/pint_fill_amber"/);
    assert.match(xml, /android:fillColor="@color\/pint_fill_foam"/);
    const amber = bounds(pathForColor(xml, "pint_fill_amber"));
    const foam = bounds(pathForColor(xml, "pint_fill_foam"));
    assert.equal(amber.minY, 100 - bucket * 5);
    assert.equal(amber.maxY, 100);
    assert.equal(foam.maxY - foam.minY, bucket >= 16 ? 9 : 5);
  }
}
// fitXY stretches the 100x100 art to the field, so only full-width bands may appear
for (const name of [...Array.from({ length: 20 }, (_, bucket) => `pint_fill_${String(bucket * 5).padStart(2, "0")}`),
  "pint_fill_full_foam", "pint_fill_draining"]) {
  const xml = drawable(name);
  const fills = [...xml.matchAll(/android:fillColor="@color\/([^"]+)"/g)].map(([, color]) => color);
  assert.deepEqual(
    [...new Set(fills)],
    name === "pint_fill_00" ? ["pint_surface"] : ["pint_surface", "pint_fill_amber", "pint_fill_foam"],
    `${name}: only flat surface, beer, and foam bands`,
  );
  assert.equal(fills.length, new Set(fills).size, `${name}: one path per band`);
  assert.doesNotMatch(xml, /fillAlpha/, `${name}: no translucent texture`);
}
assert.match(pathForColor(drawable("pint_fill_50"), "pint_fill_foam"), /C/, "Foam boundary must stay irregular");
for (const [name, amberTop, foamTop, foamBottom] of [
  ["pint_fill_full_foam", 0, 0, 11],
  ["pint_fill_draining", 45, 43, 48],
]) {
  const xml = drawable(name);
  const assetName = name.slice("pint_".length).toUpperCase();
  assert.match(mappings, new RegExp(`PintFillAsset\\.${assetName} -> R\\.drawable\\.${name}`));
  assert.deepEqual(bounds(pathForColor(xml, "pint_fill_amber")), {
    minX: 0, maxX: 100, minY: amberTop, maxY: 100,
  });
  assert.deepEqual(bounds(pathForColor(xml, "pint_fill_foam")), {
    minX: 0, maxX: 100, minY: foamTop, maxY: foamBottom,
  });
}
assert.match(drawable("pint_fill_unavailable"), /android:fillColor="@color\/pint_unavailable_surface"/);
assert.match(resource("values", "strings"), /name="pint_progress_fill">Pints Fill<.*name="pint_progress_text">Pints Count</s);
assert.doesNotMatch(resource("values", "strings"), /Pint Mug|name="pint_progress"|name="pint_count_suffix"/);
const extensionInfo = resource("xml", "extension_info");
assert.equal([...extensionInfo.matchAll(/<DataType /g)].length, 2);
assert.match(
  extensionInfo,
  /typeId="pint-progress-fill" \/><DataType[^>]*graphical="false"[^>]*typeId="pint-progress-text" \/>/,
);
assert.doesNotMatch(extensionInfo, /typeId="pint-progress"/);
assert.match(resource("layout", "activity_pint_settings"), /android:src="@drawable\/ic_pint"/);

console.log("Drawable visual contracts passed");
