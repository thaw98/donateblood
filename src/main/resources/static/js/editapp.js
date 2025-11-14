
  // ✅ Date: today and future
  const dateInput = document.getElementById('date');
  const today = new Date();
  function formatDateForInput(d) {
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const da = String(d.getDate()).padStart(2, '0');
      return `${y}-${m}-${da}`;
  }
  dateInput.min = formatDateForInput(today);

  // ⏰ Time dropdown
  const timeDropdown = document.getElementById('time');
  const startHour = 9;
  const endHour = 18;
  for (let hour = startHour; hour <= endHour; hour++) {
      for (let min of [0, 30]) {
          if (hour === endHour && min > 0) break;
          let h12 = hour % 12 || 12;
          let ampm = hour < 12 ? 'AM' : 'PM';
          let mm = min.toString().padStart(2, '0');
          let display = `${h12}:${mm} ${ampm}`;
          let opt = document.createElement('option');
          opt.value = display;
          opt.textContent = display;
          if (display === /*[[${appointment.time}]]*/ "") opt.selected = true; // pre-select current
          timeDropdown.appendChild(opt);
      }
  }

  // ❌ Form validation
  document.getElementById('editAppointmentForm').addEventListener('submit', function(e) {
      let valid = true;
      const fields = [
          {input: dateInput, name: "Date"},
          {input: document.getElementById('time'), name: "Time"},
          {input: document.getElementById('bloodType'), name: "Blood Type"},
          {input: document.getElementById('hospital'), name: "Hospital"}
      ];

      fields.forEach(f => {
          const errorDiv = f.input.parentElement.querySelector('.invalid-feedback');
          if (!f.input.value) {
              errorDiv.textContent = `${f.name} is required`;
              errorDiv.style.display = 'block';
              f.input.classList.add('is-invalid');
              valid = false;
          } else {
              errorDiv.textContent = '';
              errorDiv.style.display = 'none';
              f.input.classList.remove('is-invalid');
          }
      });

      if (!valid) e.preventDefault();
  });

