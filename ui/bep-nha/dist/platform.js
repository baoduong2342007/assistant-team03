import { recipes } from "./data.js";
import { validPlan, scheduleFor, timeLabel, dateKey } from "./core.js";

const bridge = () => typeof window !== "undefined" ? window.BepNhaAndroid : undefined;
export const isAndroid = () => typeof bridge()?.syncReminders === "function";
export function androidStatus() {
  try { return JSON.parse(bridge()?.getStatus() || "{}"); }
  catch { return {}; }
}
export function androidLaunchContext() {
  try { return JSON.parse(bridge()?.getLaunchContext() || "{}"); }
  catch { return {}; }
}
export const requestAndroidNotifications = () => bridge()?.requestNotifications();
export const requestAndroidExactAlarms = () => bridge()?.requestExactAlarms();

/** Pure payload builder: disabled, expired or malformed reminders must never reach AlarmManager. */
export function androidAlarmPayload(state, now = Date.now()) {
  const alarms = [];
  const inWindow = at => Number.isFinite(at) && at > now && at - now <= 15 * 86400000;
  for (const [key, enabled] of Object.entries(state.reminders ?? {})) {
    const match = /^(\d{4}-\d{2}-\d{2})\|(lunch|dinner)$/.exec(key);
    if (!enabled || !match) continue;
    const [, date, meal] = match;
    const ids = state.plans?.[key];
    const time = state.profile?.[meal];
    if (!validPlan(ids) || !/^([01]\d|2[0-3]):[0-5]\d$/.test(time)) continue;
    const start = scheduleFor(ids, date, time)[0].at;
    if (!inWindow(start)) continue;
    alarms.push({ id: `meal:${key}`, at: start, title: `Đến giờ chuẩn bị ${meal === "lunch" ? "bữa trưa" : "bữa tối"}`,
      body: `Cả nhà ăn lúc ${time}. ${ids.map(id => recipes[id].name).join(", ")}.`, date, meal, timer: false });
  }
  if (inWindow(state.timer?.end) && state.timer.end - now <= 3 * 60 * 60 * 1000 && typeof state.timer.label === "string") {
    alarms.push({ id: "cooking-timer", at: state.timer.end, title: "Đã hết giờ nấu",
      body: `${state.timer.label.slice(0, 200)}: kiểm tra độ chín của món ăn nhé.`,
      date: /^\d{4}-\d{2}-\d{2}$/.test(state.date) ? state.date : dateKey(new Date(now)),
      meal: state.meal === "lunch" ? "lunch" : "dinner", timer: true });
  }
  return alarms.sort((a, b) => a.id.localeCompare(b.id));
}
export function syncAndroid(state) {
  if (!isAndroid()) return true;
  try { return bridge().syncReminders(JSON.stringify(androidAlarmPayload(state))) === true; }
  catch { return false; }
}
export function androidCalendar(tasks) {
  if (!isAndroid()) return false;
  bridge().openCalendar(JSON.stringify({ title: "Vào bếp cùng Bếp Nhà", start: tasks[0].at, end: tasks.at(-1).at,
    description: tasks.map(task => `${timeLabel(task.at)} — ${task.title}`).join("\n") }));
  return true;
}
export function androidCopy(text) {
  if (!isAndroid()) return false;
  bridge().copyText(text);
  return true;
}
