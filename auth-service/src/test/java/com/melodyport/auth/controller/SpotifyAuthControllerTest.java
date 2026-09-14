package com.melodyport.auth.controller;

import com.melodyport.auth.dto.SessionDTO;
import com.melodyport.auth.dto.SpotifyTokenDTO;
import com.melodyport.auth.service.RedisSessionService;
import com.melodyport.auth.service.SpotifyOAuth2Service;
import com.melodyport.auth.service.SpotifyUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotifyAuthControllerTest {

    @Mock
    private RedisSessionService sessionService;

    @Mock
    private SpotifyOAuth2Service oauth2Service;

    @Mock
    private SpotifyUserService userService;

    @InjectMocks
    private SpotifyAuthController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "clientId", "test-client-id");
        ReflectionTestUtils.setField(controller, "redirectUri", "http://localhost:8080/auth/spotify/callback");
    }

    @Test
    void testLoginInitiatesOAuth() {
        RedirectView view = controller.login();

        assertNotNull(view);
        assertTrue(view.getUrl().contains("accounts.spotify.com/authorize"));
        assertTrue(view.getUrl().contains("test-client-id"));
        assertTrue(view.getUrl().contains("playlist-read-private"));
    }

    @Test
    void testCallbackExchangesCodeAndCreatesSession() {
        String code = "auth-code-123";
        String state = "state-456";
        String userId = "spotify-user-789";
        String sessionId = "session-uuid";

        SpotifyTokenDTO token = new SpotifyTokenDTO("access-token", "refresh-token", 3600);
        SessionDTO session = new SessionDTO(sessionId, userId);
        session.setSpotifyToken(token);

        when(oauth2Service.exchangeCodeForToken(code)).thenReturn(token);
        when(userService.fetchUserId("access-token")).thenReturn(userId);
        when(sessionService.createSession(userId)).thenReturn(session);

        RedirectView view = controller.callback(code, state);

        assertNotNull(view);
        assertTrue(view.getUrl().contains(sessionId));
        assertTrue(view.getUrl().contains("localhost:8080/dashboard"));

        verify(oauth2Service, times(1)).exchangeCodeForToken(code);
        verify(userService, times(1)).fetchUserId("access-token");
        verify(sessionService, times(1)).createSession(userId);
        verify(sessionService, times(1)).updateSession(session);
    }

    @Test
    void testCallbackHandlesError() {
        String code = "bad-code";

        when(oauth2Service.exchangeCodeForToken(code))
                .thenThrow(new RuntimeException("OAuth failed"));

        RedirectView view = controller.callback(code, null);

        assertNotNull(view);
        assertTrue(view.getUrl().contains("error"));
        assertTrue(view.getUrl().contains("spotify_auth_failed"));
    }

    @Test
    void testGetAuthStatusReturnsStatus() {
        String sessionId = "session-123";
        String userId = "user-456";

        SessionDTO session = new SessionDTO(sessionId, userId);
        SpotifyTokenDTO spotifyToken = new SpotifyTokenDTO("spotify-access", "spotify-refresh", 3600);
        session.setSpotifyToken(spotifyToken);

        when(sessionService.getSession(sessionId)).thenReturn(session);

        ResponseEntity<?> response = controller.getAuthStatus(sessionId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(sessionId, body.get("sessionId"));
        assertEquals(userId, body.get("userId"));
        assertTrue((Boolean) body.get("spotify"));
        assertFalse((Boolean) body.get("google"));
    }

    @Test
    void testGetAuthStatusSessionNotFound() {
        String sessionId = "non-existent";

        when(sessionService.getSession(sessionId)).thenReturn(null);

        ResponseEntity<?> response = controller.getAuthStatus(sessionId);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testLogoutDeletesSession() {
        String sessionId = "session-to-delete";

        ResponseEntity<?> response = controller.logout(sessionId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(sessionService, times(1)).deleteSession(sessionId);
    }

    @Test
    void testLogoutHandlesError() {
        String sessionId = "error-session";

        doThrow(new RuntimeException("Redis error"))
                .when(sessionService).deleteSession(anyString());

        ResponseEntity<?> response = controller.logout(sessionId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}