import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import assert from "node:assert/strict";
const hashes = JSON.parse(
  readFileSync(
    new URL("../src/demo-image-hashes.json", import.meta.url),
    "utf8",
  ),
);
for (const [name, expected] of Object.entries(hashes)) {
  const bytes = readFileSync(
    new URL(`../public/demo/${name}.svg`, import.meta.url),
  );
  assert.equal(
    createHash("sha256").update(bytes).digest("hex"),
    expected,
    `${name}: sample image hash mismatch`,
  );
}
console.log("Sample image hashes verified.");
