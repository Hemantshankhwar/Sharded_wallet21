package com.example.sharded_wallet21.services;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.sharded_wallet21.dtos.AuthDtos.AuthResponse;
import com.example.sharded_wallet21.entities.Role;
import com.example.sharded_wallet21.entities.User;
import com.example.sharded_wallet21.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final StringRedisTemplate redis;

    @Value("${JWT_TTL_MINUTES:60}")
    private long ttlMinutes;

    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(normalize(email))
                .filter(u -> Boolean.TRUE.equals(u.getEnabled()) && u.getPasswordHash() != null)
                .filter(u -> password != null && passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return toResponse(user);
    }

    public User createUser(String name, String email, String password, Role role) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        String normalized = normalize(email);
        if (normalized.isEmpty() || !normalized.contains("@")) {
            throw new IllegalArgumentException("a valid email is required");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        // Emails are unique per shard only, so serialize creation per email and check across shards.
        String lockKey = "lock:user-email:" + normalized;
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(15));
        if (!Boolean.TRUE.equals(locked)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        try {
            if (userRepository.findByEmail(normalized).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
            }
            User user = new User();
            user.setName(name.trim());
            user.setEmail(normalized);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRole(role == null ? Role.USER : role);
            user.setEnabled(true);
            user.setCreatedAt(Instant.now());
            return userRepository.save(user);
        } finally {
            redis.delete(lockKey);
        }
    }

    public AuthResponse toResponse(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(ttlMinutes)))
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .build();
        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthResponse(token, user.getId(), user.getName(), user.getRole());
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
