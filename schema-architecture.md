## Section 1: Architecture Summary

The Smart Clinic Management System is built as a three-tier Spring Boot application that combines both traditional MVC and modern REST-based interactions. At the Presentation tier, admin and doctor dashboards are rendered using Thymeleaf templates, while other modules such as appointments, patient dashboards, and patient records communicate with the backend through JSON-based REST APIs. All incoming requests are handled by dedicated controllers (MVC or REST), which delegate to a shared Service layer in the Application tier. This Service layer centralizes business rules, validation, and coordination across modules, and interacts with the Data tier through Spring Data repositories. Structured, relational data such as admins, doctors, patients, and appointments is stored in a MySQL database using JPA entities, while flexible, document-style data such as prescriptions is managed in MongoDB using Spring Data MongoDB. This dual-database, layered architecture provides clear separation of concerns, supports both web views and API clients, and makes the system easier to maintain, extend, and deploy in containerized, CI/CD-driven environments.

## Section 2: Numbered Flow – Request and Data Flow

1. A user (admin, doctor, or patient) begins by accessing either a Thymeleaf-based dashboard page or a REST endpoint through a browser, mobile client, or API consumer.

2. The request is routed to the appropriate backend controller:
   - MVC controllers handle server-rendered dashboard pages.
   - REST controllers handle API requests and return JSON responses.

3. The controller validates the incoming request and forwards the operation to the Service layer, ensuring the controller remains lightweight and focused on request handling.

4. The Service layer applies business rules, performs validations, and determines which repositories or operations are needed (e.g., checking doctor availability, creating appointments, retrieving records).

5. Based on the data needed, the Service layer interacts with:
   - Spring Data JPA repositories for MySQL (patients, doctors, appointments, admin records).
   - Spring Data MongoDB repositories for flexible, document-based data such as prescriptions.

6. The repositories communicate directly with the respective databases, executing queries and returning mapped entity objects (from MySQL) or document objects (from MongoDB) back to the Service layer.

7. The Service layer returns the processed results to the controller, which completes the cycle by:
   - Rendering an HTML page via Thymeleaf for MVC flows, or  
   - Serializing the response as JSON for REST API calls.