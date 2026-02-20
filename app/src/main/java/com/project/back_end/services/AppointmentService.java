package com.project.back_end.services;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
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
    // explicitly refer to your custom Service class to avoid confusion with the annotation
    private final com.project.back_end.services.Service sharedService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              TokenService tokenService,
                              com.project.back_end.services.Service sharedService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.tokenService = tokenService;
        this.sharedService = sharedService;
    }

    /**
     * Book a new appointment.
     * Returns 1 on success, 0 on failure.
     */
    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    /**
     * Update an existing appointment.
     * Uses sharedService.validateAppointment(appointment) to check:
     *  - -1 → invalid doctor / bad details
     *  -  0 → doctor not available at that time
     *  -  1 → OK
     */
    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
        Map<String, String> response = new HashMap<>();

        if (appointment.getId() == null) {
            response.put("message", "Appointment ID is required for update.");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<Appointment> existingOpt = appointmentRepository.findById(appointment.getId());
        if (existingOpt.isEmpty()) {
            response.put("message", "Appointment not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        int validationCode = sharedService.validateAppointment(appointment);
        if (validationCode == -1) {
            response.put("message", "Doctor not found or invalid appointment details.");
            return ResponseEntity.badRequest().body(response);
        }
        if (validationCode == 0) {
            response.put("message", "Doctor is not available at the selected time.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            appointmentRepository.save(appointment);
            response.put("message", "Appointment updated successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            ex.printStackTrace();
            response.put("message", "Error updating appointment.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Cancel an appointment.
     * Only the patient who owns the appointment (from token) can cancel.
     */
    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Map<String, String> response = new HashMap<>();

        Optional<Appointment> existingOpt = appointmentRepository.findById(id);
        if (existingOpt.isEmpty()) {
            response.put("message", "Appointment not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        Appointment appointment = existingOpt.get();

        Long tokenPatientId = tokenService.getPatientIdFromToken(token);
        Patient appointmentPatient = appointment.getPatient();

        if (appointmentPatient == null || !Objects.equals(appointmentPatient.getId(), tokenPatientId)) {
            response.put("message", "You are not authorized to cancel this appointment.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        try {
            appointmentRepository.delete(appointment);
            response.put("message", "Appointment cancelled successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            ex.printStackTrace();
            response.put("message", "Error cancelling appointment.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get appointments for a doctor (from token) on a given date,
     * optionally filtered by patient name.
     */
    @Transactional
    public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
        Map<String, Object> result = new HashMap<>();

        Long doctorId = tokenService.getDoctorIdFromToken(token);
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            result.put("appointments", Collections.emptyList());
            result.put("message", "Invalid doctor token.");
            return result;
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime startOfNextDay = date.plusDays(1).atStartOfDay();

        List<Appointment> appointments;

        if (pname != null && !pname.trim().isEmpty() && !"null".equalsIgnoreCase(pname.trim())) {
            appointments = appointmentRepository
                    .findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                            doctorId,
                            pname.trim(),
                            startOfDay,
                            startOfNextDay
                    );
        } else {
            appointments = appointmentRepository
                    .findByDoctorIdAndAppointmentTimeBetween(
                            doctorId,
                            startOfDay,
                            startOfNextDay
                    );
        }

        result.put("appointments", appointments);
        return result;
    }

    /**
     * Change the status of an appointment.
     */
    @Transactional
    public ResponseEntity<Map<String, String>> changeStatus(long id, int status) {
        Map<String, String> response = new HashMap<>();

        Optional<Appointment> existingOpt = appointmentRepository.findById(id);
        if (existingOpt.isEmpty()) {
            response.put("message", "Appointment not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        try {
            appointmentRepository.updateStatus(status, id);
            response.put("message", "Appointment status updated successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            ex.printStackTrace();
            response.put("message", "Error updating appointment status.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}