package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project_back_end.services.Service; // <-- if your Service is under com.project_back_end.services
// If it's actually com.project.back_end.services.Service, use this instead:
// import com.project.back_end.services.Service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/appointments") // task-spec base path
public class AppointmentController {

    // 2. Autowire Dependencies:
    //    - AppointmentService: appointment-specific business logic.
    //    - Service: shared logic (token validation, appointment validation, etc.).
    private final AppointmentService appointmentService;
    private final Service sharedService;

    public AppointmentController(AppointmentService appointmentService, Service sharedService) {
        this.appointmentService = appointmentService;
        this.sharedService = sharedService;
    }

    // 3. getAppointments:
    //    - GET /appointments/{date}/{pname}/{token}
    //    - date: ISO-8601 (e.g., 2026-02-20)
    //    - pname: patient name (or "null" / empty to ignore)
    //    - token: doctor token
    @GetMapping("/{date}/{pname}/{token}")
    public ResponseEntity<?> getAppointments(
            @PathVariable String date,
            @PathVariable String pname,
            @PathVariable String token
    ) {
        // Validate token for doctor role
        Map<String, String> validationErrors = sharedService.validateToken(token, "doctor");
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validationErrors);
        }

        LocalDate localDate;
        try {
            localDate = LocalDate.parse(date); // expects yyyy-MM-dd
        } catch (DateTimeParseException ex) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid date format. Use yyyy-MM-dd.");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> result = appointmentService.getAppointment(pname, localDate, token);
        return ResponseEntity.ok(result);
    }

    // 4. bookAppointment:
    //    - POST /appointments/{token}
    //    - body: Appointment JSON
    //    - token: patient token
    @PostMapping("/{token}")
    public ResponseEntity<?> bookAppointment(
            @PathVariable String token,
            @RequestBody Appointment appointment
    ) {
        Map<String, String> response = new HashMap<>();

        // Validate token for patient role
        Map<String, String> validationErrors = sharedService.validateToken(token, "patient");
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validationErrors);
        }

        // Validate appointment details (doctor existence, slot availability, etc.)
        int validationCode = sharedService.validateAppointment(appointment);
        if (validationCode == -1) {
            response.put("message", "Doctor not found or invalid appointment details.");
            return ResponseEntity.badRequest().body(response);
        }
        if (validationCode == 0) {
            response.put("message", "Doctor is not available at the selected time.");
            return ResponseEntity.badRequest().body(response);
        }

        // Attempt to book appointment
        int result = appointmentService.bookAppointment(appointment);
        if (result == 1) {
            response.put("message", "Appointment booked successfully.");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            response.put("message", "Error booking appointment.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 5. updateAppointment:
    //    - PUT /appointments/{token}
    //    - body: Appointment JSON (must contain id)
    //    - token: patient token
    @PutMapping("/{token}")
    public ResponseEntity<?> updateAppointment(
            @PathVariable String token,
            @RequestBody Appointment appointment
    ) {
        // Validate token for patient role
        Map<String, String> validationErrors = sharedService.validateToken(token, "patient");
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validationErrors);
        }

        // Business logic + validation is inside AppointmentService.updateAppointment
        return appointmentService.updateAppointment(appointment);
    }

    // 6. cancelAppointment:
    //    - DELETE /appointments/{id}/{token}
    //    - id: appointment ID
    //    - token: patient token
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable long id,
            @PathVariable String token
    ) {
        // Validate token for patient role
        Map<String, String> validationErrors = sharedService.validateToken(token, "patient");
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validationErrors);
        }

        // AppointmentService.cancelAppointment enforces that the token’s patient owns the appointment
        return appointmentService.cancelAppointment(id, token);
    }
}