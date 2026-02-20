# User Story Template

---

**Admin**

### Admin Story 1 – Log In
As an admin, I want to log into the portal so that I can securely manage the system.

**Acceptance Criteria:**
1. Admin can enter valid credentials and successfully log in.
2. Admin sees an error message when credentials are invalid.
3. Admin is redirected to the Admin Dashboard after successful login.

**Priority:** High  
**Story Points:** 3  
**Notes:**
- Admin accounts must be pre-created in the database.
- Consider adding rate limiting for security.

---

### Admin Story 2 – Log Out
As an admin, I want to log out of the portal so that I can protect system access.

**Acceptance Criteria:**
1. Admin can click the logout button.
2. Admin session ends immediately after logging out.
3. Admin is redirected to the login page.

**Priority:** High  
**Story Points:** 1  
**Notes:**
- Logout should invalidate the JWT token.
- Should clear session cookies.

---

### Admin Story 3 – Add Doctor
As an admin, I want to add new doctors to the portal so that they can start accepting appointments.

**Acceptance Criteria:**
1. Admin can fill and submit the Add Doctor form.
2. Doctor is added when required fields are valid.
3. Validation errors appear if information is missing.

**Priority:** Medium  
**Story Points:** 5  
**Notes:**
- Doctor email must be unique.
- Optional: auto-generate temporary password.

---

### Admin Story 4 – Delete Doctor
As an admin, I want to delete a doctor’s profile so that I can remove outdated or incorrect accounts.

**Acceptance Criteria:**
1. Admin can view the doctor list.
2. Admin can delete a doctor after confirming.
3. Deleted doctor no longer appears in the list.

**Priority:** Medium  
**Story Points:** 3  
**Notes:**
- Consider soft delete instead of hard delete.
- Should prevent deletion if doctor has upcoming appointments.

---

### Admin Story 5 – Run Stored Procedure
As an admin, I want to run a stored procedure in MySQL so that I can see monthly appointment counts and usage statistics.

**Acceptance Criteria:**
1. Admin can execute the stored procedure in MySQL CLI.
2. The procedure returns appointment counts per month.
3. Results are displayed without errors.

**Priority:** Low  
**Story Points:** 2  
**Notes:**
- Procedure may be used for reporting dashboards later.
- Only read-only execution allowed.

---

**Patient**

### Patient Story 1 – View Doctors Without Login
As a patient, I want to view a list of doctors without logging in so that I can explore my options before registering.

**Acceptance Criteria:**
1. Patient can open the doctors page without logging in.
2. Doctors list loads successfully.
3. Each doctor’s basic information is visible.

**Priority:** High  
**Story Points:** 2  
**Notes:**
- Should show only public doctor info.
- Optional: filter doctors by specialization.

---

### Patient Story 2 – Sign Up
As a patient, I want to sign up using my email and password so that I can book appointments.

**Acceptance Criteria:**
1. Patient can enter signup details and create an account.
2. Duplicate email results in an error message.
3. Successful signup shows a confirmation message.

**Priority:** High  
**Story Points:** 3  
**Notes:**
- Password should be encrypted.
- Consider email verification.

---

### Patient Story 3 – Log In
As a patient, I want to log into the portal so that I can manage my bookings.

**Acceptance Criteria:**
1. Patient can log in using valid credentials.
2. Invalid credentials show an error.
3. Successful login redirects to the patient dashboard.

**Priority:** High  
**Story Points:** 2  
**Notes:**
- JWT authentication recommended.
- Consider showing “forgot password” option.

---

### Patient Story 4 – Book Appointment
As a patient, I want to book an hour-long appointment so that I can consult with a doctor.

**Acceptance Criteria:**
1. Patient can select an available appointment slot.
2. Booking is successful when the slot is free.
3. Error message appears if the slot is unavailable.

**Priority:** High  
**Story Points:** 5  
**Notes:**
- Appointment length defaults to 1 hour.
- Should prevent double-booking.

---

### Patient Story 5 – View Upcoming Appointments
As a patient, I want to view my upcoming appointments so that I can prepare accordingly.

**Acceptance Criteria:**
1. Patient can open the "My Appointments" page.
2. Upcoming appointments are displayed.
3. Information loads with no errors.

**Priority:** Medium  
**Story Points:** 2  
**Notes:**
- Include doctor name & date/time.
- Should auto-sort by date.

---

**Doctor**

### Doctor Story 1 – Log In
As a doctor, I want to log into the portal so that I can manage my appointments.

**Acceptance Criteria:**
1. Doctor can log in with valid credentials.
2. Invalid credentials show an error message.
3. Successful login redirects to the doctor dashboard.

**Priority:** High  
**Story Points:** 2  
**Notes:**
- Ensure doctor role is validated.
- Should use JWT authentication.

---

### Doctor Story 2 – View Appointment Calendar
As a doctor, I want to view my appointment calendar so that I can stay organized.

**Acceptance Criteria:**
1. Doctor can access the appointment calendar.
2. All upcoming appointments are shown.
3. Appointments appear in correct date/time order.

**Priority:** High  
**Story Points:** 3  
**Notes:**
- Optional: color-code appointment types.
- Should include patient name & reason.

---

### Doctor Story 3 – Mark Unavailability
As a doctor, I want to mark my unavailable time slots so that patients only book available hours.

**Acceptance Criteria:**
1. Doctor can select and mark unavailable time slots.
2. Patients cannot book unavailable slots.
3. Changes are saved successfully.

**Priority:** Medium  
**Story Points:** 4  
**Notes:**
- Should support recurring unavailability.
- Prevent conflicts with existing appointments.

---

### Doctor Story 4 – Update Profile
As a doctor, I want to update my profile with specialization and contact information so that patients have up-to-date details.

**Acceptance Criteria:**
1. Doctor can edit specialization or contact info.
2. Updates save correctly.
3. Patients can view updated profile information.

**Priority:** Medium  
**Story Points:** 2  
**Notes:**
- Fields must be validated.
- Consider adding photo upload later.

---

### Doctor Story 5 – View Patient Details
As a doctor, I want to view patient details for upcoming appointments so that I can prepare ahead of time.

**Acceptance Criteria:**
1. Doctor can open patient details for an appointment.
2. Patient information loads correctly.
3. Access is allowed only when the doctor is logged in.

**Priority:** High  
**Story Points:** 3  
**Notes:**
- Only data relevant to treatment should be shown.
- Sensitive information must follow privacy rules.