package de.nofelix.inventorybackend.application.usecase;

import de.nofelix.inventorybackend.domain.exception.AuthenticationException;
import de.nofelix.inventorybackend.domain.exception.UserAlreadyExistsException;
import de.nofelix.inventorybackend.domain.model.User;
import de.nofelix.inventorybackend.domain.port.in.AuthenticateUserUseCase.AuthCommand;
import de.nofelix.inventorybackend.domain.port.in.AuthenticateUserUseCase.AuthResult;
import de.nofelix.inventorybackend.domain.port.in.RegisterUserUseCase.RegisterCommand;
import de.nofelix.inventorybackend.domain.port.out.UserRepositoryPort;
import de.nofelix.inventorybackend.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("register_withValidCommand_createsUser")
        void register_withValidCommand_createsUser() {
            // given
            RegisterCommand command = new RegisterCommand("john_doe", "john@example.com", "password123");

            when(userRepository.existsByUsername("john_doe")).thenReturn(Mono.just(false));
            when(userRepository.existsByEmail("john@example.com")).thenReturn(Mono.just(false));
            when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                return Mono.just(User.builder()
                        .id(1L)
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .passwordHash(user.getPasswordHash())
                        .role(user.getRole())
                        .enabled(user.isEnabled())
                        .build());
            });

            // when/then
            StepVerifier.create(authService.register(command))
                    .assertNext(user -> {
                        assertThat(user.getId()).isEqualTo(1L);
                        assertThat(user.getUsername()).isEqualTo("john_doe");
                        assertThat(user.getEmail()).isEqualTo("john@example.com");
                        assertThat(user.getPasswordHash()).isEqualTo("hashed_password");
                        assertThat(user.getRole()).isEqualTo(User.Role.USER);
                        assertThat(user.isEnabled()).isTrue();
                    })
                    .verifyComplete();

            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("register_withExistingUsername_throwsException")
        void register_withExistingUsername_throwsException() {
            // given
            RegisterCommand command = new RegisterCommand("existing_user", "new@example.com", "password123");

            when(userRepository.existsByUsername("existing_user")).thenReturn(Mono.just(true));

            // when/then
            StepVerifier.create(authService.register(command))
                    .expectErrorMatches(ex -> ex instanceof UserAlreadyExistsException
                            && ((UserAlreadyExistsException) ex).getField().equals("username"))
                    .verify();
        }

        @Test
        @DisplayName("register_withExistingEmail_throwsException")
        void register_withExistingEmail_throwsException() {
            // given
            RegisterCommand command = new RegisterCommand("new_user", "existing@example.com", "password123");

            when(userRepository.existsByUsername("new_user")).thenReturn(Mono.just(false));
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(Mono.just(true));

            // when/then
            StepVerifier.create(authService.register(command))
                    .expectErrorMatches(ex -> ex instanceof UserAlreadyExistsException
                            && ((UserAlreadyExistsException) ex).getField().equals("email"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("authenticate")
    class AuthenticateTests {

        @Test
        @DisplayName("authenticate_withValidCredentials_returnsTokens")
        void authenticate_withValidCredentials_returnsTokens() {
            // given
            AuthCommand command = new AuthCommand("john_doe", "password123");

            User user = User.builder()
                    .id(1L)
                    .username("john_doe")
                    .email("john@example.com")
                    .passwordHash("hashed_password")
                    .role(User.Role.USER)
                    .enabled(true)
                    .build();

            when(userRepository.findByUsername("john_doe")).thenReturn(Mono.just(user));
            when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
            when(jwtService.generateAccessToken("john_doe", "USER")).thenReturn("access-token");
            when(jwtService.generateRefreshToken("john_doe")).thenReturn("refresh-token");
            when(jwtService.getAccessTokenExpiration()).thenReturn(900L);

            // when/then
            StepVerifier.create(authService.authenticate(command))
                    .assertNext(result -> {
                        assertThat(result.accessToken()).isEqualTo("access-token");
                        assertThat(result.refreshToken()).isEqualTo("refresh-token");
                        assertThat(result.expiresIn()).isEqualTo(900L);
                        assertThat(result.tokenType()).isEqualTo("Bearer");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("authenticate_withInvalidUsername_throwsException")
        void authenticate_withInvalidUsername_throwsException() {
            // given
            AuthCommand command = new AuthCommand("unknown_user", "password123");

            when(userRepository.findByUsername("unknown_user")).thenReturn(Mono.empty());

            // when/then
            StepVerifier.create(authService.authenticate(command))
                    .expectErrorMatches(ex -> ex instanceof AuthenticationException
                            && ex.getMessage().equals("Invalid username or password"))
                    .verify();
        }

        @Test
        @DisplayName("authenticate_withWrongPassword_throwsException")
        void authenticate_withWrongPassword_throwsException() {
            // given
            AuthCommand command = new AuthCommand("john_doe", "wrong_password");

            User user = User.builder()
                    .id(1L)
                    .username("john_doe")
                    .passwordHash("hashed_password")
                    .role(User.Role.USER)
                    .enabled(true)
                    .build();

            when(userRepository.findByUsername("john_doe")).thenReturn(Mono.just(user));
            when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

            // when/then
            StepVerifier.create(authService.authenticate(command))
                    .expectErrorMatches(ex -> ex instanceof AuthenticationException
                            && ex.getMessage().equals("Invalid username or password"))
                    .verify();
        }

        @Test
        @DisplayName("authenticate_withDisabledAccount_throwsException")
        void authenticate_withDisabledAccount_throwsException() {
            // given
            AuthCommand command = new AuthCommand("disabled_user", "password123");

            User user = User.builder()
                    .id(1L)
                    .username("disabled_user")
                    .passwordHash("hashed_password")
                    .role(User.Role.USER)
                    .enabled(false)
                    .build();

            when(userRepository.findByUsername("disabled_user")).thenReturn(Mono.just(user));

            // when/then
            StepVerifier.create(authService.authenticate(command))
                    .expectErrorMatches(ex -> ex instanceof AuthenticationException
                            && ex.getMessage().equals("Account is disabled"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("refreshToken_withValidRefreshToken_returnsNewTokens")
        void refreshToken_withValidRefreshToken_returnsNewTokens() {
            // given
            String refreshToken = "valid-refresh-token";

            User user = User.builder()
                    .id(1L)
                    .username("john_doe")
                    .role(User.Role.USER)
                    .enabled(true)
                    .build();

            when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
            when(jwtService.extractUsername(refreshToken)).thenReturn("john_doe");
            when(userRepository.findByUsername("john_doe")).thenReturn(Mono.just(user));
            when(jwtService.generateAccessToken("john_doe", "USER")).thenReturn("new-access-token");
            when(jwtService.generateRefreshToken("john_doe")).thenReturn("new-refresh-token");
            when(jwtService.getAccessTokenExpiration()).thenReturn(900L);

            // when/then
            StepVerifier.create(authService.refreshToken(refreshToken))
                    .assertNext(result -> {
                        assertThat(result.accessToken()).isEqualTo("new-access-token");
                        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("refreshToken_withAccessToken_throwsException")
        void refreshToken_withAccessToken_throwsException() {
            // given
            String accessToken = "access-token-not-refresh";

            when(jwtService.isRefreshToken(accessToken)).thenReturn(false);

            // when/then
            StepVerifier.create(authService.refreshToken(accessToken))
                    .expectErrorMatches(ex -> ex instanceof AuthenticationException
                            && ex.getMessage().contains("Invalid refresh token"))
                    .verify();
        }

        @Test
        @DisplayName("refreshToken_withDisabledUser_throwsException")
        void refreshToken_withDisabledUser_throwsException() {
            // given
            String refreshToken = "valid-refresh-token";

            User user = User.builder()
                    .id(1L)
                    .username("disabled_user")
                    .role(User.Role.USER)
                    .enabled(false)
                    .build();

            when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
            when(jwtService.extractUsername(refreshToken)).thenReturn("disabled_user");
            when(userRepository.findByUsername("disabled_user")).thenReturn(Mono.just(user));

            // when/then
            StepVerifier.create(authService.refreshToken(refreshToken))
                    .expectErrorMatches(ex -> ex instanceof AuthenticationException
                            && ex.getMessage().equals("Account is disabled"))
                    .verify();
        }
    }
}
