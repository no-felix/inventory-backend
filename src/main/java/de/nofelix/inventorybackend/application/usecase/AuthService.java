package de.nofelix.inventorybackend.application.usecase;

import de.nofelix.inventorybackend.domain.exception.AuthenticationException;
import de.nofelix.inventorybackend.domain.exception.UserAlreadyExistsException;
import de.nofelix.inventorybackend.domain.model.User;
import de.nofelix.inventorybackend.domain.port.in.AuthenticateUserUseCase;
import de.nofelix.inventorybackend.domain.port.in.RegisterUserUseCase;
import de.nofelix.inventorybackend.domain.port.out.UserRepositoryPort;
import de.nofelix.inventorybackend.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Application service for authentication operations.
 *
 * <p>Implements user registration and authentication use cases.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements RegisterUserUseCase, AuthenticateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public Mono<User> register(RegisterCommand command) {
        log.info("Registering new user: {}", command.username());

        return userRepository.existsByUsername(command.username())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new UserAlreadyExistsException("username", command.username()));
                    }
                    return userRepository.existsByEmail(command.email());
                })
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new UserAlreadyExistsException("email", command.email()));
                    }
                    return createUser(command);
                });
    }

    @Override
    public Mono<AuthResult> authenticate(AuthCommand command) {
        log.info("Authenticating user: {}", command.username());

        return userRepository.findByUsername(command.username())
                .switchIfEmpty(Mono.error(new AuthenticationException("Invalid username or password")))
                .flatMap(user -> {
                    if (!user.canAuthenticate()) {
                        return Mono.error(new AuthenticationException("Account is disabled"));
                    }
                    if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
                        return Mono.error(new AuthenticationException("Invalid username or password"));
                    }
                    return generateTokens(user);
                });
    }

    @Override
    public Mono<AuthResult> refreshToken(String refreshToken) {
        log.debug("Refreshing access token");

        return Mono.fromCallable(() -> {
                    if (!jwtService.isRefreshToken(refreshToken)) {
                        throw new AuthenticationException("Invalid refresh token");
                    }
                    return jwtService.extractUsername(refreshToken);
                })
                .flatMap(userRepository::findByUsername)
                .switchIfEmpty(Mono.error(new AuthenticationException("User not found")))
                .flatMap(user -> {
                    if (!user.canAuthenticate()) {
                        return Mono.error(new AuthenticationException("Account is disabled"));
                    }
                    return generateTokens(user);
                })
                .onErrorMap(ex -> {
                    if (ex instanceof AuthenticationException) {
                        return ex;
                    }
                    return new AuthenticationException("Invalid or expired refresh token");
                });
    }

    private Mono<User> createUser(RegisterCommand command) {
        User user = User.builder()
                .username(command.username())
                .email(command.email())
                .passwordHash(passwordEncoder.encode(command.password()))
                .role(User.Role.USER)
                .enabled(true)
                .build();

        return userRepository.save(user)
                .doOnSuccess(saved -> log.info("User registered successfully: {}", saved.getUsername()));
    }

    private Mono<AuthResult> generateTokens(User user) {
        return Mono.fromCallable(() -> {
            String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole().name());
            String refreshToken = jwtService.generateRefreshToken(user.getUsername());
            long expiresIn = jwtService.getAccessTokenExpiration();

            return AuthResult.bearer(accessToken, refreshToken, expiresIn);
        });
    }
}
