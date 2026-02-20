package com.project.back_end.services;

import com.project.back_end.models.Doctor; 
import com.project.back_end.DTO.Login;
import com.project.back_end.model.Appointment;
import com.project.back_end.model.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/*
  1. **Add @Service Annotation**:
    - This class should be annotated with `@Service` to indicate that it is a service layer class.
    - The `@Service` annotation marks this class as a Spring-managed bean for business logic.
    - Instruction: Add `@Service` above the class declaration.

  2. **Constructor Injection for Dependencies**:
    - The `DoctorService` class depends on `DoctorRepository`, `AppointmentRepository`, and `TokenService`.
    - These dependencies should be injected via the constructor for proper dependency management.
    - Instruction: Ensure constructor injection is used for injecting dependencies into the service.

  3. **Add @Transactional Annotation for Methods that Modify or Fetch Database Data**:
    - Methods like `getDoctorAvailability`, `getDoctors`, `findDoctorByName`, `filterDoctorsBy*` should be annotated with `@Transactional`.
    - The `@Transactional` annotation ensures that database operations are consistent and wrapped in a single transaction.
    - Instruction: Add the `@Transactional` annotation above the methods that perform database operations or queries.

  4. **getDoctorAvailability Method**:
    - Retrieves the available time slots for a specific doctor on a particular date and filters out already booked slots.
    - The method fetches all appointments for the doctor on the given date and calculates the availability by comparing against booked slots.
    - Instruction: Ensure that the time slots are properly formatted and the available slots are correctly filtered.

  5. **saveDoctor Method**:
    - Used to save a new doctor record in the database after checking if a doctor with the same email already exists.
    - If a doctor with the same email is found, it returns `-1` to indicate conflict; `1` for success, and `0` for internal errors.
    - Instruction: Ensure that the method correctly handles conflicts and exceptions when saving a doctor.

  6. **updateDoctor Method**:
    - Updates an existing doctor's details in the database. If the doctor doesn't exist, it returns `-1`.
    - Instruction: Make sure that the doctor exists before attempting to save the updated record and handle any errors properly.

  7. **getDoctors Method**:
    - Fetches all doctors from the database. It is marked with `@Transactional` to ensure that the collection is properly loaded.
    - Instruction: Ensure that the collection is eagerly loaded, especially if dealing with lazy-loaded relationships (e.g., available times). 

  8. **deleteDoctor Method**:
    - Deletes a doctor from the system along with all appointments associated with that doctor.
    - It first checks if the doctor exists. If not, it returns `-1`; otherwise, it deletes the doctor and their appointments.
    - Instruction: Ensure the doctor and their appointments are deleted properly, with error handling for internal issues.

  9. **validateDoctor Method**:
    - Validates a doctor's login by checking if the email and password match an existing doctor record.
    - It generates a token for the doctor if the login is successful, otherwise returns an error message.
    - Instruction: Make sure to handle invalid login attempts and password mismatches properly with error responses.

  10. **findDoctorByName Method**:
    - Finds doctors based on partial name matching and returns the list of doctors with their available times.
    - This method is annotated with `@Transactional` to ensure that the database query and data retrieval are properly managed within a transaction.
    - Instruction: Ensure that available times are eagerly loaded for the doctors.


  11. **filterDoctorsByNameSpecilityandTime Method**:
    - Filters doctors based on their name, specialty, and availability during a specific time (AM/PM).
    - The method fetches doctors matching the name and specialty criteria, then filters them based on their availability during the specified time period.
    - Instruction: Ensure proper filtering based on both the name and specialty as well as the specified time period.

  12. **filterDoctorByTime Method**:
    - Filters a list of doctors based on whether their available times match the specified time period (AM/PM).
    - This method processes a list of doctors and their available times to return those that fit the time criteria.
    - Instruction: Ensure that the time filtering logic correctly handles both AM and PM time slots and edge cases.


  13. **filterDoctorByNameAndTime Method**:
    - Filters doctors based on their name and the specified time period (AM/PM).
    - Fetches doctors based on partial name matching and filters the results to include only those available during the specified time period.
    - Instruction: Ensure that the method correctly filters doctors based on the given name and time of day (AM/PM).

  14. **filterDoctorByNameAndSpecility Method**:
    - Filters doctors by name and specialty.
    - It ensures that the resulting list of doctors matches both the name (case-insensitive) and the specified specialty.
    - Instruction: Ensure that both name and specialty are considered when filtering doctors.


  15. **filterDoctorByTimeAndSpecility Method**:
    - Filters doctors based on their specialty and availability during a specific time period (AM/PM).
    - Fetches doctors based on the specified specialty and filters them based on their available time slots for AM/PM.
    - Instruction: Ensure the time filtering is accurately applied based on the given specialty and time period (AM/PM).

  16. **filterDoctorBySpecility Method**:
    - Filters doctors based on their specialty.
    - This method fetches all doctors matching the specified specialty and returns them.
    - Instruction: Make sure the filtering logic works for case-insensitive specialty matching.

  17. **filterDoctorsByTime Method**:
    - Filters all doctors based on their availability during a specific time period (AM/PM).
    - The method checks all doctors' available times and returns those available during the specified time period.
    - Instruction: Ensure proper filtering logic to handle AM/PM time periods.

*/

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // 4. getDoctorAvailability Method
    @Transactional
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            return Collections.emptyList();
        }

        Doctor doctor = doctorOpt.get();
        List<String> allSlots = doctor.getAvailableTimes();
        if (allSlots == null || allSlots.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime startOfNextDay = date.plusDays(1).atStartOfDay();

        List<Appointment> appointmentsForDay =
                appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                        doctorId, startOfDay, startOfNextDay);

        // Build a set of booked LocalTime values
        Set<LocalTime> bookedTimes = appointmentsForDay.stream()
                .map(Appointment::getAppointmentTime)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalTime)
                .collect(Collectors.toSet());

        // Filter out booked slots
        List<String> availableSlots = new ArrayList<>();
        for (String slot : allSlots) {
            try {
                // Assume slot is in "HH:mm" or "HH:mm:ss" format; adjust if needed
                LocalTime slotTime = LocalTime.parse(slot);
                if (!bookedTimes.contains(slotTime)) {
                    availableSlots.add(slot);
                }
            } catch (Exception e) {
                // If parsing fails, just keep the slot as "available" to avoid breaking UI
                availableSlots.add(slot);
            }
        }

        return availableSlots;
    }

    // 5. saveDoctor Method
    @Transactional
    public int saveDoctor(Doctor doctor) {
        try {
            Doctor existing = doctorRepository.findByEmail(doctor.getEmail());
            if (existing != null) {
                return -1; // doctor already exists
            }
            doctorRepository.save(doctor);
            return 1; // success
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // internal error
        }
    }

    // 6. updateDoctor Method
    @Transactional
    public int updateDoctor(Doctor doctor) {
        if (doctor.getId() == null) {
            return -1; // no ID provided
        }

        Optional<Doctor> existingOpt = doctorRepository.findById(doctor.getId());
        if (existingOpt.isEmpty()) {
            return -1; // doctor not found
        }

        try {
            Doctor existing = existingOpt.get();

            // Basic merge (you can customize which fields can be updated)
            existing.setName(doctor.getName());
            existing.setEmail(doctor.getEmail());
            existing.setSpecialty(doctor.getSpecialty());
            existing.setAvailableTimes(doctor.getAvailableTimes());

            // Only update password if provided (optional behavior)
            if (doctor.getPassword() != null && !doctor.getPassword().isBlank()) {
                existing.setPassword(doctor.getPassword());
            }

            doctorRepository.save(existing);
            return 1; // success
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // internal error
        }
    }

    // 7. getDoctors Method
    @Transactional
    public List<Doctor> getDoctors() {
        return doctorRepository.findAll();
    }

    // 8. deleteDoctor Method
    @Transactional
    public int deleteDoctor(long id) {
        Optional<Doctor> existingOpt = doctorRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return -1; // doctor not found
        }

        try {
            // Delete appointments first
            appointmentRepository.deleteAllByDoctorId(id);
            // Then delete the doctor
            doctorRepository.deleteById(id);
            return 1; // success
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // internal error
        }
    }

    // 9. validateDoctor Method
    @Transactional
    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        Map<String, String> response = new HashMap<>();

        String identifier = login.getIdentifier();
        String password = login.getPassword();

        if (identifier == null || identifier.isBlank() ||
                password == null || password.isBlank()) {
            response.put("message", "Email and password are required.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Doctor doctor = doctorRepository.findByEmail(identifier);
        if (doctor == null) {
            response.put("message", "Invalid credentials.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // NOTE: In a real app, this should be a password hash check.
        if (!password.equals(doctor.getPassword())) {
            response.put("message", "Invalid credentials.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String token = tokenService.generateDoctorToken(doctor.getId());

        response.put("message", "Login successful.");
        response.put("token", token);
        response.put("role", "doctor");
        return ResponseEntity.ok(response);
    }

    // 10. findDoctorByName Method
    @Transactional
    public Map<String, Object> findDoctorByName(String name) {
        Map<String, Object> result = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findByNameLike("%" + name + "%");
        result.put("doctors", doctors);
        return result;
    }

    // 11. filterDoctorsByNameSpecilityandTime Method
    @Transactional
    public Map<String, Object> filterDoctorsByNameSpecilityandTime(String name,
                                                                   String specialty,
                                                                   String amOrPm) {
        Map<String, Object> result = new HashMap<>();

        List<Doctor> doctors = doctorRepository
                .findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty);

        List<Doctor> filtered = filterDoctorByTime(doctors, amOrPm);
        result.put("doctors", filtered);
        return result;
    }

    // 13. filterDoctorByNameAndTime Method
    @Transactional
    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        Map<String, Object> result = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findByNameLike("%" + name + "%");
        List<Doctor> filtered = filterDoctorByTime(doctors, amOrPm);
        result.put("doctors", filtered);
        return result;
    }

    // 14. filterDoctorByNameAndSpecility Method
    @Transactional
    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specialty) {
        Map<String, Object> result = new HashMap<>();
        List<Doctor> doctors = doctorRepository
                .findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty);
        result.put("doctors", doctors);
        return result;
    }

    // 15. filterDoctorByTimeAndSpecility Method
    @Transactional
    public Map<String, Object> filterDoctorByTimeAndSpecility(String specialty, String amOrPm) {
        Map<String, Object> result = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);
        List<Doctor> filtered = filterDoctorByTime(doctors, amOrPm);
        result.put("doctors", filtered);
        return result;
    }

    // 16. filterDoctorBySpecility Method
    @Transactional
    public Map<String, Object> filterDoctorBySpecility(String specialty) {
        Map<String, Object> result = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);
        result.put("doctors", doctors);
        return result;
    }

    // 17. filterDoctorsByTime Method
    @Transactional
    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        Map<String, Object> result = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findAll();
        List<Doctor> filtered = filterDoctorByTime(doctors, amOrPm);
        result.put("doctors", filtered);
        return result;
    }

    // 12. filterDoctorByTime Method (private helper)
    private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        if (amOrPm == null || amOrPm.isBlank()) {
            return doctors;
        }

        String normalized = amOrPm.trim().toUpperCase(Locale.ROOT);

        return doctors.stream()
                .filter(doctor -> {
                    List<String> times = doctor.getAvailableTimes();
                    if (times == null || times.isEmpty()) {
                        return false;
                    }
                    // Very simple AM/PM check. Adjust if your time format is different.
                    return times.stream()
                            .filter(Objects::nonNull)
                            .map(String::toUpperCase)
                            .anyMatch(t -> t.contains(normalized));
                })
                .collect(Collectors.toList());
    }
}