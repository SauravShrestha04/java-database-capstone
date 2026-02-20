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

    private byte[] getSigningKey() {
        return jwtSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String generateToken(String identifier) {
        return generateToken(identifier, null);
    }

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

    // Parse + extract subject
    public String extractIdentifier(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public String getEmailFromToken(String token) {
        return extractIdentifier(token);
    }

    public boolean validateToken(String token, String user) {
        try {
            String identifier = extractIdentifier(token);
            if (identifier == null || identifier.isBlank()) {
                return false;
            }

            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Date expiration = claims.getExpiration();
            if (expiration == null || expiration.before(new Date())) {
                return false;
            }

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
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public Long getPatientIdFromToken(String token) {
        String email = extractIdentifier(token);
        if (email == null || email.isBlank()) {
            return null;
        }
        Patient patient = patientRepository.findByEmail(email);
        return patient != null ? patient.getId() : null;
    }

    public Long getDoctorIdFromToken(String token) {
        String email = extractIdentifier(token);
        if (email == null || email.isBlank()) {
            return null;
        }
        Doctor doctor = doctorRepository.findByEmail(email);
        return doctor != null ? doctor.getId() : null;
    }

    public String generateDoctorToken(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id " + doctorId));

        return generateToken(doctor.getEmail(), "doctor");
    }
}