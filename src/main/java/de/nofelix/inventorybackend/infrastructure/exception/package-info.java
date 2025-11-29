/**
 * Exception Handling - Global Exception Handlers.
 * 
 * <p>This package contains global exception handlers using
 * {@code @RestControllerAdvice} to provide consistent error
 * responses across the application.</p>
 * 
 * <p>Exception handlers should:</p>
 * <ul>
 *   <li>Return RFC 7807 Problem Detail responses</li>
 *   <li>Map domain exceptions to appropriate HTTP status codes</li>
 *   <li>Log errors appropriately</li>
 *   <li>Not expose sensitive information in error messages</li>
 * </ul>
 * 
 * @since 1.0.0
 */
package de.nofelix.inventorybackend.infrastructure.exception;
