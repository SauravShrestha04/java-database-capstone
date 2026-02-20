# Schema Design – Smart Clinic Management System

This file documents the database structure for the Smart Clinic Management System.  
The system uses two databases:

- **MySQL** → Stores structured and relational operational data  
- **MongoDB** → Stores flexible, evolving medical documents  

---

## MySQL Database Design

MySQL is used for core data that requires structure, validation, and relationships.  
A clinic must store information about patients, doctors, appointments, and admins.  
These tables form the backbone of the system.

Below are the required tables.

---

### Table: patients
Stores registered patients and their basic information.

- **id:** INT, Primary Key, AUTO_INCREMENT  
- **full_name:** VARCHAR(100), NOT NULL  
- **email:** VARCHAR(100), NOT NULL, UNIQUE  
- **date_of_birth:** DATE, NULL  
- **phone_number:** VARCHAR(20), NULL  
- **gender:** VARCHAR(20), NULL  
- **created_at:** DATETIME, DEFAULT CURRENT_TIMESTAMP  

**Notes:**  
- Email must be unique for login.  
- Optional fields make registration simple.  

---

### Table: doctors
Stores doctor information used for appointments and specialization filtering.

- **id:** INT, Primary Key, AUTO_INCREMENT  
- **full_name:** VARCHAR(100), NOT NULL  
- **email:** VARCHAR(100), NOT NULL, UNIQUE  
- **specialization:** VARCHAR(100), NOT NULL  
- **phone_number:** VARCHAR(20), NULL  
- **is_active:** TINYINT(1), DEFAULT 1  
- **created_at:** DATETIME, DEFAULT CURRENT_TIMESTAMP  

**Notes:**  
- Doctors need unique emails for login.  
- is_active allows disabling without deleting.

---

### Table: appointments
Links a patient and a doctor with a scheduled appointment time.

- **id:** INT, Primary Key, AUTO_INCREMENT  
- **doctor_id:** INT, Foreign Key → doctors(id), NOT NULL  
- **patient_id:** INT, Foreign Key → patients(id), NOT NULL  
- **start_time:** DATETIME, NOT NULL  
- **end_time:** DATETIME, NOT NULL  
- **status:** VARCHAR(20), DEFAULT 'SCHEDULED'  
- **created_at:** DATETIME, DEFAULT CURRENT_TIMESTAMP  
- **notes:** TEXT, NULL  

**Considerations:**  
- Should a doctor have overlapping appointments? (Backend validation required.)  
- Should old appointment history be retained? (Yes, for medical history.)  
- If a patient is deleted, appointments should remain for audit purposes.

---

### Table: admin
Stores admin accounts responsible for managing doctors and monitoring system usage.

- **id:** INT, Primary Key, AUTO_INCREMENT  
- **full_name:** VARCHAR(100), NOT NULL  
- **email:** VARCHAR(100), NOT NULL, UNIQUE  
- **password_hash:** VARCHAR(255), NOT NULL  
- **created_at:** DATETIME, DEFAULT CURRENT_TIMESTAMP  

**Notes:**  
- Admin credentials should be secure and encrypted.

---

### Optional Additional Tables (if needed)
You may extend the schema with:

- **clinic_locations**  
- **payments**  
- **doctor_availability** (to store time slots)

These are optional for the lab but realistic for future expansion.

---

## MongoDB Collection Design

MongoDB is used for flexible or semi-structured content that does not fit nicely into rigid SQL tables.  
Prescriptions are a good example because:

- They vary in length (multiple medicines).  
- They may include extra notes, tags, or metadata.  
- They may change in format over time.

Below is the MongoDB collection.

---

### Collection: prescriptions

Example document:

```json
{
  "_id": "ObjectId('64abc123456')",
  "appointmentId": 51,
  "patientId": 12,
  "doctorId": 4,
  "issuedAt": "2025-03-21T10:30:00Z",
  "diagnosis": "Seasonal Flu",
  "medications": [
    {
      "name": "Paracetamol",
      "dosage": "500mg",
      "frequency": "1 tablet every 6 hours",
      "durationDays": 5
    },
    {
      "name": "Cetirizine",
      "dosage": "10mg",
      "frequency": "1 tablet before bed",
      "durationDays": 7
    }
  ],
  "doctorNotes": "Stay hydrated and rest well.",
  "refillCount": 1,
  "pharmacy": {
    "name": "Walgreens SF",
    "location": "Market Street"
  },
  "tags": ["flu", "cold", "fever"],
  "createdAt": "2025-03-21T10:35:00Z",
  "updatedAt": "2025-03-21T10:35:00Z"
}