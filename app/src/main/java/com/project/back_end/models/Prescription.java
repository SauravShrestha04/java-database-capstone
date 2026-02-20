package com.project.back_end.models;

import jakarta.validation.constraints.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "prescriptions")
public class Prescription {

  // @Document annotation:
  //    - Marks the class as a MongoDB document (a collection in MongoDB).
  //    - The collection name is specified as "prescriptions" to map this class to the "prescriptions" collection in MongoDB.
  //
  // 1. 'id' field:
  //    - Type: private String
  //    - Represents the unique identifier for each prescription.
  //    - The @Id annotation marks it as the primary key in the MongoDB collection.
  //
  // 2. 'patientName' field:
  //    - Type: private String
  //    - Represents the name of the patient receiving the prescription.
  //    - @NotNull ensures patient name is required.
  //    - @Size(min = 3, max = 100) ensures reasonable name length.
  //
  // 3. 'appointmentId' field:
  //    - Type: private Long
  //    - Represents the ID of the associated appointment.
  //    - @NotNull ensures the appointment link is required.
  //
  // 4. 'medication' field:
  //    - Type: private String
  //    - Represents medication name.
  //    - @NotNull & @Size(min = 3, max = 100) ensure validity.
  //
  // 5. 'dosage' field:
  //    - Type: private String
  //    - Represents dosage instructions.
  //    - @NotNull ensures dosage is always provided.
  //
  // 6. 'doctorNotes' field:
  //    - Type: private String
  //    - Represents optional doctor notes.
  //    - @Size(max = 200) ensures notes are concise.
  //
  // 7. Constructors:
  //    - Default constructor and one parameterized constructor.
  //
  // 8. Getters & Setters:
  //    - Standard getter and setter methods for each field.

    @Id
    private String id;

    @NotNull
    @Size(min = 3, max = 100)
    private String patientName;

    @NotNull
    private Long appointmentId;

    @NotNull
    @Size(min = 3, max = 100)
    private String medication;

    @NotNull
    private String dosage;

    @Size(max = 200)
    private String doctorNotes;

    public Prescription() {}

    public Prescription(String patientName, Long appointmentId, String medication,
                        String dosage, String doctorNotes) {
        this.patientName = patientName;
        this.appointmentId = appointmentId;
        this.medication = medication;
        this.dosage = dosage;
        this.doctorNotes = doctorNotes;
    }

    public String getId() { return id; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public String getMedication() { return medication; }
    public void setMedication(String medication) { this.medication = medication; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getDoctorNotes() { return doctorNotes; }
    public void setDoctorNotes(String doctorNotes) { this.doctorNotes = doctorNotes; }
}