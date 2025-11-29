package de.nofelix.inventorybackend.adapter.in.web;

import de.nofelix.inventorybackend.adapter.in.web.model.AuthResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.LoginRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.RefreshTokenRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.RegisterRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.UserResponse;
import de.nofelix.inventorybackend.application.mapper.AuthMapper;
import de.nofelix.inventorybackend.domain.exception.AuthenticationException;
import de.nofelix.inventorybackend.domain.exception.UserAlreadyExistsException;
import de.nofelix.inventorybackend.domain.model.User;
import de.nofelix.inventorybackend.domain.port.in.AuthenticateUserUseCase;
import de.nofelix.inventorybackend.domain.port.in.AuthenticateUserUseCase.AuthCommand;
import de.nofelix.inventorybackend.domain.port.in.AuthenticateUserUseCase.AuthResult;
import de.nofelix.inventorybackend.domain.port.in.RegisterUserUseCase;
import de.nofelix.inventorybackend.domain.port.in.RegisterUserUseCase.RegisterCommand;
import de.nofelix.inventorybackend.domain.port.in.SetupAdminUseCase;
import de.nofelix.inventorybackend.infrastructure.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthController.
 *
 * <p>Tests REST endpoints with mocked use cases using WebTestClient.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    private WebTestClient webTestClient;

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    @Mock
    private AuthenticateUserUseCase authenticateUserUseCase;

    @Mock
    private SetupAdminUseCase setupAdminUseCase;

    @Mock
    private AuthMapper authMapper;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(
                registerUserUseCase,
                authenticateUserUseCase,
                setupAdminUseCase,
                authMapper
        );

        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("register_withValidRequest_returns201WithUser")
        void register_withValidRequest_returns201WithUser() {
            // given
            RegisterRequest request = new RegisterRequest()
                    .username("john_doe")
                    .email("john@example.com")
                    .password("securePass123");

            RegisterCommand command = new RegisterCommand("john_doe", "john@example.com", "securePass123");

            User user = User.builder()
                    .id(1L)
                    .username("john_doe")
                    .email("john@example.com")
                    .role(User.Role.USER)
                    .enabled(true)
                    .createdAt(Instant.now())
                    .build();

            UserResponse response = new UserResponse()
                    .id(1L)
                    .username("john_doe")
                    .email("john@example.com")
                    .role(UserResponse.RoleEnum.USER)
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC));

            when(authMapper.toRegisterCommand(any(RegisterRequest.class))).thenReturn(command);
            when(registerUserUseCase.register(any(RegisterCommand.class))).thenReturn(Mono.just(user));
            when(authMapper.toUserResponse(any(User.class))).thenReturn(response);

            // when/then
            webTestClient.post()
                    .uri("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(UserResponse.class)
                    .value(res -> {
                        assertThat(res.getId()).isEqualTo(1L);
                        assertThat(res.getUsername()).isEqualTo("john_doe");
                        assertThat(res.getEmail()).isEqualTo("john@example.com");
                    });

            verify(registerUserUseCase).register(command);
        }

        @Test
        @DisplayName("register_withDuplicateUsername_returns409")
        void register_withDuplicateUsername_returns409() {
            // given
            RegisterRequest request = new RegisterRequest()
                    .username("existing_user")
                    .email("new@example.com")
                    .password("securePass123");

            RegisterCommand command = new RegisterCommand("existing_user", "new@example.com", "securePass123");

            when(authMapper.toRegisterCommand(any(RegisterRequest.class))).thenReturn(command);
            when(registerUserUseCase.register(any(RegisterCommand.class)))
                    .thenReturn(Mono.error(new UserAlreadyExistsException("username", "existing_user")));

            // when/then
            webTestClient.post()
                    .uri("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isEqualTo(409);
        }

        @Test
        @DisplayName("register_withDuplicateEmail_returns409")
        void register_withDuplicateEmail_returns409() {
            // given
            RegisterRequest request = new RegisterRequest()
                    .username("new_user")
                    .email("existing@example.com")
                    .password("securePass123");

            RegisterCommand command = new RegisterCommand("new_user", "existing@example.com", "securePass123");

            when(authMapper.toRegisterCommand(any(RegisterRequest.class))).thenReturn(command);
            when(registerUserUseCase.register(any(RegisterCommand.class)))
                    .thenReturn(Mono.error(new UserAlreadyExistsException("email", "existing@example.com")));

            // when/then
            webTestClient.post()
                    .uri("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("login_withValidCredentials_returns200WithTokens")
        void login_withValidCredentials_returns200WithTokens() {
            // given
            LoginRequest request = new LoginRequest()
                    .username("john_doe")
                    .password("securePass123");

            AuthCommand command = new AuthCommand("john_doe", "securePass123");
            AuthResult result = AuthResult.bearer("access-token", "refresh-token", 900);

            AuthResponse response = new AuthResponse()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .expiresIn(900L)
                    .tokenType("Bearer");

            when(authMapper.toAuthCommand(any(LoginRequest.class))).thenReturn(command);
            when(authenticateUserUseCase.authenticate(any(AuthCommand.class))).thenReturn(Mono.just(result));
            when(authMapper.toAuthResponse(any(AuthResult.class))).thenReturn(response);

            // when/then
            webTestClient.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(AuthResponse.class)
                    .value(res -> {
                        assertThat(res.getAccessToken()).isEqualTo("access-token");
                        assertThat(res.getRefreshToken()).isEqualTo("refresh-token");
                        assertThat(res.getExpiresIn()).isEqualTo(900L);
                        assertThat(res.getTokenType()).isEqualTo("Bearer");
                    });

            verify(authenticateUserUseCase).authenticate(command);
        }

        @Test
        @DisplayName("login_withInvalidCredentials_returns401")
        void login_withInvalidCredentials_returns401() {
            // given
            LoginRequest request = new LoginRequest()
                    .username("john_doe")
                    .password("wrongPassword");

            AuthCommand command = new AuthCommand("john_doe", "wrongPassword");

            when(authMapper.toAuthCommand(any(LoginRequest.class))).thenReturn(command);
            when(authenticateUserUseCase.authenticate(any(AuthCommand.class)))
                    .thenReturn(Mono.error(new AuthenticationException("Invalid username or password")));

            // when/then
            webTestClient.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("login_withDisabledAccount_returns401")
        void login_withDisabledAccount_returns401() {
            // given
            LoginRequest request = new LoginRequest()
                    .username("disabled_user")
                    .password("securePass123");

            AuthCommand command = new AuthCommand("disabled_user", "securePass123");

            when(authMapper.toAuthCommand(any(LoginRequest.class))).thenReturn(command);
            when(authenticateUserUseCase.authenticate(any(AuthCommand.class)))
                    .thenReturn(Mono.error(new AuthenticationException("Account is disabled")));

            // when/then
            webTestClient.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("refreshToken_withValidToken_returns200WithNewTokens")
        void refreshToken_withValidToken_returns200WithNewTokens() {
            // given
            RefreshTokenRequest request = new RefreshTokenRequest()
                    .refreshToken("valid-refresh-token");

            AuthResult result = AuthResult.bearer("new-access-token", "new-refresh-token", 900);

            AuthResponse response = new AuthResponse()
                    .accessToken("new-access-token")
                    .refreshToken("new-refresh-token")
                    .expiresIn(900L)
                    .tokenType("Bearer");

            when(authenticateUserUseCase.refreshToken("valid-refresh-token")).thenReturn(Mono.just(result));
            when(authMapper.toAuthResponse(any(AuthResult.class))).thenReturn(response);

            // when/then
            webTestClient.post()
                    .uri("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(AuthResponse.class)
                    .value(res -> {
                        assertThat(res.getAccessToken()).isEqualTo("new-access-token");
                        assertThat(res.getRefreshToken()).isEqualTo("new-refresh-token");
                    });

            verify(authenticateUserUseCase).refreshToken("valid-refresh-token");
        }

        @Test
        @DisplayName("refreshToken_withInvalidToken_returns401")
        void refreshToken_withInvalidToken_returns401() {
            // given
            RefreshTokenRequest request = new RefreshTokenRequest()
                    .refreshToken("invalid-token");

            when(authenticateUserUseCase.refreshToken("invalid-token"))
                    .thenReturn(Mono.error(new AuthenticationException("Invalid or expired refresh token")));

            // when/then
            webTestClient.post()
                    .uri("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("refreshToken_withExpiredToken_returns401")
        void refreshToken_withExpiredToken_returns401() {
            // given
            RefreshTokenRequest request = new RefreshTokenRequest()
                    .refreshToken("expired-token");

            when(authenticateUserUseCase.refreshToken("expired-token"))
                    .thenReturn(Mono.error(new AuthenticationException("Invalid or expired refresh token")));

            // when/then
            webTestClient.post()
                    .uri("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }
}
