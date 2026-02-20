package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Admin; 
import com.project.back_end.model.Appointment;
import com.project.back_end.model.Doctor;
import com.project.back_end.model.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/*
  1. **Add @Service Annotation**:
    - The `@Service` annotation is used to mark this class as a Spring service component. 
    - It will be managed by Spring's container and used for business logic related to patients and appointments.
    - Instruction: Ensure that the `@Service` annotation is applied above the class declaration.

  2. **Constructor Injection for Dependencies**:
    - The `PatientService` class has dependencies on `PatientRepository`, `AppointmentRepository`, and `TokenService`.
    - These dependencies are injected via the constructor to maintain good practices of dependency injection and testing.
    - Instruction: Ensure constructor injection is used for all the required dependencies.

  3. **createPatient Method**:
    - Creates a new patient in the database. It saves the patient object using the `PatientRepository`.
    - If the patient is successfully saved, the method returns `1`; otherwise, it logs the error and returns `0`.
    - Instruction: Ensure that error handling is done properly and exceptions are caught and logged appropriately.

  4. **getPatientAppointment Method**:
    - Retrieves a list of appointments for a specific patient, based on their ID.
    - The appointments are then converted into `AppointmentDTO` objects for easier consumption by the API client.
    - This method is marked as `@Transactional` to ensure database consistency during the transaction.
    - Instruction: Ensure that appointment data is properly converted into DTOs and the method handles errors gracefully.

  5. **filterByCondition Method**:
    - Filters appointments for a patient based on the condition (e.g., "past" or "future").
    - Retrieves appointments with a specific status (0 for future, 1 for past) for the patient.
    - Converts the appointments into `AppointmentDTO` and returns them in the response.
    - Instruction: Ensure the method correctly handles "past" and "future" conditions, and that invalid conditions are caught and returned as errors.

  6. **filterByDoctor Method**:
    - Filters appointments for a patient based on the doctor's name.
    - It retrieves appointments where the doctor’s name matches the given value, and the patient ID matches the provided ID.
    - Instruction: Ensure that the method correctly filters by doctor's name and patient ID and handles any errors or invalid cases.

  7. **filterByDoctorAndCondition Method**:
    - Filters appointments based on both the doctor's name and the condition (past or future) for a specific patient.
    - This method combines filtering by doctor name and appointment status (past or future).
    - Converts the appointments into `AppointmentDTO` objects and returns them in the response.
    - Instruction: Ensure that the filter handles both doctor name and condition properly, and catches errors for invalid input.

  8. **getPatientDetails Method**:
    - Retrieves patient details using the `tokenService` to extract the patient's email from the provided token.
    - Once the email is extracted, it fetches the corresponding patient from the `patientRepository`.
    - It returns the patient's information in the response body.
    //    - Instruction: Make sure that the token extraction process works correctly and patient details are fetched properly based on the extracted email.

  9. **Handling Exceptions and Errors**:
    - The service methods handle exceptions using try-catch blocks and log any issues that occur. If an error occurs during database operations, the service responds with appropriate HTTP status codes (e.g., `500 Internal Server Error`).
    - Instruction: Ensure that error handling is consistent across the service, with proper logging and meaningful error messages returned to the client.

  10. **Use of DTOs (Data Transfer Objects)**:
    - The service uses `AppointmentDTO` to transfer appointment-related data between layers. This ensures that sensitive or unnecessary data (e.g., password or private patient information) is not exposed in the response.
    - Instruction: Ensure that DTOs are used appropriately to limit the exposure of internal data and only send the relevant fields to the client.
*/

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // 3. createPatient Method
    public int createPatient(Patient patient) {
        try {
            patientRepository.save(patient);
            return 1; // success
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // failure
        }
    }

    // 4. getPatientAppointment Method
    @Transactional
    public ResponseEntity<Map<String, Object>> getPatientAppointment(Long id, String token) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Extract email from token and fetch patient
            String email = tokenService.getEmailFromToken(token);
            Patient tokenPatient = patientRepository.findByEmail(email);

            if (tokenPatient == null || !tokenPatient.getId().equals(id)) {
                response.put("message", "Unauthorized access to patient appointments.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            List<Appointment> appointments = appointmentRepository.findByPatientId(id);
            List<AppointmentDTO> appointmentDTOs = appointments.stream()
                    .map(this::toAppointmentDTO)
                    .collect(Collectors.toList());

            response.put("appointments", appointmentDTOs);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "Error fetching appointments.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 5. filterByCondition Method
    @Transactional
    public ResponseEntity<Map<String, Object>> filterByCondition(String condition, Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            int status;
            if ("past".equalsIgnoreCase(condition)) {
                status = 1; // past
            } else if ("future".equalsIgnoreCase(condition)) {
                status = 0; // future
            } else {
                response.put("message", "Invalid condition. Use 'past' or 'future'.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            List<Appointment> appointments =
                    appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, status);

            List<AppointmentDTO> appointmentDTOs = appointments.stream()
                    .map(this::toAppointmentDTO)
                    .collect(Collectors.toList());

            response.put("appointments", appointmentDTOs);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "Error filtering appointments.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 6. filterByDoctor Method
    @Transactional
    public ResponseEntity<Map<String, Object>> filterByDoctor(String name, Long patientId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Appointment> appointments =
                    appointmentRepository.filterByDoctorNameAndPatientId(name, patientId);

            List<AppointmentDTO> appointmentDTOs = appointments.stream()
                    .map(this::toAppointmentDTO)
                    .collect(Collectors.toList());

            response.put("appointments", appointmentDTOs);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "Error filtering appointments by doctor.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 7. filterByDoctorAndCondition Method
    @Transactional
    public ResponseEntity<Map<String, Object>> filterByDoctorAndCondition(String condition,
                                                                          String name,
                                                                          long patientId) {
        Map<String, Object> response = new HashMap<>();
        try {
            int status;
            if ("past".equalsIgnoreCase(condition)) {
                status = 1;
            } else if ("future".equalsIgnoreCase(condition)) {
                status = 0;
            } else {
                response.put("message", "Invalid condition. Use 'past' or 'future'.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            List<Appointment> appointments =
                    appointmentRepository.filterByDoctorNameAndPatientIdAndStatus(name, patientId, status);

            List<AppointmentDTO> appointmentDTOs = appointments.stream()
                    .map(this::toAppointmentDTO)
                    .collect(Collectors.toList());

            response.put("appointments", appointmentDTOs);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "Error filtering appointments by doctor and condition.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 8. getPatientDetails Method
    @Transactional
    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        Map<String, Object> response = new HashMap<>();
        try {
            String email = tokenService.getEmailFromToken(token);
            Patient patient = patientRepository.findByEmail(email);

            if (patient == null) {
                response.put("message", "Patient not found.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            response.put("patient", patient);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "Error fetching patient details.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Helper: convert Appointment -> AppointmentDTO
    private AppointmentDTO toAppointmentDTO(Appointment appointment) {
        Long id = appointment.getId();

        Doctor doctor = appointment.getDoctor();
        Patient patient = appointment.getPatient();

        Long doctorId = (doctor != null) ? doctor.getId() : null;
        String doctorName = (doctor != null) ? doctor.getName() : null;

        Long patientId = (patient != null) ? patient.getId() : null;
        String patientName = (patient != null) ? patient.getName() : null;
        String patientEmail = (patient != null) ? patient.getEmail() : null;
        String patientPhone = (patient != null) ? patient.getPhone() : null;
        String patientAddress = (patient != null) ? patient.getAddress() : null;

        LocalDateTime appointmentTime = appointment.getAppointmentTime();
        int status = appointment.getStatus();

        return new AppointmentDTO(
                id,
                doctorId,
                doctorName,
                patientId,
                patientName,
                patientEmail,
                patientPhone,
                patientAddress,
                appointmentTime,
                status
        );
    }
}