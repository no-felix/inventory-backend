package de.nofelix.inventorybackend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * Domain model representing a User for authentication.
 *
 * <p>This is a domain object for user authentication and authorization.
 * Passwords are stored as BCrypt hashes.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "passwordHash")
public class User {

    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private Role role;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * User roles for authorization.
     */
    public enum Role {
        USER,
        ADMIN
    }

    // ========================================
    // Business Methods
    // ========================================

    /**
     * Checks if the user has admin privileges.
     *
     * @return true if user has ADMIN role
     */
    public boolean isAdmin() {
        return Role.ADMIN.equals(role);
    }

    /**
     * Checks if the user account is active and can authenticate.
     *
     * @return true if account is enabled
     */
    public boolean canAuthenticate() {
        return enabled;
    }
}
