package de.nofelix.inventorybackend.adapter.in.web;

import de.nofelix.inventorybackend.adapter.in.web.api.AuthenticationApi;
import de.nofelix.inventorybackend.adapter.in.web.model.AuthResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.LoginRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.RefreshTokenRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.RegisterRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.SetupStatusResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.UserResponse;
import de.nofelix.inventorybackend.application.mapper.AuthMapper;
import de.nofelix.inventorybackend.domain.port.in.AuthenticateUserUseCase;
import de.nofelix.inventorybackend.domain.port.in.RegisterUserUseCase;
import de.nofelix.inventorybackend.domain.port.in.SetupAdminUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * REST controller implementing the AuthenticationApi interface.
 *
 * <p>Handles user registration, login, and token refresh operations.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthenticationApi {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final SetupAdminUseCase setupAdminUseCase;
    private final AuthMapper authMapper;

    @Override
    public Mono<ResponseEntity<UserResponse>> register(
            Mono<RegisterRequest> registerRequest,
            ServerWebExchange exchange) {
        log.debug("Received registration request");

        return registerRequest
                .map(authMapper::toRegisterCommand)
                .flatMap(registerUserUseCase::register)
                .map(authMapper::toUserResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Override
    public Mono<ResponseEntity<AuthResponse>> login(
            Mono<LoginRequest> loginRequest,
            ServerWebExchange exchange) {
        log.debug("Received login request");

        return loginRequest
                .map(authMapper::toAuthCommand)
                .flatMap(authenticateUserUseCase::authenticate)
                .map(authMapper::toAuthResponse)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AuthResponse>> refreshToken(
            Mono<RefreshTokenRequest> refreshTokenRequest,
            ServerWebExchange exchange) {
        log.debug("Received token refresh request");

        return refreshTokenRequest
                .map(RefreshTokenRequest::getRefreshToken)
                .flatMap(authenticateUserUseCase::refreshToken)
                .map(authMapper::toAuthResponse)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<SetupStatusResponse>> checkSetupStatus(ServerWebExchange exchange) {
        log.debug("Checking if admin setup is required");

        return setupAdminUseCase.isSetupRequired()
                .map(required -> {
                    var response = new SetupStatusResponse();
                    response.setSetupRequired(required);
                    return ResponseEntity.ok(response);
                });
    }

    @Override
    public Mono<ResponseEntity<UserResponse>> setupAdmin(
            Mono<RegisterRequest> registerRequest,
            ServerWebExchange exchange) {
        log.info("Received initial admin setup request");

        return registerRequest
                .map(authMapper::toSetupAdminCommand)
                .flatMap(setupAdminUseCase::setupAdmin)
                .map(authMapper::toUserResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }
}
