// web/js/app.js
// Queueing Me Softly — single-page dispatcher console (vanilla ES modules, no frameworks).

/* =====================================================================================
 * Utilities
 * ===================================================================================== */
const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const pesoFmt = new Intl.NumberFormat('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const peso = (n) => (Number(n) < 0 ? '-₱' : '₱') + pesoFmt.format(Math.abs(Number(n) || 0));
const pesoShort = (n) => {
  const v = Math.abs(Number(n) || 0);
  const sign = Number(n) < 0 ? '-' : '';
  if (v >= 1000) return `${sign}₱${(v / 1000).toFixed(v >= 10000 ? 0 : 1)}k`;
  return `${sign}₱${Math.round(v)}`;
};
const int = (n) => new Intl.NumberFormat('en-PH').format(Number(n) || 0);
const todayISO = () => { const d = new Date(); d.setMinutes(d.getMinutes() - d.getTimezoneOffset()); return d.toISOString().slice(0, 10); };
const fmtDay = (iso, opts = { month: 'short', day: 'numeric' }) => new Date(iso + 'T00:00:00').toLocaleDateString('en-PH', opts);
const fmtTime = (ts) => {
  const d = new Date(String(ts).replace(' ', 'T'));
  return isNaN(d) ? ts : d.toLocaleTimeString('en-PH', { hour: 'numeric', minute: '2-digit' });
};
const initials = (name) => String(name).split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase();
const debounce = (fn, ms = 250) => { let t; return (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), ms); }; };
const store = {
  get(k, d) { try { const v = localStorage.getItem('qms.' + k); return v === null ? d : JSON.parse(v); } catch { return d; } },
  set(k, v) { try { localStorage.setItem('qms.' + k, JSON.stringify(v)); } catch { /* private mode: ignore */ } },
};

const ICON = {
  dashboard: '<path d="M4 13h6V4H4zM14 20h6v-9h-6zM4 20h6v-4H4zM14 8h6V4h-6z"/>',
  queue: '<path d="M8 6h12M8 12h12M8 18h12"/><circle cx="4" cy="6" r="1"/><circle cx="4" cy="12" r="1"/><circle cx="4" cy="18" r="1"/>',
  drivers: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c.8-3.6 3.4-5.5 6.5-5.5s5.7 1.9 6.5 5.5"/><path d="M16 4.6a3.5 3.5 0 0 1 0 6.8M18 14.8c1.9.7 3.1 2.4 3.5 5.2"/>',
  fares: '<path d="M20.6 13.4l-7.2 7.2a2 2 0 0 1-2.8 0L3 13V3h10l7.6 7.6a2 2 0 0 1 0 2.8z"/><circle cx="7.5" cy="7.5" r="1.5"/>',
  summary: '<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>',
  logs: '<path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><path d="M14 3v6h6M8 13h8M8 17h5"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/>',
  more: '<circle cx="5" cy="12" r="1.3"/><circle cx="12" cy="12" r="1.3"/><circle cx="19" cy="12" r="1.3"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  up: '<path d="M12 19V5M5 12l7-7 7 7"/>',
  top: '<path d="M5 4h14M12 20V9M6 14l6-6 6 6"/>',
  pause: '<path d="M9 5v14M15 5v14"/>',
  trash: '<path d="M4 7h16M10 11v6M14 11v6M5 7l1 13h12l1-13M9 7V4h6v3"/>',
  x: '<path d="M6 6l12 12M18 6L6 18"/>',
  send: '<path d="M22 2L11 13M22 2l-7 20-4-9-9-4z"/>',
  users: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c.8-3.6 3.4-5.5 6.5-5.5s5.7 1.9 6.5 5.5M16 11h6M19 8v6"/>',
  bolt: '<path d="M13 2L4 14h7l-1 8 9-12h-7z"/>',
  wallet: '<path d="M20 7H5a2 2 0 0 1 0-4h13v4M3 5v14a2 2 0 0 0 2 2h15V7"/><circle cx="16" cy="14" r="1.3"/>',
  trike: '<circle cx="6" cy="17" r="2.6"/><circle cx="18" cy="17" r="2.6"/><path d="M3.5 13.5h17l-2.6-6.5H9.6L7 13.5M12 7v6.5"/>',
  edit: '<path d="M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/>',
  clear: '<path d="M3 6h18M8 6V4h8v2M6 10l1 10h10l1-10"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="M20 20l-3.5-3.5"/>',
  download: '<path d="M12 3v12M7 10l5 5 5-5M4 21h16"/>',
  record: '<path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2M9 5a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2 2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2zM9 12h6M9 16h4"/>',
  peso: '<path d="M7 21V3h6a5 5 0 0 1 0 10H7M4 7h15M4 10h15"/>',
  route: '<circle cx="6" cy="19" r="2.5"/><circle cx="18" cy="5" r="2.5"/><path d="M8.5 19H17a3.5 3.5 0 0 0 0-7H7a3.5 3.5 0 0 1 0-7h8.5"/>',
};
const icon = (name) => `<svg viewBox="0 0 24 24" aria-hidden="true">${ICON[name] || ''}</svg>`;

const DRIVER_STATUS = {
  ACTIVE: { label: 'Active', pill: 'pill-green' },
  ON_BREAK: { label: 'On Break', pill: 'pill-amber' },
  MAINTENANCE: { label: 'Maintenance', pill: 'pill-red' },
  INACTIVE: { label: 'Inactive', pill: 'pill-gray' },
};
const QUEUE_STATUS = {
  IN_QUEUE: { label: 'In Queue', pill: 'pill-green' },
  BOARDING: { label: 'Boarding', pill: 'pill-blue' },
  SPECIAL_TRIP: { label: 'Special Trip', pill: 'pill-purple' },
};
const EXPENSE_CATS = [
  { id: 'FUEL', label: 'Gasoline/Fuel', emoji: '⛽' },
  { id: 'BOUNDARY', label: 'Boundary Fee', emoji: '🧾' },
  { id: 'TERMINAL', label: 'Terminal Fee', emoji: '🏁' },
  { id: 'MEALS', label: 'Meals/Misc', emoji: '🍚' },
];
const pill = (map, key) => { const s = map[key] || { label: key, pill: 'pill-gray' }; return `<span class="pill ${s.pill}">${esc(s.label)}</span>`; };

/* =====================================================================================
 * API + feedback
 * ===================================================================================== */
async function api(path, { method = 'GET', body } = {}) {
  let res;
  try {
    res = await fetch('/api' + path, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new Error('Cannot reach the terminal server. Is it still running?');
  }
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }
  if (!res.ok) {
    const err = new Error(data?.error?.message || `Request failed (${res.status}).`);
    err.code = data?.error?.code;
    err.status = res.status;
    throw err;
  }
  return data;
}

function toast(message, kind = 'ok') {
  const el = document.createElement('div');
  el.className = `toast ${kind}`;
  el.innerHTML = `<span class="ti">${kind === 'ok' ? '✓' : kind === 'err' ? '!' : 'i'}</span><span>${esc(message)}</span>`;
  $('#toasts').append(el);
  const ttl = kind === 'err' ? 5200 : 3000;
  setTimeout(() => { el.classList.add('out'); setTimeout(() => el.remove(), 240); }, ttl);
}

/** Runs an API action, shows a toast on failure, and refreshes the screen on success. */
async function act(fn, successMsg) {
  try {
    const result = await fn();
    if (successMsg) toast(typeof successMsg === 'function' ? successMsg(result) : successMsg);
    await refresh();
    return result;
  } catch (e) {
    toast(e.message, 'err');
    return undefined;
  }
}

function confirmDialog(title, text, okLabel = 'Confirm', danger = false) {
  return new Promise((resolve) => {
    const wrap = $('#modalWrap');
    $('#modalTitle').textContent = title;
    $('#modalText').textContent = text;
    const ok = $('#modalOk');
    ok.textContent = okLabel;
    ok.className = `btn ${danger ? 'btn-danger' : 'btn-primary'}`;
    wrap.hidden = false;
    ok.focus();
    const done = (v) => { wrap.hidden = true; ok.onclick = null; $('#modalCancel').onclick = null; document.removeEventListener('keydown', onKey); resolve(v); };
    const onKey = (e) => { if (e.key === 'Escape') done(false); };
    document.addEventListener('keydown', onKey);
    ok.onclick = () => done(true);
    $('#modalCancel').onclick = () => done(false);
  });
}

/* =====================================================================================
 * Drawer (slide-over on desktop, bottom sheet on mobile)
 * ===================================================================================== */
const drawer = { open: false, onClose: null, lastFocus: null };

function openDrawer({ eyebrow = '', title, body, foot = '', onMount, onClose }) {
  drawer.lastFocus = document.activeElement;
  $('#drawerEyebrow').textContent = eyebrow;
  $('#drawerTitle').textContent = title;
  $('#drawerBody').innerHTML = body;
  $('#drawerFoot').innerHTML = foot;
  $('#drawerBody').scrollTop = 0;
  const d = $('#drawer');
  const s = $('#scrim');
  d.hidden = false;
  s.hidden = false;
  requestAnimationFrame(() => { d.classList.add('show'); s.classList.add('show'); });
  drawer.open = true;
  drawer.onClose = onClose || null;
  if (onMount) onMount($('#drawerBody'), $('#drawerFoot'));
  setTimeout(() => { const f = $('#drawerBody input:not([type=hidden]), #drawerBody select, #drawerBody button'); f?.focus({ preventScroll: true }); }, 60);
}

function closeDrawer() {
  if (!drawer.open) return;
  const d = $('#drawer');
  const s = $('#scrim');
  d.classList.remove('show');
  s.classList.remove('show');
  drawer.open = false;
  setTimeout(() => { if (!drawer.open) { d.hidden = true; s.hidden = true; } }, 320);
  const cb = drawer.onClose;
  drawer.onClose = null;
  if (cb) cb();
  drawer.lastFocus?.focus?.({ preventScroll: true });
}

$('#drawerClose').addEventListener('click', closeDrawer);
$('#scrim').addEventListener('click', closeDrawer);
document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && drawer.open && $('#modalWrap').hidden) closeDrawer(); });

/** Wires a +/- stepper. Returns a getter/setter pair. */
function stepper(root, { min = 0, max = 99, value = 0, onChange }) {
  const out = $('output', root);
  const [dec, inc] = $$('button', root);
  let v = value;
  let lo = min;
  let hi = max;
  const render = () => { out.textContent = v; dec.disabled = v <= lo; inc.disabled = v >= hi; };
  dec.onclick = () => { if (v > lo) { v--; render(); onChange?.(v); } };
  inc.onclick = () => { if (v < hi) { v++; render(); onChange?.(v); } };
  render();
  return {
    get: () => v,
    set: (n) => { v = Math.max(lo, Math.min(hi, n)); render(); },
    bounds: (a, b) => { lo = a; hi = b; v = Math.max(lo, Math.min(hi, v)); render(); },
  };
}
const stepperHTML = (id, label) => `
  <div class="field"><span class="label" id="${id}-l">${label}</span>
    <div class="stepper" id="${id}" role="group" aria-labelledby="${id}-l">
      <button type="button" aria-label="Decrease">−</button><output class="num">0</output><button type="button" aria-label="Increase">+</button>
    </div></div>`;

function segmented(root, onChange) {
  $$('button', root).forEach((b) => b.addEventListener('click', () => {
    $$('button', root).forEach((x) => x.classList.toggle('on', x === b));
    onChange?.(b.dataset.v);
  }));
}

/* =====================================================================================
 * App state, theme, navigation
 * ===================================================================================== */
const state = {
  view: 'dashboard',
  dash: null,
  settings: null,
  drivers: [],
  destinations: [],
  driverFilter: store.get('driverFilter', 'ALL'),
  driverSearch: '',
  summaryRange: store.get('summaryRange', '7d'),
  summaryFrom: null,
  summaryTo: null,
  logs: { q: '', type: '', destination: '', date: '' },
};

const VIEWS = [
  { id: 'dashboard', label: 'Dashboard', icon: 'dashboard', tab: true },
  { id: 'queue', label: 'Queue', icon: 'queue', tab: true },
  { id: 'drivers', label: 'Drivers', icon: 'drivers', tab: true },
  { id: 'fares', label: 'Fare & Destination', short: 'Fares', icon: 'fares' },
  { id: 'summary', label: 'Trip Summary', short: 'Summary', icon: 'summary', tab: true },
  { id: 'logs', label: 'Records & Logs', short: 'Logs', icon: 'logs' },
  { id: 'settings', label: 'Settings', icon: 'settings' },
];

function buildNav() {
  $('#sideNav').innerHTML = VIEWS.map((v) => `<a class="nav-item" href="#/${v.id}" data-view="${v.id}">${icon(v.icon)}<span>${v.label}</span></a>`).join('');
  $('#tabBar').innerHTML = VIEWS.filter((v) => v.tab).map((v) => `<a class="tab" href="#/${v.id}" data-view="${v.id}">${icon(v.icon)}<span>${v.short || v.label}</span></a>`).join('')
    + `<button class="tab" id="tabMore" data-view="more">${icon('more')}<span>More</span></button>`;
  $('#tabMore').addEventListener('click', openMoreSheet);
}

function openMoreSheet() {
  const extra = VIEWS.filter((v) => !v.tab);
  openDrawer({
    eyebrow: 'Navigate',
    title: 'More',
    body: `<ul class="list">${extra.map((v) => `
      <li class="list-row"><a class="nav-item" style="flex:1" href="#/${v.id}">${icon(v.icon)}<span>${v.label}</span></a></li>`).join('')}</ul>`,
    onMount: (b) => $$('a', b).forEach((a) => a.addEventListener('click', closeDrawer)),
  });
}

function applyTheme(s) {
  if (!s) return;
  const root = document.documentElement;
  root.style.setProperty('--color-primary', s.colorPrimary);
  root.style.setProperty('--color-secondary', s.colorSecondary);
  root.style.setProperty('--color-accent', s.colorAccent);
  if (s.theme === 'light' || s.theme === 'dark') root.dataset.theme = s.theme; else delete root.dataset.theme;
  const dark = s.theme === 'dark' || (s.theme === 'system' && matchMedia('(prefers-color-scheme: dark)').matches);
  $('meta[name=theme-color]').setAttribute('content', dark ? '#000000' : '#F5F5F7');
  $('#brandTerminal').textContent = s.terminalName;
  document.title = `${s.terminalName} · Queueing Me Softly`;
  updateFavicon(s.colorPrimary, s.colorSecondary, s.colorAccent);
}

/** Blends two #RRGGBB colors; t = share of the second color (0..1). */
function mixHex(a, b, t) {
  const p = (h) => [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16));
  const [x, y] = [p(a), p(b)];
  if ([...x, ...y].some(Number.isNaN)) return a;
  return '#' + x.map((v, i) => Math.round(v + (y[i] - v) * t).toString(16).padStart(2, '0')).join('');
}

/** Redraws the browser-tab icon in the terminal's own colors, using the simplified tricycle mark. */
function updateFavicon(primary, secondary, accent) {
  const link = $('#favicon');
  const trike = document.getElementById('qms-trike');
  if (!link || !trike) return;
  const clip = document.getElementById('qmsWindshield').outerHTML;
  const body = trike.innerHTML.replaceAll('var(--logo-accent)', accent);
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024"><defs>
    <linearGradient id="t" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${primary}"/><stop offset="1" stop-color="${mixHex(primary, secondary, 0.3)}"/></linearGradient>${clip}</defs>
    <rect width="1024" height="1024" rx="230" fill="url(#t)"/>
    <g transform="translate(512 520) scale(1.3) translate(-533 -571)">${body}</g></svg>`;
  link.href = 'data:image/svg+xml,' + encodeURIComponent(svg);
}

function route() {
  const id = (location.hash.replace(/^#\/?/, '') || 'dashboard').split('?')[0];
  state.view = VIEWS.some((v) => v.id === id) ? id : 'dashboard';
  $$('[data-view]').forEach((a) => a.classList.toggle('active', a.dataset.view === state.view
    || (a.dataset.view === 'more' && !VIEWS.find((v) => v.id === state.view)?.tab)));
  const meta = VIEWS.find((v) => v.id === state.view);
  $('#pageTitle').textContent = meta.label;
  $('#chartTip').hidden = true;
  $('#view').innerHTML = '<div class="skeleton"></div>';
  $('#headActions').innerHTML = '';
  refresh(true);
  window.scrollTo({ top: 0 });
}

/** Re-fetches what the current view needs and re-renders it. */
let refreshing = null;
async function refresh(first = false) {
  if (refreshing) return refreshing;
  refreshing = (async () => {
    try {
      const view = state.view;
      state.dash = await api('/dashboard');
      state.settings = state.dash.settings;
      state.destinations = state.dash.destinations;
      applyTheme(state.settings);
      const renderer = RENDER[view];
      if (view !== state.view) return;
      await renderer(first);
    } catch (e) {
      if (first) $('#view').innerHTML = `<div class="card empty"><div class="em">📡</div><strong>Can't load the terminal</strong><p>${esc(e.message)}</p><button class="btn btn-primary" onclick="location.reload()">Retry</button></div>`;
      else toast(e.message, 'err');
    } finally {
      refreshing = null;
    }
  })();
  return refreshing;
}

/* =====================================================================================
 * Shared fragments
 * ===================================================================================== */
function seatDots(t, large = false) {
  const n = t.maxCapacity;
  let html = '';
  if (t.specialTrip) {
    for (let i = 0; i < n; i++) html += '<span class="seat special"></span>';
  } else {
    for (let i = 0; i < n; i++) {
      const cls = i < t.regularPax ? 'on' : i < t.regularPax + t.discountedPax ? 'disc' : '';
      html += `<span class="seat ${cls}"></span>`;
    }
  }
  const caption = t.specialTrip ? `Private · ${t.specialPartySize} pax` : `${t.currentPassengers} / ${t.maxCapacity} seats filled`;
  return `<span class="seats ${large ? 'seats-lg' : ''}" role="img" aria-label="${caption}">${html}</span>${large ? '' : `<span class="seat-caption">${t.specialTrip ? 'Private' : `${t.currentPassengers}/${t.maxCapacity}`}</span>`}`;
}

function queueActions(t, { compact = false } = {}) {
  const b = esc(t.bodyNumber);
  return `<div class="row-actions">
    ${t.currentPassengers > 0 ? `<button class="icon-btn" title="Dispatch ${b}" aria-label="Dispatch ${b}" data-act="dispatch" data-body="${b}" style="color:var(--color-primary)">${icon('send')}</button>` : ''}
    <button class="icon-btn" title="Move up" aria-label="Move ${b} up" data-act="up" data-body="${b}" ${t.position === 1 ? 'disabled' : ''}>${icon('up')}</button>
    ${compact ? '' : `<button class="icon-btn" title="Move to front" aria-label="Move ${b} to front" data-act="front" data-body="${b}" ${t.position === 1 ? 'disabled' : ''}>${icon('top')}</button>`}
    ${compact ? '' : `<button class="icon-btn" title="${t.specialTrip ? 'Cancel special trip' : 'Special trip'}" aria-label="Special trip for ${b}" data-act="special" data-body="${b}" style="${t.specialTrip ? 'color:var(--purple)' : ''}">${icon('bolt')}</button>`}
    ${!compact && t.currentPassengers > 0 ? `<button class="icon-btn" title="Unload passengers" aria-label="Unload ${b}" data-act="clear" data-body="${b}">${icon('clear')}</button>` : ''}
    <button class="icon-btn" title="Set On Break" aria-label="Set ${b} on break" data-act="break" data-body="${b}" data-driver="${esc(t.driverId)}">${icon('pause')}</button>
    <button class="icon-btn danger" title="Remove from queue" aria-label="Remove ${b}" data-act="remove" data-body="${b}">${icon('x')}</button>
  </div>`;
}

function bindQueueActions(root) {
  $$('[data-act]', root).forEach((btn) => btn.addEventListener('click', async () => {
    const body = btn.dataset.body;
    const unit = state.dash.queue.find((q) => q.bodyNumber === body);
    switch (btn.dataset.act) {
      case 'dispatch': return dispatchUnit(body);
      case 'up': return act(() => api(`/queue/${body}/move-up`, { method: 'POST' }), `${body} moved up`);
      case 'front': return act(() => api(`/queue/${body}/move-front`, { method: 'POST' }), `${body} moved to the front`);
      case 'special':
        if (unit?.specialTrip) return act(() => api(`/queue/${body}/special`, { method: 'POST', body: { enabled: false } }), `${body} is back on shared trips`);
        return openSpecialTrip(body);
      case 'clear':
        if (await confirmDialog(`Unload ${body}?`, 'Boarded passengers will be removed and the tricycle becomes empty again.', 'Unload', true)) {
          return act(() => api(`/queue/${body}/clear`, { method: 'POST' }), `${body} unloaded`);
        }
        return undefined;
      case 'break':
        return act(() => api(`/drivers/${btn.dataset.driver}/status`, { method: 'PUT', body: { status: 'ON_BREAK' } }), `${body} is on break and left the line`);
      case 'remove':
        if (await confirmDialog(`Remove ${body}?`, 'The tricycle leaves the dispatch line. The driver stays Active and can rejoin anytime.', 'Remove', true)) {
          return act(() => api(`/queue/${body}`, { method: 'DELETE' }), `${body} removed from the queue`);
        }
        return undefined;
      default: return undefined;
    }
  }));
}

async function dispatchUnit(bodyNo) {
  const unit = state.dash.queue.find((q) => q.bodyNumber === bodyNo) || state.dash.nowBoarding;
  if (!unit) return;
  if (!unit.specialTrip && unit.remainingSeats > 0) {
    const ok = await confirmDialog(`Dispatch ${unit.bodyNumber}?`, `${unit.remainingSeats} seat(s) are still empty. Dispatch to ${unit.destinationName} anyway for ${peso(unit.projectedFare)}?`, 'Dispatch');
    if (!ok) return;
  }
  await act(() => api('/dispatch', { method: 'POST', body: { bodyNumber: unit.bodyNumber } }),
    (r) => `${r.record.bodyNumber} dispatched to ${r.record.destination} · ${peso(r.record.totalFare)}`);
}

/* =====================================================================================
 * View: Dashboard
 * ===================================================================================== */
function renderDashboard() {
  const d = state.dash;
  const s = d.stats;
  $('#pageEyebrow').textContent = new Date().toLocaleDateString('en-PH', { weekday: 'long', month: 'long', day: 'numeric' });
  $('#headActions').innerHTML = `<button class="btn btn-primary" id="hdBoard">${icon('users')}Board Passengers</button>`;
  $('#hdBoard').onclick = openBoarding;

  const head = d.nowBoarding;
  const next = d.queue.slice(1, 6);
  $('#view').innerHTML = `
    <section class="grid stats" aria-label="Today at a glance">
      <div class="card stat" style="--tint:var(--color-primary)"><div class="stat-label"><span class="stat-ico">${icon('trike')}</span>Tricycles in Operation</div>
        <div class="stat-value">${s.tricyclesInOperation}<span class="faint" style="font-size:18px;font-weight:600"> / ${s.totalDrivers}</span></div>
        <div class="stat-foot">${s.statusCounts.ON_BREAK} on break · ${s.statusCounts.MAINTENANCE} in maintenance</div></div>
      <div class="card stat" style="--tint:var(--color-accent)"><div class="stat-label"><span class="stat-ico">${icon('queue')}</span>Currently in Queue</div>
        <div class="stat-value">${s.inQueue}</div>
        <div class="stat-foot">${d.queue.filter((q) => q.status === 'BOARDING').length} boarding now</div></div>
      <div class="card stat" style="--tint:var(--color-secondary)"><div class="stat-label"><span class="stat-ico">${icon('users')}</span>Passengers Served Today</div>
        <div class="stat-value">${int(s.passengersToday)}</div>
        <div class="stat-foot">${int(s.tripsToday)} trips dispatched</div></div>
      <div class="card stat" style="--tint:var(--amber)"><div class="stat-label"><span class="stat-ico">${icon('peso')}</span>Terminal Earnings Today</div>
        <div class="stat-value">${peso(s.grossToday)}</div>
        <div class="stat-foot">Net <strong class="${s.netToday < 0 ? 'neg' : 'pos'}">${peso(s.netToday)}</strong> after ${peso(s.expensesToday)} expenses</div></div>
    </section>

    <nav class="quick-actions" aria-label="Quick actions">
      <button class="qa" data-qa="add"><span class="qa-ico" style="background:var(--green-bg);color:var(--green)">${icon('plus')}</span>Add Tricycle</button>
      <button class="qa" data-qa="board"><span class="qa-ico" style="background:var(--blue-bg);color:var(--blue)">${icon('users')}</span>Board Passengers</button>
      <button class="qa" data-qa="special"><span class="qa-ico" style="background:var(--purple-bg);color:var(--purple)">⚡</span>Special Trip</button>
      <button class="qa" data-qa="expense"><span class="qa-ico" style="background:var(--amber-bg);color:var(--amber)">💸</span>Log Expense</button>
    </nav>

    <section class="grid split-2">
      ${head ? `
      <article class="card hero" aria-labelledby="nbTitle">
        <div class="hero-top">
          <div><p class="eyebrow" id="nbTitle">Now Boarding · #1</p></div>
          ${pill(QUEUE_STATUS, head.status)}
        </div>
        <div class="hero-body">
          <span class="hero-no">${esc(head.bodyNumber)}</span>
          <span class="hero-driver">${esc(head.driverName)}</span>
        </div>
        <div style="display:flex;align-items:center;gap:12px;margin-top:14px">${seatDots(head, true)}
          <span class="muted num">${head.specialTrip ? `Private hire · ${head.specialPartySize} passenger(s)` : `${head.currentPassengers} / ${head.maxCapacity} seats filled`}</span></div>
        <div class="hero-meta">
          <div class="meta-box"><div class="k">Destination</div><div class="v">${head.destinationName ? esc(head.destinationName) : '<span class="faint">Unassigned</span>'}</div></div>
          <div class="meta-box"><div class="k">Seats left</div><div class="v">${head.specialTrip ? '—' : head.remainingSeats}</div></div>
          <div class="meta-box"><div class="k">Fare if dispatched</div><div class="v">${peso(head.projectedFare)}</div></div>
        </div>
        <div class="hero-actions">
          <button class="btn btn-primary btn-lg" id="dispatchNow" ${head.currentPassengers === 0 ? 'disabled' : ''}>${icon('send')}Dispatch Now</button>
          <button class="btn btn-lg" id="heroBoard">${icon('users')}Board</button>
        </div>
        ${head.currentPassengers === 0 ? '<p class="hint" style="margin:10px 0 0">Board a party first. Dispatch unlocks once someone is aboard.</p>' : ''}
      </article>` : `
      <article class="card empty-hero">
        <div class="big">🛺</div>
        <h2 style="margin:8px 0 4px">The queue is empty</h2>
        <p class="muted">Add an active tricycle to start boarding passengers.</p>
        <button class="btn btn-primary" id="emptyAdd">${icon('plus')}Add Tricycle</button>
      </article>`}

      <article class="card">
        <div class="card-head"><div><h3>Next in Queue</h3><p class="card-sub">${d.queue.length ? `${d.queue.length - 1} waiting behind #1` : 'Nobody is in line'}</p></div>
          <a class="btn btn-plain" href="#/queue">See all</a></div>
        ${next.length ? `<ul class="list">${next.map((t) => `
          <li class="list-row">
            <span class="pos-badge">${t.position}</span>
            <div class="list-main">
              <div class="list-title"><span class="body-no">${esc(t.bodyNumber)}</span>${pill(QUEUE_STATUS, t.status)}</div>
              <div class="list-sub">${esc(t.driverName)} · ${t.destinationName ? esc(t.destinationName) : 'Any route'}</div>
            </div>
            <div class="list-end">${seatDots(t)}</div>
          </li>`).join('')}</ul>` : '<div class="empty"><div class="em">☕</div>No one else is waiting.</div>'}
      </article>
    </section>

    <section class="card">
      <div class="card-head"><div><h3>Recent Dispatches</h3><p class="card-sub">Written to today's CSV and TXT logs</p></div><a class="btn btn-plain" href="#/logs">Open logs</a></div>
      ${d.recentTrips.length ? `<div class="table-wrap"><table class="t">
        <thead><tr><th>Time</th><th>Body No.</th><th class="hide-sm">Driver</th><th>Destination</th><th class="hide-sm">Type</th><th class="r">Pax</th><th class="r">Fare</th></tr></thead>
        <tbody>${d.recentTrips.map((r) => `<tr>
          <td class="num">${fmtTime(r.timestamp)}</td><td class="body-no">${esc(r.bodyNumber)}</td><td class="hide-sm">${esc(r.driverName)}</td>
          <td>${esc(r.destination)}</td><td class="hide-sm">${r.tripType === 'SPECIAL' ? '<span class="pill pill-purple">Special</span>' : '<span class="pill pill-gray no-dot">Regular</span>'}</td>
          <td class="r">${r.totalPax}</td><td class="r">${peso(r.totalFare)}</td></tr>`).join('')}</tbody></table></div>`
        : '<div class="empty"><div class="em">🧾</div>No trips yet today. Dispatches will show up here.</div>'}
    </section>`;

  $$('[data-qa]').forEach((b) => b.addEventListener('click', () => ({
    add: openAddTricycle, board: openBoarding, special: () => openSpecialTrip(), expense: () => openExpense(),
  })[b.dataset.qa]()));
  $('#dispatchNow')?.addEventListener('click', () => dispatchUnit(head.bodyNumber));
  $('#heroBoard')?.addEventListener('click', openBoarding);
  $('#emptyAdd')?.addEventListener('click', openAddTricycle);
}

/* =====================================================================================
 * View: Queue
 * ===================================================================================== */
function renderQueue() {
  const d = state.dash;
  $('#pageEyebrow').textContent = `${d.queue.length} tricycle${d.queue.length === 1 ? '' : 's'} in line · FIFO${state.settings.autoRouteSorting ? ' + auto-route sorting' : ''}`;
  $('#headActions').innerHTML = `<button class="btn" id="qAdd">${icon('plus')}Add Tricycle</button><button class="btn btn-primary" id="qBoard">${icon('users')}Board</button>`;
  $('#qAdd').onclick = openAddTricycle;
  $('#qBoard').onclick = openBoarding;

  $('#view').innerHTML = `
    <section class="card">
      <div class="card-head"><div><h3>Live FIFO Queue</h3><p class="card-sub">First in, first out. Units leave the line automatically when set On Break or Maintenance.</p></div></div>
      ${d.queue.length ? `<div class="table-wrap hide-sm"><table class="t">
        <thead><tr><th>#</th><th>Body No.</th><th class="hide-sm">Driver</th><th>Status</th><th>Destination</th><th>Seats</th><th class="r hide-sm">Fare</th><th class="r">Actions</th></tr></thead>
        <tbody>${d.queue.map((t) => `<tr>
          <td><span class="pos-badge">${t.position}</span></td>
          <td class="body-no">${esc(t.bodyNumber)}</td>
          <td class="hide-sm">${esc(t.driverName)}</td>
          <td>${pill(QUEUE_STATUS, t.status)}</td>
          <td>${t.destinationName ? esc(t.destinationName) : '<span class="faint">Any route</span>'}</td>
          <td>${seatDots(t)}</td>
          <td class="r hide-sm">${t.projectedFare ? peso(t.projectedFare) : '<span class="faint">—</span>'}</td>
          <td>${queueActions(t)}</td></tr>`).join('')}</tbody></table></div>
        <ul class="list show-sm">${d.queue.map((t) => `
          <li class="list-row" style="flex-wrap:wrap">
            <span class="pos-badge">${t.position}</span>
            <div class="list-main">
              <div class="list-title"><span class="body-no">${esc(t.bodyNumber)}</span>${pill(QUEUE_STATUS, t.status)}</div>
              <div class="list-sub">${esc(t.driverName)} · ${t.destinationName ? esc(t.destinationName) : 'Any route'}${t.projectedFare ? ` · ${peso(t.projectedFare)}` : ''}</div>
            </div>
            <div class="list-end">${seatDots(t)}</div>
            <div style="flex-basis:100%">${queueActions(t)}</div>
          </li>`).join('')}</ul>`
        : `<div class="empty"><div class="em">🛺</div><strong>No tricycles in line</strong><p>Add an active driver's tricycle to start the queue.</p></div>`}
    </section>
    ${d.availableDrivers.length ? `<section class="card">
      <div class="card-head"><div><h3>Ready to Join</h3><p class="card-sub">Active drivers who are not in line yet</p></div></div>
      <div class="chips">${d.availableDrivers.map((dr) => `<button class="chip" data-join="${esc(dr.bodyNumber)}">+ ${esc(dr.bodyNumber)} · ${esc(dr.name.split(' ')[0])}</button>`).join('')}</div>
    </section>` : ''}`;
  bindQueueActions($('#view'));
  $$('[data-join]').forEach((b) => b.addEventListener('click', () => act(() => api('/queue', { method: 'POST', body: { bodyNumber: b.dataset.join } }), `${b.dataset.join} joined the queue`)));
}

/* =====================================================================================
 * View: Drivers
 * ===================================================================================== */
async function renderDrivers() {
  state.drivers = await api('/drivers');
  const counts = { ALL: state.drivers.length };
  Object.keys(DRIVER_STATUS).forEach((k) => { counts[k] = state.drivers.filter((x) => x.status === k).length; });
  $('#pageEyebrow').textContent = `${counts.ACTIVE} active · ${state.drivers.length} registered`;
  $('#headActions').innerHTML = `<button class="btn btn-primary" id="dAdd">${icon('plus')}Register Driver</button>`;
  $('#dAdd').onclick = () => openDriverForm();

  const queued = new Set(state.dash.queue.map((q) => q.bodyNumber));
  const needle = state.driverSearch.toLowerCase();
  const list = state.drivers.filter((x) => (state.driverFilter === 'ALL' || x.status === state.driverFilter)
    && (!needle || `${x.name} ${x.bodyNumber} ${x.licenseNumber} ${x.contactNumber}`.toLowerCase().includes(needle)));

  $('#view').innerHTML = `
    <div class="toolbar">
      <div class="search grow">${icon('search')}<input class="input" id="dSearch" type="search" placeholder="Search name, body no. or license" value="${esc(state.driverSearch)}" aria-label="Search drivers"></div>
      <div class="segmented" id="dFilter">${['ALL', ...Object.keys(DRIVER_STATUS)].map((k) => `<button data-v="${k}" class="${state.driverFilter === k ? 'on' : ''}">${k === 'ALL' ? 'All' : DRIVER_STATUS[k].label} <span class="faint num">${counts[k]}</span></button>`).join('')}</div>
    </div>
    ${list.length ? `<section class="driver-grid">${list.map((x) => `
      <article class="card driver-card">
        <div class="driver-top">
          <div class="avatar">${esc(initials(x.name))}</div>
          <div class="driver-id">
            <div class="driver-name" title="${esc(x.name)}">${esc(x.name)}</div>
            <div class="list-sub"><span class="body-no">${esc(x.bodyNumber)}</span> · ${esc(x.licenseNumber)}</div>
          </div>
        </div>
        <div class="driver-status">
          <select class="status-select s-${x.status}" data-status="${esc(x.id)}" aria-label="Status for ${esc(x.name)}">
            ${Object.entries(DRIVER_STATUS).map(([k, v]) => `<option value="${k}" ${x.status === k ? 'selected' : ''}>${v.label}</option>`).join('')}
          </select>
          ${queued.has(x.bodyNumber) ? `<span class="pill pill-blue">In queue #${state.dash.queue.find((q) => q.bodyNumber === x.bodyNumber).position}</span>` : ''}
        </div>
        <div class="driver-money">
          <div><div class="k">Gross</div><div class="v">${peso(x.grossEarnings)}</div></div>
          <div><div class="k">Expenses</div><div class="v">${peso(x.totalExpenses)}</div></div>
          <div><div class="k">Net Profit</div><div class="v ${x.netProfit < 0 ? 'neg' : 'pos'}">${peso(x.netProfit)}</div></div>
        </div>
        <div class="muted" style="font-size:13px">${int(x.tripsCompleted)} trips · ${int(x.passengersServed)} passengers${x.contactNumber ? ` · ${esc(x.contactNumber)}` : ''}</div>
        <div class="driver-actions">
          <button class="btn" data-record="${esc(x.id)}">${icon('record')}Record</button>
          <button class="btn" data-expense="${esc(x.id)}">💸 Expense</button>
          ${x.status === 'ACTIVE' && !queued.has(x.bodyNumber) ? `<button class="btn btn-plain" data-join="${esc(x.bodyNumber)}">${icon('plus')}Queue</button>` : `<button class="btn" data-edit="${esc(x.id)}">${icon('edit')}Edit</button>`}
        </div>
      </article>`).join('')}</section>`
      : `<div class="card empty"><div class="em">🔎</div>No drivers match this filter.</div>`}`;

  $('#dSearch').addEventListener('input', debounce((e) => { state.driverSearch = e.target.value; renderDrivers().then(() => { const i = $('#dSearch'); i.focus(); i.setSelectionRange(i.value.length, i.value.length); }); }, 200));
  segmented($('#dFilter'), (v) => { state.driverFilter = v; store.set('driverFilter', v); renderDrivers(); });
  $$('[data-status]').forEach((sel) => sel.addEventListener('change', async () => {
    const prev = state.drivers.find((x) => x.id === sel.dataset.status)?.status;
    const r = await act(() => api(`/drivers/${sel.dataset.status}/status`, { method: 'PUT', body: { status: sel.value } }),
      (res) => `${res.name} is now ${res.statusLabel}${res.removedFromQueue ? ' and left the queue' : ''}`);
    if (!r) sel.value = prev;
  }));
  $$('[data-record]').forEach((b) => b.addEventListener('click', () => openDriverRecord(b.dataset.record)));
  $$('[data-expense]').forEach((b) => b.addEventListener('click', () => openExpense(b.dataset.expense)));
  $$('[data-edit]').forEach((b) => b.addEventListener('click', () => openDriverForm(state.drivers.find((x) => x.id === b.dataset.edit))));
  $$('[data-join]').forEach((b) => b.addEventListener('click', () => act(() => api('/queue', { method: 'POST', body: { bodyNumber: b.dataset.join } }), `${b.dataset.join} joined the queue`)));
}

/* =====================================================================================
 * View: Fare & Destination
 * ===================================================================================== */
function renderFares() {
  const s = state.settings;
  const dests = state.destinations;
  $('#pageEyebrow').textContent = `${dests.filter((x) => x.active).length} active routes`;
  $('#headActions').innerHTML = `<button class="btn btn-primary" id="fAdd">${icon('plus')}Add Route</button>`;
  $('#fAdd').onclick = () => openDestinationForm();
  const disc = (f) => f * (1 - s.discountPercent / 100);

  $('#view').innerHTML = `
    <section class="grid split-2">
      <article class="card">
        <div class="card-head"><div><h3>Terminal Routes</h3><p class="card-sub">Base fare is per passenger. Discounted fare applies to Senior Citizen, Student and PWD.</p></div></div>
        ${dests.length ? `<div class="table-wrap"><table class="t">
          <thead><tr><th>Destination</th><th class="r">Base fare</th><th class="r hide-sm">Discounted</th><th class="r hide-sm">Special</th><th>Active</th><th class="r">Actions</th></tr></thead>
          <tbody>${dests.map((x) => `<tr style="${x.active ? '' : 'opacity:.55'}">
            <td><strong>${esc(x.name)}</strong></td>
            <td class="r">${peso(x.baseFare)}</td>
            <td class="r hide-sm">${peso(disc(x.baseFare))}</td>
            <td class="r hide-sm">${peso(s.specialMode === 'FLAT' ? s.specialFlatRate : x.baseFare * s.specialMultiplier)}</td>
            <td><label class="switch" aria-label="Toggle ${esc(x.name)}"><input type="checkbox" data-toggle="${esc(x.id)}" ${x.active ? 'checked' : ''}><span class="track"></span></label></td>
            <td><div class="row-actions">
              <button class="icon-btn" aria-label="Edit ${esc(x.name)}" data-edit="${esc(x.id)}">${icon('edit')}</button>
              <button class="icon-btn danger" aria-label="Delete ${esc(x.name)}" data-del="${esc(x.id)}">${icon('trash')}</button>
            </div></td></tr>`).join('')}</tbody></table></div>`
          : '<div class="empty"><div class="em">🗺️</div>No routes yet. Add your first destination.</div>'}
      </article>

      <article class="card">
        <div class="card-head"><div><h3>Statutory Discount</h3><p class="card-sub">Senior Citizen · Student · PWD</p></div><span class="pill pill-green no-dot num">${s.discountPercent}% off</span></div>
        <div class="field"><label for="discPct">Discount percentage</label>
          <div class="input-prefix"><span>%</span><input class="input" id="discPct" type="number" min="0" max="100" step="1" value="${s.discountPercent}"></div>
          <span class="hint">Default is 20% under RA 9994 (Seniors), RA 10754 (PWD) and RA 11314 (Students).</span></div>
        <button class="btn btn-primary btn-block" id="saveDisc" style="margin-top:16px">Save Discount</button>

        <h4 class="section-title">Special Trip Rate</h4>
        <dl class="kv">
          <dt>Pricing mode</dt><dd>${s.specialMode === 'FLAT' ? 'Flat rate' : 'Base fare × multiplier'}</dd>
          <dt>${s.specialMode === 'FLAT' ? 'Flat rate' : 'Multiplier'}</dt><dd>${s.specialMode === 'FLAT' ? peso(s.specialFlatRate) : `${s.specialMultiplier}×`}</dd>
        </dl>
        <a class="btn btn-plain" href="#/settings" style="margin-top:8px;padding:0">Change in Settings</a>

        <h4 class="section-title">Mixed-Party Example</h4>
        ${dests[0] ? `<div class="receipt" style="margin-top:0">
          <h4>${esc(dests[0].name)} · 2 regular + 1 student</h4>
          <div class="rline"><span>Regular <span class="q">× 2 @ ${peso(dests[0].baseFare)}</span></span><span>${peso(dests[0].baseFare * 2)}</span></div>
          <div class="rline"><span>Student <span class="q">× 1 @ ${peso(disc(dests[0].baseFare))}</span></span><span>${peso(disc(dests[0].baseFare))}</span></div>
          <div class="rtotal"><span>Total</span><span>${peso(dests[0].baseFare * 2 + disc(dests[0].baseFare))}</span></div>
        </div>` : ''}
      </article>
    </section>`;

  $$('[data-toggle]').forEach((c) => c.addEventListener('change', async () => {
    const r = await act(() => api(`/destinations/${c.dataset.toggle}`, { method: 'PUT', body: { active: c.checked } }), (x) => `${x.name} ${x.active ? 'activated' : 'deactivated'}`);
    if (!r) c.checked = !c.checked;
  }));
  $$('[data-edit]').forEach((b) => b.addEventListener('click', () => openDestinationForm(dests.find((x) => x.id === b.dataset.edit))));
  $$('[data-del]').forEach((b) => b.addEventListener('click', async () => {
    const dest = dests.find((x) => x.id === b.dataset.del);
    if (await confirmDialog(`Delete ${dest.name}?`, 'Past trips in the logs are kept. This route will no longer be offered for boarding.', 'Delete', true)) {
      act(() => api(`/destinations/${dest.id}`, { method: 'DELETE' }), `${dest.name} deleted`);
    }
  }));
  $('#saveDisc').onclick = () => act(() => api('/settings', { method: 'PUT', body: { discountPercent: Number($('#discPct').value) } }), (r) => `Discount set to ${r.discountPercent}%`);
}

/* =====================================================================================
 * View: Trip Summary (analytics)
 * ===================================================================================== */
async function renderSummary() {
  const r = state.summaryRange;
  if (r === 'custom' && (!state.summaryFrom || !state.summaryTo)) {
    const t = new Date();
    state.summaryTo = todayISO();
    t.setDate(t.getDate() - 13);
    t.setMinutes(t.getMinutes() - t.getTimezoneOffset());
    state.summaryFrom = t.toISOString().slice(0, 10);
  }
  const q = r === 'custom' ? `range=custom&from=${state.summaryFrom}&to=${state.summaryTo}` : `range=${r}`;
  let a;
  try {
    a = await api('/analytics?' + q);
  } catch (e) {
    toast(e.message, 'err');
    state.summaryRange = '7d';
    return renderSummary();
  }
  const t = a.totals;
  const ins = a.insights;
  $('#pageEyebrow').textContent = a.from === a.to ? fmtDay(a.from, { weekday: 'long', month: 'long', day: 'numeric' }) : `${fmtDay(a.from)} – ${fmtDay(a.to)}`;
  $('#headActions').innerHTML = `<button class="btn" id="sExpense">💸 Log Expense</button>`;
  $('#sExpense').onclick = () => openExpense();

  $('#view').innerHTML = `
    <div class="toolbar">
      <div class="segmented" id="sRange">
        <button data-v="today" class="${r === 'today' ? 'on' : ''}">Today</button>
        <button data-v="7d" class="${r === '7d' ? 'on' : ''}">Last 7 Days</button>
        <button data-v="30d" class="${r === '30d' ? 'on' : ''}">Last 30 Days</button>
        <button data-v="custom" class="${r === 'custom' ? 'on' : ''}">Custom Range</button>
      </div>
      ${r === 'custom' ? `<input class="input" style="width:auto" type="date" id="sFrom" value="${a.from}" aria-label="From date">
        <span class="muted">to</span><input class="input" style="width:auto" type="date" id="sTo" value="${a.to}" aria-label="To date">` : ''}
    </div>

    <section class="grid stats">
      <div class="card stat"><div class="stat-label">Gross Earnings</div><div class="stat-value">${peso(t.gross)}</div><div class="stat-foot">${int(t.trips)} trips · ${int(t.passengers)} passengers</div></div>
      <div class="card stat"><div class="stat-label">Total Expenses</div><div class="stat-value">${peso(t.expenses)}</div><div class="stat-foot">Fuel, boundary, terminal & meals</div></div>
      <div class="card stat"><div class="stat-label">Net Profit</div><div class="stat-value ${t.net < 0 ? 'neg' : 'pos'}">${peso(t.net)}</div><div class="stat-foot">Gross − Expenses</div></div>
      <div class="card stat"><div class="stat-label">Average Fare per Trip</div><div class="stat-value">${peso(ins.averageFarePerTrip)}</div><div class="stat-foot">${ins.averagePassengersPerTrip} passengers per trip</div></div>
    </section>

    <section class="grid split-2">
      <article class="card">
        <div class="card-head"><div><h3>Daily Gross vs. Net Earnings</h3><p class="card-sub">Hover or tap a day for details</p></div>
          <div class="legend"><span><i style="background:var(--series-gross)"></i>Gross</span><span><i style="background:var(--series-net)"></i>Net</span></div></div>
        <div class="chart" id="chart"></div>
      </article>
      <article class="card">
        <div class="card-head"><h3>Quick Insights</h3></div>
        <div class="insights">
          <div class="insight"><div class="k">Highest Earning Day</div><div class="v">${ins.highestEarningDay ? peso(ins.highestEarningDay.gross) : '—'}</div><div class="s">${ins.highestEarningDay ? fmtDay(ins.highestEarningDay.date, { weekday: 'short', month: 'short', day: 'numeric' }) : 'No trips yet'}</div></div>
          <div class="insight"><div class="k">Most Trips in a Day</div><div class="v">${ins.mostTripsDay ? int(ins.mostTripsDay.trips) : '—'}</div><div class="s">${ins.mostTripsDay ? fmtDay(ins.mostTripsDay.date, { weekday: 'short', month: 'short', day: 'numeric' }) : 'No trips yet'}</div></div>
          <div class="insight"><div class="k">Average Fare per Trip</div><div class="v">${peso(ins.averageFarePerTrip)}</div><div class="s">${int(t.trips)} trips</div></div>
          <div class="insight"><div class="k">Dispatch Efficiency</div><div class="v">${ins.dispatchEfficiency}%</div><div class="s">Seats filled on regular trips</div>
            <div class="meter" style="margin-top:8px"><i style="width:${Math.min(100, ins.dispatchEfficiency)}%"></i></div></div>
        </div>
        ${a.expenseCategories.length ? `<h4 class="section-title">Expenses by Category</h4>
          <dl class="kv">${a.expenseCategories.map((c) => `<dt>${esc(c.category)}</dt><dd>${peso(c.amount)}</dd>`).join('')}</dl>` : ''}
      </article>
    </section>

    <section class="card">
      <div class="card-head"><div><h3>Driver Net Profit</h3><p class="card-sub">Net Profit = Gross Earnings − Total Expenses, for the selected range</p></div></div>
      <div class="table-wrap"><table class="t">
        <thead><tr><th>Driver</th><th>Body No.</th><th class="r">Trips</th><th class="r hide-sm">Pax</th><th class="r">Gross</th><th class="r">Expenses</th><th class="r">Net Profit</th></tr></thead>
        <tbody>${a.drivers.map((x) => `<tr data-open="${esc(x.driverId)}" style="cursor:pointer">
          <td><strong>${esc(x.name)}</strong></td><td class="body-no">${esc(x.bodyNumber)}</td><td class="r">${x.trips}</td><td class="r hide-sm">${x.passengers}</td>
          <td class="r">${peso(x.gross)}</td><td class="r">${peso(x.expenses)}</td><td class="r ${x.net < 0 ? 'neg' : 'pos'}"><strong>${peso(x.net)}</strong></td></tr>`).join('')}</tbody>
        <tfoot><tr><td colspan="2">Terminal total</td><td class="r">${int(t.trips)}</td><td class="r hide-sm">${int(t.passengers)}</td><td class="r">${peso(t.gross)}</td><td class="r">${peso(t.expenses)}</td><td class="r ${t.net < 0 ? 'neg' : 'pos'}">${peso(t.net)}</td></tr></tfoot>
      </table></div>
    </section>

    <section class="grid split-even">
      <article class="card">
        <div class="card-head"><h3>Trips by Destination</h3></div>
        ${a.destinations.length ? `<ul class="list">${a.destinations.map((x) => {
          const pct = t.gross ? (x.gross / t.gross) * 100 : 0;
          return `<li class="list-row" style="flex-wrap:wrap;gap:8px 14px"><div class="list-main"><div class="list-title">${esc(x.destination)}</div><div class="list-sub">${x.trips} trips · ${x.passengers} pax</div></div>
            <div class="list-end">${peso(x.gross)}</div><div class="meter" style="flex-basis:100%"><i style="width:${pct.toFixed(1)}%"></i></div></li>`;
        }).join('')}</ul>` : '<div class="empty">No trips in this range.</div>'}
      </article>
      <article class="card">
        <div class="card-head"><h3>Daily Table</h3></div>
        <div class="table-wrap" style="max-height:340px;overflow-y:auto"><table class="t">
          <thead><tr><th>Date</th><th class="r">Trips</th><th class="r">Gross</th><th class="r">Expenses</th><th class="r">Net</th></tr></thead>
          <tbody>${[...a.daily].reverse().map((x) => `<tr><td class="num">${fmtDay(x.date, { weekday: 'short', month: 'short', day: 'numeric' })}</td><td class="r">${x.trips}</td><td class="r">${peso(x.gross)}</td><td class="r">${peso(x.expenses)}</td><td class="r ${x.net < 0 ? 'neg' : ''}">${peso(x.net)}</td></tr>`).join('')}</tbody>
        </table></div>
      </article>
    </section>`;

  drawChart($('#chart'), a.daily);
  segmented($('#sRange'), (v) => { state.summaryRange = v; store.set('summaryRange', v); renderSummary(); });
  const onDate = () => {
    const f = $('#sFrom').value;
    const to = $('#sTo').value;
    if (f && to) { state.summaryFrom = f; state.summaryTo = to; renderSummary(); }
  };
  $('#sFrom')?.addEventListener('change', onDate);
  $('#sTo')?.addEventListener('change', onDate);
  $$('[data-open]').forEach((tr) => tr.addEventListener('click', () => openDriverRecord(tr.dataset.open)));
}

/** Rounded grouped bar chart (gross vs net) drawn as inline SVG with a hover tooltip. */
function drawChart(el, daily) {
  const W = Math.max(320, el.clientWidth || 640);
  const H = el.clientHeight || 280;
  const pad = { l: 52, r: 8, t: 12, b: 28 };
  const iw = W - pad.l - pad.r;
  const ih = H - pad.t - pad.b;
  const maxV = Math.max(1, ...daily.map((d) => Math.max(d.gross, d.net)));
  const minV = Math.min(0, ...daily.map((d) => d.net));
  // "Nice" tick step (1, 2, 2.5 or 5 × 10^n) so axis labels are round peso amounts.
  const ticks = 4;
  const rawStep = (maxV - minV) / ticks;
  const mag = 10 ** Math.floor(Math.log10(rawStep));
  const step = [1, 2, 2.5, 5, 10].map((m) => m * mag).find((s) => s >= rawStep);
  const top = Math.ceil(maxV / step) * step;
  const bottom = Math.floor(minV / step) * step;
  const y = (v) => pad.t + ih * (1 - (v - bottom) / (top - bottom));
  const n = daily.length;
  const slot = iw / n;
  const barW = Math.max(3, Math.min(22, (slot - 8) / 2));
  const gap = 2;
  const tickVals = [];
  for (let v = bottom; v <= top + step / 2; v += step) tickVals.push(Math.round(v * 100) / 100);
  const labelEvery = Math.ceil(n / Math.max(1, Math.floor(iw / 62)));
  // Bars are anchored to the zero baseline with only the data end rounded (4px).
  const bar = (x, v, cls) => {
    const y0 = y(0);
    const y1 = y(v);
    const h = Math.abs(y0 - y1);
    if (h < 0.5) return '';
    const r = Math.min(4, h, barW / 2);
    if (v >= 0) {
      return `<path class="${cls}" d="M${x},${y0} V${y1 + r} Q${x},${y1} ${x + r},${y1} H${x + barW - r} Q${x + barW},${y1} ${x + barW},${y1 + r} V${y0} Z"/>`;
    }
    return `<path class="${cls}" d="M${x},${y0} V${y1 - r} Q${x},${y1} ${x + r},${y1} H${x + barW - r} Q${x + barW},${y1} ${x + barW},${y1 - r} V${y0} Z"/>`;
  };
  let svg = `<svg viewBox="0 0 ${W} ${H}" role="img" aria-label="Daily gross and net earnings">`;
  tickVals.forEach((v) => {
    svg += `<line class="grid-line" x1="${pad.l}" x2="${W - pad.r}" y1="${y(v)}" y2="${y(v)}" ${v === 0 ? '' : 'stroke-dasharray="2 4"'}/>`;
    svg += `<text class="axis-text" x="${pad.l - 8}" y="${y(v) + 4}" text-anchor="end">${pesoShort(v)}</text>`;
  });
  daily.forEach((d, i) => {
    const cx = pad.l + slot * i + slot / 2;
    const x1 = cx - barW - gap / 2;
    const x2 = cx + gap / 2;
    svg += bar(x1, d.gross, 'bar-gross');
    svg += bar(x2, d.net, d.net < 0 ? 'bar-loss' : 'bar-net');
    if (i % labelEvery === 0 || n <= 8) {
      const lbl = n === 1 ? fmtDay(d.date, { weekday: 'short', month: 'short', day: 'numeric' }) : n <= 8 ? fmtDay(d.date, { weekday: 'short' }) : fmtDay(d.date);
      svg += `<text class="axis-text" x="${cx}" y="${H - 8}" text-anchor="middle">${lbl}</text>`;
    }
    svg += `<rect class="hit" data-i="${i}" x="${pad.l + slot * i}" y="${pad.t}" width="${slot}" height="${ih}" rx="6"/>`;
  });
  svg += '</svg>';
  el.innerHTML = svg;

  const tip = $('#chartTip');
  const show = (e) => {
    const d = daily[Number(e.target.dataset.i)];
    tip.innerHTML = `<div class="tt">${fmtDay(d.date, { weekday: 'long', month: 'short', day: 'numeric' })}</div>
      <div class="tr"><span><span style="color:var(--series-gross)">■</span> Gross</span><b>${peso(d.gross)}</b></div>
      <div class="tr"><span class="muted">Expenses</span><span>${peso(d.expenses)}</span></div>
      <div class="tr"><span><span style="color:var(--series-net)">■</span> Net</span><b>${peso(d.net)}</b></div>
      <div class="tr"><span class="muted">Trips</span><span>${d.trips} · ${d.passengers} pax</span></div>`;
    tip.hidden = false;
    const r = e.target.getBoundingClientRect();
    const tw = tip.offsetWidth;
    let left = r.left + r.width / 2 - tw / 2;
    left = Math.max(8, Math.min(window.innerWidth - tw - 8, left));
    tip.style.left = left + 'px';
    tip.style.top = Math.max(8, r.top - tip.offsetHeight + 30) + 'px';
  };
  $$('.hit', el).forEach((h) => {
    h.addEventListener('mouseenter', show);
    h.addEventListener('click', show);
    h.addEventListener('mouseleave', () => { tip.hidden = true; });
  });
}

/* =====================================================================================
 * View: Records / Logs
 * ===================================================================================== */
async function renderLogs() {
  const f = state.logs;
  const params = new URLSearchParams();
  if (f.q) params.set('q', f.q);
  if (f.type) params.set('type', f.type);
  if (f.destination) params.set('destination', f.destination);
  if (f.date) params.set('date', f.date);
  const data = await api('/logs?' + params);
  $('#pageEyebrow').textContent = `${int(data.count)} record${data.count === 1 ? '' : 's'} · ${peso(data.totalFare)}`;
  $('#headActions').innerHTML = `<a class="btn btn-primary" id="exportCsv" href="/api/logs/export?${params}" download>${icon('download')}Export CSV</a>`;
  const destNames = [...new Set([...state.destinations.map((x) => x.name)])];

  $('#view').innerHTML = `
    <section class="card">
      <div class="toolbar" style="margin-bottom:14px">
        <div class="search grow">${icon('search')}<input class="input" id="lq" type="search" placeholder="Search trip ID, driver, body no." value="${esc(f.q)}" aria-label="Search logs"></div>
        <input class="input" style="width:auto" type="date" id="ldate" value="${esc(f.date)}" aria-label="Filter by date">
        <select class="input" style="width:auto" id="ldest" aria-label="Filter by destination"><option value="">All destinations</option>${destNames.map((n) => `<option ${f.destination === n ? 'selected' : ''}>${esc(n)}</option>`).join('')}</select>
      </div>
      <div class="chips" id="ltype" style="margin-bottom:6px">
        ${[['', 'All trips'], ['REGULAR', 'Regular'], ['SPECIAL', 'Special']].map(([v, l]) => `<button class="chip ${f.type === v ? 'on' : ''}" data-v="${v}">${l}</button>`).join('')}
        ${f.q || f.type || f.destination || f.date ? '<button class="chip" id="lreset">Clear filters</button>' : ''}
      </div>
      ${data.records.length ? `<div class="table-wrap"><table class="t">
        <thead><tr><th>Trip ID</th><th>Date & time</th><th>Body No.</th><th class="hide-sm">Driver</th><th>Destination</th><th class="r hide-sm">Regular</th><th class="r hide-sm">Disc.</th><th>Type</th><th class="r">Fare</th></tr></thead>
        <tbody>${data.records.map((r) => `<tr>
          <td class="num faint">${esc(r.tripId)}</td><td class="num">${esc(r.timestamp.slice(0, 16))}</td><td class="body-no">${esc(r.bodyNumber)}</td>
          <td class="hide-sm">${esc(r.driverName)}</td><td>${esc(r.destination)}</td><td class="r hide-sm">${r.regularPax}</td><td class="r hide-sm">${r.discountedPax}</td>
          <td>${r.tripType === 'SPECIAL' ? '<span class="pill pill-purple">Special</span>' : '<span class="pill pill-gray no-dot">Regular</span>'}</td>
          <td class="r"><strong>${peso(r.totalFare)}</strong></td></tr>`).join('')}</tbody></table></div>
          ${data.truncated ? '<p class="hint" style="margin-top:10px">Showing the latest 500 matches. Export CSV includes all of them.</p>' : ''}`
        : '<div class="empty"><div class="em">🗂️</div>No records match these filters.</div>'}
    </section>`;

  const rerun = () => renderLogs().catch((e) => toast(e.message, 'err'));
  $('#lq').addEventListener('input', debounce((e) => { f.q = e.target.value; rerun().then(() => { const i = $('#lq'); i.focus(); i.setSelectionRange(i.value.length, i.value.length); }); }, 250));
  $('#ldate').addEventListener('change', (e) => { f.date = e.target.value; rerun(); });
  $('#ldest').addEventListener('change', (e) => { f.destination = e.target.value; rerun(); });
  $$('#ltype [data-v]').forEach((b) => b.addEventListener('click', () => { f.type = b.dataset.v; rerun(); }));
  $('#lreset')?.addEventListener('click', () => { state.logs = { q: '', type: '', destination: '', date: '' }; rerun(); });
}

/* =====================================================================================
 * View: Settings
 * ===================================================================================== */
const PRESETS = [
  { name: 'System Blue', p: '#0A84FF', s: '#5E5CE6', a: '#30D158' },
  { name: 'Batangas Green', p: '#248A3D', s: '#0A84FF', a: '#FF9F0A' },
  { name: 'Sunset', p: '#FF6B35', s: '#AF52DE', a: '#FFD60A' },
  { name: 'Graphite', p: '#3A3A3C', s: '#8E8E93', a: '#0A84FF' },
  { name: 'Royal', p: '#5856D6', s: '#FF2D55', a: '#64D2FF' },
];

function renderSettings() {
  const s = { ...state.settings };
  $('#pageEyebrow').textContent = 'System & display preferences';
  $('#headActions').innerHTML = '';
  $('#view').innerHTML = `
    <section class="grid split-even">
      <article class="card">
        <div class="card-head"><div><h3>System Preferences</h3><p class="card-sub">Saved on this terminal computer</p></div></div>
        <form id="sysForm" novalidate>
          <div class="field"><label for="tName">Terminal name</label><input class="input" id="tName" maxlength="50" value="${esc(s.terminalName)}" required></div>
          <div class="row2">
            ${stepperHTML('capStep', 'Default max capacity')}
            <div class="field"><label for="tDisc">Discount (Senior / Student / PWD)</label><div class="input-prefix"><span>%</span><input class="input" id="tDisc" type="number" min="0" max="100" value="${s.discountPercent}"></div></div>
          </div>
          <div class="field"><span class="label">Special trip pricing</span>
            <div class="segmented full" id="spMode"><button type="button" data-v="MULTIPLIER" class="${s.specialMode === 'MULTIPLIER' ? 'on' : ''}">Base fare × multiplier</button><button type="button" data-v="FLAT" class="${s.specialMode === 'FLAT' ? 'on' : ''}">Flat rate</button></div></div>
          <div class="row2">
            <div class="field"><label for="spMul">Multiplier</label><div class="input-prefix"><span>×</span><input class="input" id="spMul" type="number" min="1" max="20" step="0.5" value="${s.specialMultiplier}"></div></div>
            <div class="field"><label for="spFlat">Flat rate</label><div class="input-prefix"><span>₱</span><input class="input" id="spFlat" type="number" min="1" step="1" value="${s.specialFlatRate}"></div></div>
          </div>
          <div class="switch-row" style="margin-top:16px">
            <div class="txt"><strong>Auto-Route Sorting</strong><span>Boarding units move ahead of empty ones and parties fill units already heading their way. FIFO order is kept inside each group.</span></div>
            <label class="switch"><input type="checkbox" id="autoSort" ${s.autoRouteSorting ? 'checked' : ''}><span class="track"></span></label>
          </div>
          <button class="btn btn-primary btn-block" style="margin-top:18px" type="submit">Save Preferences</button>
        </form>
      </article>

      <article class="card">
        <div class="card-head"><div><h3>Appearance</h3><p class="card-sub">Changes preview instantly</p></div></div>
        <div class="field"><span class="label">Mode</span>
          <div class="segmented full" id="themeSeg">${['light', 'dark', 'system'].map((t) => `<button data-v="${t}" class="${s.theme === t ? 'on' : ''}">${t === 'light' ? '☀️ Light' : t === 'dark' ? '🌙 Dark' : 'Auto'}</button>`).join('')}</div></div>
        <div class="field"><span class="label">TODA color scheme</span>
          <div class="preset-row">${PRESETS.map((p, i) => `<button class="preset" title="${p.name}" aria-label="${p.name}" data-preset="${i}"><span style="background:${p.p}"></span><span style="background:${p.s}"></span><span style="background:${p.a}"></span><span style="background:${p.p}"></span></button>`).join('')}</div></div>
        <div class="color-row" style="margin-top:14px">
          ${[['colorPrimary', 'Primary'], ['colorSecondary', 'Secondary'], ['colorAccent', 'Accent']].map(([k, l]) => `
            <label class="color-pick"><input type="color" data-color="${k}" value="${s[k]}"><span class="cp-txt"><b>${l}</b><span data-hex="${k}">${s[k]}</span></span></label>`).join('')}
        </div>
        <h4 class="section-title">Live Preview</h4>
        <div class="preview-brand">
          <div class="brand-mark" aria-hidden="true"><svg viewBox="0 0 1024 1024"><use href="#qms-glyph"/></svg></div>
          <div><strong>Queueing Me Softly</strong><span>Logo follows your Primary, Secondary and Accent colors</span></div>
        </div>
        <div class="preview-card hero-tint">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px"><span class="eyebrow">Now Boarding</span><span class="pill pill-blue">Boarding</span></div>
          <div style="display:flex;align-items:baseline;gap:10px"><span class="hero-no" style="font-size:40px">T-045</span><span class="muted">${esc(s.terminalName)}</span></div>
          <div style="margin:12px 0" class="seats seats-lg"><span class="seat on"></span><span class="seat on"></span><span class="seat disc"></span><span class="seat"></span></div>
          <div style="display:flex;gap:8px;flex-wrap:wrap"><button class="btn btn-primary">${icon('send')}Dispatch Now</button><span class="pill" style="background:color-mix(in srgb, var(--color-accent) 18%, transparent);color:var(--color-accent)">Accent</span></div>
        </div>
        <div style="display:flex;gap:10px;margin-top:16px">
          <button class="btn" id="resetColors" style="flex:1">Reset Colors</button>
          <button class="btn btn-primary" id="saveColors" style="flex:1">Save Appearance</button>
        </div>
      </article>
    </section>
    <section class="card">
      <div class="card-head"><div><h3>About this Terminal</h3><p class="card-sub">Offline-first. No internet, cloud, GPS or online payments are used.</p></div></div>
      <dl class="kv"><dt>Dispatch logs</dt><dd>data/logs/dispatch_logs_YYYY-MM-DD.csv & dispatch_logs.txt</dd><dt>Expense log</dt><dd>data/logs/expense_logs.csv</dd><dt>Saved state</dt><dd>data/state.json</dd></dl>
    </section>`;

  const cap = stepper($('#capStep'), { min: 1, max: 10, value: s.defaultMaxCapacity });
  let mode = s.specialMode;
  const syncMode = () => { $('#spMul').disabled = mode !== 'MULTIPLIER'; $('#spFlat').disabled = mode !== 'FLAT'; };
  segmented($('#spMode'), (v) => { mode = v; syncMode(); });
  syncMode();
  $('#sysForm').addEventListener('submit', (e) => {
    e.preventDefault();
    act(() => api('/settings', { method: 'PUT', body: {
      terminalName: $('#tName').value, defaultMaxCapacity: cap.get(), discountPercent: Number($('#tDisc').value),
      specialMode: mode, specialMultiplier: Number($('#spMul').value), specialFlatRate: Number($('#spFlat').value),
      autoRouteSorting: $('#autoSort').checked,
    } }), 'Preferences saved');
  });

  const draft = { theme: s.theme, colorPrimary: s.colorPrimary, colorSecondary: s.colorSecondary, colorAccent: s.colorAccent };
  const preview = () => applyTheme({ ...state.settings, ...draft });
  const syncPickers = () => ['colorPrimary', 'colorSecondary', 'colorAccent'].forEach((k) => { $(`[data-color=${k}]`).value = draft[k]; $(`[data-hex=${k}]`).textContent = draft[k].toUpperCase(); });
  segmented($('#themeSeg'), (v) => { draft.theme = v; preview(); act(() => api('/settings', { method: 'PUT', body: { theme: v } })); });
  $$('[data-color]').forEach((inp) => inp.addEventListener('input', () => { draft[inp.dataset.color] = inp.value.toUpperCase(); syncPickers(); preview(); }));
  $$('[data-preset]').forEach((b) => b.addEventListener('click', () => { const p = PRESETS[b.dataset.preset]; draft.colorPrimary = p.p; draft.colorSecondary = p.s; draft.colorAccent = p.a; syncPickers(); preview(); }));
  $('#resetColors').onclick = () => { const p = PRESETS[0]; draft.colorPrimary = p.p; draft.colorSecondary = p.s; draft.colorAccent = p.a; syncPickers(); preview(); };
  $('#saveColors').onclick = () => act(() => api('/settings', { method: 'PUT', body: draft }), 'Appearance saved');
}

const RENDER = { dashboard: renderDashboard, queue: renderQueue, drivers: renderDrivers, fares: renderFares, summary: renderSummary, logs: renderLogs, settings: renderSettings };

/* =====================================================================================
 * Drawers: Add Tricycle
 * ===================================================================================== */
function openAddTricycle() {
  const avail = state.dash.availableDrivers;
  openDrawer({
    eyebrow: 'Quick action',
    title: 'Add Tricycle to Queue',
    body: avail.length ? `
      <p class="muted" style="margin-top:0">Active drivers not yet in line. They join at the back (FIFO).</p>
      <ul class="list">${avail.map((d) => `
        <li class="list-row"><div class="avatar" style="width:38px;height:38px;font-size:14px">${esc(initials(d.name))}</div>
          <div class="list-main"><div class="list-title"><span class="body-no">${esc(d.bodyNumber)}</span></div><div class="list-sub">${esc(d.name)}</div></div>
          <button class="btn btn-primary" data-join="${esc(d.bodyNumber)}">${icon('plus')}Add</button></li>`).join('')}</ul>`
      : `<div class="empty"><div class="em">✅</div><strong>Every active driver is already in line.</strong><p>Set a driver to Active or register a new one.</p>
          <a class="btn" href="#/drivers">Go to Drivers</a></div>`,
    foot: `<button class="btn" id="regNew">${icon('plus')}Register New Driver</button>`,
    onMount: (b, f) => {
      $$('[data-join]', b).forEach((btn) => btn.addEventListener('click', async () => {
        btn.disabled = true;
        const r = await act(() => api('/queue', { method: 'POST', body: { bodyNumber: btn.dataset.join } }), (t) => `${t.bodyNumber} joined the queue at #${state.dash ? state.dash.queue.length + 1 : ''}`.replace(/ at #$/, ''));
        if (r) btn.closest('li').remove(); else btn.disabled = false;
        if (!$$('[data-join]', b).length) closeDrawer();
      }));
      $('#regNew', f).onclick = () => openDriverForm(null, true);
      $$('a', b).forEach((a) => a.addEventListener('click', closeDrawer));
    },
  });
}

/* =====================================================================================
 * Drawers: Board Passengers (smart batch boarding)
 * ===================================================================================== */
function openBoarding() {
  const dests = state.destinations.filter((x) => x.active);
  if (!dests.length) { toast('Add or activate a route first.', 'err'); return; }
  if (!state.dash.queue.length) { toast('The queue is empty. Add a tricycle first.', 'err'); openAddTricycle(); return; }
  const head = state.dash.nowBoarding;
  const defaultDest = (head?.destinationId && dests.find((x) => x.id === head.destinationId)) ? head.destinationId : store.get('lastDest', dests[0].id);
  openDrawer({
    eyebrow: 'Batch boarding',
    title: 'Board Passengers',
    body: `
      <div class="field"><label for="bDest">Destination</label>
        <select class="input" id="bDest">${dests.map((x) => `<option value="${esc(x.id)}" ${x.id === defaultDest ? 'selected' : ''}>${esc(x.name)} · ${peso(x.baseFare)}</option>`).join('')}</select></div>
      <div class="row2" style="margin-top:16px">
        ${stepperHTML('bSize', 'Party size')}
        ${stepperHTML('bDisc', 'Senior / Student / PWD')}
      </div>
      <div class="field"><label for="bName">Party name <span class="faint">(optional)</span></label><input class="input" id="bName" maxlength="40" placeholder="e.g. Santos family"></div>
      <div id="bPreview"><div class="skeleton" style="height:140px;margin-top:18px"></div></div>`,
    foot: '<button class="btn" id="bCancel">Cancel</button><button class="btn btn-primary" id="bConfirm" disabled>Confirm Boarding</button>',
    onMount: (b, f) => {
      let plan = null;
      let choice = null;
      let userPicked = false;
      let seq = 0;
      const size = stepper($('#bSize', b), { min: 1, max: 40, value: 1, onChange: (v) => { disc.bounds(0, v); update(); } });
      const disc = stepper($('#bDisc', b), { min: 0, max: 1, value: 0, onChange: () => update() });
      const confirmBtn = $('#bConfirm', f);
      const allocHTML = (list) => list.map((a) => `<div class="alloc"><span><span class="body-no">${esc(a.bodyNumber)}</span> <span class="faint">#${a.queuePosition} · ${esc(a.driverName.split(' ')[0])}</span></span><span>${a.seats} seat${a.seats > 1 ? 's' : ''} · ${peso(a.fare)}</span></div>`).join('');
      const renderPlan = () => {
        const p = plan;
        let opts = '';
        if (p.choiceRequired) {
          opts = `<div class="notice">${esc(p.note)}</div>
            <label class="option ${choice === 'split' ? 'on' : ''}"><input type="radio" name="bOpt" value="split" ${choice === 'split' ? 'checked' : ''}><strong>Split across tricycles</strong>${allocHTML(p.splitOption)}</label>
            <label class="option ${choice === 'whole' ? 'on' : ''}"><input type="radio" name="bOpt" value="whole" ${choice === 'whole' ? 'checked' : ''}><strong>Keep the party together</strong>${allocHTML(p.wholeOption)}</label>`;
        } else if (p.splitOption) {
          opts = `<div class="notice info">${esc(p.note)}</div><div class="option on" style="cursor:default"><strong>Split plan</strong>${allocHTML(p.splitOption)}</div>`;
        } else {
          opts = `<div class="option on" style="cursor:default"><strong>Assigned tricycle</strong>${allocHTML(p.wholeOption)}</div>`;
        }
        $('#bPreview', b).innerHTML = `
          <div class="receipt"><h4>Receipt preview · ${esc(p.destinationName)}</h4>
            ${p.receipt.map((l) => `<div class="rline"><span>${esc(l.label)} <span class="q">× ${l.quantity} @ ${peso(l.unitPrice)}</span></span><span>${peso(l.amount)}</span></div>`).join('')}
            <div class="rtotal"><span>Total</span><span>${peso(p.totalFare)}</span></div></div>
          ${opts}`;
        $$('input[name=bOpt]', b).forEach((r) => r.addEventListener('change', () => { choice = r.value; userPicked = true; renderPlan(); }));
        confirmBtn.disabled = false;
      };
      const update = debounce(async () => {
        const my = ++seq;
        confirmBtn.disabled = true;
        try {
          const p = await api('/board/preview', { method: 'POST', body: { destinationId: $('#bDest', b).value, partySize: size.get(), discounted: disc.get() } });
          if (my !== seq) return;
          plan = p;
          if (p.choiceRequired) {
            if (!userPicked) choice = 'split';
          } else {
            choice = p.splitOption ? 'split' : 'whole';
            userPicked = false;
          }
          renderPlan();
        } catch (e) {
          if (my !== seq) return;
          plan = null;
          $('#bPreview', b).innerHTML = `<div class="notice" style="background:var(--red-bg);color:var(--red)">${esc(e.message)}</div>`;
        }
      }, 120);
      $('#bDest', b).addEventListener('change', () => { store.set('lastDest', $('#bDest', b).value); update(); });
      $('#bCancel', f).onclick = closeDrawer;
      confirmBtn.onclick = async () => {
        if (!plan) return;
        confirmBtn.disabled = true;
        const r = await act(() => api('/board', { method: 'POST', body: { destinationId: $('#bDest', b).value, partySize: size.get(), discounted: disc.get(), name: $('#bName', b).value, mode: choice } }),
          (res) => `Boarded ${res.partySize} to ${res.destinationName} on ${res.boarded.map((x) => x.bodyNumber).join(' + ')} · ${peso(res.totalFare)}`);
        if (r) closeDrawer(); else { confirmBtn.disabled = false; update(); }
      };
      update();
    },
  });
}

/* =====================================================================================
 * Drawers: Special Trip Override
 * ===================================================================================== */
function openSpecialTrip(bodyNo) {
  const units = state.dash.queue;
  if (!units.length) { toast('The queue is empty. Add a tricycle first.', 'err'); return; }
  const dests = state.destinations.filter((x) => x.active);
  const s = state.settings;
  const pick = bodyNo || (units.find((u) => u.currentPassengers === 0) || units[0]).bodyNumber;
  openDrawer({
    eyebrow: 'Private hire',
    title: '⚡ Special Trip Override',
    body: `
      <p class="muted" style="margin-top:0">Locks a tricycle as a private trip. Seat capacity and per-head discounts are bypassed, the special rate applies, and the unit moves to the front for immediate dispatch.</p>
      <div class="field"><label for="spUnit">Tricycle</label>
        <select class="input" id="spUnit">${units.map((u) => `<option value="${esc(u.bodyNumber)}" ${u.bodyNumber === pick ? 'selected' : ''}>#${u.position} · ${esc(u.bodyNumber)} · ${esc(u.driverName)}${u.currentPassengers && !u.specialTrip ? ` (${u.currentPassengers} aboard)` : ''}</option>`).join('')}</select></div>
      <div class="field"><label for="spDest">Destination</label>
        <select class="input" id="spDest">${dests.map((x) => `<option value="${esc(x.id)}">${esc(x.name)}</option>`).join('')}</select></div>
      ${stepperHTML('spSize', 'Passengers (for records)')}
      <div class="switch-row" style="margin-top:16px;border-top:1px solid var(--hairline);padding-top:14px">
        <div class="txt"><strong>Private / Special Trip</strong><span id="spRateTxt"></span></div>
        <label class="switch"><input type="checkbox" id="spOn" checked><span class="track"></span></label>
      </div>
      <div class="receipt"><h4>Special fare</h4><div class="rtotal" style="border:0;margin:0;padding:0"><span id="spLabel"></span><span id="spFare"></span></div></div>
      <div id="spWarn"></div>`,
    foot: '<button class="btn" id="spCancel">Cancel</button><button class="btn btn-primary" id="spApply">Lock Special Trip</button>',
    onMount: (b, f) => {
      const size = stepper($('#spSize', b), { min: 1, max: 20, value: 1 });
      const sync = () => {
        const dest = dests.find((x) => x.id === $('#spDest', b).value);
        const unit = units.find((u) => u.bodyNumber === $('#spUnit', b).value);
        const on = $('#spOn', b).checked;
        if (unit?.specialTrip && unit.destinationId) $('#spDest', b).value = unit.destinationId;
        const fare = s.specialMode === 'FLAT' ? s.specialFlatRate : (dest ? dest.baseFare * s.specialMultiplier : 0);
        $('#spRateTxt', b).textContent = s.specialMode === 'FLAT' ? `Flat rate of ${peso(s.specialFlatRate)}` : `Base fare × ${s.specialMultiplier}`;
        $('#spLabel', b).textContent = dest ? dest.name : '—';
        $('#spFare', b).textContent = on ? peso(fare) : 'Shared fares';
        $('#spApply', f).textContent = on ? 'Lock Special Trip' : 'Return to Shared Trips';
        $('#spWarn', b).innerHTML = on && unit && unit.currentPassengers > 0 && !unit.specialTrip
          ? `<div class="notice">${esc(unit.bodyNumber)} already has ${unit.currentPassengers} shared passenger(s). Unload them from the Queue page first, or pick an empty unit.</div>` : '';
      };
      ['#spUnit', '#spDest', '#spOn'].forEach((id) => $(id, b).addEventListener('change', sync));
      sync();
      $('#spCancel', f).onclick = closeDrawer;
      $('#spApply', f).onclick = async () => {
        const unit = $('#spUnit', b).value;
        const on = $('#spOn', b).checked;
        const r = await act(() => api(`/queue/${unit}/special`, { method: 'POST', body: { enabled: on, destinationId: $('#spDest', b).value, partySize: size.get() } }),
          (t) => (on ? `${t.bodyNumber} locked as a special trip to ${t.destinationName} and moved to #1` : `${t.bodyNumber} is back on shared trips`));
        if (r) closeDrawer();
      };
    },
  });
}

/* =====================================================================================
 * Drawers: Log Expense
 * ===================================================================================== */
async function openExpense(driverId) {
  let drivers = state.drivers;
  if (!drivers.length) {
    try { drivers = state.drivers = await api('/drivers'); } catch (e) { toast(e.message, 'err'); return; }
  }
  if (!drivers.length) { toast('Register a driver first.', 'err'); return; }
  openDrawer({
    eyebrow: 'Deducted from gross earnings',
    title: '💸 Log Expense',
    body: `
      <div class="field"><label for="exDriver">Driver</label>
        <select class="input" id="exDriver">${drivers.map((d) => `<option value="${esc(d.id)}" ${d.id === driverId ? 'selected' : ''}>${esc(d.bodyNumber)} · ${esc(d.name)}</option>`).join('')}</select></div>
      <div class="field"><span class="label">Category</span>
        <div class="segmented full" id="exCat" style="flex-wrap:wrap">${EXPENSE_CATS.map((c, i) => `<button type="button" data-v="${c.id}" class="${i === 0 ? 'on' : ''}">${c.emoji} ${c.label}</button>`).join('')}</div></div>
      <div class="field"><label for="exAmt">Amount</label><div class="input-prefix"><span>₱</span><input class="input" id="exAmt" type="number" inputmode="decimal" min="0.01" step="0.01" placeholder="0.00"></div></div>
      <div class="field"><label for="exNote">Note <span class="faint">(optional)</span></label><input class="input" id="exNote" maxlength="120" placeholder="e.g. Petron Kumintang"></div>
      <div class="receipt" id="exSummary"></div>`,
    foot: '<button class="btn" id="exCancel">Cancel</button><button class="btn btn-primary" id="exSave">Save Expense</button>',
    onMount: (b, f) => {
      let cat = 'FUEL';
      segmented($('#exCat', b), (v) => { cat = v; });
      const sum = () => {
        const d = drivers.find((x) => x.id === $('#exDriver', b).value);
        const amt = Math.max(0, Number($('#exAmt', b).value) || 0);
        $('#exSummary', b).innerHTML = `<h4>${esc(d.name)} · after this expense</h4>
          <div class="rline"><span>Gross earnings</span><span>${peso(d.grossEarnings)}</span></div>
          <div class="rline"><span>Total expenses</span><span>${peso(d.totalExpenses + amt)}</span></div>
          <div class="rtotal"><span>Net profit</span><span class="${d.grossEarnings - d.totalExpenses - amt < 0 ? 'neg' : 'pos'}">${peso(d.grossEarnings - d.totalExpenses - amt)}</span></div>`;
      };
      $('#exDriver', b).addEventListener('change', sum);
      $('#exAmt', b).addEventListener('input', sum);
      sum();
      $('#exCancel', f).onclick = closeDrawer;
      $('#exSave', f).onclick = async () => {
        const amt = Number($('#exAmt', b).value);
        if (!(amt > 0)) { $('#exAmt', b).classList.add('invalid'); toast('Enter an amount greater than ₱0.00.', 'err'); return; }
        const r = await act(() => api('/expenses', { method: 'POST', body: { driverId: $('#exDriver', b).value, category: cat, amount: amt, note: $('#exNote', b).value } }),
          (e) => `${e.categoryLabel} of ${peso(e.amount)} logged`);
        if (r) { state.drivers = []; closeDrawer(); }
      };
    },
  });
}

/* =====================================================================================
 * Drawers: Register / Edit Driver, Driver Record
 * ===================================================================================== */
function openDriverForm(driver = null, fromQueue = false) {
  const editing = !!driver;
  openDrawer({
    eyebrow: editing ? driver.bodyNumber : 'Driver & tricycle registry',
    title: editing ? 'Edit Driver' : 'Register Driver',
    body: `
      <form id="drvForm" novalidate>
        <div class="field"><label for="fName">Full name</label><input class="input" id="fName" maxlength="60" autocomplete="off" value="${esc(driver?.name || '')}" placeholder="Juan Dela Cruz"></div>
        <div class="row2">
          <div class="field"><label for="fBody">Tricycle body no.</label><input class="input num" id="fBody" maxlength="10" value="${esc(driver?.bodyNumber || '')}" placeholder="T-009"></div>
          <div class="field"><label for="fLic">License number</label><input class="input" id="fLic" maxlength="20" value="${esc(driver?.licenseNumber || '')}" placeholder="D04-12-345678"></div>
        </div>
        <div class="field"><label for="fContact">Contact number <span class="faint">(optional)</span></label><input class="input num" id="fContact" inputmode="tel" maxlength="20" value="${esc(driver?.contactNumber || '')}" placeholder="0917 123 4567"></div>
        ${editing ? '' : `<div class="switch-row" style="margin-top:14px"><div class="txt"><strong>Join the queue now</strong><span>Adds the tricycle to the back of the line</span></div>
          <label class="switch"><input type="checkbox" id="fJoin" ${fromQueue ? 'checked' : ''}><span class="track"></span></label></div>`}
        <p class="hint" style="margin-top:14px">Body numbers and license numbers must be unique.</p>
      </form>`,
    foot: `${editing ? `<button class="btn btn-danger" id="fDel">${icon('trash')}Delete</button>` : '<button class="btn" id="fCancel">Cancel</button>'}<button class="btn btn-primary" id="fSave">${editing ? 'Save Changes' : 'Register'}</button>`,
    onMount: (b, f) => {
      $('#fCancel', f)?.addEventListener('click', closeDrawer);
      const save = async () => {
        const payload = { name: $('#fName', b).value, bodyNumber: $('#fBody', b).value, licenseNumber: $('#fLic', b).value, contactNumber: $('#fContact', b).value };
        if (!editing) payload.joinQueue = $('#fJoin', b).checked;
        const missing = [['#fName', 'name'], ['#fBody', 'bodyNumber'], ['#fLic', 'licenseNumber']].filter(([id, k]) => { const bad = !payload[k].trim(); $(id, b).classList.toggle('invalid', bad); return bad; });
        if (missing.length) { toast('Please fill in the highlighted fields.', 'err'); return; }
        const r = await act(() => (editing ? api(`/drivers/${driver.id}`, { method: 'PUT', body: payload }) : api('/drivers', { method: 'POST', body: payload })),
          (d) => (editing ? `${d.name} updated` : `${d.name} registered with ${d.bodyNumber}`));
        if (r) closeDrawer();
      };
      $('#fSave', f).onclick = save;
      $('#drvForm', b).addEventListener('submit', (e) => { e.preventDefault(); save(); });
      $('#fDel', f)?.addEventListener('click', async () => {
        if (await confirmDialog(`Delete ${driver.name}?`, 'Their dispatch logs stay in the CSV files, but the driver record and expenses are removed.', 'Delete', true)) {
          const r = await act(() => api(`/drivers/${driver.id}`, { method: 'DELETE' }), `${driver.name} deleted`);
          if (r) closeDrawer();
        }
      });
    },
  });
}

async function openDriverRecord(id) {
  let d;
  try { d = await api(`/drivers/${id}`); } catch (e) { toast(e.message, 'err'); return; }
  openDrawer({
    eyebrow: `${d.bodyNumber} · ${d.licenseNumber}`,
    title: d.name,
    body: `
      <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">${pill(DRIVER_STATUS, d.status)}${d.queuePosition > 0 ? `<span class="pill pill-blue">Queue #${d.queuePosition}</span>` : ''}${d.contactNumber ? `<span class="muted num">${esc(d.contactNumber)}</span>` : ''}</div>
      <div class="receipt">
        <h4>Driver Record</h4>
        <div class="rline"><span>Gross earnings <span class="q">${int(d.tripsCompleted)} trips</span></span><span>${peso(d.grossEarnings)}</span></div>
        <div class="rline"><span>Total expenses <span class="q">${d.expenses.length} entries</span></span><span>− ${peso(d.totalExpenses)}</span></div>
        <div class="rtotal"><span>Net Profit</span><span class="${d.netProfit < 0 ? 'neg' : 'pos'}">${peso(d.netProfit)}</span></div>
      </div>
      <h4 class="section-title">Expenses</h4>
      ${d.expenses.length ? `<ul class="list">${d.expenses.slice(0, 30).map((e) => `
        <li class="list-row"><div class="list-main"><div class="list-title">${esc(EXPENSE_CATS.find((c) => c.id === e.category)?.emoji || '')} ${esc(e.categoryLabel)}</div>
          <div class="list-sub num">${esc(e.timestamp.slice(0, 16).replace('T', ' '))}${e.note ? ` · ${esc(e.note)}` : ''}</div></div>
          <div class="list-end">${peso(e.amount)}</div></li>`).join('')}</ul>` : '<div class="empty">No expenses logged.</div>'}
      <h4 class="section-title">Recent Trips</h4>
      ${d.recentTrips.length ? `<ul class="list">${d.recentTrips.slice(0, 15).map((t) => `
        <li class="list-row"><div class="list-main"><div class="list-title">${esc(t.destination)} ${t.tripType === 'SPECIAL' ? '<span class="pill pill-purple">Special</span>' : ''}</div>
          <div class="list-sub num">${esc(t.timestamp.slice(0, 16))} · ${t.totalPax} pax</div></div>
          <div class="list-end">${peso(t.totalFare)}</div></li>`).join('')}</ul>` : '<div class="empty">No trips yet.</div>'}`,
    foot: `<button class="btn" id="rEdit">${icon('edit')}Edit</button><button class="btn btn-primary" id="rExp">💸 Log Expense</button>`,
    onMount: (b, f) => {
      $('#rEdit', f).onclick = () => openDriverForm(d);
      $('#rExp', f).onclick = () => openExpense(d.id);
    },
  });
}

/* =====================================================================================
 * Drawers: Destination
 * ===================================================================================== */
function openDestinationForm(dest = null) {
  const editing = !!dest;
  openDrawer({
    eyebrow: 'Fare & destination',
    title: editing ? `Edit ${dest.name}` : 'Add Route',
    body: `
      <form id="dstForm" novalidate>
        <div class="field"><label for="dName">Destination name</label><input class="input" id="dName" maxlength="40" value="${esc(dest?.name || '')}" placeholder="e.g. Kumintang Ilaya"></div>
        <div class="field"><label for="dFare">Base fare per passenger</label><div class="input-prefix"><span>₱</span><input class="input" id="dFare" type="number" inputmode="decimal" min="1" step="0.5" value="${dest?.baseFare ?? ''}" placeholder="15.00"></div></div>
        <div class="switch-row" style="margin-top:14px"><div class="txt"><strong>Active</strong><span>Inactive routes are hidden from boarding</span></div>
          <label class="switch"><input type="checkbox" id="dActive" ${!dest || dest.active ? 'checked' : ''}><span class="track"></span></label></div>
        <div class="receipt" id="dPrev"></div>
      </form>`,
    foot: `<button class="btn" id="dCancel">Cancel</button><button class="btn btn-primary" id="dSave">${editing ? 'Save Route' : 'Add Route'}</button>`,
    onMount: (b, f) => {
      const prev = () => {
        const fare = Number($('#dFare', b).value) || 0;
        const pct = state.settings.discountPercent;
        $('#dPrev', b).innerHTML = `<h4>Fare preview</h4>
          <div class="rline"><span>Regular</span><span>${peso(fare)}</span></div>
          <div class="rline"><span>Senior / Student / PWD <span class="q">${pct}% off</span></span><span>${peso(fare * (1 - pct / 100))}</span></div>`;
      };
      $('#dFare', b).addEventListener('input', prev);
      prev();
      $('#dCancel', f).onclick = closeDrawer;
      const save = async () => {
        const payload = { name: $('#dName', b).value, baseFare: Number($('#dFare', b).value), active: $('#dActive', b).checked };
        const r = await act(() => (editing ? api(`/destinations/${dest.id}`, { method: 'PUT', body: payload }) : api('/destinations', { method: 'POST', body: payload })),
          (x) => `${x.name} saved at ${peso(x.baseFare)}`);
        if (r) closeDrawer();
      };
      $('#dSave', f).onclick = save;
      $('#dstForm', b).addEventListener('submit', (e) => { e.preventDefault(); save(); });
    },
  });
}

/* =====================================================================================
 * Boot
 * ===================================================================================== */
function tickClock() {
  $('#clock').textContent = new Date().toLocaleTimeString('en-PH', { hour: 'numeric', minute: '2-digit' });
}

buildNav();
window.addEventListener('hashchange', route);
matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => applyTheme(state.settings));
let resizeT;
window.addEventListener('resize', () => { clearTimeout(resizeT); resizeT = setTimeout(() => { if (state.view === 'summary') refresh(); }, 250); });
tickClock();
setInterval(tickClock, 15000);
// Live refresh of operational screens (skipped while a drawer or modal is open so forms are never disturbed).
setInterval(() => {
  if (document.hidden || drawer.open || !$('#modalWrap').hidden) return;
  if (state.view === 'dashboard' || state.view === 'queue') refresh();
}, 5000);
route();
