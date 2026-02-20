package com.project.back_end.controllers;

import com.project.back_end.models.Patient; 
import com.project.back_end.DTO.Login;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/patient")
public class PatientController {

    private final PatientService patientService;
    private final Service service;

    public PatientController(PatientService patientService, Service service) {
        this.patientService = patientService;
        this.service = service;
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> getPatientDetails(@PathVariable String token) {
        // Validate token for patient role
        Map<String, String> validationErrors = service.validateToken(token, "patient");
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validationErrors);
        }

        // Delegate to patientService to get patient details
        return patientService.getPatientDetails(token);
    }

    @PostMapping
    public ResponseEntity<?> createPatient(@RequestBody Patient patient) {
        Map<String, String> response = new HashMap<>();

        // Check if patient already exists (email or phone)
        boolean isValid = service.validatePatient(patient);
        if (!isValid) {
            response.put("message", "Patient with email id or phone no already exist");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        int result = patientService.createPatient(patient);
        if (result == 1) {
            response.put("message", "Signup successful");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login login) {
        return service.validatePatientLogin(login);
    }

    @GetMapping("/{id}/{token}")
    public ResponseEntity<?> getPatientAppointment(
            @PathVariable Long id,
            @PathVariable String token) {

        // Validate token for patient role
        Map<String, String> validationErrors = service.validateToken(token, "patient");
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validationErrors);
        }

        // Delegate to patientService (it will also verify that token's patient matches id)
        return patientService.getPatientAppointment(id, token);
    }

    @GetMapping("/filter/{condition}/{name}/{token}")
    public ResponseEntity<?> filterPatientAppointment(
            @PathVariable String condition,
            @PathVariable String name,
            @PathVariable String token) {

        // Validate token for patient role
        Map<String, String> validationErrors = service.validateToken(token, "patient");
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validationErrors);
        }

        // Delegate filtering to shared service (which internally calls PatientService)
        return service.filterPatient(condition, name, token);
    }
}