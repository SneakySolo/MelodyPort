package com.melodyport.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodyport.auth.dto.SessionDTO;
import com.melodyport.auth.dto.SpotifyTokenDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisSessionServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisSessionService sessionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        sessionService = new RedisSessionService(redisTemplate, objectMapper);
    }

    @Test
    void testCreateSession() {
        String userId = "spotify-user-123";

        SessionDTO session = sessionService.createSession(userId);

        assertNotNull(session.getSessionId());
        assertEquals(userId, session.getUserId());
        assertNotNull(session.getExpiresAt());
        assertTrue(session.getExpiresAt().isAfter(Instant.now()));

        verify(valueOperations, times(1)).set(
                startsWith("session:"),
                contains(userId),
                eq(86400L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void testGetSession() throws Exception {
        String sessionId = "session-123";
        String userId = "spotify-user-456";
        SessionDTO originalSession = new SessionDTO(sessionId, userId);
        String sessionJson = objectMapper.writeValueAsString(originalSession);

        when(valueOperations.get("session:" + sessionId)).thenReturn(sessionJson);

        SessionDTO retrievedSession = sessionService.getSession(sessionId);

        assertNotNull(retrievedSession);
        assertEquals(sessionId, retrievedSession.getSessionId());
        assertEquals(userId, retrievedSession.getUserId());
        verify(valueOperations, times(1)).get("session:" + sessionId);
    }

    @Test
    void testGetSessionNotFound() {
        String sessionId = "non-existent-session";

        when(valueOperations.get("session:" + sessionId)).thenReturn(null);

        SessionDTO session = sessionService.getSession(sessionId);

        assertNull(session);
        verify(valueOperations, times(1)).get("session:" + sessionId);
    }

    @Test
    void testUpdateSession() throws Exception {
        String sessionId = "session-789";
        String userId = "spotify-user-789";
        SessionDTO session = new SessionDTO(sessionId, userId);

        SpotifyTokenDTO token = new SpotifyTokenDTO("access-123", "refresh-456", 3600);
        session.setSpotifyToken(token);

        sessionService.updateSession(session);

        verify(valueOperations, times(1)).set(
                eq("session:" + sessionId),
                contains("access-123"),
                eq(86400L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void testDeleteSession() {
        String sessionId = "session-to-delete";

        sessionService.deleteSession(sessionId);

        verify(redisTemplate, times(1)).delete("session:" + sessionId);
    }

    @Test
    void testIsSessionValid() {
        String sessionId = "valid-session";
        String userId = "user-123";
        SessionDTO session = new SessionDTO(sessionId, userId);
        String sessionJson;

        try {
            sessionJson = objectMapper.writeValueAsString(session);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(valueOperations.get("session:" + sessionId)).thenReturn(sessionJson);

        boolean isValid = sessionService.isSessionValid(sessionId);

        assertTrue(isValid);
    }

    @Test
    void testSessionHasSpotifyAuth() throws Exception {
        String sessionId = "session-with-spotify";
        String userId = "user-spotify";
        SessionDTO session = new SessionDTO(sessionId, userId);

        SpotifyTokenDTO token = new SpotifyTokenDTO("access-token", "refresh-token", 3600);
        session.setSpotifyToken(token);

        assertTrue(session.hasSpotifyAuth());
    }

    @Test
    void testSessionHasGoogleAuth() throws Exception {
        String sessionId = "session-with-google";
        String userId = "user-google";
        SessionDTO session = new SessionDTO(sessionId, userId);

        SpotifyTokenDTO googleToken = new SpotifyTokenDTO("google-access", "google-refresh", 3600);
        session.setGoogleToken(googleToken);

        assertTrue(session.hasGoogleAuth());
    }
}