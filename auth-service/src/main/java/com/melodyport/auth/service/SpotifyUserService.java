package com.melodyport.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import java.util.Map;

@Service
public class SpotifyUserService {
    private static final String USER_INFO_URL = "https://api.spotify.com/v1/me";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SpotifyUserService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch current user's profile from Spotify API
     */
    @SuppressWarnings("unchecked")
    public String fetchUserId(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);

            String responseJson = restTemplate.getForObject(USER_INFO_URL, String.class, request);
            Map<String, Object> userInfo = objectMapper.readValue(responseJson, Map.class);

            return (String) userInfo.get("id");
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user info from Spotify", e);
        }
    }
}