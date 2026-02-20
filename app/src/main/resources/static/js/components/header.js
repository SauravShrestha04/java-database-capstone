/*
  Header Rendering Logic (header.js)

  This file renders a reusable header on all pages based on:
  - Current page (no role header on home `/`)
  - userRole and token stored in localStorage
  - Role-specific actions (Admin, Doctor, Patient, Logged Patient)

  Roles & Behavior:
  - admin:
      * Shows "Add Doctor" button (opens add-doctor modal)
      * Shows "Logout"
  - doctor:
      * Shows "Home" button
      * Shows "Logout"
  - patient:
      * Shows "Login" and "Sign Up" buttons
  - loggedPatient:
      * Shows "Home", "Appointments", and "Logout"

  Helper functions:
  - renderHeader(): builds and injects header HTML into #header
  - attachHeaderButtonListeners(): attaches click handlers after render
  - logout(): clears token + userRole, redirects to "/"
  - logoutPatient(): clears token, sets role back to "patient",
                    redirects to patient dashboard
*/

function renderHeader() {
  const headerDiv = document.getElementById("header");
  if (!headerDiv) return;

  // 1. Do NOT show role-based header on homepage
  //    Also clear any old session when user hits root.
  const path = window.location.pathname;
  if (path === "/" || path.endsWith("/index.html")) {
    localStorage.removeItem("userRole");
    localStorage.removeItem("token");

    headerDiv.innerHTML = `
      <header class="header">
        <div class="logo-section">
          <img src="/assets/images/logo/logo.png" alt="Hospital CMS Logo" class="logo-img">
          <span class="logo-title">Hospital CMS</span>
        </div>
      </header>
    `;
    return;
  }

  // 2. Read role & token from localStorage
  const role = localStorage.getItem("userRole");
  const token = localStorage.getItem("token");

  // 3. Handle invalid / expired session for logged roles
  if ((role === "loggedPatient" || role === "admin" || role === "doctor") && !token) {
    localStorage.removeItem("userRole");
    localStorage.removeItem("token");
    alert("Session expired or invalid login. Please log in again.");
    window.location.href = "/";
    return;
  }

  // 4. Base header layout (logo + nav)
  let headerContent = `
    <header class="header">
      <div class="logo-section">
        <img src="/assets/images/logo/logo.png" alt="Hospital CMS Logo" class="logo-img">
        <span class="logo-title">Hospital CMS</span>
      </div>
      <nav class="nav-actions">
  `;

  // 5. Role-specific actions
  if (role === "admin") {
    // Admin: Add Doctor + Logout
    headerContent += `
      <button id="addDocBtn" class="adminBtn">Add Doctor</button>
      <a href="#" id="logoutLink">Logout</a>
    `;
  } else if (role === "doctor") {
    // Doctor: Home + Logout
    headerContent += `
      <button id="doctorHomeBtn" class="adminBtn">Home</button>
      <a href="#" id="logoutLink">Logout</a>
    `;
  } else if (role === "loggedPatient") {
    // Logged-in patient: Home, Appointments, Logout
    headerContent += `
      <button id="loggedPatientHomeBtn" class="adminBtn">Home</button>
      <button id="patientAppointmentsBtn" class="adminBtn">Appointments</button>
      <a href="#" id="logoutPatientLink">Logout</a>
    `;
  } else {
    // Default / plain patient role: Login + Sign Up
    // (either role === "patient" or no role set yet)
    headerContent += `
      <button id="patientLoginBtn" class="adminBtn">Login</button>
      <button id="patientSignupBtn" class="adminBtn">Sign Up</button>
    `;
  }

  // 6. Close nav + header and inject into DOM
  headerContent += `
      </nav>
    </header>
  `;

  headerDiv.innerHTML = headerContent;

  // 7. Attach listeners to dynamically added elements
  attachHeaderButtonListeners();
}

function attachHeaderButtonListeners() {
  // Admin: Add Doctor button → open add-doctor modal
  const addDocBtn = document.getElementById("addDocBtn");
  if (addDocBtn && typeof openModal === "function") {
    addDocBtn.addEventListener("click", () => openModal("addDoctor"));
  }

  // Doctor home → navigate to doctor dashboard route
  const doctorHomeBtn = document.getElementById("doctorHomeBtn");
  if (doctorHomeBtn) {
    doctorHomeBtn.addEventListener("click", () => {
      // Adjust this path to whatever your Spring mapping is
      window.location.href = "/doctor/dashboard";
    });
  }

  // Logged patient home → logged patient dashboard
  const loggedPatientHomeBtn = document.getElementById("loggedPatientHomeBtn");
  if (loggedPatientHomeBtn) {
    loggedPatientHomeBtn.addEventListener("click", () => {
      window.location.href = "/pages/loggedPatientDashboard.html";
    });
  }

  // Logged patient appointments → patient appointments page
  const patientAppointmentsBtn = document.getElementById("patientAppointmentsBtn");
  if (patientAppointmentsBtn) {
    patientAppointmentsBtn.addEventListener("click", () => {
      window.location.href = "/pages/patientAppointments.html";
    });
  }

  // Patient Login / Sign Up → open corresponding modals
  const patientLoginBtn = document.getElementById("patientLoginBtn");
  if (patientLoginBtn && typeof openModal === "function") {
    patientLoginBtn.addEventListener("click", () => openModal("patientLogin"));
  }

  const patientSignupBtn = document.getElementById("patientSignupBtn");
  if (patientSignupBtn && typeof openModal === "function") {
    patientSignupBtn.addEventListener("click", () => openModal("patientSignup"));
  }

  // Generic logout (admin / doctor)
  const logoutLink = document.getElementById("logoutLink");
  if (logoutLink) {
    logoutLink.addEventListener("click", (e) => {
      e.preventDefault();
      logout();
    });
  }

  // Logged patient logout
  const logoutPatientLink = document.getElementById("logoutPatientLink");
  if (logoutPatientLink) {
    logoutPatientLink.addEventListener("click", (e) => {
      e.preventDefault();
      logoutPatient();
    });
  }
}

// Clears session completely and returns to home (for admin/doctor)
function logout() {
  localStorage.removeItem("token");
  localStorage.removeItem("userRole");
  window.location.href = "/";
}

// For logged patients: clear token, keep them as 'patient' and go back to patient dashboard
function logoutPatient() {
  localStorage.removeItem("token");
  localStorage.setItem("userRole", "patient");
  window.location.href = "/pages/patientDashboard.html";
}

// Render header on DOM ready for all non-root pages
document.addEventListener("DOMContentLoaded", renderHeader);