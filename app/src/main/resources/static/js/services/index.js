/*
  1. Import openModal (for login modals) and API_BASE_URL (base API config)
  2. Define ADMIN_API and DOCTOR_API endpoint URLs
  3. On window load, attach click handlers to admin/doctor login buttons to open modals
  4. Define window.adminLoginHandler to authenticate admin and store token/role
  5. Define window.doctorLoginHandler to authenticate doctor and store token/role
*/

import { openModal } from "../components/modals.js";
import { API_BASE_URL } from "../config/config.js";

const ADMIN_API = API_BASE_URL + "/admin";
const DOCTOR_API = API_BASE_URL + "/doctor/login";

window.onload = function () {
  const adminBtn = document.getElementById("adminLogin");
  if (adminBtn) {
    adminBtn.addEventListener("click", () => {
      openModal("adminLogin");
    });
  }

  const doctorBtn = document.getElementById("doctorLogin");
  if (doctorBtn) {
    doctorBtn.addEventListener("click", () => {
      openModal("doctorLogin");
    });
  }
};

window.adminLoginHandler = async function (event) {
  event.preventDefault();

  const usernameInput = document.getElementById("adminUsername");
  const passwordInput = document.getElementById("adminPassword");

  if (!usernameInput || !passwordInput) {
    alert("Admin login form not found.");
    return;
  }

  const username = usernameInput.value.trim();
  const password = passwordInput.value.trim();

  if (!username || !password) {
    alert("Please fill in both username and password.");
    return;
  }

  const admin = { username, password };

  try {
    const response = await fetch(ADMIN_API, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(admin),
    });

    if (!response.ok) {
      alert("Invalid credentials!");
      return;
    }

    const data = await response.json();

    if (!data.token) {
      alert("No token received from server.");
      return;
    }

    localStorage.setItem("token", data.token);

    if (typeof selectRole === "function") {
      selectRole("admin");
    } else {
      localStorage.setItem("userRole", "admin");
      window.location.href = "/admin/dashboard";
    }
  } catch (error) {
    console.error("Admin login error:", error);
    alert("Something went wrong during admin login. Please try again.");
  }
};

window.doctorLoginHandler = async function (event) {
  event.preventDefault();

  const emailInput = document.getElementById("doctorEmail");
  const passwordInput = document.getElementById("doctorPassword");

  if (!emailInput || !passwordInput) {
    alert("Doctor login form not found.");
    return;
  }

  const email = emailInput.value.trim();
  const password = passwordInput.value.trim();

  if (!email || !password) {
    alert("Please fill in both email and password.");
    return;
  }

  const doctor = { email, password };

  try {
    const response = await fetch(DOCTOR_API, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(doctor),
    });

    if (!response.ok) {
      alert("Invalid credentials!");
      return;
    }

    const data = await response.json();

    if (!data.token) {
      alert("No token received from server.");
      return;
    }

    localStorage.setItem("token", data.token);

    if (typeof selectRole === "function") {
      selectRole("doctor");
    } else {
      localStorage.setItem("userRole", "doctor");
      window.location.href = "/doctor/dashboard";
    }
  } catch (error) {
    console.error("Doctor login error:", error);
    alert("Something went wrong during doctor login. Please try again.");
  }
};