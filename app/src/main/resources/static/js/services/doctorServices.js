/*
  Import the base API URL from the config file
  Define a constant DOCTOR_API to hold the full endpoint for doctor-related actions


  Function: getDoctors
  Purpose: Fetch the list of all doctors from the API

   Use fetch() to send a GET request to the DOCTOR_API endpoint
   Convert the response to JSON
   Return the 'doctors' array from the response
   If there's an error (e.g., network issue), log it and return an empty array


  Function: deleteDoctor
  Purpose: Delete a specific doctor using their ID and an authentication token

   Use fetch() with the DELETE method
    - The URL includes the doctor ID and token as path parameters
   Convert the response to JSON
   Return an object with:
    - success: true if deletion was successful
    - message: message from the server
   If an error occurs, log it and return a default failure response


  Function: saveDoctor
  Purpose: Save (create) a new doctor using a POST request

   Use fetch() with the POST method
    - URL includes the token in the path
    - Set headers to specify JSON content type
    - Convert the doctor object to JSON in the request body

   Parse the JSON response and return:
    - success: whether the request succeeded
    - message: from the server

   Catch and log errors
    - Return a failure response if an error occurs


  Function: filterDoctors
  Purpose: Fetch doctors based on filtering criteria (name, time, and specialty)

   Use fetch() with the GET method
    - Include the name, time, and specialty as URL path parameters
   Check if the response is OK
    - If yes, parse and return the doctor data
    - If no, log the error and return an object with an empty 'doctors' array

   Catch any other errors, alert the user, and return a default empty result
*/

import { API_BASE_URL } from "../config/config.js";

const DOCTOR_API = API_BASE_URL + "/doctor";

export async function getDoctors() {
  try {
    const response = await fetch(DOCTOR_API, {
      method: "GET",
    });

    if (!response.ok) {
      console.error("Failed to fetch doctors:", response.status, response.statusText);
      return [];
    }

    const data = await response.json();

    if (Array.isArray(data)) {
      return data;
    }

    if (Array.isArray(data.doctors)) {
      return data.doctors;
    }

    return [];
  } catch (error) {
    console.error("Error fetching doctors:", error);
    return [];
  }
}

export async function deleteDoctor(id, token) {
  if (!id || !token) {
    console.error("deleteDoctor: doctor id or token missing");
    return { success: false, message: "Missing doctor id or token." };
  }

  const url = `${DOCTOR_API}/${encodeURIComponent(id)}/${encodeURIComponent(token)}`;

  try {
    const response = await fetch(url, {
      method: "DELETE",
    });

    let data = {};
    try {
      data = await response.json();
    } catch {
      data = {};
    }

    if (!response.ok) {
      console.error("Failed to delete doctor:", response.status, response.statusText);
      return {
        success: false,
        message: data.message || "Failed to delete doctor.",
      };
    }

    return {
      success: true,
      message: data.message || "Doctor deleted successfully.",
    };
  } catch (error) {
    console.error("Error deleting doctor:", error);
    return {
      success: false,
      message: "An error occurred while deleting the doctor.",
    };
  }
}

export async function saveDoctor(doctor, token) {
  if (!token) {
    console.error("saveDoctor: token missing");
    return { success: false, message: "Missing authentication token." };
  }

  const url = `${DOCTOR_API}/${encodeURIComponent(token)}`;

  try {
    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(doctor),
    });

    let data = {};
    try {
      data = await response.json();
    } catch {
      data = {};
    }

    if (!response.ok) {
      console.error("Failed to save doctor:", response.status, response.statusText);
      return {
        success: false,
        message: data.message || "Failed to save doctor.",
      };
    }

    return {
      success: true,
      message: data.message || "Doctor saved successfully.",
      doctor: data.doctor || data,
    };
  } catch (error) {
    console.error("Error saving doctor:", error);
    return {
      success: false,
      message: "An error occurred while saving the doctor.",
    };
  }
}

export async function filterDoctors(name, time, specialty) {
  const safeName = name && name.trim() ? encodeURIComponent(name.trim()) : "all";
  const safeTime = time && time.trim() ? encodeURIComponent(time.trim()) : "all";
  const safeSpecialty =
    specialty && specialty.trim() ? encodeURIComponent(specialty.trim()) : "all";

  const url = `${DOCTOR_API}/filter/${safeName}/${safeTime}/${safeSpecialty}`;

  try {
    const response = await fetch(url, {
      method: "GET",
    });

    if (!response.ok) {
      console.error("Failed to filter doctors:", response.status, response.statusText);
      return { doctors: [] };
    }

    const data = await response.json();

    if (Array.isArray(data)) {
      return { doctors: data };
    }

    if (Array.isArray(data.doctors)) {
      return { doctors: data.doctors };
    }

    return { doctors: [] };
  } catch (error) {
    console.error("Error filtering doctors:", error);
    alert("Unable to filter doctors at the moment. Please try again later.");
    return { doctors: [] };
  }
}