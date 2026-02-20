package com.project.back_end.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "admins")
public class Admin {

    // @Entity annotation:
    // - Marks the class as a JPA entity mapped to a table in MySQL.
    // - Required for ORM frameworks like Hibernate.

    // 1. 'id' field:
    // - Primary key for the Admin table.
    // - Auto-incremented by MySQL using IDENTITY strategy.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 2. 'username' field:
    // - Cannot be null.
    // - Used to log in to the system.
    @NotNull(message = "username cannot be null")
    @Column(nullable = false, unique = true)
    private String username;

    // 3. 'password' field:
    // - Cannot be null.
    // - WRITE_ONLY ensures password is never exposed in JSON responses.
    @NotNull(message = "password cannot be null")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    // Default constructor (required by JPA)
    public Admin() {}

    // Optional constructor (useful for testing or seeding)
    public Admin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters & Setters
    public Long getId() { 
        return id; 
    }

    public String getUsername() { 
        return username; 
    }
    public void setUsername(String username) { 
        this.username = username; 
    }

    public String getPassword() { 
        return password; 
    }
    public void setPassword(String password) { 
        this.password = password; 
    }
}