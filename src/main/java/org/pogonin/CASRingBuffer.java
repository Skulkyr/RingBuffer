package org.pogonin;


import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;


public class CASRingBuffer<T> implements RingBuffer<T> {
    private final AtomicReference<State> stateRef;
    private final AtomicReferenceArray<T> buffer;
    private final int capacity;
    private final Semaphore put;
    private final Semaphore take;



    public CASRingBuffer(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("Buffer size must be positive");
        this.capacity = capacity;
        this.buffer = new AtomicReferenceArray<>(capacity);
        this.stateRef = new AtomicReference<>(new State(0, 0, 0));
        put = new Semaphore(capacity);
        take = new Semaphore(0);
    }


    @Override
    public void put(T item) {
        if (item == null) throw new NullPointerException("Null items are not allowed");

        try {
            put.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            State currentState = stateRef.get();
            int newTail = (currentState.tail + 1) % capacity;
            int newCount = currentState.count + 1;
            State newState = new State(currentState.head, newTail, newCount);

            synchronized (this) {
                if (stateRef.compareAndSet(currentState, newState)) {
                    buffer.set(currentState.tail, item);
                    take.release();
                    return;
                }
            }
        }
    }


    @Override
    public T take() {
        try {
            take.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            State currentState = stateRef.get();
            T item = buffer.get(currentState.head);
            if (item == null)
                continue;

            int newHead = (currentState.head + 1) % capacity;
            int newCount = currentState.count - 1;
            State newState = new State(newHead, currentState.tail, newCount);

            synchronized (this) {
                if (stateRef.compareAndSet(currentState, newState)) {
                    buffer.set(currentState.head, null);
                    put.release();
                    return item;
                }
            }
        }
    }

    @Override
    public int size() {
        return stateRef.get().count;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean isFull() {
        return size() == capacity;
    }

    private record State(int head, int tail, int count) {}
}
