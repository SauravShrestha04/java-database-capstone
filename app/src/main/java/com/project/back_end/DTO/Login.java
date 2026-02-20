package com.project.back_end.DTO;

public class Login {

    // 1. 'identifier' field:
    //    - Type: private String
    //    - Description:
    //      - Represents the unique identifier of the user logging in.
    //      - For Admin → username
    //      - For Doctor/Patient → email
    //      - Used by the authentication service to locate user information.

    // 2. 'password' field:
    //    - Type: private String
    //    - Description:
    //      - The plaintext password submitted by the user.
    //      - It will be validated against the stored encoded password.

    // 3. Constructor:
    //    - No custom constructor is required.
    //    - Uses the default no-args constructor for JSON deserialization.

    // 4. Getters and Setters:
    //    - Allow this DTO to be populated by Spring via @RequestBody
    //    - Used inside controller login endpoints.

    private String identifier;
    private String password;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}