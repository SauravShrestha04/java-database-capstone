package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.model.Admin;
import com.project.back_end.model.Appointment;
import com.project.back_end.model.Doctor;
import com.project.back_end.model.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class Service {

    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
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

    // Validate token
    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        Map<String, String> body = new HashMap<>();
        boolean valid = tokenService.validateToken(token, user);

        if (!valid) {
            body.put("message", "Invalid or expired token");
            return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    // Admin login
    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> body = new HashMap<>();
        try {
            Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
            if (admin == null) {
                body.put("message", "Admin not found");
                return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
            }
            if (!admin.getPassword().equals(receivedAdmin.getPassword())) {
                body.put("message", "Invalid credentials");
                return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
            }
            String token = tokenService.generateToken(admin.getUsername());
            body.put("token", token);
            body.put("message", "Login successful");
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch (Exception e) {
            body.put("message", "Internal server error");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Filter doctors
    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        boolean hasName = name != null && !"null".equalsIgnoreCase(name) && !name.isEmpty();
        boolean hasSpecialty = specialty != null && !"null".equalsIgnoreCase(specialty) && !specialty.isEmpty();
        boolean hasTime = time != null && !"null".equalsIgnoreCase(time) && !time.isEmpty();

        Map<String, Object> result;

        if (hasName && hasSpecialty && hasTime)
            result = doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
        else if (hasName && hasSpecialty)
            result = doctorService.filterDoctorByNameAndSpecility(name, specialty);
        else if (hasName && hasTime)
            result = doctorService.filterDoctorByNameAndTime(name, time);
        else if (hasSpecialty && hasTime)
            result = doctorService.filterDoctorByTimeAndSpecility(specialty, time);
        else if (hasName)
            result = doctorService.findDoctorByName(name);
        else if (hasSpecialty)
            result = doctorService.filterDoctorBySpecility(specialty);
        else if (hasTime)
            result = doctorService.filterDoctorsByTime(time);
        else {
            result = new HashMap<>();
            result.put("doctors", doctorService.getDoctors());
        }

        return result;
    }

    // Validate appointment slot
    @Transactional(readOnly = true)
    public int validateAppointment(Appointment appointment) {
        if (appointment == null || appointment.getDoctor() == null) return -1;

        Long doctorId = appointment.getDoctor().getId();
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) return -1;

        LocalDate date = appointment.getAppointmentTime().toLocalDate();
        List<String> availableSlots = doctorService.getDoctorAvailability(doctorId, date);

        String requestedTime = appointment.getAppointmentTime().toLocalTime().toString();
        return availableSlots.contains(requestedTime) ? 1 : 0;
    }

    // Check patient uniqueness
    @Transactional(readOnly = true)
    public boolean validatePatient(Patient patient) {
        if (patient == null) return false;
        Patient existing = patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone());
        return existing == null;
    }

    // Patient login
    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> body = new HashMap<>();
        try {
            Patient patient = patientRepository.findByEmail(login.getIdentifier());
            if (patient == null) {
                body.put("message", "Patient not found");
                return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
            }
            if (!patient.getPassword().equals(login.getPassword())) {
                body.put("message", "Invalid credentials");
                return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
            }

            String token = tokenService.generateToken(patient.getEmail());
            body.put("token", token);
            body.put("message", "Login successful");
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch (Exception e) {
            body.put("message", "Internal server error");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Filter patient appointments
    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        Map<String, Object> body = new HashMap<>();
        try {
            String email = tokenService.extractIdentifier(token);
            Patient patient = patientRepository.findByEmail(email);

            if (patient == null) {
                body.put("message", "Patient not found");
                return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
            }

            long patientId = patient.getId();

            boolean hasCondition = condition != null && !"null".equalsIgnoreCase(condition) && !condition.isEmpty();
            boolean hasName = name != null && !"null".equalsIgnoreCase(name) && !name.isEmpty();

            if (hasCondition && hasName)
                return patientService.filterByDoctorAndCondition(condition, name, patientId);
            else if (hasCondition)
                return patientService.filterByCondition(condition, patientId);
            else if (hasName)
                return patientService.filterByDoctor(name, patientId);
            else
                return patientService.getPatientAppointment(patientId, token);

        } catch (Exception e) {
            body.put("message", "Internal server error");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}