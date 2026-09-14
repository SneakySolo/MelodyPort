package com.melodyport.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodyport.auth.dto.SpotifyTokenDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Base64;

@Service
public class SpotifyOAuth2Service {
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";

    @Value("${SPOTIFY_CLIENT_ID:}")
    private String clientId;

    @Value("${SPOTIFY_CLIENT_SECRET:}")
    private String clientSecret;

    @Value("${SPOTIFY_REDIRECT_URI:http://localhost:8080/auth/spotify/callback}")
    private String redirectUri;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SpotifyOAuth2Service(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Exchange authorization code for access token
     */
    public SpotifyTokenDTO exchangeCodeForToken(String code) {
        try {
            if (clientId == null || clientId.isEmpty() || clientSecret == null || clientSecret.isEmpty()) {
                throw new RuntimeException("Spotify credentials not configured. Set SPOTIFY_CLIENT_ID and SPOTIFY_CLIENT_SECRET environment variables.");
            }

            // Build auth header: Base64(clientId:clientSecret)
            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            // Build request body
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("code", code);
            body.add("redirect_uri", redirectUri);

            // Build headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            // Exchange code for token
            ResponseEntity<String> response = restTemplate.postForEntity(TOKEN_URL, request, String.class);
            String responseJson = response.getBody();
            SpotifyTokenDTO tokenDTO = objectMapper.readValue(responseJson, SpotifyTokenDTO.class);

            return tokenDTO;
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange code for token: " + e.getMessage(), e);
        }
    }

    /**
     * Refresh an expired access token
     */
    public SpotifyTokenDTO refreshToken(String refreshToken) {
        try {
            if (clientId == null || clientId.isEmpty() || clientSecret == null || clientSecret.isEmpty()) {
                throw new RuntimeException("Spotify credentials not configured.");
            }

            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("refresh_token", refreshToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(TOKEN_URL, request, String.class);
            String responseJson = response.getBody();
            SpotifyTokenDTO tokenDTO = objectMapper.readValue(responseJson, SpotifyTokenDTO.class);

            return tokenDTO;
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh token: " + e.getMessage(), e);
        }
    }
}