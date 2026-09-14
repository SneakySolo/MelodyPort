package com.melodyport.auth.controller;

import com.melodyport.auth.dto.SessionDTO;
import com.melodyport.auth.dto.SpotifyTokenDTO;
import com.melodyport.auth.service.RedisSessionService;
import com.melodyport.auth.service.SpotifyOAuth2Service;
import com.melodyport.auth.service.SpotifyUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth/spotify")
public class SpotifyAuthController {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyAuthController.class);

    @Value("${SPOTIFY_CLIENT_ID:}")
    private String clientId;

    @Value("${SPOTIFY_REDIRECT_URI:http://localhost:8080/auth/spotify/callback}")
    private String redirectUri;

    private final RedisSessionService sessionService;
    private final SpotifyOAuth2Service oauth2Service;
    private final SpotifyUserService userService;

    public SpotifyAuthController(
            RedisSessionService sessionService,
            SpotifyOAuth2Service oauth2Service,
            SpotifyUserService userService) {
        this.sessionService = sessionService;
        this.oauth2Service = oauth2Service;
        this.userService = userService;
    }

    /**
     * Initiate Spotify OAuth login flow
     */
    @GetMapping("/login")
    public RedirectView login() {
        try {
            String state = UUID.randomUUID().toString();
            String scope = "playlist-read-private playlist-read-collaborative playlist-modify-public playlist-modify-private";

            String authorizationUrl = "https://accounts.spotify.com/authorize?" +
                    "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&response_type=code" +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
                    "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

            logger.info("Redirecting to Spotify authorization URL");
            return new RedirectView(authorizationUrl);
        } catch (Exception e) {
            logger.error("Error initiating Spotify login", e);
            return new RedirectView("/error");
        }
    }

    /**
     * Handle Spotify OAuth callback
     */
    @GetMapping("/callback")
    public RedirectView callback(
            @RequestParam(name = "code") String code,
            @RequestParam(name = "state", required = false) String state) {
        try {
            logger.info("Received Spotify OAuth callback with code");

            // Exchange code for token
            SpotifyTokenDTO token = oauth2Service.exchangeCodeForToken(code);
            logger.info("Successfully exchanged code for Spotify token");

            // Fetch user ID
            String userId = userService.fetchUserId(token.getAccessToken());
            logger.info("Fetched Spotify user ID: {}", userId);

            // Create session and store Spotify token
            SessionDTO session = sessionService.createSession(userId);
            session.setSpotifyToken(token);
            sessionService.updateSession(session);
            logger.info("Created session {} for user {}", session.getSessionId(), userId);

            // Redirect to dashboard with sessionId
            String redirectUrl = "http://localhost:8080/dashboard?sessionId=" + session.getSessionId();
            return new RedirectView(redirectUrl);
        } catch (Exception e) {
            logger.error("Error during Spotify OAuth callback", e);
            return new RedirectView("http://localhost:8080/error?reason=spotify_auth_failed");
        }
    }

    /**
     * Get authentication status for a session
     */
    @GetMapping("/status")
    public ResponseEntity<?> getAuthStatus(@RequestParam(name = "sessionId") String sessionId) {
        try {
            SessionDTO session = sessionService.getSession(sessionId);

            if (session == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Session not found or expired"));
            }

            Map<String, Object> status = new HashMap<>();
            status.put("sessionId", session.getSessionId());
            status.put("userId", session.getUserId());
            status.put("spotify", session.hasSpotifyAuth());
            status.put("google", session.hasGoogleAuth());

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting auth status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get auth status"));
        }
    }

    /**
     * Logout: delete session
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam(name = "sessionId") String sessionId) {
        try {
            sessionService.deleteSession(sessionId);
            logger.info("Logged out session: {}", sessionId);
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            logger.error("Error during logout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to logout"));
        }
    }
}