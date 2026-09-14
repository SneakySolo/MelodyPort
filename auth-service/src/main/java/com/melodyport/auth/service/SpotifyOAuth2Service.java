package com.melodyport.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodyport.auth.dto.SpotifyTokenDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
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

    @Value("${spring.security.oauth2.client.registration.spotify.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.spotify.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.spotify.redirect-uri}")
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
            String responseJson = restTemplate.postForObject(TOKEN_URL, request, String.class);
            SpotifyTokenDTO tokenDTO = objectMapper.readValue(responseJson, SpotifyTokenDTO.class);

            return tokenDTO;
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange code for token", e);
        }
    }

    /**
     * Refresh an expired access token
     */
    public SpotifyTokenDTO refreshToken(String refreshToken) {
        try {
            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("refresh_token", refreshToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            String responseJson = restTemplate.postForObject(TOKEN_URL, request, String.class);
            SpotifyTokenDTO tokenDTO = objectMapper.readValue(responseJson, SpotifyTokenDTO.class);

            return tokenDTO;
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh token", e);
        }
    }
}