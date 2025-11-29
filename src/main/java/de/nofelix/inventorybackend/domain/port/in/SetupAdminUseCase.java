package de.nofelix.inventorybackend.domain.port.in;

import de.nofelix.inventorybackend.domain.model.User;
import reactor.core.publisher.Mono;

/**
 * Use case for initial admin setup.
 *
 * <p>Allows creating the first admin account when no admin exists.</p>
 */
public interface SetupAdminUseCase {

    /**
     * Creates the initial admin account.
     * Only succeeds if no admin account exists yet.
     *
     * @param command the admin setup command
     * @return the created admin user
     */
    Mono<User> setupAdmin(SetupAdminCommand command);

    /**
     * Checks if initial setup is required (no admin exists).
     *
     * @return true if setup is needed
     */
    Mono<Boolean> isSetupRequired();

    record SetupAdminCommand(
            String username,
            String email,
            String password
    ) {}
}
