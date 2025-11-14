// ✅ Navbar scroll effect
window.addEventListener('scroll', function () {
  const header = document.getElementById('mainHeader');
  if (header) {
    if (window.scrollY > 50) header.classList.add('scrolled');
    else header.classList.remove('scrolled');
  }
});

document.addEventListener('DOMContentLoaded', function () {
  // ✅ Donate Now button
  const donateBtn = document.getElementById('donateNowBtn');
  if (donateBtn) {
    donateBtn.addEventListener('click', function () {
      if (confirm("If you want to donate blood, first register your data. Do you want to register now?")) {
        window.location.href = '/register';
      }
    });
  }

  // ✅ Eligibility form toggle
  const eligibilityBtn = document.getElementById('showEligibilityFormBtn');
  if (eligibilityBtn) {
    eligibilityBtn.addEventListener('click', () => {
      const container = document.getElementById('eligibilityFormContainer');
      container.style.display = container.style.display === 'none' ? 'block' : 'none';
    });
  }

  // ✅ Eligibility check form
  const eligibilityForm = document.getElementById('eligibilityForm');
  if (eligibilityForm) {
    eligibilityForm.addEventListener('submit', function (e) {
      e.preventDefault();
      const health = document.getElementById('health').value;
      const age = document.getElementById('age').value;
      const weight = document.getElementById('weight').value;
      const recentDonation = document.getElementById('recentDonation').value;
      const resultDiv = document.getElementById('eligibilityResult');

      if (health === 'yes' && age === 'yes' && weight === 'yes' && recentDonation === 'no') {
        resultDiv.innerHTML = '<div class="alert alert-success">You are eligible to donate blood!</div>';
      } else {
        resultDiv.innerHTML = '<div class="alert alert-danger">Sorry, you are not eligible at this time.</div>';
      }
    });
  }

  // ✅ Add View Details buttons with popup functionality
  const hospitalCards = document.querySelectorAll('#locations .info-card');
  hospitalCards.forEach(card => {
    const hospitalName = card.querySelector('h3').innerText;
    const hospitalAddress = card.querySelector('p:nth-child(2) span').innerText;
    const hospitalPhone = card.querySelector('p:nth-child(3) span').innerText;
    const hospitalEmail = card.querySelector('p:nth-child(4) span').innerText;

    // Remove any existing Google Map links
    const existingMapLinks = card.querySelectorAll('a[href*="google.com/maps"], a.cta-button[href*="maps"]');
    existingMapLinks.forEach(link => link.remove());

    // Check if View Details button already exists
    if (card.querySelector('a.view-details-btn')) return;

    // Create View Details button
    const detailsLink = document.createElement('a');
    detailsLink.href = 'javascript:void(0)';
    detailsLink.className = "cta-button view-details-btn";
    detailsLink.style.display = "inline-block";
    detailsLink.style.marginTop = "10px";
    detailsLink.innerText = "View Details";
    
    // Add click event to show popup
    detailsLink.addEventListener('click', function() {
      showHospitalPopup(hospitalName, hospitalAddress, hospitalPhone, hospitalEmail);
    });

    card.appendChild(detailsLink);
  });
});

// ✅ Function to show hospital details in popup
function showHospitalPopup(name, address, phone, email) {
  // Create popup overlay
  const popupOverlay = document.createElement('div');
  popupOverlay.className = 'hospital-popup-overlay';
  popupOverlay.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0,0,0,0.7);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
    animation: fadeIn 0.3s ease;
  `;

  // Create popup content
  const popupContent = document.createElement('div');
  popupContent.className = 'hospital-popup-content';
  popupContent.style.cssText = `
    background: white;
    padding: 30px;
    border-radius: 15px;
    max-width: 500px;
    width: 90%;
    max-height: 80vh;
    overflow-y: auto;
    position: relative;
    animation: slideIn 0.3s ease;
    box-shadow: 0 20px 40px rgba(0,0,0,0.3);
  `;

  // Create close button
  const closeBtn = document.createElement('button');
  closeBtn.innerHTML = '&times;';
  closeBtn.style.cssText = `
    position: absolute;
    top: 15px;
    right: 20px;
    background: none;
    border: none;
    font-size: 28px;
    cursor: pointer;
    color: #666;
    transition: color 0.3s;
  `;
  closeBtn.addEventListener('mouseenter', () => closeBtn.style.color = '#ff0000');
  closeBtn.addEventListener('mouseleave', () => closeBtn.style.color = '#666');

  // Create popup content HTML
  popupContent.innerHTML = `
    <h2 style="color: #d32f2f; margin-bottom: 20px; border-bottom: 2px solid #f0f0f0; padding-bottom: 10px;">${name}</h2>
    <div style="margin-bottom: 20px;">
      <h4 style="color: #333; margin-bottom: 8px; display: flex; align-items: center;">
        <i class="fas fa-map-marker-alt" style="color: #d32f2f; margin-right: 10px;"></i>
        Address
      </h4>
      <p style="margin: 0; color: #666; line-height: 1.5;">${address}</p>
    </div>
    <div style="margin-bottom: 20px;">
      <h4 style="color: #333; margin-bottom: 8px; display: flex; align-items: center;">
        <i class="fas fa-phone" style="color: #d32f2f; margin-right: 10px;"></i>
        Phone
      </h4>
      <p style="margin: 0; color: #666;">${phone}</p>
    </div>
    <div style="margin-bottom: 25px;">
      <h4 style="color: #333; margin-bottom: 8px; display: flex; align-items: center;">
        <i class="fas fa-envelope" style="color: #d32f2f; margin-right: 10px;"></i>
        Email
      </h4>
      <p style="margin: 0; color: #666;">${email}</p>
    </div>
    <div style="display: flex; gap: 10px; margin-top: 25px;">
      <button class="cta-button" onclick="window.open('https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(address)}', '_blank')" style="flex: 1;">
        <i class="fas fa-map-marked-alt" style="margin-right: 8px;"></i>View on Map
      </button>
      <button class="cta-button" onclick="window.location.href='tel:${phone}'" style="flex: 1; background: #28a745;">
        <i class="fas fa-phone" style="margin-right: 8px;"></i>Call Now
      </button>
    </div>
  `;

  // Add close functionality
  closeBtn.addEventListener('click', function() {
    document.body.removeChild(popupOverlay);
  });

  popupOverlay.addEventListener('click', function(e) {
    if (e.target === popupOverlay) {
      document.body.removeChild(popupOverlay);
    }
  });

  // Add ESC key to close
  document.addEventListener('keydown', function closeOnEsc(e) {
    if (e.key === 'Escape') {
      document.body.removeChild(popupOverlay);
      document.removeEventListener('keydown', closeOnEsc);
    }
  });

  // Append elements
  popupContent.insertBefore(closeBtn, popupContent.firstChild);
  popupOverlay.appendChild(popupContent);
  document.body.appendChild(popupOverlay);

  // Add animations
  const style = document.createElement('style');
  style.textContent = `
    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    @keyframes slideIn {
      from { transform: translateY(-50px); opacity: 0; }
      to { transform: translateY(0); opacity: 1; }
    }
  `;
  document.head.appendChild(style);
}

// ✅ Smooth scroll for anchor links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault(); // Prevent default jump
        const target = document.querySelector(this.getAttribute('href')); // Get target element
        if(target){
            target.scrollIntoView({ behavior: 'smooth', block: 'start' }); // Smooth scroll
        }
    });
});
