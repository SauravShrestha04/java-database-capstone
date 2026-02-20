package com.project.back_end.controllers;

import com.project.back_end.model.Appointment;
import com.project.back_end.models.Admin;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

// 1. Set Up the Controller Class:
//    - Annotated with `@RestController` to define it as a REST API controller.
//    - Uses `@RequestMapping("/appointments")` to set a base path for all appointment-related endpoints.
//    - This centralizes all routes that deal with booking, updating, retrieving, and canceling appointments.

    private final AppointmentService appointmentService;
    private final Service service;

// 2. Autowire Dependencies:
//    - Inject `AppointmentService` for handling the business logic specific to appointments.
//    - Inject the general `Service` class, which provides shared functionality like token validation and appointment checks.

    public AppointmentController(AppointmentService appointmentService, Service service) {
        this.appointmentService = appointmentService;
        this.service = service;
    }

// 3. Define the `getAppointments` Method:
//    - Handles HTTP GET requests to fetch appointments based on date and patient name.
//    - Takes the appointment date, patient name, and token as path variables.
//    - First validates the token for role `"doctor"` using the `Service`.
//    - If the token is valid, returns appointments for the given patient on the specified date.
//    - If the token is invalid or expired, responds with the appropriate message and status code.

    @GetMapping("/{date}/{patientName}/{token}")
    public ResponseEntity<?> getAppointments(
            @PathVariable String date,
            @PathVariable String patientName,
            @PathVariable String token) {

        // Validate token for doctor role
        Map<String, String> validationErrors = service.validateToken(token, "doctor");
        if (!validationErrors.isEmpty()) {
            // Token invalid or expired
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validationErrors);
        }

        // Parse date (expects ISO-8601 format: yyyy-MM-dd)
        LocalDate appointmentDate = LocalDate.parse(date);

        Map<String, Object> result = appointmentService.getAppointment(patientName, appointmentDate, token);
        return ResponseEntity.ok(result);
    }

// 4. Define the `bookAppointment` Method:
//    - Handles HTTP POST requests to create a new appointment.
//    - Accepts an `Appointment` object in the request body and a token as a path variable.
//    - Validates the token for the `"patient"` role.
//    - Uses service logic to validate the appointment data (e.g., check for doctor availability and time conflicts).
//    - Returns success if booked, or appropriate error messages if the doctor ID is invalid or the slot is already taken.

    @PostMapping("/{token}")
    public ResponseEntity<?> bookAppointment(
            @PathVariable String token,
            @RequestBody Appointment appointment) {

        // Validate token for patient role
        Map<String, String> validationErrors = service.validateToken(token, "patient");
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validationErrors);
        }

        // Validate appointment (doctor exists, slot available, etc.)
        int validationResult = service.validateAppointment(appointment);
        Map<String, String> response = new HashMap<>();

        if (validationResult == -1) {
            response.put("message", "Doctor does not exist.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } else if (validationResult == 0) {
            response.put("message", "Selected time slot is not available.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        // validationResult == 1 → proceed with booking
        int booked = appointmentService.bookAppointment(appointment);
        if (booked == 1) {
            response.put("message", "Appointment booked successfully.");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            response.put("message", "Failed to book appointment. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

// 5. Define the `updateAppointment` Method:
//    - Handles HTTP PUT requests to modify an existing appointment.
//    - Accepts an `Appointment` object and a token as input.
//    - Validates the token for `"patient"` role.
//    - Delegates the update logic to the `AppointmentService`.
//    - Returns an appropriate success or failure response based on the update result.

    @PutMapping("/{token}")
    public ResponseEntity<?> updateAppointment(
            @PathVariable String token,
            @RequestBody Appointment appointment) {

        // Validate token for patient role
        Map<String, String> validationErrors = service.validateToken(token, "patient");
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validationErrors);
        }

        // Business logic (including validation) is handled in AppointmentService
        return appointmentService.updateAppointment(appointment);
    }

// 6. Define the `cancelAppointment` Method:
//    - Handles HTTP DELETE requests to cancel a specific appointment.
//    - Accepts the appointment ID and a token as path variables.
//    - Validates the token for `"patient"` role to ensure the user is authorized to cancel the appointment.
//    - Calls `AppointmentService` to handle the cancellation process and returns the result.

    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable long id,
            @PathVariable String token) {

        // Validate token for patient role
        Map<String, String> validationErrors = service.validateToken(token, "patient");
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validationErrors);
        }

        return appointmentService.cancelAppointment(id, token);
    }
}