package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.Service;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("${api.path}appointment")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final Service service;

    public AppointmentController(AppointmentService appointmentService, Service service) {
        this.appointmentService = appointmentService;
        this.service = service;
    }

    @GetMapping("/{date}/{pname}/{token}")
    public ResponseEntity<?> getAppointments(
            @PathVariable String date,
            @PathVariable String pname,
            @PathVariable String token
    ) {

        Map<String, String> errors = service.validateToken(token, "doctor");
        if (!errors.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);

        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(date);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid date format"));
        }

        return ResponseEntity.ok(
                appointmentService.getAppointment(pname, parsedDate, token)
        );
    }

    @PostMapping("/{token}")
    public ResponseEntity<?> bookAppointment(
            @PathVariable String token,
            @RequestBody Appointment appointment
    ) {
        Map<String, String> errors = service.validateToken(token, "patient");
        if (!errors.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);

        int validation = service.validateAppointment(appointment);

        if (validation == -1)
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid doctor or appointment details"));

        if (validation == 0)
            return ResponseEntity.badRequest().body(Map.of("message", "Doctor not available"));

        int result = appointmentService.bookAppointment(appointment);

        if (result == 1)
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Appointment booked"));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error booking appointment"));
    }

    @PutMapping("/{token}")
    public ResponseEntity<?> updateAppointment(
            @PathVariable String token,
            @RequestBody Appointment appointment
    ) {
        Map<String, String> errors = service.validateToken(token, "patient");
        if (!errors.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);

        return appointmentService.updateAppointment(appointment);
    }

    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable long id,
            @PathVariable String token
    ) {
        Map<String, String> errors = service.validateToken(token, "patient");
        if (!errors.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);

        return appointmentService.cancelAppointment(id, token);
    }
}