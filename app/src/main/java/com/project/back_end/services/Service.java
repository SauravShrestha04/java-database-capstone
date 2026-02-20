package com.project.back_end.services;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

@Service
public class Service {

    // Token service for JWT operations
    private final TokenService tokenService;

    // Repositories for data access
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    // Other services for business logic
    private final DoctorService doctorService;
    private final PatientService patientService;

    public Service(TokenService tokenService,
                   AdminRepository adminRepository,
                   DoctorRepository doctorRepository,
                   PatientRepository patientRepository,
                   DoctorService doctorService,
                   PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    // Validate token for a given user type
    // EMPTY map = token is valid
    // NON-EMPTY map (contains "message") = token invalid / missing
    public Map<String, String> validateToken(String token, String user) {
        if (token == null || token.isBlank()) {
            Map<String, String> body = new HashMap<>();
            body.put("message", "Token is missing");
            return body;
        }

        boolean isValid = tokenService.validateToken(token, user);

        if (!isValid) {
            Map<String, String> body = new HashMap<>();
            body.put("message", "Token is invalid or expired");
            return body;
        }

        // Valid token → no errors
        return Collections.emptyMap();
    }

    // Validate admin credentials and generate token
    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> body = new HashMap<>();
        try {
            Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
            if (admin == null) {
                body.put("message", "Admin not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }

            if (!admin.getPassword().equals(receivedAdmin.getPassword())) {
                body.put("message", "Invalid username or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }

            String token = tokenService.generateToken(admin.getUsername());
            body.put("token", token);
            body.put("role", "admin");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // Filter doctors by name, specialty, and time
    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        boolean hasName = name != null && !name.isBlank();
        boolean hasSpec = specialty != null && !specialty.isBlank();
        boolean hasTime = time != null && !time.isBlank();

        if (hasName && hasSpec && hasTime) {
            // name + specialty + time
            return doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
        } else if (hasName && hasSpec) {
            // name + specialty
            return doctorService.filterDoctorByNameAndSpecility(name, specialty);
        } else if (hasName && hasTime) {
            // name + time
            return doctorService.filterDoctorByNameAndTime(name, time);
        } else if (hasSpec && hasTime) {
            // specialty + time
            return doctorService.filterDoctorByTimeAndSpecility(specialty, time);
        } else if (hasName) {
            // only name
            return doctorService.findDoctorByName(name);
        } else if (hasSpec) {
            // only specialty
            return doctorService.filterDoctorBySpecility(specialty);
        } else if (hasTime) {
            // only time
            return doctorService.filterDoctorsByTime(time);
        }

        // no filters: return all doctors
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", doctorService.getDoctors());
        return result;
    }

    // Validate whether an appointment is possible
    public int validateAppointment(Appointment appointment) {
        if (appointment == null || appointment.getDoctor() == null) {
            return -1;
        }

        Long doctorId = appointment.getDoctor().getId();
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);

        if (doctorOpt.isEmpty()) {
            // doctor does not exist
            return -1;
        }

        if (appointment.getAppointmentTime() == null) {
            // missing appointment time
            return 0;
        }

        LocalDate date = appointment.getAppointmentTime().toLocalDate();
        List<String> availableSlots = doctorService.getDoctorAvailability(doctorId, date);

        if (availableSlots == null || availableSlots.isEmpty()) {
            return 0;
        }

        String requestedTime = appointment.getAppointmentTime().toLocalTime().toString();

        return availableSlots.contains(requestedTime) ? 1 : 0;
    }

    // Check if patient does not already exist
    public boolean validatePatient(Patient patient) {
        if (patient == null) {
            return false;
        }

        String email = patient.getEmail();
        String phone = patient.getPhone();

        Patient existing = patientRepository.findByEmailOrPhone(email, phone);
        return existing == null;
    }

    // Validate patient login and generate token
    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> body = new HashMap<>();

        try {
            String identifier = login.getIdentifier(); // for patient this is email
            Patient patient = patientRepository.findByEmail(identifier);

            if (patient == null) {
                body.put("message", "Patient not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }

            if (!patient.getPassword().equals(login.getPassword())) {
                body.put("message", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }

            String token = tokenService.generateToken(patient.getEmail());
            body.put("token", token);
            body.put("role", "patient");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // Filter patient appointments based on condition and doctor name
    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        try {
            String email = tokenService.extractIdentifier(token);
            if (email == null || email.isBlank()) {
                Map<String, Object> body = new HashMap<>();
                body.put("message", "Invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }

            Patient patient = patientRepository.findByEmail(email);
            if (patient == null) {
                Map<String, Object> body = new HashMap<>();
                body.put("message", "Patient not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }

            Long patientId = patient.getId();
            boolean hasCondition = condition != null && !condition.isBlank();
            boolean hasName = name != null && !name.isBlank();

            if (hasCondition && hasName) {
                // filter by doctor and condition
                return patientService.filterByDoctorAndCondition(condition, name, patientId);
            } else if (hasCondition) {
                // filter by condition only
                return patientService.filterByCondition(condition, patientId);
            } else if (hasName) {
                // filter by doctor only
                return patientService.filterByDoctor(name, patientId);
            } else {
                // no filters: return all appointments for patient
                return patientService.getPatientAppointment(patientId, token);
            }

        } catch (Exception e) {
            Map<String, Object> body = new HashMap<>();
            body.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
}