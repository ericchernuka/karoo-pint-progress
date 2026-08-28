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
const composite = (foreground, background, alpha) => `#${[1, 3, 5].map((offset) => {
  const front = Number.parseInt(foreground.slice(offset, offset + 2), 16);
  const back = Number.parseInt(background.slice(offset, offset + 2), 16);
  return Math.round(front * alpha + back * (1 - alpha)).toString(16).padStart(2, "0");
}).join("")}`;

const attributesForColor = (xml, colorName) =>
  [...xml.matchAll(/<path\s+([\s\S]*?)\s*\/>/g)]
    .map(([, attributes]) => attributes)
    .filter((attributes) => attributes.includes(`android:fillColor="@color/${colorName}"`));
const pathsForColor = (xml, colorName) =>
  attributesForColor(xml, colorName)
    .map((attributes) => attributes.match(/android:pathData="([^"]+)"/)?.[1]);
const alphasForColor = (xml, colorName) =>
  attributesForColor(xml, colorName)
    .map((attributes) => Number(attributes.match(/android:fillAlpha="([^"]+)"/)?.[1] ?? "1"));
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
const verticalCapDepth = (pathData) => {
  const match = pathData.match(/^M[^,]+,([^ ]+) C[^ ]+ [^ ]+ [^,]+,([^ ]+)/);
  assert.ok(match, "Expected a rounded vertical path");
  return Number(match[2]) - Number(match[1]);
};

const foamDrawables = fs.readdirSync("pint/src/main/res/drawable")
  .filter((name) => name.endsWith(".xml"))
  .filter((name) => !name.startsWith("pint_fill_"))
  .filter((name) => drawable(name.slice(0, -4)).includes("@color/pint_foam"));
const fullFoamDrawables = new Set([
  "pint_full_bubbles",
  "pint_full_bubbles_compact",
  "pint_full_bubbles_icon",
]);

for (const fileName of foamDrawables) {
  const name = fileName.slice(0, -4);
  const xml = drawable(name);
  const foam = bounds(pathForColor(xml, "pint_foam"));
  const amber = bounds(pathForColor(xml, "pint_amber"));
  const [foamTop, foamBottom, foamMessage] = fullFoamDrawables.has(name)
    ? [3, 23, "celebration foam must stay within the mug envelope and vector viewport"]
    : [13, 99, "foam must remain inside the inner glass"];
  assert.ok(foam.minX >= 21 && foam.maxX <= 64 && foam.minY >= foamTop && foam.maxY <= foamBottom,
    `${name}: ${foamMessage}`);
  assert.ok(amber.minX >= 21 && amber.maxX <= 64 && amber.minY >= 21 && amber.maxY <= 99,
    `${name}: amber must remain inside the straight section of the inner glass`);
  assert.ok(foam.maxY >= amber.minY,
    `${name}: foam must overlap the amber body so the fill has no visual gap`);
}

// Check full-pint amber and foam in all layout variants because Karoo can select any variant
for (const name of ["pint_full_bubbles", "pint_full_bubbles_compact", "pint_full_bubbles_icon"]) {
  const xml = drawable(name);
  const foam = bounds(pathForColor(xml, "pint_foam"));
  const amber = bounds(pathForColor(xml, "pint_amber"));
  assert.equal(amber.minY, 21, `${name}: full amber must reach the cap boundary`);
  assert.deepEqual(foam, { minX: 21, maxX: 64, minY: 4, maxY: 23 },
    `${name}: full foam must crown the rim and meet the amber body`);
}

const light = colors();
const night = colors("values-night");
assert.equal(light.pint_fill_amber, light.pint_amber, "Light Pints Fill must keep the mug beer color");
assert.equal(light.pint_fill_foam, light.pint_foam, "Light Pints Fill must keep the mug foam color");
assert.equal(light.pint_fill_bubble, light.pint_bubble, "Light Pints Fill must keep the mug bubble color");
assert.equal(light.pint_fill_count_shadow, "#00000000", "Light Pints Fill halo must be invisible");
assert.equal(night.pint_fill_amber, "#A85F00", "Night Pints Fill must use restrained beer");
assert.equal(night.pint_fill_foam, "#E2D5B8", "Night Pints Fill must use restrained foam");
assert.ok(night.pint_fill_bubble, "Night Pints Fill must define its own bubble color");
assert.notEqual(night.pint_fill_bubble, night.pint_foreground, "Night Pints Fill bubbles must not blend into the count");
for (const alpha of new Set(alphasForColor(drawable("pint_fill_full_foam"), "pint_fill_bubble"))) {
  assert.ok(
    contrast(composite(night.pint_fill_bubble, night.pint_fill_amber, alpha), night.pint_fill_amber) >= 1.4,
    `Night Pints Fill bubbles at ${alpha} alpha must remain distinct from the beer body`,
  );
}
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
for (const treatment of ["regular", "compact", "adaptive"]) {
  for (const [alignment, gravity] of [["left", "left|center_vertical"], ["center", "center"], ["right", "right|center_vertical"]]) {
    const wrapper = resource("layout", `pint_progress_${treatment}_${alignment}_view`);
    assert.match(wrapper, /android:id="@\+id\/pint_root"/);
    assert.match(wrapper, new RegExp(`android:gravity="${gravity.replace("|", "\\|")}"`));
    if (treatment !== "adaptive") {
      assert.equal(attribute(wrapper, "pint_root", "android:baselineAligned"), "false");
    }
    assert.match(wrapper, new RegExp(`layout="@layout/pint_progress_${treatment}_content"`));
  }
}
for (const [treatment, width, height, suffixSize] of [
  ["regular", "66dp", "89dp", "52sp"],
  ["compact", "48dp", "65dp", "52sp"],
]) {
  const content = resource("layout", `pint_progress_${treatment}_content`);
  assert.equal(attribute(content, "pint_image", "android:maxWidth"), width);
  assert.equal(attribute(content, "pint_image", "android:maxHeight"), height);
  assert.equal(attribute(content, "pint_image", "android:layout_width"), "wrap_content");
  assert.equal(attribute(content, "pint_image", "android:layout_height"), "wrap_content");
  assert.equal(attribute(content, "pint_image", "android:adjustViewBounds"), "true");
  assert.equal(attribute(content, "pint_image", "android:scaleType"), "fitCenter");
  const textHeight = treatment === "regular" ? "wrap_content" : "match_parent";
  assert.equal(attribute(content, "pint_count", "android:layout_height"), textHeight);
  assert.equal(attribute(content, "pint_count_suffix", "android:layout_height"), textHeight);
  assert.equal(attribute(content, "pint_count", "android:includeFontPadding"), "false");
  assert.equal(attribute(content, "pint_count_suffix", "android:includeFontPadding"), "false");
  assert.equal(attribute(content, "pint_count", "android:paddingTop"), undefined);
  assert.equal(
    attribute(content, "pint_count", "android:paddingBottom"),
    treatment === "regular" ? "8dp" : undefined,
  );
  assert.equal(attribute(content, "pint_count", "android:paddingStart"), "6dp");
  assert.equal(attribute(content, "pint_count", "android:paddingEnd"), "2dp");
  assert.equal(attribute(content, "pint_count", "android:letterSpacing"), undefined);
  assert.equal(attribute(content, "pint_count_suffix", "android:paddingTop"), undefined);
  assert.equal(
    attribute(content, "pint_count_suffix", "android:paddingBottom"),
    undefined,
  );
  assert.equal(attribute(content, "pint_count", "android:translationY"), undefined);
  assert.equal(attribute(content, "pint_count_suffix", "android:translationY"), undefined);
  assert.equal(attribute(content, "pint_count_suffix", "android:layout_marginStart"), "2dp");
  assert.equal(attribute(content, "pint_count_suffix", "android:layout_marginEnd"), "2dp");
  assert.equal(attribute(content, "pint_count_suffix", "android:textSize"), suffixSize);
  assert.equal(attribute(content, "pint_count", "android:gravity"), "center_vertical");
  assert.equal(attribute(content, "pint_count_suffix", "android:text"), "@string/pint_count_suffix");
  assert.equal(attribute(content, "pint_count", "android:visibility"), "gone");
  assert.equal(attribute(content, "pint_count_suffix", "android:visibility"), "gone");
}
const adaptive = resource("layout", "pint_progress_adaptive_content");
assert.equal(attribute(adaptive, "pint_image", "android:layout_width"), "wrap_content");
assert.equal(attribute(adaptive, "pint_image", "android:layout_height"), "match_parent");
assert.equal(attribute(adaptive, "pint_image", "android:adjustViewBounds"), "true");
assert.equal(attribute(adaptive, "pint_image", "android:scaleType"), "fitCenter");
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
  assert.equal(attribute(fill, "pint_fill_image", "android:scaleX"), alignment === "left" ? "-1" : undefined);
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

const layoutPolicy = fs.readFileSync(
  "pint/src/main/kotlin/io/ericchernuka/pintprogress/core/PintFieldLayout.kt",
  "utf8",
);
assert.match(layoutPolicy, /if \(boundariesEnabled\) 6 else 2/);
const dataType = fs.readFileSync(
  "pint/src/main/kotlin/io/ericchernuka/pintprogress/PintProgressDataType.kt",
  "utf8",
);
assert.match(dataType, /UpdateGraphicConfig\(showHeader = style != PintFieldStyle\.FILL\)/);
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
const assertSideTexture = (name, paths) => {
  for (const path of paths) {
    const pathBounds = bounds(path);
    assert.ok(
      pathBounds.maxX <= 25 || pathBounds.minX >= 75,
      `${name}: beer texture must leave the central half clear`,
    );
    assert.ok(
      pathBounds.minX >= 0 && pathBounds.maxX <= 100 &&
        pathBounds.minY >= 0 && pathBounds.maxY <= 100,
      `${name}: beer texture must stay inside the field viewport`,
    );
  }
};
for (const [name, expectedBubbles, expectedFoamPockets] of [
  ["pint_fill_50", 7, 0],
  ["pint_fill_80", 8, 3],
  ["pint_fill_95", 8, 3],
  ["pint_fill_full_foam", 8, 3],
  ["pint_fill_draining", 7, 0],
]) {
  const xml = drawable(name);
  const bubbles = pathsForColor(xml, "pint_fill_bubble");
  const highlights = pathsForColor(xml, "pint_beer_highlight");
  const amber = pathsForColor(xml, "pint_fill_amber");
  assert.match(pathForColor(xml, "pint_fill_foam"), /C/, `${name}: foam boundary must be irregular`);
  assert.equal(bubbles.length, expectedBubbles, `${name}: stable body bubble field`);
  assert.equal(highlights.length, 2, `${name}: two restrained side highlights`);
  assert.equal(amber.length - 1, expectedFoamPockets, `${name}: foam pocket count`);
  assertSideTexture(name, [...bubbles, ...highlights, ...amber.slice(1)]);
  highlights.forEach((path) => {
    assert.match(path, /C/, `${name}: highlights must have rounded ends`);
    assert.ok(verticalCapDepth(path) >= 5, `${name}: highlight end caps must stay soft after fitXY`);
  });
}
assert.equal(pathsForColor(drawable("pint_fill_00"), "pint_fill_bubble").length, 0);
assert.equal(pathsForColor(drawable("pint_fill_00"), "pint_beer_highlight").length, 0);
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
assert.match(resource("values", "strings"), /name="pint_progress_fill">Pints Fill<.*name="pint_progress">Pint Mug<.*name="pint_progress_text">Pints Count</s);
assert.match(resource("values", "strings"), /name="pint_count_suffix">\+</);
assert.deepEqual([...resource("xml", "extension_info").matchAll(/<DataType[^>]*graphical="false"[^>]*typeId="pint-progress-text"/g)].length, 1);
assert.match(
  resource("xml", "extension_info"),
  /typeId="pint-progress-fill" \/><DataType[^>]*graphical="true"[^>]*typeId="pint-progress" \/><DataType[^>]*graphical="false"[^>]*typeId="pint-progress-text" \/>/,
);
assert.match(mappings, /PintAsset\.PINT_50 -> R\.drawable\.pint_50_compact/);

console.log("Drawable visual contracts passed");
