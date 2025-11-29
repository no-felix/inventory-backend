package de.nofelix.inventorybackend.adapter.out.persistence.entity;

import de.nofelix.inventorybackend.domain.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * R2DBC entity representing a user in the database.
 */
@Table("users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class UserEntity {

    @Id
    private Long id;

    @Column("username")
    private String username;

    @Column("email")
    private String email;

    @Column("password_hash")
    private String passwordHash;

    @Column("role")
    private String role;

    @Column("enabled")
    private boolean enabled;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    /**
     * Converts the role string to the domain enum.
     *
     * @return the User.Role enum value
     */
    public User.Role getRoleEnum() {
        return User.Role.valueOf(role);
    }
}
