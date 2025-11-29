package de.nofelix.inventorybackend.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JwtService.
 */
@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;

    // A valid 256-bit secret for testing (at least 32 characters)
    private static final String TEST_SECRET = "test-secret-key-for-jwt-tokens-must-be-at-least-256-bits";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604800L);
        jwtService.init();
    }

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessTokenTests {

        @Test
        @DisplayName("generateAccessToken_withUsernameAndRole_returnsValidToken")
        void generateAccessToken_withUsernameAndRole_returnsValidToken() {
            // when
            String token = jwtService.generateAccessToken("john_doe", "USER");

            // then
            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtService.extractUsername(token)).isEqualTo("john_doe");
            assertThat(jwtService.extractRole(token)).isEqualTo("USER");
        }

        @Test
        @DisplayName("generateAccessToken_containsCorrectClaims")
        void generateAccessToken_containsCorrectClaims() {
            // when
            String token = jwtService.generateAccessToken("admin", "ADMIN");

            // then
            Claims claims = jwtService.validateAndGetClaims(token);
            assertThat(claims.getSubject()).isEqualTo("admin");
            assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
            assertThat(claims.get("type", String.class)).isEqualTo("access");
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isNotNull();
        }
    }

    @Nested
    @DisplayName("generateRefreshToken")
    class GenerateRefreshTokenTests {

        @Test
        @DisplayName("generateRefreshToken_withUsername_returnsValidToken")
        void generateRefreshToken_withUsername_returnsValidToken() {
            // when
            String token = jwtService.generateRefreshToken("john_doe");

            // then
            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtService.extractUsername(token)).isEqualTo("john_doe");
            assertThat(jwtService.isRefreshToken(token)).isTrue();
        }

        @Test
        @DisplayName("generateRefreshToken_hasNoRoleClaim")
        void generateRefreshToken_hasNoRoleClaim() {
            // when
            String token = jwtService.generateRefreshToken("john_doe");

            // then
            Claims claims = jwtService.validateAndGetClaims(token);
            assertThat(claims.get("role", String.class)).isNull();
            assertThat(claims.get("type", String.class)).isEqualTo("refresh");
        }
    }

    @Nested
    @DisplayName("validateAndGetClaims")
    class ValidateAndGetClaimsTests {

        @Test
        @DisplayName("validateAndGetClaims_withValidToken_returnsClaims")
        void validateAndGetClaims_withValidToken_returnsClaims() {
            // given
            String token = jwtService.generateAccessToken("john_doe", "USER");

            // when
            Claims claims = jwtService.validateAndGetClaims(token);

            // then
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo("john_doe");
        }

        @Test
        @DisplayName("validateAndGetClaims_withInvalidToken_throwsException")
        void validateAndGetClaims_withInvalidToken_throwsException() {
            // given
            String invalidToken = "invalid.token.here";

            // when/then
            assertThatThrownBy(() -> jwtService.validateAndGetClaims(invalidToken))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("validateAndGetClaims_withTamperedToken_throwsException")
        void validateAndGetClaims_withTamperedToken_throwsException() {
            // given
            String validToken = jwtService.generateAccessToken("john_doe", "USER");
            String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";

            // when/then
            assertThatThrownBy(() -> jwtService.validateAndGetClaims(tamperedToken))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("validateAndGetClaims_withExpiredToken_throwsExpiredException")
        void validateAndGetClaims_withExpiredToken_throwsExpiredException() {
            // given - Create a service with very short expiration
            JwtService shortLivedService = new JwtService();
            ReflectionTestUtils.setField(shortLivedService, "secret", TEST_SECRET);
            ReflectionTestUtils.setField(shortLivedService, "accessTokenExpiration", -1L); // Already expired
            ReflectionTestUtils.setField(shortLivedService, "refreshTokenExpiration", -1L);
            shortLivedService.init();

            String expiredToken = shortLivedService.generateAccessToken("john_doe", "USER");

            // when/then
            assertThatThrownBy(() -> jwtService.validateAndGetClaims(expiredToken))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }

    @Nested
    @DisplayName("isRefreshToken")
    class IsRefreshTokenTests {

        @Test
        @DisplayName("isRefreshToken_withRefreshToken_returnsTrue")
        void isRefreshToken_withRefreshToken_returnsTrue() {
            // given
            String refreshToken = jwtService.generateRefreshToken("john_doe");

            // when/then
            assertThat(jwtService.isRefreshToken(refreshToken)).isTrue();
        }

        @Test
        @DisplayName("isRefreshToken_withAccessToken_returnsFalse")
        void isRefreshToken_withAccessToken_returnsFalse() {
            // given
            String accessToken = jwtService.generateAccessToken("john_doe", "USER");

            // when/then
            assertThat(jwtService.isRefreshToken(accessToken)).isFalse();
        }
    }

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsernameTests {

        @Test
        @DisplayName("extractUsername_fromAccessToken_returnsUsername")
        void extractUsername_fromAccessToken_returnsUsername() {
            // given
            String token = jwtService.generateAccessToken("john_doe", "USER");

            // when/then
            assertThat(jwtService.extractUsername(token)).isEqualTo("john_doe");
        }

        @Test
        @DisplayName("extractUsername_fromRefreshToken_returnsUsername")
        void extractUsername_fromRefreshToken_returnsUsername() {
            // given
            String token = jwtService.generateRefreshToken("john_doe");

            // when/then
            assertThat(jwtService.extractUsername(token)).isEqualTo("john_doe");
        }
    }

    @Nested
    @DisplayName("extractRole")
    class ExtractRoleTests {

        @Test
        @DisplayName("extractRole_fromAccessToken_returnsRole")
        void extractRole_fromAccessToken_returnsRole() {
            // given
            String token = jwtService.generateAccessToken("admin", "ADMIN");

            // when/then
            assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("extractRole_fromRefreshToken_returnsNull")
        void extractRole_fromRefreshToken_returnsNull() {
            // given
            String token = jwtService.generateRefreshToken("john_doe");

            // when/then
            assertThat(jwtService.extractRole(token)).isNull();
        }
    }

    @Nested
    @DisplayName("getAccessTokenExpiration")
    class GetAccessTokenExpirationTests {

        @Test
        @DisplayName("getAccessTokenExpiration_returnsConfiguredValue")
        void getAccessTokenExpiration_returnsConfiguredValue() {
            // when/then
            assertThat(jwtService.getAccessTokenExpiration()).isEqualTo(900L);
        }
    }
}
