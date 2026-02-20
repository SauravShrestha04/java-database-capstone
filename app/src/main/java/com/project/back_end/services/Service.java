package com.project.back_end.services;

public class Service {
// 1. **@Service Annotation**
// The @Service annotation marks this class as a service component in Spring. This allows Spring to automatically detect it through component scanning
// and manage its lifecycle, enabling it to be injected into controllers or other services using @Autowired or constructor injection.

// 2. **Constructor Injection for Dependencies**
// The constructor injects all required dependencies (TokenService, Repositories, and other Services). This approach promotes loose coupling, improves testability,
// and ensures that all required dependencies are provided at object creation time.

// 3. **validateToken Method**
// This method checks if the provided JWT token is valid for a specific user. It uses the TokenService to perform the validation.
// If the token is invalid or expired, it returns a 401 Unauthorized response with an appropriate error message. This ensures security by preventing
// unauthorized access to protected resources.

// 4. **validateAdmin Method**
// This method validates the login credentials for an admin user.
// - It first searches the admin repository using the provided username.
// - If an admin is found, it checks if the password matches.
// - If the password is correct, it generates and returns a JWT token (using the admin’s username) with a 200 OK status.
// - If the password is incorrect, it returns a 401 Unauthorized status with an error message.
// - If no admin is found, it also returns a 401 Unauthorized.
// - If any unexpected error occurs during the process, a 500 Internal Server Error response is returned.
// This method ensures that only valid admin users can access secured parts of the system.

// 5. **filterDoctor Method**
// This method provides filtering functionality for doctors based on name, specialty, and available time slots.
// - It supports various combinations of the three filters.
// - If none of the filters are provided, it returns all available doctors.
// This flexible filtering mechanism allows the frontend or consumers of the API to search and narrow down doctors based on user criteria.

// 6. **validateAppointment Method**
// This method validates if the requested appointment time for a doctor is available.
// - It first checks if the doctor exists in the repository.
// - Then, it retrieves the list of available time slots for the doctor on the specified date.
// - It compares the requested appointment time with the start times of these slots.
// - If a match is found, it returns 1 (valid appointment time).
// - If no matching time slot is found, it returns 0 (invalid).
// - If the doctor doesn’t exist, it returns -1.
// This logic prevents overlapping or invalid appointment bookings.

// 7. **validatePatient Method**
// This method checks whether a patient with the same email or phone number already exists in the system.
// - If a match is found, it returns false (indicating the patient is not valid for new registration).
// - If no match is found, it returns true.
// This helps enforce uniqueness constraints on patient records and prevent duplicate entries.

// 8. **validatePatientLogin Method**
// This method handles login validation for patient users.
// - It looks up the patient by email.
// - If found, it checks whether the provided password matches the stored one.
// - On successful validation, it generates a JWT token and returns it with a 200 OK status.
// - If the password is incorrect or the patient doesn't exist, it returns a 401 Unauthorized with a relevant error.
// - If an exception occurs, it returns a 500 Internal Server Error.
// This method ensures only legitimate patients can log in and access their data securely.

// 9. **filterPatient Method**
// This method filters a patient's appointment history based on condition and doctor name.
// - It extracts the email from the JWT token to identify the patient.
// - Depending on which filters (condition, doctor name) are provided, it delegates the filtering logic to PatientService.
// - If no filters are provided, it retrieves all appointments for the patient.
// This flexible method supports patient-specific querying and enhances user experience on the client side.


}
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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Service {
// 1. **@Service Annotation**
// The @Service annotation marks this class as a service component in Spring. This allows Spring to automatically detect it through component scanning
// and manage its lifecycle, enabling it to be injected into controllers or other services using @Autowired or constructor injection.

// 2. **Constructor Injection for Dependencies**
// The constructor injects all required dependencies (TokenService, Repositories, and other Services). This approach promotes loose coupling, improves testability,
// and ensures that all required dependencies are provided at object creation time.

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

// 3. **validateToken Method**
// This method checks if the provided JWT token is valid for a specific user. It uses the TokenService to perform the validation.
// If the token is invalid or expired, it returns a 401 Unauthorized response with an appropriate error message. This ensures security by preventing
// unauthorized access to protected resources.

    public Map<String, String> validateToken(String token, String user) {
        Map<String, String> result = new HashMap<>();
        try {
            boolean valid = tokenService.validateToken(token, user);
            if (!valid) {
                result.put("message", "Invalid or expired token");
            }
        } catch (Exception e) {
            result.put("message", "Error while validating token");
        }
        return result;
    }

// 4. **validateAdmin Method**
// This method validates the login credentials for an admin user.
// - It first searches the admin repository using the provided username.
// - If an admin is found, it checks if the password matches.
// - If the password is correct, it generates and returns a JWT token (using the admin’s username) with a 200 OK status.
// - If the password is incorrect, it returns a 401 Unauthorized status with an error message.
// - If no admin is found, it also returns a 401 Unauthorized.
// - If any unexpected error occurs during the process, a 500 Internal Server Error response is returned.
// This method ensures that only valid admin users can access secured parts of the system.

    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> response = new HashMap<>();
        try {
            if (receivedAdmin == null || receivedAdmin.getUsername() == null) {
                response.put("message", "Invalid request");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
            if (admin == null) {
                response.put("message", "Admin not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            if (!admin.getPassword().equals(receivedAdmin.getPassword())) {
                response.put("message", "Invalid username or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String token = tokenService.generateToken(admin.getUsername(), "admin");
            response.put("token", token);
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

// 5. **filterDoctor Method**
// This method provides filtering functionality for doctors based on name, specialty, and available time slots.
// - It supports various combinations of the three filters.
// - If none of the filters are provided, it returns all available doctors.
// This flexible filtering mechanism allows the frontend or consumers of the API to search and narrow down doctors based on user criteria.

    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        boolean hasName = name != null && !name.trim().isEmpty();
        boolean hasSpec = specialty != null && !specialty.trim().isEmpty();
        boolean hasTime = time != null && !time.trim().isEmpty();

        if (hasName && hasSpec && hasTime) {
            return doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
        } else if (hasName && hasTime) {
            return doctorService.filterDoctorByNameAndTime(name, time);
        } else if (hasName && hasSpec) {
            return doctorService.filterDoctorByNameAndSpecility(name, specialty);
        } else if (hasSpec && hasTime) {
            return doctorService.filterDoctorByTimeAndSpecility(specialty, time);
        } else if (hasSpec) {
            return doctorService.filterDoctorBySpecility(specialty);
        } else if (hasTime) {
            return doctorService.filterDoctorsByTime(time);
        } else if (hasName) {
            return doctorService.findDoctorByName(name);
        } else {
            Map<String, Object> result = new HashMap<>();
            result.put("doctors", doctorService.getDoctors());
            return result;
        }
    }

// 6. **validateAppointment Method**
// This method validates if the requested appointment time for a doctor is available.
// - It first checks if the doctor exists in the repository.
// - Then, it retrieves the list of available time slots for the doctor on the specified date.
// - It compares the requested appointment time with the start times of these slots.
// - If a match is found, it returns 1 (valid appointment time).
// - If no matching time slot is found, it returns 0 (invalid).
// - If the doctor doesn’t exist, it returns -1.
// This logic prevents overlapping or invalid appointment bookings.

    public int validateAppointment(Appointment appointment) {
        try {
            if (appointment == null || appointment.getDoctor() == null) {
                return -1;
            }

            Long doctorId = appointment.getDoctor().getId();
            if (doctorId == null || doctorRepository.findById(doctorId).isEmpty()) {
                return -1;
            }

            LocalDateTime appointmentTime = appointment.getAppointmentTime();
            if (appointmentTime == null) {
                return 0;
            }

            LocalDate date = appointmentTime.toLocalDate();
            LocalTime time = appointmentTime.toLocalTime();

            List<String> availableSlots = doctorService.getDoctorAvailability(doctorId, date);
            if (availableSlots == null || availableSlots.isEmpty()) {
                return 0;
            }

            String timeStr = time.toString(); // e.g. "10:00" or "10:00:00" depending on how slots are stored
            for (String slot : availableSlots) {
                if (slot != null && slot.startsWith(timeStr.substring(0, 5))) {
                    return 1;
                }
            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

// 7. **validatePatient Method**
// This method checks whether a patient with the same email or phone number already exists in the system.
// - If a match is found, it returns false (indicating the patient is not valid for new registration).
// - If no match is found, it returns true.
// This helps enforce uniqueness constraints on patient records and prevent duplicate entries.

    public boolean validatePatient(Patient patient) {
        if (patient == null) {
            return false;
        }
        String email = patient.getEmail();
        String phone = patient.getPhone();
        Patient existing = patientRepository.findByEmailOrPhone(email, phone);
        return existing == null;
    }

// 8. **validatePatientLogin Method**
// This method handles login validation for patient users.
// - It looks up the patient by email.
// - If found, it checks whether the provided password matches the stored one.
// - On successful validation, it generates a JWT token and returns it with a 200 OK status.
// - If the password is incorrect or the patient doesn't exist, it returns a 401 Unauthorized with a relevant error.
// - If an exception occurs, it returns a 500 Internal Server Error.
// This method ensures only legitimate patients can log in and access their data securely.

    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> response = new HashMap<>();
        try {
            if (login == null || login.getIdentifier() == null) {
                response.put("message", "Invalid request");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            String email = login.getIdentifier();
            Patient patient = patientRepository.findByEmail(email);
            if (patient == null) {
                response.put("message", "Patient not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            if (!patient.getPassword().equals(login.getPassword())) {
                response.put("message", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String token = tokenService.generateToken(patient.getEmail(), "patient");
            response.put("token", token);
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

// 9. **filterPatient Method**
// This method filters a patient's appointment history based on condition and doctor name.
// - It extracts the email from the JWT token to identify the patient.
// - Depending on which filters (condition, doctor name) are provided, it delegates the filtering logic to PatientService.
// - If no filters are provided, it retrieves all appointments for the patient.
// This flexible method supports patient-specific querying and enhances user experience on the client side.

    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        try {
            String email = tokenService.getEmailFromToken(token);
            if (email == null) {
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
            boolean hasCondition = condition != null && !condition.trim().isEmpty();
            boolean hasName = name != null && !name.trim().isEmpty();

            if (hasCondition && hasName) {
                return patientService.filterByDoctorAndCondition(condition, name, patientId);
            } else if (hasCondition) {
                return patientService.filterByCondition(condition, patientId);
            } else if (hasName) {
                return patientService.filterByDoctor(name, patientId);
            } else {
                return patientService.getPatientAppointment(patientId, token);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> body = new HashMap<>();
            body.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

}