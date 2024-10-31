package org.pogonin;


public class App {
    public static void main(String[] args) {
        final RingBuffer<Integer> ringBuffer = new CASRingBuffer<>(8);

        Runnable producer = () -> {
            for (int i = 1; i <= 100; i++) {
                ringBuffer.put(i);
                System.out.println("The producer added: " + i);
            }
        };

        Runnable consumer = () -> {
            for (int i = 1; i <= 100; i++) {
                int item = ringBuffer.take();
                System.out.println("The consumer received: " + item);
            }
        };

        Thread producerThread1 = new Thread(producer);
        Thread consumerThread1 = new Thread(consumer);

        producerThread1.start();
        consumerThread1.start();

        try {
            producerThread1.join();
            consumerThread1.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("The work is completed.");
    }
}
