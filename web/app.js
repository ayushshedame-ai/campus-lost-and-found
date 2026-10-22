/**
 * Campus Lost & Found Management System - Frontend Controller
 * 
 * Written in clean, modern vanilla JavaScript (no npm/build tools needed).
 * Connects directly to the Java REST API backend.
 */

// Global State
const API_BASE = window.location.origin.includes('http') ? window.location.origin : 'http://localhost:8080';
let currentItems = [];
let activeModalItem = null;

// =============================================================================
// Initialization
// =============================================================================
document.addEventListener('DOMContentLoaded', () => {
  console.log('🚀 Campus Lost & Found Web Portal initialized.');
  
  // Set default dates in forms to today (YYYY-MM-DD)
  const today = new Date().toISOString().split('T')[0];
  const lostDateInput = document.getElementById('lostDate');
  const foundDateInput = document.getElementById('foundDate');
  if (lostDateInput) lostDateInput.value = today;
  if (foundDateInput) foundDateInput.value = today;

  // Initialize event listeners
  setupTabNavigation();
  setupFiltersAndSearch();
  setupForms();
  setupModals();
  setupNavbarActions();

  // Load initial data from the backend
  refreshAllData();
});

// =============================================================================
// API Service Methods (Backend Communication via fetch)
// =============================================================================

async function apiGet(endpoint) {
  try {
    const res = await fetch(`${API_BASE}${endpoint}`);
    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      throw new Error(errData.error || `HTTP error ${res.status}`);
    }
    return await res.json();
  } catch (err) {
    console.error(`API GET error on ${endpoint}:`, err);
    throw err;
  }
}

async function apiPost(endpoint, bodyData) {
  try {
    const res = await fetch(`${API_BASE}${endpoint}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(bodyData)
    });
    const data = await res.json();
    if (!res.ok || data.success === false) {
      throw new Error(data.error || `HTTP error ${res.status}`);
    }
    return data;
  } catch (err) {
    console.error(`API POST error on ${endpoint}:`, err);
    throw err;
  }
}

// =============================================================================
// Data Loading & Dashboard UI
// =============================================================================

async function refreshAllData() {
  await Promise.all([
    loadStats(),
    loadItems()
  ]);
  populateMatchDropdown();
  populateClaimDropdown();
}

/** Fetches and updates the top stat cards */
async function loadStats() {
  try {
    const stats = await apiGet('/api/stats');
    document.getElementById('statTotalLost').textContent = stats.totalLost;
    document.getElementById('statOpenLost').textContent = `${stats.openLost} Open reports`;

    document.getElementById('statTotalFound').textContent = stats.totalFound;
    document.getElementById('statOpenFound').textContent = `${stats.openFound} Open reports`;

    document.getElementById('statMatched').textContent = (stats.matchedLost + stats.matchedFound) / 2;
    document.getElementById('statClaimed').textContent = (stats.claimedLost + stats.claimedFound) / 2;

    // Server connected badge
    const badge = document.getElementById('serverStatus');
    badge.classList.remove('offline');
    badge.querySelector('.status-text').textContent = 'Backend Connected';
  } catch (err) {
    const badge = document.getElementById('serverStatus');
    badge.classList.add('offline');
    badge.querySelector('.status-text').textContent = 'Backend Disconnected';
  }
}

/** Loads items based on current search & filter selections */
async function loadItems() {
  const container = document.getElementById('itemsContainer');
  const countText = document.getElementById('itemsCountText');

  try {
    const type = document.getElementById('filterType').value;
    const category = document.getElementById('filterCategory').value;
    const status = document.getElementById('filterStatus').value;
    const q = document.getElementById('searchInput').value.trim();

    let queryParams = [];
    if (type !== 'ALL') queryParams.push(`type=${encodeURIComponent(type)}`);
    if (category !== 'ALL') queryParams.push(`category=${encodeURIComponent(category)}`);
    if (status !== 'ALL') queryParams.push(`status=${encodeURIComponent(status)}`);
    if (q) queryParams.push(`q=${encodeURIComponent(q)}`);

    const queryString = queryParams.length ? `?${queryParams.join('&')}` : '';
    const items = await apiGet(`/api/items${queryString}`);
    currentItems = items;

    countText.textContent = `Showing ${items.length} item${items.length === 1 ? '' : 's'}`;
    renderItems(items);
  } catch (err) {
    container.innerHTML = `
      <div class="empty-state" style="grid-column: 1 / -1;">
        <span class="empty-icon">⚠️</span>
        <h3>Could not load items</h3>
        <p>${err.message}</p>
        <button class="btn btn-secondary btn-sm" onclick="loadItems()" style="margin-top:1rem;">Retry</button>
      </div>
    `;
    countText.textContent = 'Error loading items';
  }
}

/** Renders item cards in the grid */
function renderItems(items) {
  const container = document.getElementById('itemsContainer');
  if (!items || items.length === 0) {
    container.innerHTML = `
      <div class="empty-state" style="grid-column: 1 / -1;">
        <span class="empty-icon">📭</span>
        <h3>No items found</h3>
        <p>Try clearing your search query or filters, or report a new lost/found item.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = items.map(item => {
    const isLost = item.type === 'LOST';
    const typeBadgeClass = isLost ? 'badge-lost' : 'badge-found';
    const typeIcon = isLost ? '🔍 LOST' : '🎁 FOUND';
    const statusClass = `status-${item.status}`;

    return `
      <div class="item-card" data-id="${item.id}">
        <div>
          <div class="item-card-top">
            <div class="card-badges">
              <span class="badge ${typeBadgeClass}">${typeIcon}</span>
              <span class="badge badge-category">${escapeHtml(item.categoryLabel || item.category)}</span>
            </div>
            <span class="status-pill ${statusClass}">${escapeHtml(item.statusLabel || item.status)}</span>
          </div>

          <h3 class="item-title">${escapeHtml(item.name)} <span style="font-size:0.75rem; color:#94a3b8; font-weight:600;">#${item.id}</span></h3>
          <p class="item-desc">${escapeHtml(item.description)}</p>
        </div>

        <div>
          <div class="item-meta">
            <div class="meta-row">
              <span class="meta-icon">📍</span>
              <span><strong>Location:</strong> ${escapeHtml(item.location)}</span>
            </div>
            <div class="meta-row">
              <span class="meta-icon">📅</span>
              <span><strong>Date:</strong> ${item.date}</span>
            </div>
            ${item.storedAt ? `
              <div class="meta-row">
                <span class="meta-icon">🔒</span>
                <span><strong>Stored At:</strong> ${escapeHtml(item.storedAt)}</span>
              </div>
            ` : ''}
            ${item.matchedWithId ? `
              <div class="meta-row">
                <span class="meta-icon">🔗</span>
                <span><strong>Matched with:</strong> ${escapeHtml(item.matchedWithId)}</span>
              </div>
            ` : ''}
          </div>

          <div class="item-actions">
            <button class="btn btn-outline btn-sm" onclick="openDetailsModal('${item.id}')">
              Details
            </button>
            ${item.status === 'OPEN' ? `
              <button class="btn btn-primary btn-sm" onclick="startMatchFor('${item.id}')">
                Find Matches ⚡
              </button>
            ` : ''}
          </div>
        </div>
      </div>
    `;
  }).join('');
}

// =============================================================================
// Form Handling (Reporting Lost / Found)
// =============================================================================

function setupForms() {
  // Report Lost Form
  const lostForm = document.getElementById('lostForm');
  if (lostForm) {
    lostForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const submitBtn = document.getElementById('btnSubmitLost');
      submitBtn.disabled = true;
      submitBtn.textContent = 'Saving...';

      const payload = {
        name: document.getElementById('lostName').value.trim(),
        category: document.getElementById('lostCategory').value,
        location: document.getElementById('lostLocation').value.trim(),
        dateLost: document.getElementById('lostDate').value,
        description: document.getElementById('lostDescription').value.trim(),
        personName: document.getElementById('lostPersonName').value.trim(),
        studentId: document.getElementById('lostStudentId').value.trim(),
        phone: document.getElementById('lostPhone').value.trim(),
        email: document.getElementById('lostEmail').value.trim()
      };

      try {
        const result = await apiPost('/api/lost', payload);
        showToast(`Lost report created successfully with ID: ${result.id}`, 'success');
        lostForm.reset();
        document.getElementById('lostDate').value = new Date().toISOString().split('T')[0];
        await refreshAllData();
        switchToTab('browseTab');
      } catch (err) {
        showToast(`Error: ${err.message}`, 'error');
      } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Submit Lost Report';
      }
    });
  }

  // Report Found Form
  const foundForm = document.getElementById('foundForm');
  if (foundForm) {
    foundForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const submitBtn = document.getElementById('btnSubmitFound');
      submitBtn.disabled = true;
      submitBtn.textContent = 'Saving...';

      const payload = {
        name: document.getElementById('foundName').value.trim(),
        category: document.getElementById('foundCategory').value,
        location: document.getElementById('foundLocation').value.trim(),
        dateFound: document.getElementById('foundDate').value,
        storedAt: document.getElementById('foundStoredAt').value.trim(),
        description: document.getElementById('foundDescription').value.trim(),
        personName: document.getElementById('foundPersonName').value.trim(),
        studentId: document.getElementById('foundStudentId').value.trim(),
        phone: document.getElementById('foundPhone').value.trim(),
        email: document.getElementById('foundEmail').value.trim()
      };

      try {
        const result = await apiPost('/api/found', payload);
        showToast(`Found report created successfully with ID: ${result.id}`, 'success');
        foundForm.reset();
        document.getElementById('foundDate').value = new Date().toISOString().split('T')[0];
        await refreshAllData();
        switchToTab('browseTab');
      } catch (err) {
        showToast(`Error: ${err.message}`, 'error');
      } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Submit Found Report';
      }
    });
  }

  // Claim Desk Form
  const claimForm = document.getElementById('claimForm');
  if (claimForm) {
    claimForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const lostId = document.getElementById('claimLostSelect').value;
      const studentId = document.getElementById('claimStudentId').value.trim();
      const submitBtn = document.getElementById('btnSubmitClaim');

      if (!lostId) {
        showToast('Please choose a matched lost item.', 'error');
        return;
      }

      submitBtn.disabled = true;
      submitBtn.textContent = 'Verifying...';

      try {
        const result = await apiPost('/api/claim', { lostId, studentId });
        showToast(result.message || 'Item claimed successfully!', 'success');
        claimForm.reset();
        await refreshAllData();
      } catch (err) {
        showToast(err.message, 'error');
      } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Verify & Return Item';
      }
    });
  }
}

// =============================================================================
// Smart Match Finder Engine
// =============================================================================

/** Fills dropdown with Open items */
async function populateMatchDropdown() {
  const select = document.getElementById('matchItemSelect');
  if (!select) return;

  const prevVal = select.value;
  select.innerHTML = '<option value="">-- Choose an open item to compare --</option>';

  const openItems = currentItems.filter(item => item.status === 'OPEN');
  
  const optGroupLost = document.createElement('optgroup');
  optGroupLost.label = 'Lost Items (Open)';
  const optGroupFound = document.createElement('optgroup');
  optGroupFound.label = 'Found Items (Open)';

  openItems.forEach(item => {
    const opt = document.createElement('option');
    opt.value = item.id;
    opt.textContent = `[${item.id}] ${item.name} (${item.location})`;
    if (item.type === 'LOST') {
      optGroupLost.appendChild(opt);
    } else {
      optGroupFound.appendChild(opt);
    }
  });

  if (optGroupLost.children.length) select.appendChild(optGroupLost);
  if (optGroupFound.children.length) select.appendChild(optGroupFound);

  if (prevVal) select.value = prevVal;
}

/** Pre-selects an item and opens the match tab */
function startMatchFor(itemId) {
  switchToTab('matchFinderTab');
  const select = document.getElementById('matchItemSelect');
  select.value = itemId;
  runMatchComparison(itemId);
}

async function runMatchComparison(itemId) {
  const resultsArea = document.getElementById('matchResultsArea');
  if (!itemId) {
    resultsArea.innerHTML = `
      <div class="empty-state">
        <span class="empty-icon">💡</span>
        <h3>Please select an item</h3>
        <p>Pick an open lost or found item from the dropdown to compare.</p>
      </div>
    `;
    return;
  }

  resultsArea.innerHTML = `
    <div class="empty-state">
      <span class="empty-icon">⏳</span>
      <h3>Analyzing Matches...</h3>
      <p>Running scoring algorithm (Category, Keywords, Location, Date)...</p>
    </div>
  `;

  try {
    const matches = await apiGet(`/api/matches?id=${encodeURIComponent(itemId)}`);
    const sourceItem = currentItems.find(i => i.id === itemId);

    if (!matches || matches.length === 0) {
      resultsArea.innerHTML = `
        <div class="empty-state">
          <span class="empty-icon">🔍</span>
          <h3>No confident matches found yet</h3>
          <p>No open opposite reports scored 50% or higher. When a new report is added that shares the category, location, or keywords, it will appear here.</p>
        </div>
      `;
      return;
    }

    resultsArea.innerHTML = `
      <div style="margin-bottom:1rem; font-weight:600; color:var(--text-main);">
        Found ${matches.length} candidate match${matches.length === 1 ? '' : 'es'} for <strong>${escapeHtml(sourceItem ? sourceItem.name : itemId)}</strong> (${itemId}):
      </div>
      ${matches.map(m => {
        const isSourceLost = sourceItem && sourceItem.type === 'LOST';
        const candidate = isSourceLost ? m.foundItem : m.lostItem;
        const lostId = m.lostItem.id;
        const foundId = m.foundItem.id;

        return `
          <div class="match-card">
            <div class="match-score-row">
              <div>
                <span class="badge ${candidate.type === 'LOST' ? 'badge-lost' : 'badge-found'}">
                  ${candidate.type === 'LOST' ? 'LOST REPORT' : 'FOUND REPORT'}
                </span>
                <strong style="margin-left:0.5rem; font-size:1.05rem;">${escapeHtml(candidate.name)}</strong>
                <span style="font-size:0.8rem; color:var(--text-muted);">#${candidate.id}</span>
              </div>
              <div style="display:flex; align-items:center; gap:0.75rem;">
                <div class="match-percent">${m.score}%</div>
              </div>
            </div>

            <div class="match-bar-bg">
              <div class="match-bar-fill" style="width: ${m.score}%;"></div>
            </div>

            <div class="match-details-grid">
              <div>
                <strong>Candidate Details:</strong>
                <div style="color:var(--text-muted); margin-top:0.25rem;">
                  <div>📂 <strong>Category:</strong> ${escapeHtml(candidate.categoryLabel || candidate.category)}</div>
                  <div>📍 <strong>Location:</strong> ${escapeHtml(candidate.location)}</div>
                  <div>📅 <strong>Date:</strong> ${candidate.date}</div>
                  ${candidate.storedAt ? `<div>🔒 <strong>Stored At:</strong> ${escapeHtml(candidate.storedAt)}</div>` : ''}
                </div>
              </div>
              <div>
                <strong>Description:</strong>
                <p style="color:var(--text-muted); margin-top:0.25rem;">${escapeHtml(candidate.description)}</p>
              </div>
            </div>

            <div style="display:flex; justify-content:flex-end; gap:0.5rem; margin-top:0.5rem;">
              <button class="btn btn-outline btn-sm" onclick="openDetailsModal('${candidate.id}')">View Full Details</button>
              <button class="btn btn-success btn-sm" onclick="confirmMatch('${lostId}', '${foundId}')">
                ✅ Confirm Match
              </button>
            </div>
          </div>
        `;
      }).join('')}
    `;
  } catch (err) {
    resultsArea.innerHTML = `
      <div class="empty-state">
        <span class="empty-icon">⚠️</span>
        <h3>Error comparing matches</h3>
        <p>${err.message}</p>
      </div>
    `;
  }
}

async function confirmMatch(lostId, foundId) {
  if (!confirm(`Confirm match between Lost #${lostId} and Found #${foundId}?`)) {
    return;
  }

  try {
    const res = await apiPost('/api/matches/confirm', { lostId, foundId });
    showToast(res.message || 'Match confirmed!', 'success');
    await refreshAllData();
    runMatchComparison(lostId);
  } catch (err) {
    showToast(`Failed to confirm match: ${err.message}`, 'error');
  }
}

// =============================================================================
// Claim & Return Dropdown
// =============================================================================

function populateClaimDropdown() {
  const select = document.getElementById('claimLostSelect');
  if (!select) return;

  select.innerHTML = '<option value="">-- Choose a matched item --</option>';
  const matchedLost = currentItems.filter(i => i.type === 'LOST' && i.status === 'MATCHED');

  matchedLost.forEach(item => {
    const opt = document.createElement('option');
    opt.value = item.id;
    opt.textContent = `[${item.id}] ${item.name} (Matched with ${item.matchedWithId || 'found item'})`;
    select.appendChild(opt);
  });
}

// =============================================================================
// Item Details Modal
// =============================================================================

function setupModals() {
  const modal = document.getElementById('detailsModal');
  const closeBtn = document.getElementById('modalCloseBtn');
  const closeBtn2 = document.getElementById('modalCloseBtn2');
  const backdrop = document.getElementById('modalBackdrop');
  const matchBtn = document.getElementById('modalMatchBtn');

  const closeModal = () => {
    modal.classList.remove('active');
    activeModalItem = null;
  };

  if (closeBtn) closeBtn.addEventListener('click', closeModal);
  if (closeBtn2) closeBtn2.addEventListener('click', closeModal);
  if (backdrop) backdrop.addEventListener('click', closeModal);

  if (matchBtn) {
    matchBtn.addEventListener('click', () => {
      if (activeModalItem) {
        closeModal();
        startMatchFor(activeModalItem.id);
      }
    });
  }
}

async function openDetailsModal(itemId) {
  const modal = document.getElementById('detailsModal');
  const title = document.getElementById('modalTitle');
  const body = document.getElementById('modalBody');
  const matchBtn = document.getElementById('modalMatchBtn');

  title.textContent = `Item #${itemId}`;
  body.innerHTML = '<div style="text-align:center; padding:1.5rem;">Loading details...</div>';
  modal.classList.add('active');

  try {
    const item = await apiGet(`/api/items/${encodeURIComponent(itemId)}`);
    activeModalItem = item;
    title.textContent = `${item.name} (${item.id})`;

    if (item.status === 'OPEN') {
      matchBtn.style.display = 'inline-flex';
    } else {
      matchBtn.style.display = 'none';
    }

    body.innerHTML = `
      <div class="modal-detail-row">
        <span class="modal-detail-label">Type:</span>
        <span class="modal-detail-val"><strong>${item.type}</strong></span>
      </div>
      <div class="modal-detail-row">
        <span class="modal-detail-label">Status:</span>
        <span class="modal-detail-val"><span class="status-pill status-${item.status}">${item.statusLabel || item.status}</span></span>
      </div>
      <div class="modal-detail-row">
        <span class="modal-detail-label">Category:</span>
        <span class="modal-detail-val">${escapeHtml(item.categoryLabel || item.category)}</span>
      </div>
      <div class="modal-detail-row">
        <span class="modal-detail-label">Reported Date:</span>
        <span class="modal-detail-val">${item.date}</span>
      </div>
      <div class="modal-detail-row">
        <span class="modal-detail-label">Location:</span>
        <span class="modal-detail-val">${escapeHtml(item.location)}</span>
      </div>
      ${item.storedAt ? `
        <div class="modal-detail-row">
          <span class="modal-detail-label">Stored At:</span>
          <span class="modal-detail-val">${escapeHtml(item.storedAt)}</span>
        </div>
      ` : ''}
      ${item.matchedWithId ? `
        <div class="modal-detail-row">
          <span class="modal-detail-label">Matched With:</span>
          <span class="modal-detail-val">${escapeHtml(item.matchedWithId)}</span>
        </div>
      ` : ''}

      <div style="margin-top:0.75rem;">
        <strong>Description:</strong>
        <p style="color:var(--text-muted); margin-top:0.25rem;">${escapeHtml(item.description)}</p>
      </div>

      <div style="background:var(--bg-page); padding:0.85rem; border-radius:var(--radius-md); margin-top:0.75rem;">
        <strong>${item.contactRole || 'Contact'} Information:</strong>
        <div style="font-size:0.85rem; margin-top:0.35rem; color:var(--text-muted);">
          <div>👤 <strong>Name:</strong> ${escapeHtml(item.contact ? item.contact.name : 'N/A')}</div>
          <div>🆔 <strong>Student/Staff ID:</strong> ${escapeHtml(item.contact ? item.contact.studentId : 'N/A')}</div>
          <div>📞 <strong>Phone:</strong> ${escapeHtml(item.contact ? item.contact.phone : 'N/A')}</div>
          <div>✉️ <strong>Email:</strong> ${escapeHtml(item.contact ? item.contact.email : 'N/A')}</div>
        </div>
      </div>
    `;
  } catch (err) {
    body.innerHTML = `<div style="color:var(--danger); padding:1rem;">Failed to load item: ${err.message}</div>`;
  }
}

// =============================================================================
// Tab & Filter Setup
// =============================================================================

function setupTabNavigation() {
  const tabs = document.querySelectorAll('.tab-btn');
  tabs.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-tab');
      switchToTab(targetId);
    });
  });
}

function switchToTab(tabId) {
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-tab') === tabId);
  });
  document.querySelectorAll('.tab-panel').forEach(panel => {
    panel.classList.toggle('active', panel.id === tabId);
  });
}

function setupFiltersAndSearch() {
  const searchInput = document.getElementById('searchInput');
  const clearBtn = document.getElementById('searchClearBtn');
  const filterType = document.getElementById('filterType');
  const filterCategory = document.getElementById('filterCategory');
  const filterStatus = document.getElementById('filterStatus');
  const refreshBtn = document.getElementById('btnRefreshItems');

  let debounceTimer;
  searchInput.addEventListener('input', () => {
    clearBtn.style.display = searchInput.value ? 'block' : 'none';
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(loadItems, 300);
  });

  clearBtn.addEventListener('click', () => {
    searchInput.value = '';
    clearBtn.style.display = 'none';
    loadItems();
  });

  [filterType, filterCategory, filterStatus].forEach(el => {
    el.addEventListener('change', loadItems);
  });

  if (refreshBtn) {
    refreshBtn.addEventListener('click', refreshAllData);
  }

  // Match tab button
  const runMatchBtn = document.getElementById('btnRunMatch');
  if (runMatchBtn) {
    runMatchBtn.addEventListener('click', () => {
      const select = document.getElementById('matchItemSelect');
      runMatchComparison(select.value);
    });
  }
}

function setupNavbarActions() {
  // Demo Data button
  const btnDemo = document.getElementById('btnLoadDemo');
  if (btnDemo) {
    btnDemo.addEventListener('click', async () => {
      btnDemo.disabled = true;
      btnDemo.textContent = 'Loading...';
      try {
        const res = await apiPost('/api/demo', {});
        showToast(res.message || 'Sample data loaded!', 'success');
        await refreshAllData();
      } catch (err) {
        showToast(`Demo load error: ${err.message}`, 'error');
      } finally {
        btnDemo.disabled = false;
        btnDemo.textContent = '⚡ Load Sample Data';
      }
    });
  }

  // Export Report button
  const btnExport = document.getElementById('btnExportReport');
  if (btnExport) {
    btnExport.addEventListener('click', async () => {
      btnExport.disabled = true;
      btnExport.textContent = 'Exporting...';
      try {
        const res = await apiGet('/api/export');
        showToast(res.message || 'Report generated successfully!', 'success');
      } catch (err) {
        showToast(`Export error: ${err.message}`, 'error');
      } finally {
        btnExport.disabled = false;
        btnExport.textContent = '📄 Export Report';
      }
    });
  }
}

// =============================================================================
// Helper Functions (Toasts, Escaping)
// =============================================================================

function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  
  const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
  toast.innerHTML = `<span>${icon}</span><span>${escapeHtml(message)}</span>`;

  container.appendChild(toast);
  setTimeout(() => {
    toast.style.transition = 'opacity 0.3s ease';
    toast.style.opacity = '0';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

function escapeHtml(str) {
  if (!str) return '';
  return str.toString()
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
