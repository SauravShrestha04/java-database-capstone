package com.project.back_end.services;

import com.project.back_end.models.Prescription;
import com.project.back_end.repo.PrescriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PrescriptionService {
    
    private final PrescriptionRepository prescriptionRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    public ResponseEntity<Map<String, String>> savePrescription(Prescription prescription) {
        Map<String, String> response = new HashMap<>();
        try {
            Long appointmentId = prescription.getAppointmentId();
            // Check if a prescription for this appointment already exists
            List<Prescription> existing =
                    prescriptionRepository.findByAppointmentId(appointmentId);

            if (existing != null && !existing.isEmpty()) {
                response.put("message", "Prescription already exists for this appointment.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            prescriptionRepository.save(prescription);
            response.put("message", "Prescription saved");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "Error saving prescription");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    public ResponseEntity<Map<String, Object>> getPrescription(Long appointmentId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Prescription> prescriptions =
                    prescriptionRepository.findByAppointmentId(appointmentId);

            if (prescriptions == null || prescriptions.isEmpty()) {
                response.put("message", "No prescription found for the given appointment ID.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // If you expect only one, you could return prescriptions.get(0)
            response.put("prescriptions", prescriptions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "Error fetching prescription");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}