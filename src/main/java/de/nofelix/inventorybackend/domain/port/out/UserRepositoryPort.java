package de.nofelix.inventorybackend.domain.port.out;

import de.nofelix.inventorybackend.domain.model.User;
import reactor.core.publisher.Mono;

/**
 * Output port for user persistence operations.
 *
 * <p>This port defines the contract for persisting and retrieving
 * users. It is implemented by the persistence adapter.</p>
 */
public interface UserRepositoryPort {

    /**
     * Saves a user (create or update).
     *
     * @param user the user to save
     * @return the saved user with generated ID
     */
    Mono<User> save(User user);

    /**
     * Finds a user by their ID.
     *
     * @param id the user ID
     * @return the user wrapped in a Mono, or empty if not found
     */
    Mono<User> findById(Long id);

    /**
     * Finds a user by their username.
     *
     * @param username the username
     * @return the user wrapped in a Mono, or empty if not found
     */
    Mono<User> findByUsername(String username);

    /**
     * Finds a user by their email address.
     *
     * @param email the email address
     * @return the user wrapped in a Mono, or empty if not found
     */
    Mono<User> findByEmail(String email);

    /**
     * Checks if a user with the given username exists.
     *
     * @param username the username
     * @return true if exists, false otherwise
     */
    Mono<Boolean> existsByUsername(String username);

    /**
     * Checks if a user with the given email exists.
     *
     * @param email the email address
     * @return true if exists, false otherwise
     */
    Mono<Boolean> existsByEmail(String email);
}
