import test, { beforeEach } from "node:test";
import assert from "node:assert/strict";
import { loadProgress, saveProgress } from "../src/game/progress.js";

let values;
beforeEach(() => {
  values = new Map();
  globalThis.localStorage = {
    getItem(key) { return values.has(key) ? values.get(key) : null; },
    setItem(key, value) { values.set(key, String(value)); }
  };
});

test("new players start with an empty journey", () => {
  assert.deepEqual(loadProgress("ccs-save-v2", 8), {
    foundIds: [], built: [], totalFound: 0, currency: 0
  });
});

test("legacy saves migrate found seeds separately from spendable currency", () => {
  values.set("ccs-save", JSON.stringify({
    collected: 2,
    built: ["garden"],
    seedIds: [0, 2, 4, 6]
  }));
  const progress = loadProgress("ccs-save-v2", 8);
  assert.deepEqual(progress, {
    foundIds: [0, 2, 4, 6],
    built: ["garden"],
    totalFound: 4,
    currency: 2
  });
});

test("unknown seed ids and unknown buildings are ignored", () => {
  values.set("ccs-save-v2", JSON.stringify({
    totalFound: 4,
    currency: 3,
    seedIds: [0, 2, 88, -1, "4"],
    built: ["garden", "spaceship"]
  }));
  const progress = loadProgress("ccs-save-v2", 8);
  assert.deepEqual(progress, {
    foundIds: [0, 2],
    built: ["garden"],
    totalFound: 4,
    currency: 3
  });
});

test("saving and loading keeps the full journey", () => {
  const state = {
    foundIds: [0, 1, 2],
    currency: 1,
    totalFound: 3,
    built: ["garden"]
  };
  saveProgress("ccs-save-v2", state);
  assert.deepEqual(loadProgress("ccs-save-v2", 8), state);
});
test("duplicate and invalid save entries are normalized", () => {
  values.set("ccs-save-v2", JSON.stringify({
    totalFound: 99,
    currency: -5,
    seedIds: [1, 1, 3, -2, 40],
    built: ["garden", "garden", "spaceship"]
  }));
  assert.deepEqual(loadProgress("ccs-save-v2", 8), {
    foundIds: [1, 3],
    built: ["garden"],
    totalFound: 8,
    currency: 0
  });
});
