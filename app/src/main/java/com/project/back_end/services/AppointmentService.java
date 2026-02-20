package com.project.back_end.services;

import com.project.back_end.model.Appointment;
import com.project.back_end.model.Doctor;
import com.project.back_end.model.Patient;
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

/*
  1. **Add @Service Annotation**:
    - To indicate that this class is a service layer class for handling business logic.
    - The `@Service` annotation should be added before the class declaration to mark it as a Spring service component.
    - Instruction: Add `@Service` above the class definition.

  2. **Constructor Injection for Dependencies**:
    - The `AppointmentService` class requires several dependencies like `AppointmentRepository`, `Service`, `TokenService`, `PatientRepository`, and `DoctorRepository`.
    - These dependencies should be injected through the constructor.
    - Instruction: Ensure constructor injection is used for proper dependency management in Spring.

  3. **Add @Transactional Annotation for Methods that Modify Database**:
    - The methods that modify or update the database should be annotated with `@Transactional` to ensure atomicity and consistency of the operations.
    - Instruction: Add the `@Transactional` annotation above methods that interact with the database, especially those modifying data.

  4. **Book Appointment Method**:
    - Responsible for saving the new appointment to the database.
    - If the save operation fails, it returns `0`; otherwise, it returns `1`.
    - Instruction: Ensure that the method handles any exceptions and returns an appropriate result code.

  5. **Update Appointment Method**:
    - This method is used to update an existing appointment based on its ID.
    - It validates whether the patient ID matches, checks if the appointment is available for updating, and ensures that the doctor is available at the specified time.
    - If the update is successful, it saves the appointment; otherwise, it returns an appropriate error message.
    - Instruction: Ensure proper validation and error handling is included for appointment updates.

  6. **Cancel Appointment Method**:
    - This method cancels an appointment by deleting it from the database.
    - It ensures the patient who owns the appointment is trying to cancel it and handles possible errors.
    - Instruction: Make sure that the method checks for the patient ID match before deleting the appointment.

  7. **Get Appointments Method**:
    - This method retrieves a list of appointments for a specific doctor on a particular day, optionally filtered by the patient's name.
    - It uses `@Transactional` to ensure that database operations are consistent and handled in a single transaction.
    - Instruction: Ensure the correct use of transaction boundaries, especially when querying the database for appointments.

  8. **Change Status Method**:
    - This method updates the status of an appointment by changing its value in the database.
    - It should be annotated with `@Transactional` to ensure the operation is executed in a single transaction.
    - Instruction: Add `@Transactional` before this method to ensure atomicity when updating appointment status.
*/

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TokenService tokenService;
    private final Service sharedService; // common service that contains validateAppointment / validateToken

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

    // 4. Book Appointment Method
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

    // 5. Update Appointment Method
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

        // Validate appointment details using shared service (business rules, conflicts, etc.)
        Map<String, String> validationErrors = sharedService.validateAppointment(appointment);
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(validationErrors);
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

    // 6. Cancel Appointment Method
    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Map<String, String> response = new HashMap<>();

        Optional<Appointment> existingOpt = appointmentRepository.findById(id);
        if (existingOpt.isEmpty()) {
            response.put("message", "Appointment not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        Appointment appointment = existingOpt.get();

        // Ensure the patient canceling the appointment is the owner
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

    // 7. Get Appointments Method
    @Transactional
    public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
        Map<String, Object> result = new HashMap<>();

        // Extract doctor id from token
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

    // 8. Change Status Method
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