/*
Import the overlay function for booking appointments from loggedPatient.js

Import the deleteDoctor API function to remove doctors (admin role) from doctorServices.js

Import function to fetch patient details (used during booking) from patientServices.js

Function to create and return a DOM element for a single doctor card:
  - Create the main container for the doctor card
  - Retrieve the current user role from localStorage
  - Create a div to hold doctor information
  - Create and set the doctor’s name
  - Create and set the doctor's specialization
  - Create and set the doctor's email
  - Create and list available appointment times
  - Append all info elements to the doctor info container
  - Create a container for card action buttons

  === ADMIN ROLE ACTIONS ===
    - Create a delete button
    - Add click handler for delete button
      - Confirm deletion
      - Get the admin token from localStorage
      - Call API to delete the doctor
      - Show result and remove card if successful
    - Add delete button to actions container

  === PATIENT (NOT LOGGED-IN) ROLE ACTIONS ===
    - Create a book now button
    - Alert patient to log in before booking
    - Add button to actions container

  === LOGGED-IN PATIENT ROLE ACTIONS === 
    - Create a book now button
    - Handle booking logic for logged-in patient   
      - Redirect if token not available
      - Fetch patient data with token
      - Show booking overlay UI with doctor and patient info
    - Add button to actions container

  - Append doctor info and action buttons to the card
  - Return the complete doctor card element
*/

import { showBookingOverlay } from "../loggedPatient.js";
import { deleteDoctor } from "../services/doctorServices.js";
import { getPatientData } from "../services/patientServices.js";

export function createDoctorCard(doctor) {
  // Main card container
  const card = document.createElement("div");
  card.classList.add("doctor-card");

  // Current user role
  const role = localStorage.getItem("userRole");

  // ===== Doctor Info Section =====
  const infoDiv = document.createElement("div");
  infoDiv.classList.add("doctor-info");

  const nameEl = document.createElement("h3");
  nameEl.classList.add("doctor-name");
  nameEl.textContent = doctor.name || "Unnamed Doctor";

  const specializationEl = document.createElement("p");
  specializationEl.classList.add("doctor-specialization");
  const specialization =
    doctor.specialization || doctor.specialty || "Not specified";
  specializationEl.textContent = `Specialty: ${specialization}`;

  const emailEl = document.createElement("p");
  emailEl.classList.add("doctor-email");
  emailEl.textContent = doctor.email
    ? `Email: ${doctor.email}`
    : "Email: Not provided";

  const availabilityEl = document.createElement("p");
  availabilityEl.classList.add("doctor-availability");

  let availabilityText = "Availability: Not specified";
  if (Array.isArray(doctor.availability)) {
    availabilityText = `Availability: ${doctor.availability.join(", ")}`;
  } else if (doctor.availability) {
    availabilityText = `Availability: ${doctor.availability}`;
  } else if (Array.isArray(doctor.availableTimes)) {
    availabilityText = `Availability: ${doctor.availableTimes.join(", ")}`;
  }
  availabilityEl.textContent = availabilityText;

  infoDiv.appendChild(nameEl);
  infoDiv.appendChild(specializationEl);
  infoDiv.appendChild(emailEl);
  infoDiv.appendChild(availabilityEl);

  // ===== Actions Section =====
  const actionsDiv = document.createElement("div");
  actionsDiv.classList.add("card-actions");

  // --- Admin Actions: Delete Doctor ---
  if (role === "admin") {
    const removeBtn = document.createElement("button");
    removeBtn.textContent = "Delete";
    removeBtn.classList.add("adminBtn");

    removeBtn.addEventListener("click", async () => {
      const confirmDelete = window.confirm(
        `Are you sure you want to delete Dr. ${doctor.name || ""}?`
      );
      if (!confirmDelete) return;

      const token = localStorage.getItem("token");
      if (!token) {
        alert("Admin session expired. Please log in again.");
        window.location.href = "/";
        return;
      }

      try {
        await deleteDoctor(doctor.id, token);
        card.remove();
        alert("Doctor deleted successfully.");
      } catch (error) {
        console.error("Error deleting doctor:", error);
        alert("Unable to delete doctor. Please try again.");
      }
    });

    actionsDiv.appendChild(removeBtn);
  }
  // --- Patient (not logged in) Actions ---
  else if (role === "patient") {
    const bookNowBtn = document.createElement("button");
    bookNowBtn.textContent = "Book Now";
    bookNowBtn.classList.add("dashboard-btn");

    bookNowBtn.addEventListener("click", () => {
      alert("Please log in as a patient to book an appointment.");
    });

    actionsDiv.appendChild(bookNowBtn);
  }
  // --- Logged-in Patient Actions ---
  else if (role === "loggedPatient") {
    const bookNowBtn = document.createElement("button");
    bookNowBtn.textContent = "Book Now";
    bookNowBtn.classList.add("dashboard-btn");

    bookNowBtn.addEventListener("click", async (event) => {
      const token = localStorage.getItem("token");
      if (!token) {
        alert("Session expired. Please log in again.");
        window.location.href = "/";
        return;
      }

      try {
        const patientData = await getPatientData(token);
        showBookingOverlay(event, doctor, patientData);
      } catch (error) {
        console.error("Error preparing booking:", error);
        alert("Unable to load patient information. Please try again.");
      }
    });

    actionsDiv.appendChild(bookNowBtn);
  }

  // Assemble card
  card.appendChild(infoDiv);
  card.appendChild(actionsDiv);

  return card;
}