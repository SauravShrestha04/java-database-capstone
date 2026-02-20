/*
  This script handles the admin dashboard functionality for managing doctors:
  - Loads all doctor cards
  - Filters doctors by name, time, or specialty
  - Adds a new doctor via modal form


  Attach a click listener to the "Add Doctor" button
  When clicked, it opens a modal form using openModal('addDoctor')


  When the DOM is fully loaded:
    - Call loadDoctorCards() to fetch and display all doctors


  Function: loadDoctorCards
  Purpose: Fetch all doctors and display them as cards

    Call getDoctors() from the service layer
    Clear the current content area
    For each doctor returned:
    - Create a doctor card using createDoctorCard()
    - Append it to the content div

    Handle any fetch errors by logging them


  Attach 'input' and 'change' event listeners to the search bar and filter dropdowns
  On any input change, call filterDoctorsOnChange()


  Function: filterDoctorsOnChange
  Purpose: Filter doctors based on name, available time, and specialty

    Read values from the search bar and filters
    Normalize empty values to null
    Call filterDoctors(name, time, specialty) from the service

    If doctors are found:
    - Render them using createDoctorCard()
    If no doctors match the filter:
    - Show a message: "No doctors found with the given filters."

    Catch and display any errors with an alert


  Function: renderDoctorCards
  Purpose: A helper function to render a list of doctors passed to it

    Clear the content area
    Loop through the doctors and append each card to the content area


  Function: adminAddDoctor
  Purpose: Collect form data and add a new doctor to the system

    Collect input values from the modal form
    - Includes name, email, phone, password, specialty, and available times

    Retrieve the authentication token from localStorage
    - If no token is found, show an alert and stop execution

    Build a doctor object with the form values

    Call saveDoctor(doctor, token) from the service

    If save is successful:
    - Show a success message
    - Close the modal and reload the page

    If saving fails, show an error message
*/

import { openModal } from "./components/modals.js";
import { getDoctors, filterDoctors, saveDoctor } from "./services/doctorServices.js";
import { createDoctorCard } from "./components/doctorCard.js";

function attachAddDoctorButton() {
  const addDocBtn = document.getElementById("addDocBtn");
  if (addDocBtn) {
    addDocBtn.addEventListener("click", () => {
      openModal("addDoctor");
    });
  }
}

async function loadDoctorCards() {
  try {
    const contentDiv = document.getElementById("content");
    if (!contentDiv) return;

    const doctors = await getDoctors();
    renderDoctorCards(doctors);
  } catch (error) {
    console.error("Error loading doctors:", error);
  }
}

function renderDoctorCards(doctors) {
  const contentDiv = document.getElementById("content");
  if (!contentDiv) return;

  contentDiv.innerHTML = "";

  if (!doctors || doctors.length === 0) {
    const msg = document.createElement("p");
    msg.textContent = "No doctors found.";
    contentDiv.appendChild(msg);
    return;
  }

  doctors.forEach((doctor) => {
    const card = createDoctorCard(doctor);
    contentDiv.appendChild(card);
  });
}

async function filterDoctorsOnChange() {
  try {
    const searchInput = document.getElementById("searchBar");
    const timeSelect = document.getElementById("filterTime");
    const specialtySelect = document.getElementById("filterSpecialty");

    const name = searchInput ? searchInput.value.trim() : "";
    const time = timeSelect ? timeSelect.value : "";
    const specialty = specialtySelect ? specialtySelect.value : "";

    const normalizedName = name || null;
    const normalizedTime = time || null;
    const normalizedSpecialty = specialty || null;

    const doctors = await filterDoctors(
      normalizedName,
      normalizedTime,
      normalizedSpecialty
    );

    // Assuming filterDoctors returns an array; if it returns { doctors: [] }, adjust accordingly:
    const doctorList = Array.isArray(doctors)
      ? doctors
      : doctors?.doctors || [];

    if (!doctorList || doctorList.length === 0) {
      const contentDiv = document.getElementById("content");
      if (contentDiv) {
        contentDiv.innerHTML = "";
        const msg = document.createElement("p");
        msg.textContent = "No doctors found with the given filters.";
        contentDiv.appendChild(msg);
      }
      return;
    }

    renderDoctorCards(doctorList);
  } catch (error) {
    console.error("Error filtering doctors:", error);
    alert("Something went wrong while filtering doctors.");
  }
}

async function adminAddDoctor(event) {
  event.preventDefault();

  const nameInput = document.getElementById("doctorName");
  const emailInput = document.getElementById("doctorEmail");
  const phoneInput = document.getElementById("doctorPhone");
  const passwordInput = document.getElementById("doctorPassword");
  const specialtyInput = document.getElementById("doctorSpecialty");
  const availabilityCheckboxes = document.querySelectorAll(
    'input[name="availability"]:checked'
  );

  if (
    !nameInput ||
    !emailInput ||
    !phoneInput ||
    !passwordInput ||
    !specialtyInput
  ) {
    alert("Doctor form is not properly loaded.");
    return;
  }

  const name = nameInput.value.trim();
  const email = emailInput.value.trim();
  const phone = phoneInput.value.trim();
  const password = passwordInput.value.trim();
  const specialty = specialtyInput.value.trim();
  const availability = Array.from(availabilityCheckboxes).map(
    (cb) => cb.value
  );

  if (!name || !email || !phone || !password || !specialty) {
    alert("Please fill in all required fields.");
    return;
  }

  const token = localStorage.getItem("token");
  if (!token) {
    alert("Admin not authenticated. Please log in again.");
    window.location.href = "/";
    return;
  }

  const doctor = {
    name,
    email,
    mobileNo: phone,
    password,
    specialization: specialty,
    availability,
  };

  try {
    const result = await saveDoctor(doctor, token);

    if (result && result.success) {
      alert(result.message || "Doctor added successfully.");

      const modal = document.getElementById("modal");
      if (modal) {
        modal.style.display = "none";
      }

      await loadDoctorCards();
    } else {
      alert(result?.message || "Failed to add doctor.");
    }
  } catch (error) {
    console.error("Error adding doctor:", error);
    alert("Something went wrong while adding the doctor.");
  }
}

document.addEventListener("DOMContentLoaded", () => {
  attachAddDoctorButton();

  const searchBar = document.getElementById("searchBar");
  if (searchBar) {
    searchBar.addEventListener("input", filterDoctorsOnChange);
  }

  const filterTime = document.getElementById("filterTime");
  if (filterTime) {
    filterTime.addEventListener("change", filterDoctorsOnChange);
  }

  const filterSpecialty = document.getElementById("filterSpecialty");
  if (filterSpecialty) {
    filterSpecialty.addEventListener("change", filterDoctorsOnChange);
  }

  loadDoctorCards();
});

// Expose adminAddDoctor globally so the modal form can call it via onsubmit="adminAddDoctor(event)"
window.adminAddDoctor = adminAddDoctor;