package com.project.back_end.controllers;

import com.project.back_end.models.Prescription;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.PrescriptionService;
import com.project.back_end.services.Service;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("${api.path}prescription")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final AppointmentService appointmentService;
    private final Service service;

    public PrescriptionController(PrescriptionService prescriptionService,
                                  AppointmentService appointmentService,
                                  Service service) {
        this.prescriptionService = prescriptionService;
        this.appointmentService = appointmentService;
        this.service = service;
    }

    @PostMapping("/{token}")
    public ResponseEntity<?> savePrescription(
            @PathVariable String token,
            @RequestBody Prescription prescription
    ) {
        Map<String, String> errors = service.validateToken(token, "doctor");
        if (!errors.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);

        if (prescription.getAppointmentId() != null) {
            appointmentService.changeStatus(prescription.getAppointmentId(), 1);
        }

        return prescriptionService.savePrescription(prescription);
    }

    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<?> getPrescription(
            @PathVariable Long appointmentId,
            @PathVariable String token
    ) {
        Map<String, String> errors = service.validateToken(token, "doctor");
        if (!errors.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);

        return prescriptionService.getPrescription(appointmentId);
    }
}