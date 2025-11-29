package de.nofelix.inventorybackend.domain.port.in;

import de.nofelix.inventorybackend.domain.model.User;
import reactor.core.publisher.Mono;

/**
 * Input port for user registration use case.
 */
public interface RegisterUserUseCase {

    /**
     * Registers a new user.
     *
     * @param command the registration command
     * @return the created user
     */
    Mono<User> register(RegisterCommand command);

    /**
     * Command for user registration.
     *
     * @param username the desired username
     * @param email the email address
     * @param password the plain text password (will be hashed)
     */
    record RegisterCommand(String username, String email, String password) {}
}
