import { recipes } from "./data.js";
import { currency, quantity, dateKey, nextDays, createPlan, validPlan, mealCost, ingredientsFor, mealMinutes, scheduleFor, timeLabel, calendarFor } from "./core.js";
import { isAndroid, androidStatus, androidLaunchContext, requestAndroidNotifications, requestAndroidExactAlarms, syncAndroid, androidCalendar, androidCopy } from "./platform.js";

const $ = (selector, parent = document) => parent.querySelector(selector);
const escape = value => String(value).replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const paths = {
  home: '<path d="m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z"/>',
  calendar: '<rect x="3" y="5" width="18" height="16" rx="3"/><path d="M16 3v4M8 3v4M3 11h18m-13 4h2m4 0h2m-8 3h2"/>',
  bag: '<path d="M5 7h14l2 14H3L5 7Z"/><path d="M8 8V6a4 4 0 0 1 8 0v2"/>',
  pot: '<path d="M4 11h16v6a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4Zm-3 2h3m16 0h3M3 8h18m-11 0V6h4v2M8 3V1m8 2V1"/>',
  bowl: '<path d="M3 11h18c-1 6-4 9-9 9s-8-3-9-9Zm5 12h8M8 7c-3-3 3-3 0-6m8 6c-3-3 3-3 0-6"/>',
  leaf: '<path d="M20 3C8 1 2 7 4 15s17 6 16-12Z"/><path d="M3 22 16 8m-9 9v-5m4 1h5"/>',
  heart: '<path d="m12 21-8-8a5.5 5.5 0 0 1 8-8 5.5 5.5 0 0 1 8 8Z"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 6v6l4 2"/>',
  people: '<circle cx="9" cy="7" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3m2-17a3 3 0 0 1 0 6m2 5a5 5 0 0 1 2 4v2"/>',
  bell: '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9Zm-9 12a3 3 0 0 0 6 0"/>',
  chevron: '<path d="m9 5 7 7-7 7"/>',
  down: '<path d="m6 9 6 6 6-6"/>',
  swap: '<path d="M4 7h15m-4-4 4 4-4 4M20 17H5m4-4-4 4 4 4"/>',
  arrow: '<path d="M4 12h16m-6-6 6 6-6 6"/>',
  spark: '<path d="m12 3 2.6 6.4L21 12l-6.4 2.6L12 21l-2.6-6.4L3 12l6.4-2.6L12 3Zm8-2v4m-2-2h4"/>',
  book: '<path d="M12 5v16M3 3c4-1 7 0 9 2 2-2 5-3 9-2v16c-4-1-7 0-9 2-2-2-5-3-9-2Z"/>',
  check: '<path d="m5 12 4 4L20 5"/>',
  close: '<path d="m6 6 12 12M6 18 18 6"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="m9 2 6 0 1 3 3 1 3 3v6l-3 1-1 3-3 3H9l-1-3-3-1-3-3V9l3-1 1-3Z"/>',
  light: '<path d="M9 18h6v-3a6 6 0 1 0-6 0Zm0 3h6M12 3v2"/>',
  send: '<path d="m22 2-7 20-4-9L2 9l20-7ZM11 13 22 2"/>',
  download: '<path d="M12 3v12m-5-5 5 5 5-5M4 17v4h16v-4"/>',
  play: '<path d="m8 4 13 8-13 8Z"/>',
  pause: '<path d="M8 4v16M16 4v16"/>',
  reset: '<path d="M3 11a9 9 0 1 1 2 7M3 4v7h7"/>',
  wallet: '<rect x="3" y="5" width="18" height="15" rx="2"/><path d="M3 8h18m-5 4h5v5h-5Z"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v6m0-10h.01"/>'
};
const icon = (name, extra = "") => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" ${extra}>${paths[name] ?? paths.pot}</svg>`;
const button = (text, action, style = "secondary", data = "", symbol = "") => `<button ${/\btype=/.test(data) ? "" : 'type="button"'} class="button ${style}" data-action="${action}" ${data}>${symbol ? icon(symbol) : ""}${text}</button>`;
const today = dateKey();
const days = nextDays(today);
const weekdays = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];
const dateObject = date => new Date(`${date}T12:00:00+07:00`);
const dateLabel = date => new Intl.DateTimeFormat("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", day: "numeric", month: "numeric" }).format(dateObject(date));
const weekday = date => weekdays[dateObject(date).getUTCDay()];
const fullDate = date => new Intl.DateTimeFormat("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", weekday: "long", day: "numeric", month: "long" }).format(dateObject(date));
const STORAGE = "bep-nha-v1";
let saved = {};
try { saved = JSON.parse(localStorage.getItem(STORAGE) || "{}"); } catch { /* Start fresh if browser storage is unavailable or damaged. */ }
if (!saved || typeof saved !== "object" || Array.isArray(saved)) saved = {};
const androidLaunch = androidLaunchContext();
const state = {
  view: isAndroid() && ["home", "plan", "shopping", "cook", "settings", "favorites"].includes(androidLaunch.view ?? saved.view) ? (androidLaunch.view ?? saved.view) : "home",
  date: days.includes(androidLaunch.date) ? androidLaunch.date : days.includes(saved.date) ? saved.date : today,
  meal: (androidLaunch.meal ?? saved.meal) === "lunch" ? "lunch" : "dinner",
  profile: { name: typeof saved.profile?.name === "string" ? saved.profile.name.slice(0, 35) : "Nhà mình", people: Number.isInteger(saved.profile?.people) && saved.profile.people >= 1 && saved.profile.people <= 12 ? saved.profile.people : 4, lunch: /^\d{2}:\d{2}$/.test(saved.profile?.lunch) ? saved.profile.lunch : "11:30", dinner: /^\d{2}:\d{2}$/.test(saved.profile?.dinner) ? saved.profile.dinner : "18:30" },
  plans: Object.fromEntries(Object.entries(saved.plans ?? {}).filter(([, v]) => validPlan(v))),
  checked: saved.checked && typeof saved.checked === "object" ? saved.checked : {},
  reminders: saved.reminders && typeof saved.reminders === "object" ? saved.reminders : {},
  notified: saved.notified && typeof saved.notified === "object" ? saved.notified : {},
  favorites: Array.isArray(saved.favorites) ? saved.favorites.filter(id => recipes[id]) : [],
  done: saved.done && typeof saved.done === "object" ? saved.done : {},
  timer: saved.timer && Number.isFinite(saved.timer.end) && typeof saved.timer.label === "string" ? saved.timer : null
};
let storageWarning = false;
let nativeWarning = false;
function persist() {
  try { localStorage.setItem(STORAGE, JSON.stringify(state)); }
  catch { if (!storageWarning) { storageWarning = true; toast("Trình duyệt chưa cho lưu dữ liệu. Hãy giữ trang mở để dùng tiếp."); } }
  if (!syncAndroid(state) && !nativeWarning) { nativeWarning = true; toast("Android chưa lưu được lịch nhắc. Hãy mở lại ứng dụng và kiểm tra mục Nhắc nấu."); }
}
const planKey = (date = state.date, meal = state.meal) => `${date}|${meal}`;
// Retain generated plans by date so tomorrow's reminder uses today's saved choice.
for (const [dayIndex, date] of days.entries()) for (const meal of ["lunch", "dinner"]) {
  state.plans[planKey(date, meal)] ??= createPlan(dayIndex, meal);
}
function plan(date = state.date, meal = state.meal) {
  const key = planKey(date, meal);
  return state.plans[key] ?? createPlan(Math.max(0, days.indexOf(date)), meal);
}
const mealName = () => state.meal === "lunch" ? "Bữa trưa" : "Bữa tối";
const readyAt = () => state.profile[state.meal];
const schedule = () => scheduleFor(plan(), state.date, readyAt());
const selectedCaption = () => state.date === today ? "hôm nay" : `${weekday(state.date)}, ${dateLabel(state.date)}`;
const navItems = [["home", "home", "Hôm nay"], ["plan", "calendar", "Thực đơn"], ["shopping", "bag", "Đi chợ"], ["cook", "pot", "Vào bếp"]];
const pageNames = { home: "Hôm nay", plan: "Thực đơn của nhà", shopping: "Danh sách đi chợ", cook: "Cùng nhau vào bếp", favorites: "Món đã lưu", settings: "Gia đình của bạn" };

function nav() {
  return `<aside class="sidebar"><a class="brand" href="#home" data-action="nav" data-view="home"><span class="brand-mark">${icon("bowl")}</span>Bếp Nhà</a><div class="brand-caption">GIỮ VỊ NHÀ · GỬI YÊU THƯƠNG</div><p class="nav-label">GÓC BẾP CỦA BẠN</p><nav class="side-nav" aria-label="Điều hướng chính">${navItems.map(([view, symbol, title]) => `<button class="nav-item ${state.view === view ? "active" : ""}" data-action="nav" data-view="${view}" ${state.view === view ? 'aria-current="page"' : ""}>${icon(symbol)}${title}${view === "plan" ? '<span class="nav-badge">7 ngày</span>' : ""}</button>`).join("")}<button class="nav-item ${state.view === "favorites" ? "active" : ""}" data-action="nav" data-view="favorites">${icon("heart")}Món đã lưu</button></nav><div class="side-bottom"><div class="side-note">${icon("leaf")}<strong>Bữa cơm nhỏ, yêu thương lớn.</strong><p>Mỗi ngày một chút chăm chút,<br>để căn bếp luôn ấm.</p></div><button class="profile" data-action="nav" data-view="settings"><span class="avatar">${icon("people")}</span><span><strong>${escape(state.profile.name)}</strong><small>Gia đình ${state.profile.people} người</small></span>${icon("settings")}</button></div></aside>`;
}
function render() {
  $("#app").innerHTML = `${nav()}<div class="workspace"><header class="topbar"><div class="breadcrumb">${icon("home")}<span>Góc bếp</span>${icon("chevron")}<strong>${pageNames[state.view]}</strong></div><a class="brand mobile-brand" href="#home" data-action="nav" data-view="home"><span class="brand-mark">${icon("bowl")}</span>Bếp Nhà</a><div class="top-actions"><span class="top-date">${fullDate(today)}</span><button class="icon-btn" data-action="reminder" aria-label="Cài đặt nhắc nấu">${icon("bell")}${state.reminders[planKey()] ? '<span class="notification-dot"></span>' : ""}</button><button class="avatar" data-action="nav" data-view="settings" aria-label="Cài đặt gia đình">${icon("people")}</button></div></header><main id="main" class="main" tabindex="-1">${({ home: homeView, plan: planView, shopping: shoppingView, cook: cookView, settings: settingsView, favorites: favoritesView })[state.view]()}<p class="footer-note">${icon("heart")}Một chút chuẩn bị, một bữa cơm tròn vị.</p></main></div><nav class="bottom-nav" aria-label="Điều hướng trên điện thoại">${navItems.map(([view, symbol, title]) => `<button class="${state.view === view ? "active" : ""}" data-action="nav" data-view="${view}" ${state.view === view ? 'aria-current="page"' : ""}>${icon(symbol)}<span>${title}</span></button>`).join("")}</nav>`;
  updateTimer();
}
function heading(title, subtitle, action = "", overline = "GÓC BẾP CỦA BẠN") {
  return `<div class="page-heading"><div><div class="eyebrow">${icon("leaf")}${overline}</div><h1>${title}</h1>${subtitle ? `<p class="description">${subtitle}</p>` : ""}</div>${action}</div>`;
}
function daySelector() {
  return `<div class="day-selector" aria-label="Chọn ngày">${days.map(d => `<button class="day ${d === state.date ? "active" : ""} ${d === today ? "today" : ""}" data-action="date" data-date="${d}" aria-label="${escape(fullDate(d))}${d === today ? ", hôm nay" : ""}" aria-pressed="${d === state.date}"><span>${d === today ? "Hôm nay" : weekday(d)}</span><strong>${Number(d.slice(-2))}</strong></button>`).join("")}</div>`;
}
function mealToggle() {
  return `<div class="segmented" aria-label="Chọn bữa ăn"><button class="${state.meal === "lunch" ? "active" : ""}" data-action="meal" data-meal="lunch" aria-pressed="${state.meal === "lunch"}">Bữa trưa</button><button class="${state.meal === "dinner" ? "active" : ""}" data-action="meal" data-meal="dinner" aria-pressed="${state.meal === "dinner"}">Bữa tối</button></div>`;
}
function dishCard(id, index, allowSwap = true) {
  const r = recipes[id];
  return `<article class="dish"><div class="dish-image ${r.photo || r.kind}" role="img" aria-label="${escape(r.photo ? `Ảnh minh họa ${r.name}` : r.name)}">${r.photo ? "" : `<span class="food-emoji" aria-hidden="true">${r.emoji}</span>`}<button class="${state.favorites.includes(id) ? "saved" : ""}" data-action="favorite" data-id="${id}" aria-label="${state.favorites.includes(id) ? "Bỏ lưu" : "Lưu"} ${escape(r.name)}" aria-pressed="${state.favorites.includes(id)}">${icon("heart")}</button></div><div class="dish-body"><div class="dish-type">${r.label}</div><button class="dish-name" data-action="recipe" data-id="${id}"><h3>${r.name}</h3></button><div class="dish-meta">${icon("clock")} ${r.minutes} phút <span aria-hidden="true">·</span> Dễ nấu</div></div><div class="dish-footer"><button data-action="recipe" data-id="${id}">${icon("book")}Công thức</button>${allowSwap ? `<button data-action="swap" data-index="${index}" aria-label="Đổi món ${r.name}">${icon("swap")}Đổi món</button>` : ""}</div></article>`;
}
function timelinePanel() {
  const tasks = schedule();
  const enabled = !!state.reminders[planKey()];
  return `<section class="panel"><div class="panel-heading"><h3>${icon("clock")}Lịch vào bếp</h3><button class="text-button" data-action="reminder">Chỉnh giờ ${icon("chevron")}</button></div><div class="ready-time"><small>Cả nhà ăn lúc</small><strong>${readyAt()}<span>${selectedCaption()}</span></strong></div><div class="timeline">${tasks.map(t => `<div class="timeline-item"><span class="timeline-dot">${icon(t.icon)}</span><div><time>${timeLabel(t.at)}</time><strong>${escape(t.title)}</strong></div></div>`).join("")}</div><div class="reminder-switch"><span>${icon("bell")} ${isAndroid() ? "Nhắc trên Android" : "Nhắc khi mở Bếp Nhà"}</span><button class="switch ${enabled ? "on" : ""}" role="switch" aria-checked="${enabled}" aria-label="Nhắc giờ bắt đầu nấu" data-action="toggle-reminder"><span></span></button></div></section>`;
}
function assistantStrip() {
  return `<div class="assistant-strip"><span class="assistant-icon">${icon("spark")}</span><div><strong>Hôm nay bạn muốn nấu gì?</strong><p>Hỏi Bếp Nhà để tìm món, xem cách nấu và đổi vị.</p></div><button class="button secondary" data-action="assistant">Hỏi bếp ${icon("arrow")}</button></div>`;
}
function homeView() {
  return `${heading("Hôm nay nấu gì?", "Bữa cơm ngon, nhẹ chuyện bếp núc.", button(`${icon("calendar")}<span class="desktop-only">Thực đơn </span>7 ngày`, "nav", "secondary", 'data-view="plan"'))}<section class="hero" aria-label="Cảm hứng bữa cơm nhà"><div class="hero-copy"><span class="tiny-pill">${icon("leaf")}VỊ NGON QUEN THUỘC</span><h2>Tròn vị bữa cơm,<br>ấm lòng cả nhà.</h2><p>Món ngon mỗi ngày,<br>vừa khẩu vị nhà mình.</p><span class="hero-deco">${icon("leaf")}</span></div><div class="hero-photo"><img src="./assets/family-meal.webp" alt="Mâm cơm Việt với thịt kho trứng, canh cải và rau muống xào tỏi" fetchpriority="high"/><span class="hero-caption">${icon("heart")}Vị quen, thương nhiều</span></div></section><div class="content-columns"><section><div class="section-top"><div><h2>Thực đơn của nhà</h2><p>Mỗi ngày một vị, cả tuần ngon cơm</p></div><button class="text-button" data-action="nav" data-view="plan">Xem cả tuần ${icon("arrow")}</button></div>${daySelector()}<div class="meal-heading">${mealToggle()}<div class="meal-label"><span>${icon("people")}${state.profile.people} người</span><span>${icon("clock")}~${mealMinutes(plan())} phút</span></div></div><div class="dishes">${plan().map((id, i) => dishCard(id, i)).join("")}</div><div class="meal-summary"><span>${icon("wallet")} Chi phí ước tính <small>· 3 món / ${state.profile.people} người</small></span><strong>${currency(mealCost(plan(), state.profile.people))}</strong></div><div class="action-row">${button("Chuẩn bị đi chợ", "nav", "secondary", 'data-view="shopping"', "bag")}${button("Bắt đầu vào bếp", "nav", "primary", 'data-view="cook"', "pot")}</div>${assistantStrip()}</section><aside class="side-column">${timelinePanel()}<div class="tip-panel"><span class="tip-icon">${icon("light")}</span><div><strong>Mẹo nhỏ cho bữa cơm ngon</strong><p>${recipes[plan()[0]].tip}</p></div></div></aside></div>`;
}
function planView() {
  return `${heading("Tuần này, ăn ngon mỗi ngày.", "Một món mặn, một món canh, thêm rau xanh. Vậy là đủ đầy.", button("Gia đình", "nav", "secondary", 'data-view="settings"', "people"), "THỰC ĐƠN 7 NGÀY")}<div class="view-toolbar">${mealToggle()}<span class="small-copy">${dateLabel(days[0])} – ${dateLabel(days.at(-1))} · ${state.profile.people} người</span></div><div class="week-grid">${days.map(d => { const ids = plan(d); return `<article class="week-card ${d === state.date ? "selected" : ""}"><div class="week-card-head"><h3>${d === today ? "Hôm nay" : fullDate(d).split(",")[0]}</h3><span>${dateLabel(d)}</span></div>${ids.map(id => `<div class="week-food"><span class="food-emoji">${recipes[id].emoji}</span><div><strong>${recipes[id].name}</strong><small>${recipes[id].label} · ${recipes[id].minutes} phút</small></div><button class="icon-btn" style="margin-left:auto" data-action="recipe" data-id="${id}" aria-label="Xem công thức ${recipes[id].name}">${icon("book")}</button></div>`).join("")}<div class="week-card-bottom"><strong>~${currency(mealCost(ids, state.profile.people))}</strong><button class="text-button" data-action="open-day" data-date="${d}">Xem & đổi món ${icon("arrow")}</button></div></article>`; }).join("")}</div><div class="help-box">${icon("info")}<p>Chi phí là ước tính tham khảo cho 3 món, chưa tính gạo và gia vị có sẵn. Thực đơn và món đã đổi được lưu trên trình duyệt này.</p></div>`;
}
function shoppingView() {
  const ingredients = ingredientsFor(plan(), state.profile.people);
  const checked = state.checked[planKey()] ?? {};
  const count = ingredients.filter(i => checked[i.key]).length;
  const groups = [...new Set(ingredients.map(i => i.group))];
  return `${heading("Đi chợ một lần, đủ bữa ngon.", `${mealName()} ${selectedCaption()} · ${state.profile.people} người · ${plan().map(id => recipes[id].name).join(", ")}`, button("Chọn bữa", "nav", "secondary", 'data-view="home"', "calendar"), "DANH SÁCH ĐI CHỢ")}<div class="shopping-layout"><div>${groups.map(group => `<section class="shopping-group"><h3>${icon(group.startsWith("Rau") ? "leaf" : group.startsWith("Thịt") ? "bowl" : "bag")}${group}</h3>${ingredients.filter(i => i.group === group).map(i => `<label class="shopping-item ${checked[i.key] ? "checked" : ""}"><input type="checkbox" data-check="ingredient" data-key="${escape(i.key)}" ${checked[i.key] ? "checked" : ""}/><span class="item-name">${i.name}</span><span class="amount">${quantity(i.amount)} ${i.unit}</span></label>`).join("")}</section>`).join("")}</div><aside><section class="panel"><h3>${count === ingredients.length ? "Đã sẵn sàng vào bếp!" : "Giỏ đi chợ của bạn"}</h3><div class="progress-track"><div class="progress-fill" style="width:${count / ingredients.length * 100}%"></div></div><p class="small-copy">Đã chuẩn bị ${count}/${ingredients.length} nguyên liệu</p><div class="help-box" style="display:block"><strong>~${currency(mealCost(plan(), state.profile.people))}</strong><p>Chi phí tham khảo cho 3 món.<br>Đánh dấu cả nguyên liệu có sẵn ở nhà.</p></div>${button("Sao chép danh sách", "copy-shopping", "secondary full", "", "book")}<div style="height:10px"></div>${button("Đủ đồ rồi, vào bếp thôi", "nav", "primary full", 'data-view="cook"', "pot")}</section></aside></div>`;
}
function cookView() {
  const tasks = schedule();
  const done = state.done[planKey()] ?? {};
  return `${heading("Bếp đã sẵn, mình cùng nấu nhé!", `${mealName()} ${selectedCaption()} · ${state.profile.people} người · Lịch dự kiến cho bếp có 2 vùng nấu.`, button("Chỉnh giờ", "reminder", "secondary", "", "clock"), "NẤU THEO THỰC ĐƠN")}<div class="cook-summary"><div><h2>Dọn cơm lúc ${readyAt()}</h2><p>Bắt đầu từ ${timeLabel(tasks[0].at)} · Khoảng ${mealMinutes(plan())} phút<br>Đã xong ${tasks.filter(t => done[t.key]).length}/${tasks.length} công việc. Bạn có thể nấu sớm hoặc muộn hơn lịch dự kiến.</p></div>${button("Thêm vào lịch điện thoại", "calendar", "secondary", "", "calendar")}</div><div class="cook-timeline">${tasks.map(t => `<article class="cook-task"><time>${timeLabel(t.at)}</time><div class="cook-task-card ${done[t.key] ? "done" : ""}"><input class="task-check" type="checkbox" data-check="task" data-key="${t.key}" ${done[t.key] ? "checked" : ""} aria-label="Đã hoàn thành ${escape(t.title)}"/><h3>${escape(t.title)}</h3><p>${escape(t.detail)}</p><div class="task-actions">${t.recipe ? button("Xem từng bước", "recipe", "soft", `data-id="${t.recipe}"`, "book") : ""}${t.duration ? button(`Hẹn ${t.duration} phút`, "timer", "secondary", `data-minutes="${t.duration}" data-label="${escape(t.title)}"`, "clock") : ""}</div></div></article>`).join("")}</div>${state.timer ? timerMarkup() : ""}${assistantStrip()}`;
}
function timerMarkup() { return `<div class="timer-bar" role="region" aria-label="Đồng hồ nấu ăn"><span>${icon("clock")} ${escape(state.timer.label)}</span><strong id="timer-display">--:--</strong><button data-action="stop-timer" aria-label="Dừng hẹn giờ">${icon("close")}</button></div>`; }
function favoritesView() {
  return `${heading("Giữ lại những món nhà mình thích.", "Chạm vào trái tim trên mỗi món để lưu cho lần sau.", "", "MÓN ĐÃ LƯU")}${state.favorites.length ? `<div class="dishes" style="max-width:850px">${state.favorites.map(id => dishCard(id, 0, false)).join("")}</div>` : `<div class="empty-state">${icon("heart")}<h2>Món ngon đang chờ bạn lưu</h2><p>Ghé thực đơn và chọn những món cả nhà yêu thích.</p>${button("Xem thực đơn", "nav", "primary", 'data-view="home"', "calendar")}</div>`}`;
}
function settingsView() {
  return `${heading("Hiểu nhà mình, nấu vừa ý.", "Một vài thông tin giúp Bếp Nhà chuẩn bị bữa cơm phù hợp hơn.", "", "GIA ĐÌNH CỦA BẠN")}<div class="settings-grid"><section class="panel"><h2>Khẩu phần & giờ ăn</h2><form id="profile-form"><label class="field"><span>Tên gia đình</span><input name="name" value="${escape(state.profile.name)}" maxlength="35" required autocomplete="off"/></label><label class="field"><span>Nhà mình có mấy người ăn?</span><select name="people">${Array.from({ length: 12 }, (_, i) => `<option value="${i + 1}" ${state.profile.people === i + 1 ? "selected" : ""}>${i + 1} người</option>`).join("")}</select></label><div class="form-row"><label class="field"><span>Giờ ăn trưa</span><input name="lunch" type="time" value="${state.profile.lunch}" required/></label><label class="field"><span>Giờ ăn tối</span><input name="dinner" type="time" value="${state.profile.dinner}" required/></label></div>${button("Lưu thông tin gia đình", "submit-profile", "primary full", 'type="submit"', "check")}</form></section><section class="panel"><h2>Những điều cần biết</h2><p class="small-copy">Bếp Nhà đang có 14 công thức món Việt để bạn thử lên thực đơn và nấu theo từng bước. Khẩu phần nguyên liệu tự thay đổi theo số người ăn.</p><div class="help-box">${icon("bell")}<p>${isAndroid() ? "Android nhắc nấu và hẹn giờ bằng thông báo của ứng dụng. Hãy cấp quyền thông báo; quyền nhắc đúng giờ giúp hạn chế nhắc trễ. Dừng bắt buộc ứng dụng sẽ tắt lịch nhắc đến khi mở lại." : "Nhắc nấu trong ứng dụng hoạt động khi trang đang mở. Để được nhắc khi đóng trang, hãy thêm lịch nấu vào ứng dụng lịch trên điện thoại."}</p></div><p class="small-copy">Dữ liệu được lưu ${isAndroid() ? "trên thiết bị" : "trong trình duyệt"}, chưa đồng bộ giữa các thiết bị. Trợ lý hiện tìm món từ công thức có sẵn; chưa kết nối mô hình AI tạo sinh.</p><div style="height:20px"></div>${button("Cài đặt nhắc nấu", "reminder", "secondary full", "", "clock")}${isAndroid() ? nativePermissions() : ""}</section></div>`;
}

let toastTimeout;
function toast(message) { clearTimeout(toastTimeout); $("#toast").textContent = message; $("#toast").classList.add("show"); toastTimeout = setTimeout(() => $("#toast").classList.remove("show"), 5000); }
function go(view) { if (!pageNames[view]) return; state.view = view; render(); window.scrollTo({ top: 0, behavior: "instant" }); $("#main").focus({ preventScroll: true }); }
function openModal(title, body) {
  const modal = $("#modal");
  modal.innerHTML = `<div class="modal-head"><h2 id="modal-title">${title}</h2><button class="icon-btn" data-action="close" aria-label="Đóng">${icon("close")}</button></div><div class="modal-content">${body}</div>`;
  if (!modal.open) modal.showModal();
  modal.scrollTop = 0;
}
function closeModal() { $("#modal").close(); }
function showRecipe(id) {
  const r = recipes[id]; if (!r) return;
  openModal("Công thức nhà mình", `<div class="recipe-title"><span>${r.emoji}</span><div><h3>${r.name}</h3><p>${r.minutes} phút · ${state.profile.people} người · Dễ nấu</p></div></div><p class="modal-subtitle">${r.note}</p><h3 class="recipe-section">Chuẩn bị nguyên liệu</h3>${r.ingredients.map(i => `<div class="ingredient-row"><span>${i.name}</span><span>${quantity(i.amount * state.profile.people / 4)} ${i.unit}</span></div>`).join("")}<h3 class="recipe-section">Cách nấu từng bước</h3>${r.steps.map((step, i) => `<div class="recipe-step"><span class="step-number">${i + 1}</span><p>${step}</p></div>`).join("")}<div class="help-box">${icon("light")}<p>${r.tip}</p></div><p class="small-copy" style="margin-bottom:15px">Các khoảng thời gian là gợi ý. Kiểm tra độ chín thực tế trước khi dùng.</p>${button(`Hẹn giờ ${r.minutes} phút`, "timer", "primary full", `data-minutes="${r.minutes}" data-label="${r.name}"`, "clock")}`);
}
function showSwap(index) {
  const current = plan()[index]; if (!recipes[current]) return;
  const choices = Object.entries(recipes).filter(([id, r]) => r.kind === recipes[current].kind && id !== current);
  openModal("Đổi một món, thêm một vị", `<p class="modal-subtitle">Thay <strong>${recipes[current].name}</strong> bằng món nào? Danh sách đi chợ và lịch nấu sẽ tự cập nhật.</p>${choices.map(([id, r]) => `<button class="swap-option" data-action="choose-swap" data-id="${id}" data-index="${index}"><span>${r.emoji}</span><div><strong>${r.name}</strong><small>${r.minutes} phút · ~${currency(r.cost * state.profile.people / 4)} / ${state.profile.people} người</small></div>${icon("chevron")}</button>`).join("")}`);
}
function reminderModal() {
  const enabled = !!state.reminders[planKey()];
  openModal("Nhắc mình vào bếp", `<p class="modal-subtitle">${mealName()} ${selectedCaption()} · Bếp Nhà sắp lịch theo giờ cả nhà dùng cơm.</p><form id="reminder-form"><label class="field"><span>Cả nhà ăn lúc mấy giờ?</span><input type="time" name="time" value="${readyAt()}" required/></label><label class="checkbox-line"><input name="enabled" type="checkbox" ${enabled ? "checked" : ""}/>Nhắc khi đến giờ chuẩn bị bữa ăn</label>${!isAndroid() && "Notification" in window && Notification.permission !== "denied" ? `<label class="checkbox-line"><input name="notification" type="checkbox" ${Notification.permission === "granted" ? "checked" : ""}/>Hiện thông báo của trình duyệt nếu được hỗ trợ</label>` : ""}${isAndroid() ? nativePermissions() : `<div class="help-box">${icon("info")}<p>Nhắc trong Bếp Nhà cần trang đang mở. Để được nhắc khi đóng trang, bấm “Thêm vào lịch điện thoại” rồi nhập sự kiện vào lịch của bạn.</p></div>`}${button("Lưu giờ & nhắc nấu", "submit-reminder", "primary full", 'type="submit"', "check")}</form><div style="height:12px"></div>${button("Thêm vào lịch điện thoại", "calendar", "secondary full", "", "calendar")}`);
}
function nativePermissionContent() {
  const status = androidStatus();
  return `<p>${status.notifications ? "Thông báo Android đã bật." : "Cho phép thông báo để Bếp Nhà nhắc bạn khi ứng dụng đóng."}<br>${status.exactAlarms ? "Quyền nhắc đúng giờ đã bật." : "Android có thể nhắc trễ. Bạn có thể bật quyền “Nhắc đúng giờ” bên dưới."}</p>${!status.notifications ? button("Cho phép thông báo Android", "android-notifications", "secondary", "", "bell") : ""}${!status.exactAlarms ? button("Bật quyền nhắc đúng giờ", "android-exact", "secondary", "", "clock") : ""}`;
}
function nativePermissions() { return `<div class="native-permissions" data-native-permissions>${nativePermissionContent()}</div>`; }
function assistantModal() {
  openModal("Hỏi Bếp Nhà", `<p class="modal-subtitle">Gợi ý dựa trên thực đơn và 14 công thức có sẵn.</p><div id="chat-messages"><div class="assistant-message">Chào bạn! ${mealName()} nhà mình có ${plan().map(id => recipes[id].name.toLowerCase()).join(", ")}.
Bạn muốn xem cách nấu, tìm món nhanh hay đổi sang món tiết kiệm hơn?</div></div><div class="prompt-chips"><button data-action="ask" data-question="Hôm nay nấu theo thứ tự nào?">Nấu món nào trước?</button><button data-action="ask" data-question="Gợi ý món nấu nhanh">Món nhanh, dễ làm</button><button data-action="ask" data-question="Đổi món tiết kiệm hơn">Tiết kiệm hơn</button></div><form class="chat-form" id="chat-form"><label class="sr-only" for="chat-input">Bạn muốn hỏi gì?</label><input id="chat-input" name="question" class="chat-input" placeholder="Ví dụ: Cách nấu thịt kho trứng" maxlength="300" required autocomplete="off"/><button type="submit" class="button primary" aria-label="Gửi câu hỏi">${icon("send")}</button></form>`);
}
const normalize = text => text.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d");
function askAssistant(question) {
  const q = normalize(question.trim()); if (!q) return;
  const matches = Object.entries(recipes).filter(([, r]) => q.includes(normalize(r.name)));
  let answer = "", extra = "";
  if (matches.length) {
    const [id, r] = matches[0];
    answer = `${r.name} nấu khoảng ${r.minutes} phút cho ${state.profile.people} người.\n\n${r.steps.map((s, i) => `${i + 1}. ${s}`).join("\n\n")}\n\nMẹo nhỏ: ${r.tip}`;
    extra = button("Xem nguyên liệu", "recipe", "soft", `data-id="${id}"`, "book");
  } else if (/truoc|thu tu|lich|may gio|khi nao/.test(q)) {
    answer = `Để ăn lúc ${readyAt()}, bạn có thể bắt đầu từ ${timeLabel(schedule()[0].at)}:\n\n${schedule().map(t => `${timeLabel(t.at)} — ${t.title}`).join("\n")}\n\nLịch này giả định bạn có thể nấu song song trên 2 vùng bếp.`;
    extra = button("Mở lịch vào bếp", "chat-cook", "soft", "", "pot");
  } else if (/tiet kiem|re hon|ngan sach|chi phi/.test(q)) {
    answer = `Đậu hũ sốt cà chua (~${currency(recipes.tofu.cost * state.profile.people / 4)}) và trứng chiên hành (~${currency(recipes.eggs.cost * state.profile.people / 4)}) là hai gợi ý tiết kiệm cho ${state.profile.people} người. Chi phí là ước tính cho riêng món, thay đổi theo nơi mua.`;
    extra = button("Đổi món mặn", "swap", "soft", 'data-index="0"', "swap");
  } else if (/nhanh|ban ron|it thoi gian/.test(q)) {
    answer = "Bạn thử trứng chiên hành (10 phút), đậu hũ sốt cà chua (20 phút) hoặc tôm rim mặn ngọt (20 phút). Kết hợp canh bắp cải và rau củ luộc để chuẩn bị gọn hơn.";
    extra = button("Xem trứng chiên hành", "recipe", "soft", 'data-id="eggs"', "book");
  } else if (/doi|goi y|mon gi|an gi/.test(q)) {
    answer = `Mình có thể giúp bạn đổi món mặn trong ${mealName().toLowerCase()} ${selectedCaption()}, hoặc bạn chọn nút “Đổi món” bên cạnh món canh, món rau.`;
    extra = button("Chọn món thay thế", "swap", "soft", 'data-index="0"', "swap");
  } else if (/nguyen lieu|di cho|mua gi/.test(q)) {
    answer = `Bữa này cần ${ingredientsFor(plan(), state.profile.people).length} loại nguyên liệu, đã gộp những thứ dùng chung. Bạn xem ở danh sách đi chợ và đánh dấu đồ có sẵn nhé.`;
    extra = button("Mở danh sách đi chợ", "chat-shopping", "soft", "", "bag");
  } else {
    const keyword = q.split(/\s+/).filter(w => w.length > 2 && !["cach", "nau", "cho", "minh", "mon", "cong", "thuc"].includes(w));
    const found = Object.entries(recipes).filter(([, r]) => keyword.some(w => normalize(r.name).includes(w)));
    if (found.length) {
      answer = `Mình tìm thấy: ${found.map(([, r]) => `${r.name} (${r.minutes} phút)`).join(", ")}.`;
      extra = found.slice(0, 3).map(([id, r]) => button(r.name, "recipe", "soft", `data-id="${id}"`, "book")).join(" ");
    } else answer = "Mình chưa có câu trả lời phù hợp trong các công thức hiện tại. Bạn có thể hỏi tên món trong thực đơn, “nấu món nào trước”, “món nấu nhanh” hoặc “nguyên liệu đi chợ”.";
  }
  $("#chat-messages").insertAdjacentHTML("beforeend", `<div class="user-message">${escape(question)}</div><div class="assistant-message">${escape(answer)}${extra ? `<div style="margin-top:12px;display:flex;gap:8px;flex-wrap:wrap">${extra}</div>` : ""}</div>`);
  $("#chat-input").value = "";
  $("#chat-form").scrollIntoView({ block: "nearest", behavior: "smooth" });
}
function downloadFile(name, body, type) {
  const url = URL.createObjectURL(new Blob([body], { type }));
  const a = document.createElement("a"); a.href = url; a.download = name; a.click(); setTimeout(() => URL.revokeObjectURL(url), 10000);
}
function startTimer(minutes, label) {
  if (!Number.isFinite(minutes) || minutes < 1 || minutes > 180) return;
  if (state.timer && state.timer.end > Date.now()) {
    openModal("Đang có một hẹn giờ", `<p class="modal-subtitle">Đồng hồ “${escape(state.timer.label)}” vẫn đang chạy. Bạn có thể giữ đồng hồ hiện tại hoặc thay bằng hẹn giờ mới.</p>${button(`Thay bằng ${minutes} phút`, "replace-timer", "primary full", `data-minutes="${minutes}" data-label="${escape(label)}"`, "clock")}<div style="height:12px"></div>${button("Giữ hẹn giờ hiện tại", "close", "secondary full")}`);
    return;
  }
  setTimer(minutes, label);
}
function setTimer(minutes, label) {
  state.timer = { end: Date.now() + minutes * 60000, label }; persist(); closeModal(); go("cook");
  if (isAndroid()) {
    if (!androidStatus().notifications) requestAndroidNotifications();
    toast(androidStatus().exactAlarms ? `Đã hẹn ${minutes} phút trên Android.` : `Đã hẹn ${minutes} phút. Vào Nhắc nấu để bật quyền nhắc đúng giờ.`);
  } else toast(`Đã hẹn ${minutes} phút. Giữ trang mở để nghe nhắc.`);
}
function updateTimer() {
  if (!state.timer) return;
  const seconds = Math.max(0, Math.ceil((state.timer.end - Date.now()) / 1000));
  const display = $("#timer-display"); if (display) display.textContent = `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`;
  if (!seconds) {
    const label = state.timer.label; state.timer = null; persist();
    toast(`Hết giờ: ${label}. Hãy kiểm tra món ăn nhé!`);
    notify("Đã hết giờ nấu", `${label}: kiểm tra độ chín của món ăn nhé.`);
    try { const AudioContext = window.AudioContext || window.webkitAudioContext; const context = new AudioContext(); const oscillator = context.createOscillator(); const gain = context.createGain(); oscillator.connect(gain); gain.connect(context.destination); gain.gain.value = 0.12; oscillator.frequency.value = 660; oscillator.start(); oscillator.stop(context.currentTime + 0.5); oscillator.onended = () => context.close(); } catch { /* A visual notice remains available when sound is blocked. */ }
    if (state.view === "cook") render();
  }
}
function notify(title, body) { try { if (!isAndroid() && "Notification" in window && Notification.permission === "granted") new Notification(title, { body, icon: "./assets/favicon.svg" }); } catch { /* Some mobile browsers only support push via service workers. */ } }
function checkReminders() {
  if (isAndroid()) return; // AlarmManager owns Android reminders, including when the WebView is paused.
  const now = Date.now();
  for (const [key, enabled] of Object.entries(state.reminders)) {
    if (!enabled) continue;
    const [date, meal] = key.split("|"); if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || !["lunch", "dinner"].includes(meal)) continue;
    const start = scheduleFor(plan(date, meal), date, state.profile[meal])[0].at;
    if (now >= start && now < start + 10 * 60000 && state.notified[key] !== start) {
      state.notified[key] = start; persist();
      const message = `Đến giờ chuẩn bị ${meal === "lunch" ? "bữa trưa" : "bữa tối"}. Cả nhà ăn lúc ${state.profile[meal]} nhé!`;
      toast(message); notify("Bếp Nhà nhắc bạn", message);
    }
  }
}

document.addEventListener("click", async event => {
  const target = event.target.closest("[data-action]"); if (!target) return;
  const { action, id, view, index, date, meal, minutes, label, question } = target.dataset;
  if (action.startsWith("submit-")) return;
  event.preventDefault();
  switch (action) {
    case "nav": go(view); break;
    case "date": state.date = date; persist(); render(); break;
    case "meal": state.meal = meal; persist(); render(); break;
    case "open-day": state.date = date; persist(); go("home"); break;
    case "recipe": showRecipe(id); break;
    case "swap": showSwap(Number(index)); break;
    case "choose-swap": {
      const position = Number(index); if (![0, 1, 2].includes(position) || recipes[id]?.kind !== ["main", "soup", "veg"][position]) break;
      const ids = [...plan()]; ids[position] = id; state.plans[planKey()] = ids;
      delete state.done[planKey()]; persist(); closeModal(); render(); toast(`Đã đổi sang ${recipes[id].name.toLowerCase()}. Nguyên liệu và lịch nấu đã cập nhật.`); break;
    }
    case "favorite": state.favorites = state.favorites.includes(id) ? state.favorites.filter(x => x !== id) : [...state.favorites, id]; persist(); render(); toast(state.favorites.includes(id) ? "Đã lưu món vào góc yêu thích." : "Đã bỏ lưu món."); break;
    case "close": closeModal(); break;
    case "reminder": reminderModal(); break;
    case "toggle-reminder":
      if (state.reminders[planKey()]) { state.reminders[planKey()] = false; persist(); render(); toast("Đã tắt nhắc nấu cho bữa này."); } else reminderModal();
      break;
    case "calendar":
      if (androidCalendar(schedule())) break;
      downloadFile(`bep-nha-${state.date}-${state.meal}.ics`, calendarFor(plan(), state.date, readyAt()), "text/calendar;charset=utf-8"); toast("Đã tạo tệp lịch. Mở tệp và thêm sự kiện vào lịch điện thoại để nhận nhắc."); break;
    case "android-notifications": requestAndroidNotifications(); break;
    case "android-exact": requestAndroidExactAlarms(); break;
    case "assistant": assistantModal(); break;
    case "ask": askAssistant(question); break;
    case "chat-cook": closeModal(); go("cook"); break;
    case "chat-shopping": closeModal(); go("shopping"); break;
    case "timer": startTimer(Number(minutes), label); break;
    case "replace-timer": setTimer(Number(minutes), label); break;
    case "stop-timer": state.timer = null; persist(); render(); toast("Đã dừng hẹn giờ."); break;
    case "copy-shopping": {
      const text = `${mealName()} ${dateLabel(state.date)} — ${state.profile.people} người\n${ingredientsFor(plan(), state.profile.people).map(i => `□ ${i.name}: ${quantity(i.amount)} ${i.unit}`).join("\n")}`;
      if (androidCopy(text)) { toast("Đã sao chép danh sách đi chợ."); break; }
      try { await navigator.clipboard.writeText(text); toast("Đã sao chép danh sách đi chợ."); }
      catch { downloadFile("danh-sach-di-cho.txt", text, "text/plain;charset=utf-8"); toast("Đã tải danh sách đi chợ dạng chữ."); }
      break;
    }
  }
});
document.addEventListener("change", event => {
  const input = event.target;
  if (input.dataset.check === "ingredient") {
    state.checked[planKey()] ??= {}; state.checked[planKey()][input.dataset.key] = input.checked; persist(); render();
  } else if (input.dataset.check === "task") {
    state.done[planKey()] ??= {}; state.done[planKey()][input.dataset.key] = input.checked; persist(); render();
  }
});
document.addEventListener("submit", async event => {
  if (!["profile-form", "reminder-form", "chat-form"].includes(event.target.id)) return;
  event.preventDefault(); const data = new FormData(event.target);
  if (event.target.id === "profile-form") {
    const people = Number(data.get("people")); if (!Number.isInteger(people) || people < 1 || people > 12) return;
    state.profile = { name: String(data.get("name")).trim() || "Nhà mình", people, lunch: data.get("lunch"), dinner: data.get("dinner") };
    persist(); render(); toast("Đã lưu. Khẩu phần nguyên liệu và lịch nấu đã cập nhật.");
  } else if (event.target.id === "reminder-form") {
    state.profile[state.meal] = data.get("time"); state.reminders[planKey()] = data.has("enabled"); delete state.notified[planKey()];
    let permission = "";
    if (data.has("notification") && "Notification" in window) { try { permission = await Notification.requestPermission(); } catch { permission = "denied"; } }
    persist(); closeModal(); render();
    const past = schedule()[0].at < Date.now();
    if (isAndroid()) {
      if (state.reminders[planKey()] && !androidStatus().notifications) requestAndroidNotifications();
      toast(!state.reminders[planKey()] ? "Đã tắt nhắc nấu cho bữa này." : past ? "Giờ bắt đầu đã qua. Bạn hãy chỉnh giờ ăn nếu nấu muộn hơn." : `Đã lưu nhắc ${timeLabel(schedule()[0].at)} trên Android. Kiểm tra quyền thông báo và nhắc đúng giờ trong mục Nhắc nấu.`);
    } else toast(permission === "denied" ? "Trình duyệt chưa cho phép thông báo. Bạn vẫn được nhắc trong trang khi đang mở." : state.reminders[planKey()] ? past ? "Đã lưu. Giờ bắt đầu của bữa này đã qua; hãy chỉnh giờ ăn nếu bạn nấu muộn hơn." : `Đã bật nhắc lúc ${timeLabel(schedule()[0].at)} khi Bếp Nhà đang mở.` : "Đã lưu giờ ăn. Nhắc nấu đang tắt.");
    checkReminders();
  } else askAssistant(String(data.get("question")));
});
$("#modal").addEventListener("click", event => { if (event.target === $("#modal")) { const rect = event.target.getBoundingClientRect(); if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) closeModal(); } });
document.addEventListener("visibilitychange", () => { if (!document.hidden) { updateTimer(); checkReminders(); } });
window.addEventListener("bepnha:status", () => {
  document.querySelectorAll("[data-native-permissions]").forEach(element => { element.innerHTML = nativePermissionContent(); });
});
window.addEventListener("bepnha:open", event => {
  const context = event.detail ?? {};
  if (days.includes(context.date)) state.date = context.date;
  if (["lunch", "dinner"].includes(context.meal)) state.meal = context.meal;
  closeModal(); go("cook"); persist();
});
window.bepNhaBack = () => {
  if ($("#modal").open) { closeModal(); return true; }
  if (state.view !== "home") { go("home"); return true; }
  return false;
};
render();
persist();
setInterval(updateTimer, 1000);
setInterval(checkReminders, 15000);
checkReminders();
