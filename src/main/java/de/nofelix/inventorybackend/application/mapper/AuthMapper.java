package de.nofelix.inventorybackend.application.mapper;

import de.nofelix.inventorybackend.adapter.in.web.model.AuthResponse;
import de.nofelix.inventorybackend.adapter.in.web.model.LoginRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.RegisterRequest;
import de.nofelix.inventorybackend.adapter.in.web.model.UserResponse;
import de.nofelix.inventorybackend.domain.model.User;
import de.nofelix.inventorybackend.domain.port.in.AuthenticateUserUseCase.AuthCommand;
import de.nofelix.inventorybackend.domain.port.in.AuthenticateUserUseCase.AuthResult;
import de.nofelix.inventorybackend.domain.port.in.RegisterUserUseCase.RegisterCommand;
import de.nofelix.inventorybackend.domain.port.in.SetupAdminUseCase.SetupAdminCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * MapStruct mapper for authentication-related conversions.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface AuthMapper {

    /**
     * Converts a RegisterRequest to a RegisterCommand.
     */
    RegisterCommand toRegisterCommand(RegisterRequest request);

    /**
     * Converts a LoginRequest to an AuthCommand.
     */
    AuthCommand toAuthCommand(LoginRequest request);

    /**
     * Converts a User domain object to a UserResponse.
     */
    @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(user.getCreatedAt()))")
    UserResponse toUserResponse(User user);

    /**
     * Converts an AuthResult to an AuthResponse.
     */
    AuthResponse toAuthResponse(AuthResult result);

    /**
     * Converts a RegisterRequest to a SetupAdminCommand.
     */
    SetupAdminCommand toSetupAdminCommand(RegisterRequest request);

    /**
     * Helper method to convert Instant to OffsetDateTime.
     */
    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }
}
