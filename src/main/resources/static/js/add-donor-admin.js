<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8">
  <title>All Donors</title>
  <!-- Head -->
  <th:block th:replace="fragments/head :: head(${title})"></th:block>
  
  <!-- Bootstrap Icons CDN -->
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css">
  
  <style>
    /* Dashboard color variables */
    :root {
      --primary-color: #333;
      --secondary-color: #c62828;
      --success-color: #4cc9f0;
      --warning-color: #f72585;
      --info-color: #4895ef;
      --light-color: #f8f9fa;
      --dark-color: #212529;
    }
    
    /* Filter toolbar styling - compact */
    .filter-toolbar {
      background: white;
      padding: 1rem;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
      margin-bottom: 1rem;
    }
    
    .filter-toolbar .form-control,
    .filter-toolbar .form-select {
      border: 1px solid #e0e0e0;
      border-radius: 6px;
      padding: 0.4rem 0.6rem;
      font-size: 0.875rem;
      transition: all 0.3s ease;
    }
    
    .filter-toolbar .form-control:focus,
    .filter-toolbar .form-select:focus {
      border-color: var(--info-color);
      box-shadow: 0 0 0 0.15rem rgba(72, 149, 239, 0.1);
    }
    
    .filter-toolbar .form-label {
      font-size: 0.8rem;
      margin-bottom: 0.3rem;
      font-weight: 600;
    }
    
    .search-input-wrapper {
      position: relative;
    }
    
    .search-input-wrapper i {
      position: absolute;
      left: 10px;
      top: 50%;
      transform: translateY(-50%);
      color: var(--info-color);
      font-size: 0.85rem;
      z-index: 10;
    }
    
    .search-input-wrapper input {
      padding-left: 2rem;
    }
    
    .btn-clear {
      background: linear-gradient(135deg, var(--secondary-color) 0%, #b71c1c 100%);
      border: none;
      color: white;
      padding: 0.4rem 1rem;
      border-radius: 6px;
      font-weight: 600;
      font-size: 0.875rem;
      transition: all 0.3s ease;
    }
    
    .btn-clear:hover {
      transform: translateY(-1px);
      box-shadow: 0 3px 8px rgba(198, 40, 40, 0.3);
      background: linear-gradient(135deg, #b71c1c 0%, var(--secondary-color) 100%);
      color: white;
    }
    
    .match-count {
      background: var(--success-color);
      color: white;
      padding: 0.35rem 0.75rem;
      border-radius: 15px;
      font-weight: 600;
      font-size: 0.8rem;
    }
    
    /* Table card styling - compact */
    .table-card {
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
      overflow: hidden;
    }
    
    .table-card-header {
      background: linear-gradient(135deg, var(--primary-color) 0%, var(--dark-color) 100%);
      color: white;
      padding: 0.75rem 1rem;
      font-weight: 600;
      font-size: 0.95rem;
    }
    
    .table-card-header i {
      font-size: 1.1rem;
      margin-right: 0.4rem;
    }
    
    /* Compact table styling */
    .donor-table {
      margin-bottom: 0;
      font-size: 0.8rem;
    }
    
    .donor-table thead {
      background: var(--light-color);
      border-bottom: 2px solid var(--secondary-color);
    }
    
    .donor-table thead th {
      color: var(--dark-color);
      font-weight: 700;
      font-size: 0.75rem;
      text-transform: uppercase;
      letter-spacing: 0.3px;
      padding: 0.5rem 0.4rem;
      border: none;
      white-space: nowrap;
      vertical-align: middle;
    }
    
    .donor-table thead th i {
      color: var(--info-color);
      margin-right: 0.2rem;
      font-size: 0.85rem;
    }
    
    .donor-table tbody tr {
      transition: all 0.2s ease;
      border-bottom: 1px solid #e9ecef;
    }
    
    .donor-table tbody tr:hover {
      background-color: rgba(72, 149, 239, 0.05);
    }
    
    .donor-table tbody td {
      padding: 0.5rem 0.4rem;
      vertical-align: middle;
      color: var(--dark-color);
      font-size: 0.8rem;
    }
    
    /* Status badges - compact */
    .status-badge {
      padding: 0.2rem 0.5rem;
      border-radius: 12px;
      font-size: 0.7rem;
      font-weight: 600;
      text-transform: uppercase;
      display: inline-flex;
      align-items: center;
      gap: 0.2rem;
      white-space: nowrap;
    }
    
    .status-badge i {
      font-size: 0.7rem;
    }
    
    .status-available {
      background-color: rgba(76, 201, 240, 0.15);
      color: var(--success-color);
      border: 1px solid var(--success-color);
    }
    
    .status-used {
      background-color: rgba(247, 37, 133, 0.15);
      color: var(--warning-color);
      border: 1px solid var(--warning-color);
    }
    
    /* Blood type badge - compact */
    .blood-type-badge {
      background: linear-gradient(135deg, var(--secondary-color) 0%, #b71c1c 100%);
      color: white;
      padding: 0.2rem 0.5rem;
      border-radius: 5px;
      font-weight: 700;
      font-size: 0.75rem;
      display: inline-block;
      white-space: nowrap;
    }
    
    /* Action buttons - compact */
    .btn-action {
      background: var(--info-color);
      color: white;
      padding: 0.3rem 0.6rem;
      border-radius: 5px;
      text-decoration: none;
      font-weight: 600;
      font-size: 0.75rem;
      transition: all 0.2s ease;
      display: inline-flex;
      align-items: center;
      gap: 0.2rem;
      white-space: nowrap;
    }
    
    .btn-action:hover {
      background: #3a7bc8;
      color: white;
      transform: translateY(-1px);
      box-shadow: 0 2px 6px rgba(72, 149, 239, 0.3);
    }
    
    .btn-action i {
      font-size: 0.75rem;
    }
    
    /* No results row */
    .no-results {
      text-align: center;
      padding: 2rem;
      color: #999;
      font-size: 0.95rem;
    }
    
    .no-results i {
      font-size: 2rem;
      color: #ddd;
      margin-bottom: 0.5rem;
      display: block;
    }
    
    /* Row number badge - compact */
    .row-number {
      background: var(--light-color);
      color: var(--dark-color);
      padding: 0.2rem 0.4rem;
      border-radius: 4px;
      font-weight: 700;
      font-size: 0.75rem;
    }
    
    /* Page header - compact */
    .page-header-icon {
      color: var(--secondary-color);
      font-size: 1.5rem;
      margin-right: 0.4rem;
    }
    
    .page-header h2 {
      font-size: 1.5rem;
    }
    
    .page-header p {
      font-size: 0.85rem;
    }
    
    /* Column width optimization */
    .donor-table th:nth-child(1),
    .donor-table td:nth-child(1) { width: 4%; }
    
    .donor-table th:nth-child(2),
    .donor-table td:nth-child(2) { width: 10%; }
    
    .donor-table th:nth-child(3),
    .donor-table td:nth-child(3) { width: 7%; }
    
    .donor-table th:nth-child(4),
    .donor-table td:nth-child(4) { width: 9%; }
    
    .donor-table th:nth-child(5),
    .donor-table td:nth-child(5) { width: 15%; }
    
    .donor-table th:nth-child(6),
    .donor-table td:nth-child(6) { width: 10%; }
    
    .donor-table th:nth-child(7),
    .donor-table td:nth-child(7) { width: 6%; }
    
    .donor-table th:nth-child(8),
    .donor-table td:nth-child(8) { width: 20%; }
    
    .donor-table th:nth-child(9),
    .donor-table td:nth-child(9) { width: 8%; }
    
    .donor-table th:nth-child(10),
    .donor-table td:nth-child(10) { width: 7%; }
    
    /* Icon size adjustments */
    .donor-table .bi {
      font-size: 0.8rem;
    }
    
    /* Responsive adjustments */
    @media (max-width: 1200px) {
      .donor-table {
        font-size: 0.75rem;
      }
      
      .donor-table thead th,
      .donor-table tbody td {
        padding: 0.4rem 0.3rem;
      }
    }
  </style>
</head>
<body>
  <!-- Sidebar -->
  <div th:replace="fragments/sidebar :: sidebar(${active})"></div>

  <!-- Main Content -->
  <div id="content">
    <!-- Top nav -->
    <div th:replace="~{fragments/topnav :: topnav(${userName}, ${avatarUrl})}"></div>

    <!-- Page content -->
    <main class="container-fluid p-4">
      
      <!-- Page Header - compact -->
      <div class="mb-3 page-header">
        <div class="d-flex align-items-center">
          <i class="bi bi-people-fill page-header-icon"></i>
          <div>
            <h2 class="mb-0 fw-bold" style="color: var(--primary-color);">All Donors</h2>
            <p class="text-muted mb-0">
              <i class="bi bi-arrow-right-short"></i>Manage and filter donor records
            </p>
          </div>
        </div>
      </div>

      <!-- Filter Toolbar - compact -->
      <div class="filter-toolbar">
        <div class="row align-items-end g-2">
          <!-- Search Input -->
          <div class="col-lg-4 col-md-6">
            <label class="form-label" style="color: var(--dark-color);">
              <i class="bi bi-search text-info"></i> Search
            </label>
            <div class="search-input-wrapper">
              <i class="bi bi-search"></i>
              <input id="q" 
                     type="search" 
                     class="form-control" 
                     placeholder="Name or email…" 
                     autocomplete="off">
            </div>
          </div>

          <!-- Blood Type Filter -->
          <div class="col-lg-2 col-md-6">
            <label class="form-label" style="color: var(--dark-color);">
              <i class="bi bi-droplet-fill text-danger"></i> Blood
            </label>
            <select id="btFilter" class="form-select">
              <option value="">All</option>
              <option th:each="e : ${bloodTypeMap}"
                      th:value="${e.key}"
                      th:text="${e.value}">
              </option>
            </select>
          </div>

          <!-- Status Filter -->
          <div class="col-lg-2 col-md-6">
            <label class="form-label" style="color: var(--dark-color);">
              <i class="bi bi-activity text-info"></i> Status
            </label>
            <select id="statusFilter" class="form-select">
              <option value="">All</option>
              <option value="available">Available</option>
              <option value="used">Used</option>
            </select>
          </div>

          <!-- Clear Button -->
          <div class="col-lg-2 col-md-6">
            <button type="button" id="clearFilters" class="btn btn-clear w-100">
              <i class="bi bi-x-circle me-1"></i> Clear
            </button>
          </div>

          <!-- Match Count -->
          <div class="col-lg-2 col-md-6 text-end">
            <span id="matchCount" class="match-count"></span>
          </div>
        </div>
      </div>

      <!-- Table Card - compact -->
      <div class="table-card">
        <div class="table-card-header">
          <i class="bi bi-table"></i>
          <span>Donor Records</span>
        </div>
        
        <div class="table-responsive">
          <table id="donorTable" class="table table-sm donor-table">
            <thead>
              <tr>
                <th><i class="bi bi-hash"></i>No.</th>
                <th><i class="bi bi-person-badge"></i>Username</th>
                <th><i class="bi bi-droplet-fill"></i>Blood</th>
                <th><i class="bi bi-calendar-event"></i>DOB</th>
                <th><i class="bi bi-envelope"></i>Email</th>
                <th><i class="bi bi-telephone"></i>Phone</th>
                <th><i class="bi bi-gender-ambiguous"></i>Gender</th>
                <th><i class="bi bi-geo-alt"></i>Address</th>
                <th><i class="bi bi-activity"></i>Status</th>
                <th><i class="bi bi-pencil-square"></i>Action</th>
              </tr>
            </thead>
            <tbody>
              <tr th:each="d, no : ${donorList}"
                  th:data-name="${d.username}"
                  th:data-email="${d.email}"
                  th:data-btid="${d.bloodTypeId}"
                  th:data-bt="${d.bloodTypeId != null ? bloodTypeMap[d.bloodTypeId] : null}"
                  th:data-status="${d.status}">
                
                <td>
                  <span class="row-number" th:text="${no.count}"></span>
                </td>
                <td>
                  <strong th:text="${d.username}"></strong>
                </td>
                <td>
                  <span class="blood-type-badge" 
                        th:text="${d.bloodTypeId != null ? bloodTypeMap[d.bloodTypeId] : '?'}"></span>
                </td>
                <td th:text="${d.dateOfBirth}"></td>
                <td th:text="${d.email}"></td>
                <td th:text="${d.phone}"></td>
                <td>
                  <i th:classappend="${d.gender == 'Male' ? 'bi bi-gender-male text-info' : 'bi bi-gender-female text-danger'}"></i>
                  <span th:text="${d.gender}"></span>
                </td>
                <td th:text="${d.address}"></td>
                <td>
                  <span th:classappend="${d.status == 'available' ? 'status-badge status-available' : 'status-badge status-used'}">
                    <i th:classappend="${d.status == 'available' ? 'bi bi-check-circle-fill' : 'bi bi-x-circle-fill'}"></i>
                    <span th:text="${d.status}"></span>
                  </span>
                </td>
                <td>
                  <a th:href="@{/admin/donors/edit/{id}(id=${d.id})}" class="btn-action">
                    <i class="bi bi-pencil-fill"></i>Edit
                  </a>
                </td>
              </tr>

              <tr id="noRows" style="display:none;">
                <td colspan="10" class="no-results">
                  <i class="bi bi-inbox"></i>
                  <div>No matching donors found</div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

    </main>
  </div>

  <!-- JS loaded directly -->
  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
  <script th:src="@{/js/dashboard.js}"></script>

  <script>
  (function () {
    const $  = (s, c=document) => c.querySelector(s);
    const $$ = (s, c=document) => Array.from(c.querySelectorAll(s));

    const qInput   = $('#q');
    const btSelect = $('#btFilter');
    const stSelect = $('#statusFilter');
    const clearBtn = $('#clearFilters');
    const countEl  = $('#matchCount');
    const noRows   = $('#noRows');

    const rows = $$('#donorTable tr').filter(tr =>
      tr.rowIndex !== 0 && tr.id !== 'noRows'
    );

    const normalize = s => (s || '').toString().trim().toLowerCase();

    function matches(row, q, bt, st) {
      const name  = normalize(row.dataset.name);
      const email = normalize(row.dataset.email);
      const text  = name + ' ' + email;

      const okSearch = !q || text.includes(q);

      const rowBtId = (row.dataset.btid || '').toString();
      const okBt = !bt || rowBtId === bt;

      const rowSt = normalize(row.dataset.status);
      const okSt = !st || rowSt === st;

      return okSearch && okBt && okSt;
    }

    function debounce(fn, ms) {
      let t; return function () { clearTimeout(t); t = setTimeout(() => fn.apply(this, arguments), ms); };
    }

    function applyFilters() {
      const q  = normalize(qInput?.value);
      const bt = btSelect?.value || '';
      const st = normalize(stSelect?.value || '');
      let visible = 0;

      rows.forEach(tr => {
        if (matches(tr, q, bt, st)) {
          tr.style.display = '';
          visible++;
        } else {
          tr.style.display = 'none';
        }
      });

      if (countEl) {
        countEl.textContent = visible ? `${visible} match${visible > 1 ? 'es' : ''}` : '';
        countEl.style.display = visible ? 'inline-block' : 'none';
      }
      if (noRows) noRows.style.display = visible ? 'none' : '';
    }

    const update = debounce(applyFilters, 120);

    if (qInput)   qInput.addEventListener('input', update);
    if (btSelect) btSelect.addEventListener('change', applyFilters);
    if (stSelect) stSelect.addEventListener('change', applyFilters);
    if (clearBtn) clearBtn.addEventListener('click', () => {
      if (qInput)   qInput.value = '';
      if (btSelect) btSelect.value = '';
      if (stSelect) stSelect.value = '';
      applyFilters();
      if (qInput) qInput.focus();
    });

    applyFilters();
  })();
  </script>

</body>
</html>
