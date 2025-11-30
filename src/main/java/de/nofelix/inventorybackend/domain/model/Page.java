package de.nofelix.inventorybackend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Generic pagination response wrapper.
 * 
 * <p>Provides metadata for paginated API responses including
 * total elements, pages, and navigation hints.</p>
 *
 * @param <T> the type of content in the page
 */
@Getter
@AllArgsConstructor
@Builder
public class Page<T> {

    /**
     * The content of this page.
     */
    private final List<T> content;

    /**
     * Total number of elements across all pages.
     */
    private final long totalElements;

    /**
     * Total number of pages.
     */
    private final int totalPages;

    /**
     * Current page number (0-indexed).
     */
    private final int page;

    /**
     * Page size (number of elements per page).
     */
    private final int size;

    /**
     * Whether there is a next page.
     */
    private final boolean hasNext;

    /**
     * Whether there is a previous page.
     */
    private final boolean hasPrevious;

    /**
     * Creates a Page from content and pagination parameters.
     *
     * @param content       the content list
     * @param totalElements total number of elements
     * @param page          current page number (0-indexed)
     * @param size          page size
     * @param <T>           content type
     * @return a new Page instance
     */
    public static <T> Page<T> of(List<T> content, long totalElements, int page, int size) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;

        return Page.<T>builder()
                .content(content)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .page(page)
                .size(size)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }

    /**
     * Creates an empty page.
     *
     * @param page current page number
     * @param size page size
     * @param <T>  content type
     * @return an empty Page instance
     */
    public static <T> Page<T> empty(int page, int size) {
        return of(List.of(), 0, page, size);
    }
}
