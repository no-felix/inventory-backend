package de.nofelix.inventorybackend.domain.exception;

/**
 * Exception thrown when a user already exists with the given username or email.
 */
public class UserAlreadyExistsException extends RuntimeException {

    private final String field;
    private final String value;

    public UserAlreadyExistsException(String field, String value) {
        super("User with " + field + " '" + value + "' already exists");
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
