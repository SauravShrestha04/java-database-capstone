package com.project.back_end.services;

import com.project_back_end.models.Appointment;
import com.project_back_end.models.Doctor;
import com.project_back_end.models.Patient;
import com.project_back_end.repo.AppointmentRepository;
import com.project_back_end.repo.DoctorRepository;
import com.project_back_end.repo.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TokenService tokenService;
    private final Service sharedService; // THIS IS YOUR custom Service class

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              TokenService tokenService,
                              Service sharedService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.tokenService = tokenService;
        this.sharedService = sharedService;
    }

    // Book a new appointment
    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // Update an existing appointment
    @Transactional
    public ResponseEntity<Map<String,String>> updateAppointment(Appointment appointment) {
        Map<String,String> res = new HashMap<>();

        if (appointment.getId() == null) {
            res.put("message", "Appointment ID is required.");
            return ResponseEntity.badRequest().body(res);
        }

        if (!appointmentRepository.existsById(appointment.getId())) {
            res.put("message", "Appointment not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
        }

        int validation = sharedService.validateAppointment(appointment);
        if (validation == -1) {
            res.put("message", "Doctor not found or invalid details.");
            return ResponseEntity.badRequest().body(res);
        }
        if (validation == 0) {
            res.put("message", "Doctor not available at selected time.");
            return ResponseEntity.badRequest().body(res);
        }

        appointmentRepository.save(appointment);
        res.put("message", "Appointment updated.");
        return ResponseEntity.ok(res);
    }

    // Cancel an appointment
    @Transactional
    public ResponseEntity<Map<String,String>> cancelAppointment(long id, String token) {
        Map<String,String> res = new HashMap<>();

        Optional<Appointment> opt = appointmentRepository.findById(id);
        if (opt.isEmpty()) {
            res.put("message", "Appointment not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
        }

        Appointment appointment = opt.get();
        Long patientIdFromToken = tokenService.getPatientIdFromToken(token);

        if (appointment.getPatient() == null ||
            !Objects.equals(appointment.getPatient().getId(), patientIdFromToken)) {
            res.put("message", "Unauthorized to cancel.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
        }

        appointmentRepository.delete(appointment);
        res.put("message", "Appointment cancelled.");
        return ResponseEntity.ok(res);
    }

    // Get appointments for doctor
    @Transactional
    public Map<String,Object> getAppointment(String pname, LocalDate date, String token) {
        Map<String,Object> res = new HashMap<>();

        Long doctorId = tokenService.getDoctorIdFromToken(token);
        Optional<Doctor> doc = doctorRepository.findById(doctorId);

        if (doc.isEmpty()) {
            res.put("message", "Invalid doctor token.");
            res.put("appointments", Collections.emptyList());
            return res;
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Appointment> list;

        if (pname != null && !pname.equalsIgnoreCase("null") && !pname.isBlank()) {
            list = appointmentRepository.findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                    doctorId, pname.trim(), start, end
            );
        } else {
            list = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                    doctorId, start, end
            );
        }

        res.put("appointments", list);
        return res;
    }

    // Update appointment status
    @Transactional
    public ResponseEntity<Map<String,String>> changeStatus(long id, int status) {
        Map<String,String> res = new HashMap<>();

        if (!appointmentRepository.existsById(id)) {
            res.put("message", "Appointment not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
        }

        appointmentRepository.updateStatus(status, id);

        res.put("message", "Status updated.");
        return ResponseEntity.ok(res);
    }
}