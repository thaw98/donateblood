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
  
   // Set maximum date to today so future dates cannot be picked
  const dobInput = document.getElementById('dob');
  const today = new Date();
  const yyyy = today.getFullYear();
  const mm = String(today.getMonth() + 1).padStart(2, '0');
  const dd = String(today.getDate()).padStart(2, '0');
  dobInput.max = `${yyyy}-${mm}-${dd}`;