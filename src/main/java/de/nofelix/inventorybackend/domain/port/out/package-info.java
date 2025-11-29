/**
 * Output Ports (Driven Ports) - Repository and External Service Interfaces.
 * 
 * <p>This package contains interfaces that define how the domain
 * interacts with external systems (databases, message queues, etc.).
 * These ports are implemented by adapters in the infrastructure layer.</p>
 * 
 * <p>Output ports define WHAT the domain needs from external systems.</p>
 * 
 * <p>Examples:</p>
 * <ul>
 *   <li>{@code ProductRepositoryPort} - Persistence operations for products</li>
 *   <li>{@code PurchaseOrderRepositoryPort} - Persistence operations for purchase orders</li>
 *   <li>{@code StockMovementRepositoryPort} - Persistence operations for stock movements</li>
 * </ul>
 * 
 * @since 1.0.0
 */
package de.nofelix.inventorybackend.domain.port.out;
