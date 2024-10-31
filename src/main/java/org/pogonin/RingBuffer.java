package org.pogonin;

/**
 * Introduces a ring buffer that allows for thread-safe
 * interaction between producers and consumers of data.
 *
 * @param <T> The type of elements stored in the buffer.
 */
public interface RingBuffer<T> {

    /**
     * Adds an element to the buffer. If the buffer is full, the method blocks until
     * until space becomes available.
     *
     * @param item The item to add to the buffer.
     */
    void put(T item);

    /**
     * Retrieves and removes an element from the buffer. If the buffer is empty, the method blocks until
     * until the element appears in the buffer.
     *
     * @return The element retrieved from the buffer.
     */
    T take();

    /**
     * Returns the current number of elements in the buffer.
     *
     * @return The number of elements in the buffer.
     */
    int size();

    /**
     * Checks if the buffer is empty.
     *
     * @return {@code true} if the buffer is empty, {@code false} otherwise.
     */
    boolean isEmpty();

    /**
     * Checks if the buffer is full.
     *
     * @return {@code true} if the buffer is full, {@code false} otherwise.
     */
    boolean isFull();
}
