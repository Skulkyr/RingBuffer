package org.pogonin;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of the {@link RingBuffer} interface based on a blocking ring buffer.
 * Uses {@link ReentrantLock} and conditions to control buffer access.
 *
 * @param <T> The type of elements stored in the buffer.
 */
public class BlockingRingBuffer<T> implements RingBuffer<T> {
    private final T[] buffer;
    private int head;
    private int tail;
    private int count;

    private final ReentrantLock lock;
    private final Condition notFull;
    private final Condition notEmpty;

    /**
     * Creates a new instance of {@code BlockingRingBuffer} with the given capacity.
     *
     * @param capacity Buffer capacity. Must be a positive number.
     * @throws IllegalArgumentException If {@code capacity} is less than or equal to zero.
     */
    @SuppressWarnings("unchecked")
    public BlockingRingBuffer(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("Buffer size must be positive");
        buffer = (T[]) new Object[capacity];
        head = 0;
        tail = 0;
        count = 0;
        lock = new ReentrantLock(true);
        notFull = lock.newCondition();
        notEmpty = lock.newCondition();
    }

    /**
     * Adds an element to the buffer. If the buffer is full, the method blocks until
     * until there is room to add an element.
     *
     * @param item The item to add to the buffer.
     * @throws RuntimeException If the thread is interrupted while waiting.
     */
    public void put(T item) {
        if (item == null) throw new NullPointerException("Null items are not allowed");
        lock.lock();
        try {
            while (count == buffer.length) {
                notFull.await();
            }
            buffer[tail] = item;
            tail = (tail + 1) % buffer.length;
            count++;
            notEmpty.signal();
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread was interrupted while waiting to put item.", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves and removes an element from the buffer. If the buffer is empty, the method blocks until
     * until the element appears in the buffer.
     *
     * @return The element retrieved from the buffer.
     * @throws RuntimeException If the thread is interrupted while waiting.
     */
    public T take() {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await();
            }
            T item = buffer[head];
            buffer[head] = null;
            head = (head + 1) % buffer.length;
            count--;
            notFull.signal();
            return item;
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread was interrupted while waiting to take item.", e);
        } finally {
            lock.unlock();
        }
    }


    public int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean isFull() {
        return size() == buffer.length;
    }
}

