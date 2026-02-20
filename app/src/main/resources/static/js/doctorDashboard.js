/*
  1. Import getAllAppointments to fetch appointments from the backend
  2. Import createPatientRow to generate a table row for each patient appointment


  3. Get the table body where patient rows will be added
  4. Initialize selectedDate with today's date in 'YYYY-MM-DD' format
  5. Get the saved token from localStorage (used for authenticated API calls)
  6. Initialize patientName to null (used for filtering by name)


  7. Add an 'input' event listener to the search bar
  8. On each keystroke:
      - Trim and check the input value
      - If not empty, use it as the patientName for filtering
      - Else, reset patientName to "null" (as expected by backend)
      - Reload the appointments list with the updated filter


  9. Add a click listener to the "Today" button
  10. When clicked:
      - Set selectedDate to today's date
      - Update the date picker UI to match
      - Reload the appointments for today


  11. Add a change event listener to the date picker
  12. When the date changes:
      - Update selectedDate with the new value
      - Reload the appointments for that specific date


  13. Function: loadAppointments
      Purpose: Fetch and display appointments based on selected date and optional patient name

      Step 1: Call getAllAppointments with selectedDate, patientName, and token
      Step 2: Clear the table body content before rendering new rows

      Step 3: If no appointments are returned:
        - Display a message row: "No Appointments found for today."

      Step 4: If appointments exist:
        - Loop through each appointment
        - Call createPatientRow to generate a table row for the appointment
        - Append each row to the table body

      Step 5: Catch and handle any errors during fetch:
        - Show a message row: "Error loading appointments. Try again later."


  14. When the page is fully loaded (DOMContentLoaded):
      - Call renderContent() (assumes it sets up the UI layout)
      - Call loadAppointments() to display today's appointments by default
*/

import { getAllAppointments } from "./services/appointmentRecordService.js";
import { createPatientRow } from "./components/patientRows.js";

let patientTableBody = null;
let selectedDate = new Date().toISOString().split("T")[0];
let token = localStorage.getItem("token");
let patientName = null;

function getTodayAsString() {
  return new Date().toISOString().split("T")[0];
}

async function loadAppointments() {
  if (!patientTableBody) return;

  if (!token) {
    patientTableBody.innerHTML = "";
    const row = document.createElement("tr");
    const cell = document.createElement("td");
    cell.colSpan = 5;
    cell.textContent = "Doctor not authenticated. Please log in again.";
    row.appendChild(cell);
    patientTableBody.appendChild(row);
    return;
  }

  try {
    patientTableBody.innerHTML = "";

    const effectiveDate = selectedDate || getTodayAsString();
    const effectiveName = patientName && patientName.trim() !== "" ? patientName : "null";

    const appointments = await getAllAppointments(effectiveDate, effectiveName, token);

    const list = Array.isArray(appointments)
      ? appointments
      : appointments?.appointments || [];

    if (!list || list.length === 0) {
      const row = document.createElement("tr");
      const cell = document.createElement("td");
      cell.colSpan = 5;
      cell.textContent = "No Appointments found for today.";
      row.appendChild(cell);
      patientTableBody.appendChild(row);
      return;
    }

    list.forEach((appointment) => {
      const row = createPatientRow(appointment);
      patientTableBody.appendChild(row);
    });
  } catch (error) {
    console.error("Error loading appointments:", error);
    patientTableBody.innerHTML = "";
    const row = document.createElement("tr");
    const cell = document.createElement("td");
    cell.colSpan = 5;
    cell.textContent = "Error loading appointments. Try again later.";
    row.appendChild(cell);
    patientTableBody.appendChild(row);
  }
}

document.addEventListener("DOMContentLoaded", () => {
  patientTableBody = document.getElementById("patientTableBody");
  token = localStorage.getItem("token");
  selectedDate = getTodayAsString();
  patientName = null;

  const searchInput = document.getElementById("searchBar");
  if (searchInput) {
    searchInput.addEventListener("input", () => {
      const value = searchInput.value.trim();
      patientName = value !== "" ? value : "null";
      loadAppointments();
    });
  }

  const todayButton = document.getElementById("todayButton");
  const datePicker = document.getElementById("datePicker");

  if (todayButton) {
    todayButton.addEventListener("click", () => {
      selectedDate = getTodayAsString();
      if (datePicker) {
        datePicker.value = selectedDate;
      }
      loadAppointments();
    });
  }

  if (datePicker) {
    datePicker.value = selectedDate;
    datePicker.addEventListener("change", () => {
      selectedDate = datePicker.value || getTodayAsString();
      loadAppointments();
    });
  }

  if (typeof renderContent === "function") {
    renderContent();
  }

  loadAppointments();
});