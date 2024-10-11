package org.pogonin;


import org.pogonin.exception.HaventDataForTakeException;
import org.pogonin.exception.NotEnjoySpaceToPutException;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class NonBlockingRingBuffer<T> implements RingBuffer<T> {
    private final AtomicReference<State> stateRef;
    private final AtomicReferenceArray<T> buffer;
    private final int capacity;
    private long attempt;

    @SuppressWarnings("unchecked")
    public NonBlockingRingBuffer(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("Buffer size must be positive");
        this.capacity = capacity;
        this.buffer = new AtomicReferenceArray<>(capacity);
        this.stateRef = new AtomicReference<>(new State(0, 0, 0));
        this.attempt = Long.MAX_VALUE;
    }

    public NonBlockingRingBuffer(int capacity, int attempt) {
        this(capacity);
        this.attempt = attempt;
    }

    @Override
    public void put(T item) {
        if (item == null) throw new NullPointerException("Null items are not allowed");
        for(int i = 0; i < attempt; i++) {
            State currentState = stateRef.get();
            if (currentState.count == capacity)
                continue;

            int newTail = (currentState.tail + 1) % capacity;
            int newCount = currentState.count + 1;
            State newState = new State(currentState.head, newTail, newCount);

            if (stateRef.compareAndSet(currentState, newState)) {
                buffer.set(currentState.tail, item);
                return;
            }
        }
        throw new NotEnjoySpaceToPutException("Buffer full");
    }

    @Override
    public T take() {
        for (int i = 0; i < attempt; i++) {
            State currentState = stateRef.get();
            if (currentState.count == 0) continue;

            T item = buffer.get(currentState.head);
            if (item == null) continue;

            int newHead = (currentState.head + 1) % capacity;
            int newCount = currentState.count - 1;
            State newState = new State(newHead, currentState.tail, newCount);

            if (stateRef.compareAndSet(currentState, newState)) {
                buffer.set(currentState.head, null);
                return item;
            }
        }
        throw new HaventDataForTakeException("Buffer is empty");
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

    private static class State {
        final int head;
        final int tail;
        final int count;

        State(int head, int tail, int count) {
            this.head = head;
            this.tail = tail;
            this.count = count;
        }
    }
}
