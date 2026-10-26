import { renderLab, renderTasks } from './devops.js';

const paths = {
  building: '<path d="m3 9 9-6 9 6H3Zm2 3v7m5-7v7m4-7v7m5-7v7M3 21h18"/>',
  grid: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
  inbox: '<path d="M4 4h16l2 11v5H2v-5L4 4Z"/><path d="M2 15h6l2 3h4l2-3h6M8 8h8M8 11h8"/>',
  plus: '<rect x="3" y="3" width="18" height="18" rx="4"/><path d="M12 7v10M7 12h10"/>',
  search: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',
  workflow: '<rect x="3" y="3" width="6" height="6" rx="1"/><rect x="15" y="15" width="6" height="6" rx="1"/><path d="M6 9v9h9M9 6h9v9"/>',
  book: '<path d="M12 5c-4-3-8-2-10-1v15c3-2 7-2 10 0 3-2 7-2 10 0V4c-2-1-6-2-10 1Zm0 0v14"/>',
  terminal: '<rect x="2" y="4" width="20" height="16" rx="3"/><path d="m6 9 3 3-3 3m6 0h5"/>',
  menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
  play: '<path d="m8 4 13 8-13 8V4Z"/>',
  pause: '<path d="M8 5v14M16 5v14"/>',
  arrow: '<path d="M4 12h16m-6-6 6 6-6 6"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
  checkCircle: '<circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  refresh: '<path d="M20 7v5h-5M4 17v-5h5"/><path d="M6 6a8 8 0 0 1 13 2M5 16a8 8 0 0 0 13 2"/>',
  calendar: '<rect x="3" y="5" width="18" height="16" rx="3"/><path d="M7 3v4m10-4v4M3 11h18"/>',
  code: '<path d="m8 6-6 6 6 6m8-12 6 6-6 6m-3-15-2 18"/>',
  box: '<path d="m12 2 10 5-10 5L2 7l10-5Zm0 10v10M2 7v10l10 5 10-5V7M7 4.5l10 5"/>',
  shield: '<path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6l8-3Z"/><path d="m8 12 3 3 5-6"/>',
  git: '<circle cx="6" cy="5" r="2"/><circle cx="6" cy="19" r="2"/><circle cx="18" cy="6" r="2"/><path d="M6 7v10m0-4h6a6 6 0 0 0 6-5"/>',
  server: '<rect x="3" y="3" width="18" height="7" rx="2"/><rect x="3" y="14" width="18" height="7" rx="2"/><path d="M7 6.5h.01M7 17.5h.01m5-11h5m-5 11h5"/>',
  layers: '<path d="m12 3 10 5-10 5L2 8l10-5ZM2 12l10 5 10-5M2 16l10 5 10-5"/>',
  road: '<path d="m6 3-3 18m15-18 3 18M12 3v4m0 4v3m0 4v3"/>',
  water: '<path d="M12 2S5 10 5 15a7 7 0 0 0 14 0c0-5-7-13-7-13Z"/><path d="M9 16a3 3 0 0 0 3 3"/>',
  light: '<path d="M9 18h6m-5 3h4M8 14a6 6 0 1 1 8 0c-1 1-1 2-1 2H9s0-1-1-2Z"/>',
  waste: '<path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7"/>',
  health: '<path d="M20 5c-2-2-5-1-8 2-3-3-6-4-8-2-5 5 1 11 8 16C19 16 25 10 20 5Z"/><path d="M6 12h3l2-3 2 6 2-3h3"/>',
  download: '<path d="M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5"/>',
  close: '<path d="m6 6 12 12M6 18 18 6"/>',
  copy: '<rect x="8" y="8" width="13" height="13" rx="2"/><path d="M16 8V3H3v13h5"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v6m0-10h.01"/>',
  next: '<path d="m8 5 7 7-7 7m11-14v14"/>',
  cloud: '<path d="M6 19a5 5 0 0 1-1-10 7 7 0 0 1 13-2 6 6 0 0 1 0 12H6Z"/>',
};

const icon = (name, extra = '') => `<svg class="icon ${extra}" viewBox="0 0 24 24" aria-hidden="true">${paths[name] || paths.layers}</svg>`;
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[character]));
const statuses = ['PENDING', 'UNDER_REVIEW', 'APPROVED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'];
const statusNames = { PENDING: 'Pending', UNDER_REVIEW: 'Under review', APPROVED: 'Approved', IN_PROGRESS: 'In progress', RESOLVED: 'Resolved', REJECTED: 'Rejected' };
const colors = ['#ddc58b', '#92b1d3', '#8ebaa5', '#b0a4ce', '#438c67', '#d8a49b'];
const categories = ['Roads & Potholes', 'Solid Waste Management', 'Water Supply & Leakage', 'Streetlights & Electricity', 'Drainage & Sewage', 'Public Health & Sanitation'];
const shortCategory = category => ({ 'Roads & Potholes': 'Roads & potholes', 'Solid Waste Management': 'Waste management', 'Water Supply & Leakage': 'Water supply', 'Streetlights & Electricity': 'Streetlights', 'Drainage & Sewage': 'Drainage & sewage', 'Public Health & Sanitation': 'Public health' }[category] || category);
const categoryIcon = category => ({ 'Roads & Potholes': 'road', 'Solid Waste Management': 'waste', 'Water Supply & Leakage': 'water', 'Streetlights & Electricity': 'light', 'Drainage & Sewage': 'layers', 'Public Health & Sanitation': 'health' }[category] || 'inbox');
const titles = { overview: 'Overview', complaints: 'Complaints', submit: 'Submit complaint', track: 'Track status', devops: 'DevOps Lab', tasks: 'Tasks & reports' };
const state = { complaints: [], health: null, loaded: false, error: '', query: '', status: '', category: '', ward: '', page: 1, sort: 'updated', trackToken: 'CMP-2026-1001', busy: false };
const view = document.querySelector('#view');
let cleanup = () => {};
let toastTimer;
let loadSequence = 0;

document.querySelectorAll('[data-icon]').forEach(element => { element.innerHTML = icon(element.dataset.icon); });

function statusBadge(status) {
  return `<span class="status ${escapeHtml(status.toLowerCase())}">${escapeHtml(statusNames[status] || status)}</span>`;
}

function date(value, full = false) {
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? 'Not available' : parsed.toLocaleString('en-GB', full
    ? { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }
    : { day: 'numeric', month: 'short', year: 'numeric' });
}

function toast(message) {
  const element = document.querySelector('#toast');
  element.textContent = message;
  element.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => element.classList.remove('show'), 3500);
}

async function api(path, body) {
  const response = await fetch(path, { signal: AbortSignal.timeout(8000), ...(body ? { method: 'POST', body: new URLSearchParams(body) } : {}) });
  const result = await response.json();
  if (!response.ok) throw new Error(result.error || 'The request could not be completed');
  return result;
}

async function load(redraw = true) {
  const sequence = ++loadSequence;
  const routeAtStart = location.hash;
  state.busy = true;
  const [complaints, health] = await Promise.allSettled([api('/api/complaints'), api('/api/health')]);
  if (sequence !== loadSequence) return;
  state.error = complaints.status === 'rejected' ? 'The local service could not be reached. Check that the portal is running, then try again.' : '';
  if (complaints.status === 'fulfilled') state.complaints = complaints.value;
  state.health = health.status === 'fulfilled' ? health.value : null;
  state.loaded = true;
  state.busy = false;
  document.querySelector('#nav-count').textContent = complaints.status === 'fulfilled' ? state.complaints.length : '—';
  const healthPill = document.querySelector('#health-pill');
  healthPill.classList.toggle('offline', !state.health);
  healthPill.innerHTML = `<span class="dot"></span><span>${state.health ? 'Local service online' : 'Service unavailable'}</span>`;
  healthPill.title = state.health ? `Fresh local health check · ${date(state.health.checkedAt, true)} · Not a check of Jenkins or infrastructure` : 'Local health check failed; this does not indicate pipeline status';
  if (redraw && routeAtStart === location.hash) render();
}

function heading(title, description, actions = '') {
  return `<div class="page-header"><div><h1>${title}</h1><p>${description}</p></div><div class="page-actions">${actions}</div></div>`;
}

function notice() {
  return state.error ? `<div class="notice error" role="alert">${icon('info')}<div>${escapeHtml(state.error)} <button class="text-link" data-refresh>Retry connection</button></div></div>` : '';
}

function demoNotice() {
  return `<div class="notice">${icon('info')}<span><strong>${state.health?.demoMode ? 'Demo workspace.' : 'In-memory workspace.'}</strong> Sample records are synthetic. Data and event history reset on restart.${state.health?.demoMode ? ' Reviewer actions are local demonstrations, not authenticated access.' : ' Reviewer actions are disabled. Start with --demo on localhost to enable them.'}</span></div>`;
}

function stats() {
  const counts = Object.fromEntries(statuses.map(status => [status, state.complaints.filter(c => c.status === status).length]));
  const values = [
    ['Total complaints', state.complaints.length, 'inbox', '', 'All records in this session'],
    ['Awaiting review', counts.PENDING + counts.UNDER_REVIEW, 'clock', 'amber', 'Pending + under review'],
    ['Active field work', counts.APPROVED + counts.IN_PROGRESS, 'workflow', 'purple', 'Approved + in progress'],
    ['Resolved issues', counts.RESOLVED, 'checkCircle', 'green', 'Confirmed resolutions'],
  ];
  return `<div class="stats-grid">${values.map(([title, value, symbol, tone, note]) => `<article class="stat-card"><div class="stat-top"><span>${title}</span><span class="stat-icon ${tone}">${icon(symbol)}</span></div><div class="stat-value">${value.toString().padStart(2, '0')}</div><div class="stat-footer"><span class="dot"></span>${note}</div></article>`).join('')}</div>`;
}

function complaintRows(records) {
  return records.map(complaint => `<tr><td><button class="token-link" data-detail="${escapeHtml(complaint.token)}">${escapeHtml(complaint.token)}</button></td><td><div class="citizen-cell"><span class="avatar">${escapeHtml(complaint.citizen.split(/\s+/).map(word => word[0]).slice(0, 2).join(''))}</span><span><strong>${escapeHtml(complaint.citizen)}</strong><small>${escapeHtml(complaint.ward)}</small></span></div></td><td>${escapeHtml(shortCategory(complaint.category))}</td><td>${statusBadge(complaint.status)}</td><td>${date(complaint.updatedAt, true)}</td><td><button class="icon-button" data-detail="${escapeHtml(complaint.token)}" aria-label="View ${escapeHtml(complaint.token)}">${icon('arrow')}</button></td></tr>`).join('');
}

function table(records, footer = '') {
  return `<div class="table-wrap"><table><thead><tr><th scope="col">COMPLAINT ID</th><th scope="col">CITIZEN / WARD</th><th scope="col">CATEGORY</th><th scope="col">STATUS</th><th scope="col">LAST UPDATED</th><th scope="col"><span class="sr-only">Actions</span></th></tr></thead><tbody>${records.length ? complaintRows(records) : '<tr><td colspan="6"><div class="empty-state"><h3>No complaints found</h3><p>Try a different search or filter.</p></div></td></tr>'}</tbody></table></div>${footer}`;
}

function overview() {
  const total = state.complaints.length;
  const counts = statuses.map(status => state.complaints.filter(c => c.status === status).length);
  return heading('Workspace overview', 'Your civic operations and delivery journey, in one place.',
    `<span class="button date-action">${icon('calendar')}${new Date().toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })}</span><button class="button" data-refresh aria-label="Refresh workspace">${icon('refresh')}</button>`)
    + notice()
    + `<section class="hero"><div class="hero-copy"><div class="eyebrow"><span class="dot"></span>CIVIC IMPACT. CONTINUOUS DELIVERY.</div><h2>Better civic services.<br>Built on better delivery.</h2><p>A working complaint portal. An end-to-end DevOps story.<br>Explore the engineering behind every release.</p><div class="hero-actions"><a class="button primary" href="#devops">${icon('workflow')}Explore DevOps Lab${icon('arrow')}</a><a class="text-link" href="#tasks">View 15-task journey${icon('arrow')}</a></div></div><div class="hero-graphic" aria-hidden="true"><div class="delivery-window"><div class="window-bar"><i></i><i></i><i></i><span>municipal-portal / delivery workflow</span></div><div class="window-body"><div class="window-title">From commit to confidence<span class="pill green">EXPLAINED</span></div><div class="mini-pipeline"><span class="mini-node">${icon('git')}</span><span class="mini-line"></span><span class="mini-node">${icon('code')}</span><span class="mini-line"></span><span class="mini-node">${icon('shield')}</span><span class="mini-line"></span><span class="mini-node">${icon('box')}</span></div><div class="window-caption"><span>COMMIT</span><span>BUILD</span><span>TEST</span><span>DELIVER</span></div></div></div><div class="floating-label">${icon('layers')}One project. The complete learning journey.</div></div></section>`
    + stats()
    + `<div class="dashboard-grid"><section class="card"><div class="card-header"><div><h2>Complaints by category</h2><p>Where civic attention is needed</p></div><span class="pill outline">This session</span></div><div class="card-body category-bars">${categories.map(category => {
      const count = state.complaints.filter(c => c.category === category).length;
      return `<div class="bar-row"><span class="bar-label">${icon(categoryIcon(category))}${escapeHtml(shortCategory(category))}</span><div class="bar-track" role="img" aria-label="${escapeHtml(category)}: ${count}"><div class="bar-fill" data-width="${total ? count / Math.max(...categories.map(c => state.complaints.filter(r => r.category === c).length), 1) * 100 : 0}"></div></div><span class="bar-value">${count}</span></div>`;
    }).join('')}</div></section><section class="card"><div class="card-header"><div><h2>Status distribution</h2><p>A snapshot of the current queue</p></div>${icon('layers')}</div><div class="card-body distribution"><div class="donut" id="status-donut" role="img" aria-label="${escapeHtml(statuses.map((s, i) => `${statusNames[s]}: ${counts[i]}`).join(', '))}"><div class="donut-center"><strong>${total}</strong><small>Total complaints</small></div></div><div class="legend">${statuses.map((status, i) => `<div class="legend-row"><span class="legend-dot" data-color="${colors[i]}"></span><span>${statusNames[status]}</span><strong>${counts[i]}</strong></div>`).join('')}</div></div></section></div>`
    + `<section class="card table-card"><div class="card-header"><div><h2>Recent complaints</h2><p>Latest submissions and reviewer updates</p></div><a class="text-link" href="#complaints">View all complaints${icon('arrow')}</a></div>${table(state.complaints.slice(0, 5), `<div class="table-footer"><span>${Math.min(5, total)} of ${total} complaints · In-memory session data</span><a class="text-link" href="#submit">${icon('plus')}New complaint</a></div>`)}</section>`
    + `<div class="section-heading"><h2>Behind the application</h2><span>Recorded evidence · 20 Oct 2026 · revision abc900735330</span></div><div class="evidence-strip"><a class="evidence-tile" href="#devops"><span class="tile-icon">${icon('shield')}</span><span><strong>17 unit tests · 5 browser journeys</strong><p>Recorded local gates, not a live build</p></span><span class="pill green">RECORDED</span></a><a class="evidence-tile" href="#tasks"><span class="tile-icon">${icon('book')}</span><span><strong>15 tasks. One connected story.</strong><p>Reports, deliverables & learning outcomes</p></span><span class="pill blue">EXPLORE</span></a><a class="evidence-tile" href="#devops"><span class="tile-icon">${icon('box')}</span><span><strong>Docker & Ansible delivery</strong><p>Implemented · runtime evidence pending</p></span><span class="pill amber">PENDING</span></a></div>`;
}

function filteredComplaints() {
  return state.complaints.filter(c => (!state.query || `${c.token} ${c.citizen}`.toLowerCase().includes(state.query.toLowerCase()))
    && (!state.status || c.status === state.status) && (!state.category || c.category === state.category) && (!state.ward || c.ward === state.ward))
    .sort(state.sort === 'token' ? (a, b) => a.token.localeCompare(b.token) : (a, b) => b.updatedAt.localeCompare(a.updatedAt));
}

function options(values, selected, label = 'All') {
  return `<option value="">${label}</option>${values.map(value => `<option value="${escapeHtml(value)}"${value === selected ? ' selected' : ''}>${escapeHtml(statusNames[value] || value)}</option>`).join('')}`;
}

function complaintsPage() {
  return heading('Complaint workspace', 'A clear queue. Meaningful actions. Transparent outcomes.',
    `<button class="button" data-refresh aria-label="Refresh complaints">${icon('refresh')}</button><a class="button primary" href="#submit">${icon('plus')}New complaint</a>`)
    + notice() + demoNotice()
    + `<section class="card"><div class="filters"><div class="filter-field"><label for="complaint-search">Search complaints</label><input id="complaint-search" placeholder="Search token or citizen name..." maxlength="100" value="${escapeHtml(state.query)}"></div><div class="filter-field"><label for="status-filter">Status</label><select id="status-filter">${options(statuses, state.status, 'All statuses')}</select></div><div class="filter-field"><label for="category-filter">Category</label><select id="category-filter">${options(categories, state.category, 'All categories')}</select></div><div class="filter-field"><label for="ward-filter">Ward</label><select id="ward-filter">${options([...new Set(state.complaints.map(c => c.ward))].sort(), state.ward, 'All wards')}</select></div></div><div id="complaint-results"></div></section>`;
}

function updateTable() {
  const records = filteredComplaints();
  state.page = Math.max(1, Math.min(state.page, Math.ceil(records.length / 8) || 1));
  const start = (state.page - 1) * 8;
  document.querySelector('#complaint-results').innerHTML = table(records.slice(start, start + 8),
    `<div class="table-footer"><span>${records.length ? start + 1 : 0}–${Math.min(start + 8, records.length)} of ${records.length} complaints</span><div class="inline-actions"><label class="sr-only" for="sort-order">Sort complaints</label><select class="control-select" id="sort-order"><option value="updated"${state.sort === 'updated' ? ' selected' : ''}>Latest updated</option><option value="token"${state.sort === 'token' ? ' selected' : ''}>Complaint ID</option></select><button class="button small" data-page="-1"${state.page === 1 ? ' disabled' : ''} aria-label="Previous page">Previous</button><span>${state.page} / ${Math.max(1, Math.ceil(records.length / 8))}</span><button class="button small" data-page="1"${start + 8 >= records.length ? ' disabled' : ''} aria-label="Next page">Next</button></div></div>`);
}

function submitPage() {
  return heading('Make your voice count.', 'Tell us about a civic issue. We’ll give you a clear way to follow it.')
    + notice()
    + `<div class="form-layout"><section class="card"><form id="complaint-form" class="form-content" novalidate><div class="form-section-title"><span class="section-number">01</span>Your details</div><div class="form-grid"><div class="field"><label for="name">Citizen name <span class="muted">*</span></label><input id="name" name="citizenName" autocomplete="name" maxlength="100" placeholder="Your full name" required></div><div class="field"><label for="contact">Mobile number or email <span class="muted">*</span></label><input id="contact" name="citizenContact" autocomplete="email" maxlength="150" placeholder="10-digit mobile or email" required><div class="field-hint">Mobile numbers must begin with 6, 7, 8 or 9.</div></div></div><hr class="form-divider"><div class="form-section-title"><span class="section-number">02</span>About the issue</div><div class="form-grid"><div class="field"><label for="category">Civic category <span class="muted">*</span></label><select id="category" name="category">${categories.map(category => `<option>${escapeHtml(category)}</option>`).join('')}</select></div><div class="field"><label for="ward">Ward / sector <span class="muted">*</span></label><input id="ward" name="ward" maxlength="10" placeholder="e.g. Ward 07" required></div><div class="field full"><label for="desc">Describe the issue <span class="muted">*</span></label><textarea id="desc" name="description" minlength="15" maxlength="1500" placeholder="What happened? Where is the issue? Share enough detail for a reviewer to understand." required></textarea><div class="field-hint"><span>15–1,500 characters. Please use synthetic details for the demo.</span><span id="description-count">0 / 1500</span></div></div></div><div class="notice">${icon('info')}<span>This learning portal stores records in memory. Attachments and notifications are not part of this release.</span></div><div class="form-bottom"><p>You’ll receive a tracking token after successful submission. Save it to follow your complaint.</p><button class="button primary" type="submit">Submit Complaint${icon('arrow')}</button></div><div id="sub-res" aria-live="polite"></div></form></section><aside class="card info-panel"><span class="pill green">A SIMPLE, TRANSPARENT PROCESS</span><h3 class="section-heading">From concern to resolution</h3><ol class="steps-list"><li><strong>Share the issue</strong><p>Select the category and tell us what needs attention.</p></li><li><strong>Save your token</strong><p>Every accepted complaint receives a unique tracking ID.</p></li><li><strong>Review & field action</strong><p>A demo reviewer inspects and updates the complaint.</p></li><li><strong>Follow the outcome</strong><p>See status changes, official remarks and recorded events.</p></li></ol><div class="side-note"><strong>Designed for demonstration</strong>No identity verification or production authentication is configured. Do not enter sensitive personal information.</div></aside></div>`;
}

function historyMarkup(complaint) {
  return `<ol class="timeline">${complaint.history.map(event => `<li><strong>${statusNames[event.status] || escapeHtml(event.status)}</strong><p>${escapeHtml(event.remarks || 'Status updated')}</p><small>${date(event.at, true)} · ${escapeHtml(event.actor === 'CITIZEN' ? 'Citizen submission' : 'Demo reviewer')}</small></li>`).join('')}</ol>`;
}

function complaintSummary(complaint) {
  return `<div class="detail-top"><div class="inline-actions"><span class="pill outline">${escapeHtml(complaint.token)}</span>${statusBadge(complaint.status)}</div><h2>${escapeHtml(shortCategory(complaint.category))}</h2><p class="detail-description">${escapeHtml(complaint.desc)}</p><dl class="detail-meta"><div><dt>CITIZEN</dt><dd>${escapeHtml(complaint.citizen)}</dd></div><div><dt>WARD / SECTOR</dt><dd>${escapeHtml(complaint.ward)}</dd></div><div><dt>SUBMITTED</dt><dd>${date(complaint.createdAt, true)}</dd></div><div><dt>LAST UPDATED</dt><dd>${date(complaint.updatedAt, true)}</dd></div></dl>${complaint.remarks ? `<div class="side-note"><strong>Official remarks</strong>${escapeHtml(complaint.remarks)}</div>` : ''}</div>`;
}

function trackPage() {
  return heading('Every complaint has a story.', 'Follow your complaint from registration to its latest recorded outcome.')
    + `<div class="track-layout">${notice()}<section class="card track-search"><form id="track-form" class="track-form"><div class="field"><label for="track-token">Your tracking token</label><input id="track-token" placeholder="e.g. CMP-2026-1001" maxlength="40" value="${escapeHtml(state.trackToken)}" required></div><button class="button primary" type="submit">${icon('search')}Track Status</button></form><p>Try the synthetic sample <button class="text-link" id="sample-token">CMP-2026-1001</button>. Tokens are case-insensitive.</p></section><div id="track-res" class="track-result" aria-live="polite"><div class="empty-state">${icon('search')}<h3>A clearer path to an answer.</h3><p>Enter your token to see current status, remarks and recorded events.</p></div></div><div class="notice">${icon('info')}<span>Only events recorded by this application are shown. In-memory records and history reset when the application restarts.</span></div></div>`;
}

async function track(token) {
  const output = document.querySelector('#track-res');
  if (!output) return;
  state.trackToken = token.trim();
  output.innerHTML = '<div class="loading-state">Looking up your complaint…</div>';
  try {
    const complaint = await api(`/api/complaints/track?token=${encodeURIComponent(state.trackToken)}`);
    if (!output.isConnected) return;
    output.innerHTML = `<section class="card">${complaintSummary(complaint)}<div class="card-body"><h3>Recorded activity</h3><p class="page-explainer">Actual events in this session—not an inferred lifecycle.</p>${historyMarkup(complaint)}</div></section>`;
  } catch (error) {
    if (output.isConnected) output.innerHTML = `<div class="notice error" role="alert">${icon('info')}<span>${escapeHtml(error.message)}</span></div>`;
  }
}

async function openDetail(token) {
  const dialog = document.querySelector('#complaint-dialog');
  dialog.innerHTML = '<div class="loading-state">Loading complaint…</div>';
  if (!dialog.open) dialog.showModal();
  try {
    const complaint = await api(`/api/complaints/track?token=${encodeURIComponent(token)}`);
    if (!dialog.open) return;
    dialog.innerHTML = `<div class="dialog-header"><h3 id="detail-title">Complaint details</h3><button class="icon-button" data-close aria-label="Close complaint details">${icon('close')}</button></div>${complaintSummary(complaint)}<div class="card-body"><h3>Recorded activity</h3><div class="page-explainer">Updates recorded during this in-memory session.</div>${historyMarkup(complaint)}${complaint.allowedStatuses.length && state.health?.demoMode ? `<form class="review-form" id="review-form" data-token="${escapeHtml(token)}"><div class="notice amber">${icon('info')}<span>Local demo reviewer · not authenticated</span></div><div class="field"><label for="review-status">Next status</label><select id="review-status">${complaint.allowedStatuses.map(status => `<option value="${status}">${statusNames[status]}</option>`).join('')}</select></div><div class="field"><label for="review-remarks">Official remarks</label><textarea id="review-remarks" maxlength="1500" placeholder="Explain the decision. Rejections require at least 5 characters."></textarea></div><button class="button primary" type="submit">${icon('check')}Save status update</button><div id="review-error" class="form-error" role="alert"></div></form>` : `<div class="side-note"><strong>${complaint.allowedStatuses.length ? 'Reviewer actions disabled' : 'Final outcome recorded'}</strong>${complaint.allowedStatuses.length ? 'Start the local application with --demo to enable demonstration actions.' : 'Resolved and rejected complaints cannot be reopened in this demo workflow.'}</div>`}</div>`;
    dialog.querySelector('[data-close]').focus();
  } catch (error) {
    dialog.innerHTML = `<div class="dialog-header"><h3 id="detail-title">Unable to load complaint</h3><button class="icon-button" data-close aria-label="Close">${icon('close')}</button></div><div class="card-body">${escapeHtml(error.message)}</div>`;
  }
}

function render() {
  cleanup();
  cleanup = () => {};
  const route = location.hash.substring(1) || 'overview';
  const validRoute = Object.hasOwn(titles, route) ? route : 'overview';
  document.querySelector('#breadcrumb').textContent = titles[validRoute];
  document.title = `${titles[validRoute]} | CivicFlow Municipal Complaint Portal`;
  document.querySelectorAll('[data-route]').forEach(link => {
    link.classList.toggle('active', link.dataset.route === validRoute);
    if (link.dataset.route === validRoute) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  });
  if (!state.loaded && ['overview', 'complaints'].includes(validRoute)) {
    view.innerHTML = heading('Your workspace is getting ready.', 'Connecting to the local application…') + '<div class="skeleton-grid"><div class="skeleton"></div><div class="skeleton"></div><div class="skeleton"></div><div class="skeleton"></div></div>';
    return;
  }
  if (validRoute === 'devops' || validRoute === 'tasks') {
    const context = { icon, escapeHtml, heading, toast };
    cleanup = validRoute === 'devops' ? renderLab(view, context) : renderTasks(view, context);
  } else {
    view.innerHTML = ({ overview, complaints: complaintsPage, submit: submitPage, track: trackPage }[validRoute])();
    if (validRoute === 'complaints') updateTable();
    if (validRoute === 'overview') {
      document.querySelectorAll('[data-width]').forEach(element => { element.style.width = `${Number(element.dataset.width)}%`; });
      document.querySelectorAll('[data-color]').forEach(element => { element.style.backgroundColor = element.dataset.color; });
      const total = state.complaints.length;
      let previous = 0;
      const segments = statuses.map((status, index) => {
        const next = previous + (total ? state.complaints.filter(c => c.status === status).length / total * 100 : 0);
        const segment = `${colors[index]} ${previous}% ${next}%`;
        previous = next;
        return segment;
      });
      if (total) document.querySelector('#status-donut').style.background = `conic-gradient(${segments.join(',')})`;
    }
  }
}

window.addEventListener('hashchange', () => {
  document.querySelector('#sidebar').classList.remove('open');
  document.querySelector('#menu-toggle').setAttribute('aria-expanded', 'false');
  const refresh = ['overview', 'complaints'].includes(location.hash.substring(1) || 'overview');
  if (refresh) state.loaded = false;
  render();
  if (refresh) load();
  document.querySelector('#main').focus({ preventScroll: true });
  window.scrollTo(0, 0);
});

document.querySelector('#menu-toggle').addEventListener('click', event => {
  const open = document.querySelector('#sidebar').classList.toggle('open');
  event.currentTarget.setAttribute('aria-expanded', String(open));
});

document.querySelector('#global-search').addEventListener('submit', event => {
  event.preventDefault();
  state.query = document.querySelector('#global-query').value.trim();
  state.status = state.category = state.ward = '';
  state.page = 1;
  if (location.hash === '#complaints') render();
  else location.hash = 'complaints';
});

document.addEventListener('keydown', event => {
  if (event.key === '/' && !['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement.tagName) && !document.querySelector('dialog[open]')) {
    event.preventDefault();
    document.querySelector('#global-query').focus();
  }
  if (event.key === 'Escape') {
    document.querySelector('#sidebar').classList.remove('open');
    document.querySelector('#menu-toggle').setAttribute('aria-expanded', 'false');
  }
});

document.addEventListener('click', async event => {
  const detail = event.target.closest('[data-detail]');
  if (detail) openDetail(detail.dataset.detail);
  if (event.target.closest('[data-refresh]')) {
    const button = event.target.closest('[data-refresh]');
    button.disabled = true;
    await load();
    toast(state.error ? 'Connection still unavailable. Please check the local server.' : 'Workspace refreshed from the local service.');
  }
  const page = event.target.closest('[data-page]');
  if (page) { state.page += Number(page.dataset.page); updateTable(); }
  if (event.target.closest('[data-close]')) event.target.closest('dialog').close();
  const copy = event.target.closest('[data-copy]');
  if (copy) {
    try { await navigator.clipboard.writeText(copy.dataset.copy); toast('Tracking token copied.'); }
    catch { toast(`Copy this token: ${copy.dataset.copy}`); }
  }
  if (event.target.closest('#sample-token')) {
    document.querySelector('#track-token').value = 'CMP-2026-1001';
    track('CMP-2026-1001');
  }
  const image = event.target.closest('[data-image]');
  if (image) {
    const dialog = document.querySelector('#image-dialog');
    dialog.innerHTML = `<button class="icon-button" data-close aria-label="Close image preview">${icon('close')}</button><img src="${escapeHtml(image.dataset.image)}" alt="${escapeHtml(image.dataset.alt || 'Recorded evidence diagram')}">`;
    dialog.showModal();
  }
  if (event.target.id === 'complaint-dialog' || event.target.id === 'image-dialog') {
    const bounds = event.target.getBoundingClientRect();
    if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) event.target.close();
  }
});

view.addEventListener('input', event => {
  if (event.target.id === 'complaint-search') { state.query = event.target.value; state.page = 1; updateTable(); }
  if (event.target.id === 'desc') document.querySelector('#description-count').textContent = `${event.target.value.length} / 1500`;
  if (event.target.matches('[aria-invalid=true]')) event.target.removeAttribute('aria-invalid');
});

view.addEventListener('change', event => {
  if (event.target.id === 'complaint-search' && state.query !== event.target.value) {
    state.query = event.target.value; state.page = 1; updateTable();
  }
  const fields = { 'status-filter': 'status', 'category-filter': 'category', 'ward-filter': 'ward', 'sort-order': 'sort' };
  if (fields[event.target.id]) { state[fields[event.target.id]] = event.target.value; state.page = 1; updateTable(); }
});

view.addEventListener('submit', async event => {
  if (!['complaint-form', 'track-form'].includes(event.target.id)) return;
  event.preventDefault();
  if (event.target.id === 'track-form') { track(document.querySelector('#track-token').value); return; }
  const form = event.target;
  const output = document.querySelector('#sub-res');
  const fields = [...form.querySelectorAll('input,select,textarea')];
  const contact = document.querySelector('#contact');
  contact.setCustomValidity(/^[6-9]\d{9}$|^[A-Za-z0-9+_.-]+@[^@\s]+\.[^@\s]+$/.test(contact.value.trim()) ? '' : 'Use a valid phone or email.');
  fields.forEach(field => field.setAttribute('aria-invalid', String(!field.checkValidity())));
  if (!form.checkValidity()) {
    output.innerHTML = '<p class="form-error" role="alert">Validation error: Invalid complaint input data. Check the highlighted fields; the description needs at least 15 characters.</p>';
    form.querySelector('[aria-invalid=true]')?.focus();
    return;
  }
  const button = form.querySelector('button[type=submit]');
  button.disabled = true;
  output.textContent = 'Submitting your complaint…';
  try {
    const response = await api('/api/complaints', Object.fromEntries(new FormData(form)));
    state.trackToken = response.token;
    if (!output.isConnected) return;
    output.innerHTML = `<div class="receipt"><span class="pill green">SUCCESS · REGISTERED</span><h3>Your complaint has been received.</h3><p>Save your tracking token. It connects you to every recorded update.</p><div class="receipt-token">${escapeHtml(response.token)}</div><div class="receipt-actions"><button type="button" class="button small" data-copy="${escapeHtml(response.token)}">${icon('copy')}Copy token</button><a class="button small primary" href="#track">Track this complaint${icon('arrow')}</a></div></div>`;
    await load(false);
  } catch (error) {
    if (output.isConnected) output.innerHTML = `<p class="form-error" role="alert">Submission error: ${escapeHtml(error.message)}</p>`;
  } finally { button.disabled = false; }
});

document.querySelector('#complaint-dialog').addEventListener('submit', async event => {
  if (event.target.id !== 'review-form') return;
  event.preventDefault();
  const form = event.target;
  const button = form.querySelector('button[type=submit]');
  button.disabled = true;
  try {
    await api('/api/complaints/status', { token: form.dataset.token, status: document.querySelector('#review-status').value, remarks: document.querySelector('#review-remarks').value });
    await load(false);
    render();
    await openDetail(form.dataset.token);
    toast('Status updated. Dashboard and tracking now reflect the change.');
  } catch (error) {
    if (form.isConnected) document.querySelector('#review-error').textContent = error.message;
  } finally { button.disabled = false; }
});

render();
load(['overview', 'complaints'].includes(location.hash.substring(1) || 'overview'));
