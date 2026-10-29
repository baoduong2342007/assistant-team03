import test from "node:test";
import assert from "node:assert/strict";
import { androidAlarmPayload, isAndroid, syncAndroid } from "../dist/platform.js";

const now = Date.parse("2026-10-29T06:00:00Z");
function state() {
  return { date: "2026-10-29", meal: "dinner", profile: { lunch: "11:30", dinner: "18:30" },
    plans: { "2026-10-29|dinner": ["pork", "soup", "greens"], "2026-10-29|lunch": ["tofu", "soup", "greens"] },
    reminders: { "2026-10-29|dinner": true, "2026-10-29|lunch": true }, timer: null };
}

test("only enabled future meals become native alarms, using Vietnam time", () => {
  const s = state();
  const alarms = androidAlarmPayload(s, now);
  assert.equal(alarms.length, 1); // Lunch has already passed.
  assert.equal(alarms[0].at, Date.parse("2026-10-29T10:30:00Z"));
  assert.equal(alarms[0].date, "2026-10-29");
  s.reminders["2026-10-29|dinner"] = false;
  assert.deepEqual(androidAlarmPayload(s, now), []);
});

test("changing a dish or eating time replaces the same native alarm, rather than adding another", () => {
  const s = state();
  const original = androidAlarmPayload(s, now)[0];
  s.plans["2026-10-29|dinner"][0] = "eggs";
  const changed = androidAlarmPayload(s, now)[0];
  assert.equal(changed.id, original.id);
  assert.ok(changed.at > original.at);
  s.profile.dinner = "19:00";
  assert.equal(androidAlarmPayload(s, now)[0].at - changed.at, 30 * 60000);
});

test("an active timer is scheduled independently and cancelling it removes its alarm", () => {
  const s = state();
  s.timer = { end: now + 10000, label: "Nấu canh" };
  const timer = androidAlarmPayload(s, now).find(a => a.timer);
  assert.equal(timer.at, now + 10000);
  assert.equal(timer.id, "cooking-timer");
  s.timer = null;
  assert.ok(!androidAlarmPayload(s, now).some(a => a.timer));
  s.timer = { end: now - 1, label: "Already expired" };
  assert.ok(!androidAlarmPayload(s, now).some(a => a.timer));
});

test("malformed plans, invalid times and distant reminders are not sent to Android", () => {
  const s = state();
  s.profile.dinner = "99:00";
  assert.deepEqual(androidAlarmPayload(s, now), []);
  s.profile.dinner = "18:30";
  s.plans["2026-10-29|dinner"] = ["pork", "missing", "greens"];
  assert.deepEqual(androidAlarmPayload(s, now), []);
  s.plans["2026-12-01|dinner"] = ["pork", "soup", "greens"];
  s.reminders["2026-12-01|dinner"] = true;
  assert.deepEqual(androidAlarmPayload(s, now), []);
});

test("a normal browser needs no native bridge", () => {
  assert.equal(isAndroid(), false);
  assert.equal(syncAndroid(state()), true);
});
