package org.pogonin;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RingBufferImpl<T> implements RingBuffer<T> {
    private final T[] buffer;
    private int head;
    private int tail;
    private int count;

    private final ReentrantLock lock;
    private final Condition notFull;
    private final Condition notEmpty;

    @SuppressWarnings("unchecked")
    public RingBufferImpl(int capacity) {
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

    public void put(T item) {
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
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

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
            throw new RuntimeException(e);
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

