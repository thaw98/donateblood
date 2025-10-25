const links = document.querySelectorAll('[data-section]');
const container = document.getElementById('section-container');

// -------------------- INITIAL LOAD --------------------
window.addEventListener('DOMContentLoaded', () => {
    const userRole = document.body.getAttribute('data-role'); // 'donor' or 'receiver'

    // Determine default section
    let defaultSection = 'my-appointments'; // donor default
    if (userRole && userRole.toLowerCase() === 'receiver') defaultSection = 'blood-request';

    loadSection(defaultSection);

    // Highlight active sidebar link
    links.forEach(l => l.classList.remove('active'));
    const defaultLink = document.querySelector(`[data-section="${defaultSection}"]`);
    if (defaultLink) defaultLink.classList.add('active');
});

// -------------------- SIDEBAR CLICK --------------------
links.forEach(link => {
    link.addEventListener('click', () => {
        const section = link.getAttribute('data-section');
        links.forEach(l => l.classList.remove('active'));
        link.classList.add('active');
        loadSection(section);
    });
});

// -------------------- LOAD SECTION --------------------
async function loadSection(section) {
    try {
        const resp = await fetch(`/profile/${section}`);
        if (!resp.ok) throw new Error('Network response not ok');

        const html = await resp.text();
        container.innerHTML = html;

        // Bind forms and buttons
        bindEditProfileForm();
        bindEditAppointmentButtons();
        bindEditBloodRequestButtons();
    } catch (err) {
        console.error('Error loading section:', err);
        container.innerHTML = '<p class="text-danger">Failed to load section.</p>';
    }
}

// -------------------- EDIT PROFILE --------------------
function bindEditProfileForm() {
    const form = document.getElementById('editProfileForm');
    if (!form) return;

    form.addEventListener('submit', async e => {
        e.preventDefault();
        const data = new URLSearchParams(new FormData(form));
        const res = await fetch(form.action, { method: 'POST', body: data });
        const text = await res.text();
        const msgDiv = document.getElementById('editProfileMsg');
        if (msgDiv) {
            msgDiv.innerHTML = text === 'success'
                ? '<span class="text-success">Profile updated!</span>'
                : '<span class="text-danger">Update failed!</span>';
        }
    });
}

// -------------------- EDIT PROFILE IMAGE --------------------
function uploadProfileImage(event) {
    const file = event.target.files[0];
    if (!file) return;

    // Preview
    const preview = document.getElementById('profilePreview');
    preview.src = URL.createObjectURL(file);

    // Upload via AJAX
    const formData = new FormData();
    formData.append('filePart', file);

    fetch('/profile/update-image', {
        method: 'POST',
        body: formData
    })
    .then(res => res.text())
    .then(res => {
        if(res === 'success'){
            alert('Profile image updated!');
        } else {
            alert('Failed to update image.');
        }
    })
    .catch(err => console.error(err));
}

// -------------------- EDIT APPOINTMENT --------------------
function bindEditAppointmentButtons() {
    const buttons = container.querySelectorAll('.btn-edit-appointment');
    buttons.forEach(btn => {
        btn.addEventListener('click', () => {
            const id = btn.getAttribute('data-id');
            editAppointment(id);
        });
    });
}

function editAppointment(id) {
    fetch(`/profile/edit-appointment/${id}`)
        .then(res => res.text())
        .then(html => {
            container.innerHTML = html;
            bindAppointmentForm();
            setupAppointmentJS(); // <-- Apply your new JS here
        })
        .catch(err => console.error('Failed to load edit form:', err));
}

function bindAppointmentForm() {
    const form = document.getElementById('editAppointmentForm');
    if (!form) return;

    form.addEventListener('submit', async e => {
        e.preventDefault();
        const data = new URLSearchParams(new FormData(form));
        const res = await fetch('/profile/update-appointment', { method: 'POST', body: data });
        const text = await res.text();
        if (text === 'success') loadSection('my-appointments');
        else alert('Failed to update appointment!');
    });

    const cancelBtn = form.querySelector('.btn-cancel');
    if (cancelBtn) cancelBtn.addEventListener('click', () => loadSection('my-appointments'));
}

// -------------------- BLOOD REQUEST --------------------
function bindEditBloodRequestButtons() {
    const buttons = container.querySelectorAll('.btn-edit-blood-request');
    buttons.forEach(btn => {
        btn.addEventListener('click', () => {
            const id = btn.getAttribute('data-id');
            editBloodRequest(id);
        });
    });
}

function editBloodRequest(id) {
    fetch(`/profile/edit-blood-request/${id}`)
        .then(res => res.text())
        .then(html => {
            container.innerHTML = html;
            bindBloodRequestForm();
        })
        .catch(err => console.error('Failed to load blood request edit form:', err));
}

function bindBloodRequestForm() {
    const form = document.getElementById('editBloodRequestForm');
    if (!form) return;

    form.addEventListener('submit', async e => {
        e.preventDefault();
        const data = new URLSearchParams(new FormData(form));
        const res = await fetch('/profile/update-blood-request', { method: 'POST', body: data });
        const text = await res.text();
        if (text === 'success') loadSection('blood-request');
        else alert('Failed to update blood request!');
    });

    const cancelBtn = form.querySelector('.btn-cancel');
    if (cancelBtn) cancelBtn.addEventListener('click', () => loadSection('blood-request'));
}

// -------------------- NEW APPOINTMENT JS --------------------
function setupAppointmentJS() {
    const dateInput = document.getElementById('date');
    if (!dateInput) return;

    const today = new Date();
    function formatDateForInput(d) {
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const da = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${da}`;
    }
    dateInput.min = formatDateForInput(today);

    // Time dropdown
    const timeDropdown = document.getElementById('time');
    if (timeDropdown) {
        timeDropdown.innerHTML = ''; // clear existing
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
                timeDropdown.appendChild(opt);
            }
        }
    }

    // Form validation
    const form = document.getElementById('editAppointmentForm');
    if (!form) return;

    form.addEventListener('submit', function(e) {
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
                if (errorDiv) {
                    errorDiv.textContent = `${f.name} is required`;
                    errorDiv.style.display = 'block';
                }
                f.input.classList.add('is-invalid');
                valid = false;
            } else {
                if (errorDiv) {
                    errorDiv.textContent = '';
                    errorDiv.style.display = 'none';
                }
                f.input.classList.remove('is-invalid');
            }
        });

        if (!valid) e.preventDefault();
    });
}
