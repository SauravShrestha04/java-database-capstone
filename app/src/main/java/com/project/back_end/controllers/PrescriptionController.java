package com.project.back_end.controllers;

import com.project.back_end.models.Admin; 
import com.project.back_end.model.Prescription;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.PrescriptionService;
import com.project.back_end.services.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("${api.path}" + "prescription")
public class PrescriptionController {

    // 2. Autowire Dependencies:
    //    - Inject `PrescriptionService` to handle logic related to saving and fetching prescriptions.
    //    - Inject the shared `Service` class for token validation and role-based access control.
    //    - Inject `AppointmentService` to update appointment status after a prescription is issued.
    private final PrescriptionService prescriptionService;
    private final Service service;
    private final AppointmentService appointmentService;

    public PrescriptionController(PrescriptionService prescriptionService,
                                  Service service,
                                  AppointmentService appointmentService) {
        this.prescriptionService = prescriptionService;
        this.service = service;
        this.appointmentService = appointmentService;
    }

    // 3. Define the `savePrescription` Method:
    //    - Handles HTTP POST requests to save a new prescription for a given appointment.
    //    - Accepts a `Prescription` object in the request body and a doctor’s token as a path variable.
    //    - Validates the token for the `"doctor"` role.
    //    - If the token is valid, optionally updates the status of the corresponding appointment
    //      (for example, mark as "completed" or "prescribed") and then saves the prescription.
    //    - Delegates the saving logic to `PrescriptionService` and returns its response.
    @PostMapping("/{token}")
    public ResponseEntity<?> savePrescription(
            @PathVariable String token,
            @RequestBody Prescription prescription
    ) {
        // Validate token for doctor role
        Map<String, String> validationErrors = service.validateToken(token, "doctor");
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validationErrors);
        }

        // Optional: update appointment status when prescription is created
        // (Assumes appointmentId is present in the prescription object and
        //  AppointmentService has a method changeStatus(int status, long id).)
        try {
            if (prescription.getAppointmentId() != null) {
                // For example, status = 1 could mean "completed / prescribed"
                appointmentService.changeStatus(1, prescription.getAppointmentId());
            }
        } catch (Exception e) {
            // If updating status fails, you may still allow saving the prescription,
            // or you could choose to treat it as an error. Here we just ignore it
            // and rely on PrescriptionService for the main logic.
        }

        return prescriptionService.savePrescription(prescription);
    }

    // 4. Define the `getPrescription` Method:
    //    - Handles HTTP GET requests to retrieve a prescription by its associated appointment ID.
    //    - Accepts the appointment ID and a doctor’s token as path variables.
    //    - Validates the token for the `"doctor"` role using the shared service.
    //    - If the token is valid, fetches the prescription using the `PrescriptionService`.
    //    - Returns the prescription details or an appropriate error message if validation fails.
    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<?> getPrescription(
            @PathVariable Long appointmentId,
            @PathVariable String token
    ) {
        // Validate token for doctor role
        Map<String, String> validationErrors = service.validateToken(token, "doctor");
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validationErrors);
        }

        return prescriptionService.getPrescription(appointmentId);
    }
}