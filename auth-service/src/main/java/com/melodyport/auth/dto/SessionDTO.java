package com.melodyport.auth.dto;

import java.time.Instant;

public class SessionDTO {
    private String sessionId;
    private String userId;
    private SpotifyTokenDTO spotifyToken;
    private SpotifyTokenDTO googleToken;
    private Instant expiresAt;

    public SessionDTO() {
    }

    public SessionDTO(String sessionId, String userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.expiresAt = Instant.now().plusSeconds(86400); // 24 hours
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public SpotifyTokenDTO getSpotifyToken() {
        return spotifyToken;
    }

    public void setSpotifyToken(SpotifyTokenDTO spotifyToken) {
        this.spotifyToken = spotifyToken;
    }

    public SpotifyTokenDTO getGoogleToken() {
        return googleToken;
    }

    public void setGoogleToken(SpotifyTokenDTO googleToken) {
        this.googleToken = googleToken;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean hasSpotifyAuth() {
        return spotifyToken != null && !spotifyToken.isExpired();
    }

    public boolean hasGoogleAuth() {
        return googleToken != null && !googleToken.isExpired();
    }
}