# Schema Design – Smart Clinic Management System

This document describes the database design for the Smart Clinic Management System.

The system uses a **hybrid storage approach**:

- **MySQL** for structured, relational data (users, doctors, patients, appointments, admin).
- **MongoDB** for flexible, document-style data (prescriptions).

This design keeps core transactional data strongly structured, while allowing more flexibility for medical details that can change over time.

---

## 1. MySQL Relational Database Design

### 1.1 Overview

The relational database will store:

- Patient profiles  
- Doctor profiles  
- Admin users  
- Appointments linking patients and doctors  

Main relationships:

- One **patient** can have many **appointments**.  
- One **doctor** can have many **appointments**.  
- Each **appointment** links one patient and one doctor.

---

### 1.2 Tables

#### 1.2.1 `admins`

| Column        | Data Type        | Constraints                           | Description                     |
|--------------|------------------|---------------------------------------|---------------------------------|
| id           | INT              | PRIMARY KEY, AUTO_INCREMENT           | Admin unique ID                 |
| email        | VARCHAR(100)     | NOT NULL, UNIQUE                      | Admin login email               |
| password_hash| VARCHAR(255)     | NOT NULL                              | Encrypted password              |
| full_name    | VARCHAR(100)     | NOT NULL                              | Admin full name                 |
| created_at   | DATETIME         | NOT NULL, DEFAULT CURRENT_TIMESTAMP   | When account was created        |
| is_active    | TINYINT(1)       | NOT NULL, DEFAULT 1                   | Soft-active flag                |

> **Why?**  
> Admins are few and managed centrally, so a simple table with unique email and active flag is enough.

---

#### 1.2.2 `doctors`

| Column         | Data Type        | Constraints                           | Description                       |
|---------------|------------------|---------------------------------------|-----------------------------------|
| id            | INT              | PRIMARY KEY, AUTO_INCREMENT           | Doctor unique ID                  |
| full_name     | VARCHAR(100)     | NOT NULL                              | Doctor full name                  |
| email         | VARCHAR(100)     | NOT NULL, UNIQUE                      | Doctor contact / login email      |
| specialization| VARCHAR(100)     | NOT NULL                              | e.g., "Cardiology", "Dermatology" |
| phone_number  | VARCHAR(20)      | NULL                                  | Optional phone                    |
| created_at    | DATETIME         | NOT NULL, DEFAULT CURRENT_TIMESTAMP   | When doctor was added             |
| is_active     | TINYINT(1)       | NOT NULL, DEFAULT 1                   | Whether doctor is active          |

> **Why?**  
> Doctors need unique emails for login, and specialization is important for search and display.

---

#### 1.2.3 `patients`

| Column        | Data Type        | Constraints                           | Description                          |
|--------------|------------------|---------------------------------------|--------------------------------------|
| id           | INT              | PRIMARY KEY, AUTO_INCREMENT           | Patient unique ID                    |
| full_name    | VARCHAR(100)     | NOT NULL                              | Patient full name                    |
| email        | VARCHAR(100)     | NOT NULL, UNIQUE                      | Patient login / contact email        |
| date_of_birth| DATE             | NULL                                  | Patient date of birth                |
| phone_number | VARCHAR(20)      | NULL                                  | Optional phone                       |
| created_at   | DATETIME         | NOT NULL, DEFAULT CURRENT_TIMESTAMP   | When patient registered              |
| gender       | VARCHAR(20)      | NULL                                  | Simple text field (e.g., "Male")     |

> **Why?**  
> Patients are first-class users and must be uniquely identified by email. Other fields are optional to keep registration simple.

---

#### 1.2.4 `appointments`

| Column          | Data Type        | Constraints                                        | Description                                  |
|----------------|------------------|----------------------------------------------------|----------------------------------------------|
| id             | INT              | PRIMARY KEY, AUTO_INCREMENT                        | Appointment unique ID                         |
| patient_id     | INT              | NOT NULL, FOREIGN KEY → patients(id)              | Linked patient                                |
| doctor_id      | INT              | NOT NULL, FOREIGN KEY → doctors(id)               | Linked doctor                                 |
| start_time     | DATETIME         | NOT NULL                                           | Appointment start date/time                   |
| end_time       | DATETIME         | NOT NULL                                           | Appointment end date/time                     |
| status         | VARCHAR(20)      | NOT NULL, DEFAULT 'SCHEDULED'                      | e.g., SCHEDULED, COMPLETED, CANCELLED         |
| created_at     | DATETIME         | NOT NULL, DEFAULT CURRENT_TIMESTAMP                | When appointment was created                   |
| notes          | TEXT             | NULL                                               | Optional short notes (non-clinical)           |

**Constraints & Relationships:**

- `patient_id` → references `patients.id`  
- `doctor_id` → references `doctors.id`  
- Optional unique constraint on (`doctor_id`, `start_time`) to avoid double booking.

> **Why?**  
> Appointments represent the main link between patients and doctors. Storing status allows simple reporting and history.

---

### 1.3 Relationship Summary

- `patients (1) —— (N) appointments`  
- `doctors (1) —— (N) appointments`  
- `admins` are independent and mainly used for management and reporting.

This structure supports common queries like:

- All appointments for a given patient  
- Daily schedule of a doctor  
- Monthly appointment counts for reporting

---

## 2. MongoDB Document Database Design

### 2.1 Overview

MongoDB is used for **flexible, document-based data** where the structure may change over time or contain nested details.

For this project, we will store **prescriptions** in a MongoDB collection named `prescriptions`.

A prescription can include:

- Basic link to patient and doctor  
- Visit date  
- List of prescribed medicines  
- Optional notes, instructions, or follow-up recommendations  

This structure is a good fit for MongoDB because medication lists and instructions can vary per appointment.

---

### 2.2 `prescriptions` Collection

**Collection Name:** `prescriptions`

Each document will represent **one prescription** for a specific appointment.

#### Example Document

```json
{
  "_id": "6740b8f90f1c4a1234567890",
  "appointmentId": 101,                 // matches appointments.id in MySQL
  "patientId": 25,                       // matches patients.id
  "doctorId": 7,                         // matches doctors.id
  "issuedAt": "2025-03-21T10:30:00Z",    // ISO date string
  "diagnosis": "Acute sinusitis",
  "medications": [
    {
      "name": "Amoxicillin 500mg",
      "dosage": "1 capsule",
      "frequency": "3 times a day",
      "durationDays": 7,
      "instructions": "Take after meals"
    },
    {
      "name": "Paracetamol 650mg",
      "dosage": "1 tablet",
      "frequency": "As needed for pain",
      "durationDays": 5,
      "instructions": "Do not exceed 4 tablets per day"
    }
  ],
  "followUp": {
    "required": true,
    "recommendedDate": "2025-04-01",
    "notes": "Review symptoms and adjust medication if no improvement"
  },
  "createdBy": "Dr. John Doe",
  "createdAt": "2025-03-21T10:35:00Z",
  "lastUpdatedAt": "2025-03-21T10:35:00Z"
}