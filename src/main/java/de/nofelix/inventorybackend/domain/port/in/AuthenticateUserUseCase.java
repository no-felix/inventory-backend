package de.nofelix.inventorybackend.domain.port.in;

import reactor.core.publisher.Mono;

/**
 * Input port for user authentication use case.
 */
public interface AuthenticateUserUseCase {

    /**
     * Authenticates a user and returns JWT tokens.
     *
     * @param command the authentication command
     * @return the authentication result with tokens
     */
    Mono<AuthResult> authenticate(AuthCommand command);

    /**
     * Refreshes an access token using a refresh token.
     *
     * @param refreshToken the refresh token
     * @return new authentication result with fresh tokens
     */
    Mono<AuthResult> refreshToken(String refreshToken);

    /**
     * Command for user authentication.
     *
     * @param username the username
     * @param password the plain text password
     */
    record AuthCommand(String username, String password) {}

    /**
     * Result of successful authentication.
     *
     * @param accessToken the JWT access token
     * @param refreshToken the JWT refresh token
     * @param expiresIn access token expiration in seconds
     * @param tokenType the token type (Bearer)
     */
    record AuthResult(
            String accessToken,
            String refreshToken,
            long expiresIn,
            String tokenType
    ) {
        public static AuthResult bearer(String accessToken, String refreshToken, long expiresIn) {
            return new AuthResult(accessToken, refreshToken, expiresIn, "Bearer");
        }
    }
}
