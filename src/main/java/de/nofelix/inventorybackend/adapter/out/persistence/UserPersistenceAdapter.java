package de.nofelix.inventorybackend.adapter.out.persistence;

import de.nofelix.inventorybackend.adapter.out.persistence.entity.UserEntity;
import de.nofelix.inventorybackend.adapter.out.persistence.repository.UserR2dbcRepository;
import de.nofelix.inventorybackend.domain.model.User;
import de.nofelix.inventorybackend.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Persistence adapter implementing the UserRepositoryPort.
 *
 * <p>This adapter translates between domain models and persistence entities,
 * delegating actual persistence operations to the R2DBC repository.</p>
 */
@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserR2dbcRepository repository;

    @Override
    public Mono<User> save(User user) {
        UserEntity entity = toEntity(user);

        if (entity.getId() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());

        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<User> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Mono<User> findByUsername(String username) {
        return repository.findByUsername(username)
                .map(this::toDomain);
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return repository.findByEmail(email)
                .map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    // ========================================
    // Mapping Methods
    // ========================================

    private UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .role(user.getRole() != null ? user.getRole().name() : User.Role.USER.name())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private User toDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .role(entity.getRoleEnum())
                .enabled(entity.isEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
