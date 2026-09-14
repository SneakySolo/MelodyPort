package com.melodyport.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodyport.auth.dto.SessionDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RedisSessionService {
    private static final String SESSION_KEY_PREFIX = "session:";
    private static final long SESSION_TTL_SECONDS = 86400; // 24 hours

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisSessionService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Create a new session for a user
     */
    public SessionDTO createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        SessionDTO session = new SessionDTO(sessionId, userId);

        try {
            String sessionJson = objectMapper.writeValueAsString(session);
            String key = SESSION_KEY_PREFIX + sessionId;
            redisTemplate.opsForValue().set(key, sessionJson, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
            return session;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create session", e);
        }
    }

    /**
     * Retrieve a session by ID
     */
    public SessionDTO getSession(String sessionId) {
        try {
            String key = SESSION_KEY_PREFIX + sessionId;
            String sessionJson = redisTemplate.opsForValue().get(key);

            if (sessionJson == null) {
                return null;
            }

            SessionDTO session = objectMapper.readValue(sessionJson, SessionDTO.class);
            if (session.isExpired()) {
                deleteSession(sessionId);
                return null;
            }
            return session;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve session", e);
        }
    }

    /**
     * Update an existing session (e.g., add token after OAuth callback)
     */
    public void updateSession(SessionDTO session) {
        try {
            String sessionJson = objectMapper.writeValueAsString(session);
            String key = SESSION_KEY_PREFIX + session.getSessionId();
            redisTemplate.opsForValue().set(key, sessionJson, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update session", e);
        }
    }

    /**
     * Delete a session
     */
    public void deleteSession(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        redisTemplate.delete(key);
    }

    /**
     * Check if session exists and is valid
     */
    public boolean isSessionValid(String sessionId) {
        SessionDTO session = getSession(sessionId);
        return session != null && !session.isExpired();
    }
}