package com.project.back_end.controllers;

import com.project.back_end.models.Prescription; 
import com.project.back_end.services.AppointmentController;
import com.project.back_end.services.PrescriptionService;
import com.project.back_end.services.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("${api.path}" + "prescription")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final Service service;
    private final AppointmentController appointmentService;

    public PrescriptionController(PrescriptionService prescriptionService,
                                  Service service,
                                  AppointmentController appointmentService) {
        this.prescriptionService = prescriptionService;
        this.service = service;
        this.appointmentService = appointmentService;
    }

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

        try {
            if (prescription.getAppointmentId() != null) {
                // id first, status second
                appointmentService.changeStatus(prescription.getAppointmentId(), 1);
            }
        } catch (Exception e) {
            // swallow or log if you want; keeping your behaviour
        }

        return prescriptionService.savePrescription(prescription);
    }

    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<?> getPrescription(
            @PathVariable Long appointmentId,
            @PathVariable String token
    ) {
        Map<String, String> validationErrors = service.validateToken(token, "doctor");
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validationErrors);
        }

        return prescriptionService.getPrescription(appointmentId);
    }
}