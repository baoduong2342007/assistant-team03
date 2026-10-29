import { recipes, combinations } from "./data.js";
export const currency = value => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value);
export const quantity = value => new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(value);
export function dateKey(date = new Date()) {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Ho_Chi_Minh", year: "numeric", month: "2-digit", day: "2-digit" }).format(date);
}
export function nextDays(start = dateKey(), count = 7) {
  const base = new Date(`${start}T12:00:00+07:00`);
  return Array.from({ length: count }, (_, index) => dateKey(new Date(base.getTime() + index * 86400000)));
}
export function createPlan(dayIndex, meal) {
  return [...combinations[(dayIndex + (meal === "lunch" ? 3 : 0)) % combinations.length]];
}
export function validPlan(value) {
  return Array.isArray(value) && value.length === 3 && value.every((id, i) => recipes[id]?.kind === ["main", "soup", "veg"][i]);
}
export function mealCost(ids, people = 4) {
  return Math.round(ids.reduce((total, id) => total + recipes[id].cost, 0) * people / 4);
}
export function ingredientsFor(ids, people = 4) {
  const merged = new Map();
  for (const id of ids) for (const item of recipes[id].ingredients) {
    const key = `${item.name}|${item.unit}`;
    const previous = merged.get(key);
    merged.set(key, { ...item, amount: (previous?.amount ?? 0) + item.amount * people / 4, key });
  }
  // Rice is always included alongside the three planned dishes.
  merged.set("Gạo|g", { name: "Gạo", amount: people * 90, unit: "g", group: "Gia vị & đồ khô", key: "Gạo|g" });
  return [...merged.values()];
}
export function mealMinutes(ids) {
  // One burner for the main; the second cooks the soup then the vegetables.
  return Math.max(recipes[ids[0]].minutes, recipes[ids[1]].minutes + recipes[ids[2]].minutes) + 15;
}
export function scheduleFor(ids, date, time = "18:30") {
  const finish = new Date(`${date}T${time}:00+07:00`).getTime();
  const duration = mealMinutes(ids);
  const start = finish - duration * 60000;
  const tasks = [
    { key: "prep", at: start, title: "Sơ chế & nấu cơm", detail: "Vo gạo, bật nồi cơm. Rửa rau, chuẩn bị thịt và gia vị.", duration: 15, icon: "leaf" },
    { key: "main", at: finish - recipes[ids[0]].minutes * 60000, title: `Nấu ${recipes[ids[0]].name.toLowerCase()}`, detail: recipes[ids[0]].tip, duration: recipes[ids[0]].minutes, recipe: ids[0], icon: "pot" },
    { key: "soup", at: finish - (recipes[ids[1]].minutes + recipes[ids[2]].minutes) * 60000, title: `Nấu ${recipes[ids[1]].name.toLowerCase()}`, detail: "Nấu canh trên vùng bếp thứ hai, rồi dùng vùng bếp này làm món rau.", duration: recipes[ids[1]].minutes, recipe: ids[1], icon: "bowl" },
    { key: "veg", at: finish - recipes[ids[2]].minutes * 60000, title: `Làm ${recipes[ids[2]].name.toLowerCase()}`, detail: "Chuẩn bị món rau sau cùng để giữ độ tươi ngon.", duration: recipes[ids[2]].minutes, recipe: ids[2], icon: "leaf" },
    { key: "serve", at: finish, title: "Dọn cơm, cả nhà cùng ăn", detail: "Nếm lại các món, bày ra mâm và thưởng thức.", duration: 0, icon: "heart" }
  ];
  return tasks.sort((a, b) => a.at - b.at);
}
export const timeLabel = ms => new Intl.DateTimeFormat("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", hour: "2-digit", minute: "2-digit", hour12: false }).format(new Date(ms));
const escapeICS = s => String(s).replace(/\\/g, "\\\\").replace(/;/g, "\\;").replace(/,/g, "\\,").replace(/\r?\n/g, "\\n");
const utcStamp = ms => new Date(ms).toISOString().replace(/[-:]/g, "").replace(/\.\d{3}/, "");
function foldICS(line) {
  const encoder = new TextEncoder();
  let result = "", current = "", bytes = 0;
  for (const character of line) {
    const size = encoder.encode(character).length;
    if (bytes + size > 75) { result += current + "\r\n"; current = " "; bytes = 1; }
    current += character; bytes += size;
  }
  return result + current;
}
export function calendarFor(ids, date, time, now = Date.now()) {
  const tasks = scheduleFor(ids, date, time);
  return ["BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Bep Nha//Meal Planner//VI", "CALSCALE:GREGORIAN", "BEGIN:VEVENT", `UID:bep-nha-${date}-${time.replace(":", "")}@bepnha.local`, `DTSTAMP:${utcStamp(now)}`, `DTSTART:${utcStamp(tasks[0].at)}`, `DTEND:${utcStamp(tasks.at(-1).at)}`, `SUMMARY:${escapeICS("Vào bếp cùng Bếp Nhà")}`, `DESCRIPTION:${escapeICS(tasks.map(t => `${timeLabel(t.at)} — ${t.title}`).join("\n"))}`, "BEGIN:VALARM", "TRIGGER:-PT10M", "ACTION:DISPLAY", "DESCRIPTION:10 phút nữa đến giờ chuẩn bị bữa cơm", "END:VALARM", "END:VEVENT", "END:VCALENDAR", ""].map(foldICS).join("\r\n");
}
