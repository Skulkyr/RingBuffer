package org.pogonin;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;


public class BlockingRingBuffer<T> implements RingBuffer<T> {
    private final T[] buffer;
    private final AtomicInteger head;
    private final AtomicInteger tail;
    private final AtomicInteger count;

    private final Semaphore putSemaphore;
    private final Semaphore takeSemaphore;



    @SuppressWarnings("unchecked")
    public BlockingRingBuffer(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("Buffer size must be positive");
        buffer = (T[]) new Object[capacity];
        head = new AtomicInteger(0);
        tail = new AtomicInteger(0);
        count = new AtomicInteger(0);
        putSemaphore = new Semaphore(capacity);
        takeSemaphore = new Semaphore(0);
    }


    public void put(T item) {
        if (item == null) throw new NullPointerException("Null items are not allowed");
        try {
            putSemaphore.acquire();
            int currentTail = tail.getAndUpdate(t -> (t+1) % buffer.length);
            buffer[currentTail] = item;
            count.getAndIncrement();
            takeSemaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread was interrupted while waiting to put item.", e);
        }
    }


    public T take() {
        try {
            takeSemaphore.acquire();
            int currentHead = head.getAndUpdate(t -> (t+1) % buffer.length);
            T item = buffer[currentHead];
            count.getAndDecrement();
            putSemaphore.release();
            return item;
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread was interrupted while waiting to take item.", e);
        }
    }

    public int size() {
            return count.get();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean isFull() {
        return size() == buffer.length;
    }
}

