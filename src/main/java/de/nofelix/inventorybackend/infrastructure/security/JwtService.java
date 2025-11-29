package de.nofelix.inventorybackend.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Service for JWT token generation and validation.
 *
 * <p>Handles creation and parsing of JWT access and refresh tokens
 * using the JJWT library.</p>
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:900}")
    private long accessTokenExpiration; // 15 minutes default

    @Value("${jwt.refresh-token-expiration:604800}")
    private long refreshTokenExpiration; // 7 days default

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates an access token for the given username and role.
     *
     * @param username the username
     * @param role the user's role
     * @return the JWT access token
     */
    public String generateAccessToken(String username, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpiration);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a refresh token for the given username.
     *
     * @param username the username
     * @return the JWT refresh token
     */
    public String generateRefreshToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(refreshTokenExpiration);

        return Jwts.builder()
                .subject(username)
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates and parses a JWT token.
     *
     * @param token the JWT token
     * @return the claims if valid
     * @throws JwtException if the token is invalid or expired
     */
    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extracts the username from a token.
     *
     * @param token the JWT token
     * @return the username
     */
    public String extractUsername(String token) {
        return validateAndGetClaims(token).getSubject();
    }

    /**
     * Extracts the role from a token.
     *
     * @param token the JWT token
     * @return the role
     */
    public String extractRole(String token) {
        return validateAndGetClaims(token).get("role", String.class);
    }

    /**
     * Checks if the token is a refresh token.
     *
     * @param token the JWT token
     * @return true if refresh token
     */
    public boolean isRefreshToken(String token) {
        String type = validateAndGetClaims(token).get("type", String.class);
        return "refresh".equals(type);
    }

    /**
     * Gets the access token expiration time in seconds.
     *
     * @return expiration time in seconds
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
}
