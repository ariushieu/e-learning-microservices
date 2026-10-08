import assert from "node:assert/strict";
import test from "node:test";
import { lessonCompletionRows } from "../frontend/src/components/enrollment/course-learning-summary-model.ts";

const lesson = (id, position = id) => ({ id, position, title: `Bài ${id}` });
const section = (id, position, lessons) => ({ id, position, title: `Chương ${id}`, lessons });
const count = (lessonId, completionRate, completedCount = 0) => ({ lessonId, completionRate, completedCount });

test("assignment fixture preserves precise rates and highlights the second lesson", () => {
  const rows = lessonCompletionRows([section(1, 1, [lesson(1), lesson(2)])], [count(1, 66.67, 2), count(2, 33.33, 1)]);
  assert.deepEqual(rows.map(({ completedCount, completionRate, drop, highlighted }) =>
    ({ completedCount, completionRate, drop, highlighted })), [
    { completedCount: 2, completionRate: 66.67, drop: 0, highlighted: false },
    { completedCount: 1, completionRate: 33.33, drop: 33.34, highlighted: true },
  ]);
});

test("current curriculum adds untouched lessons at zero and hides deleted lessons", () => {
  const rows = lessonCompletionRows([section(1, 1, [lesson(1), lesson(3)])], [count(1, 75, 3), count(2, 90, 4)]);
  assert.deepEqual(rows.map(({ id, completedCount, completionRate }) => ({ id, completedCount, completionRate })), [
    { id: 1, completedCount: 3, completionRate: 75 }, { id: 3, completedCount: 0, completionRate: 0 },
  ]);
});

test("sorts by chapter and lesson position without mutating server props", () => {
  const sections = [section(2, 2, [lesson(4, 1)]), section(1, 1, [lesson(2, 2), lesson(1, 1)])];
  const original = structuredClone(sections);
  const rows = lessonCompletionRows(sections, [count(1, 100), count(2, 80), count(4, 50)]);
  assert.deepEqual(rows.map((row) => row.id), [1, 2, 4]);
  assert.equal(rows[2].highlighted, true);
  assert.equal(rows[2].drop, 30);
  assert.deepEqual(sections, original);
});

test("20 percentage points is inclusive despite floating-point subtraction", () => {
  const rows = lessonCompletionRows([section(1, 1, [lesson(1), lesson(2), lesson(3)])],
    [count(1, 80.01), count(2, 60.01), count(3, 40.02)]);
  assert.equal(rows[1].drop, 20);
  assert.equal(rows[1].highlighted, true);
  assert.equal(rows[2].drop, 19.99);
  assert.equal(rows[2].highlighted, false);
});

test("zero participation and increasing rates do not suggest a drop", () => {
  const curriculum = [section(1, 1, [lesson(1), lesson(2), lesson(3)])];
  assert.ok(lessonCompletionRows(curriculum, []).every((row) => !row.highlighted));
  assert.ok(lessonCompletionRows(curriculum, [count(1, 0), count(2, 20), count(3, 40)])
    .every((row) => !row.highlighted));
});

test("empty curriculum never displays obsolete progress records", () => {
  assert.deepEqual(lessonCompletionRows([], [count(1, 100, 1)]), []);
});
