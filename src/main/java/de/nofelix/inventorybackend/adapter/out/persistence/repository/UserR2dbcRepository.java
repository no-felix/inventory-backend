package de.nofelix.inventorybackend.adapter.out.persistence.repository;

import de.nofelix.inventorybackend.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for user persistence operations.
 */
@Repository
public interface UserR2dbcRepository extends ReactiveCrudRepository<UserEntity, Long> {

    /**
     * Finds a user by their username.
     *
     * @param username the username
     * @return the user entity if found
     */
    Mono<UserEntity> findByUsername(String username);

    /**
     * Finds a user by their email.
     *
     * @param email the email address
     * @return the user entity if found
     */
    Mono<UserEntity> findByEmail(String email);

    /**
     * Checks if a user with the given username exists.
     *
     * @param username the username
     * @return true if exists
     */
    Mono<Boolean> existsByUsername(String username);

    /**
     * Checks if a user with the given email exists.
     *
     * @param email the email address
     * @return true if exists
     */
    Mono<Boolean> existsByEmail(String email);

    /**
     * Checks if any admin user exists.
     *
     * @param role the admin role
     * @return true if at least one admin exists
     */
    Mono<Boolean> existsByRoleAndEnabledTrue(String role);
}
