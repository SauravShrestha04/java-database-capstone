package com.project.back_end.services;

import com.project.back_end.models.Admin; 
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class TokenService {

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public TokenService(AdminRepository adminRepository,
                        DoctorRepository doctorRepository,
                        PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * Return the signing key bytes for HS256.
     */
    private byte[] getSigningKey() {
        return jwtSecret.getBytes(StandardCharsets.UTF_8);
    }

    // Simple form matching the lab description
    public String generateToken(String identifier) {
        return generateToken(identifier, null);
    }

    // Extended form (used elsewhere in your services) with role as a claim
    public String generateToken(String identifier, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 7L * 24 * 60 * 60 * 1000); // 7 days

        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .setSubject(identifier)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS256, getSigningKey());

        if (role != null && !role.isBlank()) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    // Name per the lab text
    public String extractIdentifier(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(getSigningKey())
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // Convenience wrapper to match how you used it elsewhere (getEmailFromToken)
    public String getEmailFromToken(String token) {
        return extractIdentifier(token);
    }

    /**
     * Validate whether a provided JWT token is valid for a specific user role.
     */
    public boolean validateToken(String token, String user) {
        try {
            String identifier = extractIdentifier(token);
            if (identifier == null || identifier.isBlank()) {
                return false;
            }

            // Explicit expiration check (parser already enforces, but this is defensive)
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token)
                    .getBody();
            Date expiration = claims.getExpiration();
            if (expiration == null || expiration.before(new Date())) {
                return false;
            }

            // Validate against DB based on user type
            switch (user.toLowerCase()) {
                case "admin": {
                    Admin admin = adminRepository.findByUsername(identifier);
                    return admin != null;
                }
                case "doctor": {
                    Doctor doctor = doctorRepository.findByEmail(identifier);
                    return doctor != null;
                }
                case "patient": {
                    Patient patient = patientRepository.findByEmail(identifier);
                    return patient != null;
                }
                default:
                    // Unknown user type
                    return false;
            }
        } catch (Exception e) {
            // Any parsing/verification error means invalid token
            return false;
        }
    }

    /**
     * Get patient ID from a token whose subject is the patient's email.
     */
    public Long getPatientIdFromToken(String token) {
        String email = extractIdentifier(token);
        if (email == null || email.isBlank()) {
            return null;
        }
        Patient patient = patientRepository.findByEmail(email);
        return patient != null ? patient.getId() : null;
    }

    /**
     * Get doctor ID from a token whose subject is the doctor's email.
     */
    public Long getDoctorIdFromToken(String token) {
        String email = extractIdentifier(token);
        if (email == null || email.isBlank()) {
            return null;
        }
        Doctor doctor = doctorRepository.findByEmail(email);
        return doctor != null ? doctor.getId() : null;
    }

    /**
     * Generate a token for a doctor by their ID.
     * Uses the doctor's email as the subject, to keep tokens consistent.
     */
    public String generateDoctorToken(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id " + doctorId));

        // Reuse existing generateToken logic with identifier = email
        return generateToken(doctor.getEmail(), "doctor");
    }
}