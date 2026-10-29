import test from "node:test";
import assert from "node:assert/strict";
import { recipes } from "../dist/data.js";
import { dateKey, nextDays, createPlan, validPlan, mealCost, ingredientsFor, scheduleFor, mealMinutes, timeLabel, calendarFor } from "../dist/core.js";

test("Vietnamese planning dates cross UTC midnight and month boundaries correctly", () => {
  assert.equal(dateKey(new Date("2026-10-28T18:00:00Z")), "2026-10-29");
  assert.deepEqual(nextDays("2026-10-30", 4), ["2026-10-30", "2026-10-31", "2026-11-01", "2026-11-02"]);
});

test("each generated lunch and dinner contains a valid main, soup and vegetable dish", () => {
  for (let day = 0; day < 7; day++) for (const meal of ["lunch", "dinner"]) assert.ok(validPlan(createPlan(day, meal)));
  assert.equal(validPlan(["pork", "chicken", "greens"]), false);
  assert.equal(validPlan(["pork", "missing", "greens"]), false);
});

test("shopping combines shared ingredients and scales amounts, rice and cost with people", () => {
  const ids = ["pork", "soup", "greens"];
  const items = ingredientsFor(ids, 2);
  assert.equal(items.filter(i => i.name === "Nước mắm" && i.unit === "muỗng cà phê").length, 1);
  assert.equal(items.find(i => i.name === "Nước mắm" && i.unit === "muỗng cà phê").amount, 1);
  assert.equal(items.find(i => i.name === "Thịt ba chỉ").amount, 200);
  assert.equal(items.find(i => i.name === "Gạo").amount, 180);
  assert.equal(mealCost(ids, 2), 52500);
  const swapped = ingredientsFor(["tofu", "soup", "greens"], 2);
  assert.equal(swapped.some(i => i.name === "Thịt ba chỉ"), false);
  assert.equal(swapped.find(i => i.name === "Đậu hũ").amount, 2);
});

test("every possible menu finishes at the requested time without using more than two burners", () => {
  const ids = kind => Object.keys(recipes).filter(id => recipes[id].kind === kind);
  for (const main of ids("main")) for (const soup of ids("soup")) for (const veg of ids("veg")) {
    const menu = [main, soup, veg];
    const tasks = scheduleFor(menu, "2026-10-29", "18:30");
    const prep = tasks.find(t => t.key === "prep");
    const finish = tasks.at(-1).at;
    assert.equal(timeLabel(finish), "18:30");
    assert.equal((finish - prep.at) / 60000, mealMinutes(menu));
    const cooking = tasks.filter(t => t.recipe);
    for (const task of cooking) {
      assert.ok(task.at >= prep.at + prep.duration * 60000);
      assert.ok(task.at + task.duration * 60000 <= finish);
    }
    for (let at = prep.at; at < finish; at += 60000) {
      assert.ok(cooking.filter(t => at >= t.at && at < t.at + t.duration * 60000).length <= 2);
    }
  }
});

test("calendar export contains a real alarm, UTC timestamps and UTF-8-safe folded lines", () => {
  const ics = calendarFor(["pork", "soup", "greens"], "2026-10-29", "18:30", Date.parse("2026-10-29T01:00:00Z"));
  assert.match(ics, /DTSTART:20261029T103000Z/);
  assert.match(ics, /DTEND:20261029T113000Z/);
  assert.match(ics, /BEGIN:VALARM\r\nTRIGGER:-PT10M/);
  for (const line of ics.split("\r\n")) assert.ok(Buffer.byteLength(line, "utf8") <= 75);
  const unfolded = ics.replace(/\r\n /g, "");
  assert.match(unfolded, /18:30 — Dọn cơm\\, cả nhà cùng ăn/);
  assert.ok(ics.endsWith("END:VCALENDAR\r\n"));
});
