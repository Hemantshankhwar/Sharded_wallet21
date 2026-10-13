package com.example.sharded_wallet21.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String KEY_PREFIX = "idempotency:";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String DONE = "DONE";
    private static final Duration IN_PROGRESS_TTL = Duration.ofSeconds(60);
    private static final Duration COMPLETED_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private record StoredEntry(String status, String requestHash, Integer httpStatus, JsonNode body) {
    }

    /**
     * Runs {@code action} at most once per (scope, key). Retries with the same key get the stored
     * response; the same key with a different request body is rejected.
     */
    public ResponseEntity<?> execute(String scope, String key, Object request, Supplier<ResponseEntity<?>> action) {
        String redisKey = KEY_PREFIX + scope + ":" + key;
        String requestHash = hash(request);

        Boolean acquired = redis.opsForValue().setIfAbsent(
                redisKey, write(new StoredEntry(IN_PROGRESS, requestHash, null, null)), IN_PROGRESS_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            return replay(redisKey, requestHash);
        }

        ResponseEntity<?> response;
        try {
            response = action.get();
        } catch (RuntimeException e) {
            redis.delete(redisKey);
            throw e;
        }

        // A 5xx may be transient, so release the key and let the client retry.
        if (response.getStatusCode().is5xxServerError()) {
            redis.delete(redisKey);
            return response;
        }

        redis.opsForValue().set(redisKey,
                write(new StoredEntry(DONE, requestHash, response.getStatusCode().value(),
                        objectMapper.valueToTree(response.getBody()))),
                COMPLETED_TTL);
        return response;
    }

    private ResponseEntity<?> replay(String redisKey, String requestHash) {
        String raw = redis.opsForValue().get(redisKey);
        if (raw == null) {
            return error(HttpStatus.CONFLICT, "Request is being processed, retry shortly");
        }

        StoredEntry entry = read(raw);
        if (!entry.requestHash().equals(requestHash)) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY, "Idempotency-Key was already used with a different request");
        }
        if (IN_PROGRESS.equals(entry.status())) {
            return error(HttpStatus.CONFLICT, "Request is being processed, retry shortly");
        }

        log.info("Replaying stored response for {}", redisKey);
        return ResponseEntity.status(entry.httpStatus())
                .header("Idempotent-Replayed", "true")
                .body(entry.body());
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    private String hash(Object request) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash request", e);
        }
    }

    private String write(StoredEntry entry) {
        try {
            return objectMapper.writeValueAsString(entry);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize idempotency entry", e);
        }
    }

    private StoredEntry read(String raw) {
        try {
            return objectMapper.readValue(raw, StoredEntry.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to read idempotency entry", e);
        }
    }
}
