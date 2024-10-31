package org.pogonin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

public class CASRingBufferTest {
    private CASRingBuffer<Integer> ringBuffer;
    private static final int CAPACITY = 16;

    @BeforeEach
    public void setUp() {
        ringBuffer = new CASRingBuffer<>(CAPACITY);
    }

    @Test
    @DisplayName("Buffer initialization test with positive capacity")
    public void testInitializationWithPositiveCapacity() {
        int capacity = 5;

        CASRingBuffer<String> buffer = new CASRingBuffer<>(capacity);

        assertThat(buffer.size()).isEqualTo(0);
        assertThat(buffer.isEmpty()).isTrue();
        assertThat(buffer.isFull()).isFalse();
    }

    @Test
    @DisplayName("Buffer initialization test with zero or negative capacity")
    public void testInitializationWithInvalidCapacity() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CASRingBuffer<>(0))
                .withMessage("Buffer size must be positive");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CASRingBuffer<>(-1))
                .withMessage("Buffer size must be positive");
    }

    @Test
    @DisplayName("Test adding and retrieving elements in single-threaded mode")
    public void testSingleThreadedPutAndTake() {

        for (int i = 0; i < CAPACITY; i++) {
            ringBuffer.put(i);
            assertThat(ringBuffer.size()).isEqualTo(i + 1);
        }
        assertThat(ringBuffer.isFull()).isTrue();

        for (int i = 0; i < CAPACITY; i++) {
            int item = ringBuffer.take();
            assertThat(item).isEqualTo(i);
            assertThat(ringBuffer.size()).isEqualTo(CAPACITY - i - 1);
        }
        assertThat(ringBuffer.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Test for adding a null element")
    public void testPutNull() {
        assertThatThrownBy(() -> ringBuffer.put(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Null items are not allowed");
    }

    @Test
    @DisplayName("Test for retrieving from an empty buffer")
    public void testTakeFromEmptyBuffer() throws ExecutionException, InterruptedException {
        @SuppressWarnings("all")
        ExecutorService executor = Executors.newSingleThreadExecutor();


        Future<Integer> future = executor.submit(() -> ringBuffer.take());
        try {
            future.get(100, TimeUnit.MILLISECONDS);
            fail("Expected a TimeoutException to be thrown");
        } catch (TimeoutException e) {
            future.cancel(true);
        } finally {
            executor.shutdownNow();
        }

        assertThat(ringBuffer.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Buffer full test")
    public void testBufferFull() throws ExecutionException, InterruptedException {
        for (int i = 0; i < CAPACITY; i++) {
            ringBuffer.put(i);
        }
        assertThat(ringBuffer.isFull()).isTrue();

        @SuppressWarnings("all")
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> ringBuffer.put(100));

        try {
            future.get(100, TimeUnit.MILLISECONDS);
            fail("Expected a TimeoutException to be thrown");
        } catch (TimeoutException e) {
            future.cancel(true);
        } finally {
            executor.shutdownNow();
        }

        assertThat(ringBuffer.size()).isEqualTo(CAPACITY);
    }

    @Test
    @DisplayName("Test for multi-threaded addition of elements")
    public void testMultiThreadedPut() throws InterruptedException {
        int threads = 5;
        int itemsPerThread = 2;
        @SuppressWarnings("all")
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);


        IntStream.range(0, threads).forEach(t -> executor.execute(() -> {
                for (int i = 0; i < itemsPerThread; i++) {
                    ringBuffer.put(t * itemsPerThread + i);
                }
                latch.countDown();
        }));
        latch.await();
        executor.shutdownNow();

        assertThat(ringBuffer.isFull()).isTrue();
        assertThat(ringBuffer.size()).isEqualTo(CAPACITY);
    }

    @Test
    @DisplayName("Multi-threaded element retrieval test")
    public void testMultiThreadedTake() throws InterruptedException {
        IntStream.range(0, CAPACITY).forEach(ringBuffer::put);
        int threads = 5;
        int itemsPerThread = 2;
        @SuppressWarnings("all")
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        ConcurrentLinkedQueue<Integer> takenItems = new ConcurrentLinkedQueue<>();

        IntStream.range(0, threads).forEach(_ -> executor.execute(() -> {
                for (int i = 0; i < itemsPerThread; i++) {
                    int item = ringBuffer.take();
                    takenItems.add(item);
                }
                latch.countDown();
        }));
        latch.await();
        executor.shutdownNow();

        assertThat(ringBuffer.isEmpty()).isTrue();
        assertThat(takenItems).hasSize(CAPACITY);
        for (int i = 0; i < CAPACITY; i++) {
            assertThat(takenItems).contains(i);
        }
    }

    @Test
    @DisplayName("Test for multi-threaded adding and retrieving elements")
    public void testMultiThreadedPutAndTake() throws InterruptedException {
        int producerThreads = 3;
        int consumerThreads = 3;
        int itemsPerProducer = 10;
        @SuppressWarnings("all")
        ExecutorService executor = Executors.newFixedThreadPool(producerThreads + consumerThreads);
        CountDownLatch latch = new CountDownLatch(producerThreads + consumerThreads);
        ConcurrentLinkedQueue<Integer> takenItems = new ConcurrentLinkedQueue<>();
        AtomicInteger producedCount = new AtomicInteger(0);


        IntStream.range(0, producerThreads).forEach(_ -> executor.execute(() -> {
                for (int i = 0; i < itemsPerProducer; i++) {
                    int item = producedCount.getAndIncrement();
                    ringBuffer.put(item);
                }
                latch.countDown();
        }));
        IntStream.range(0, consumerThreads).forEach(_ -> executor.execute(() -> {

                for (int i = 0; i < itemsPerProducer; i++) {
                    int item = ringBuffer.take();
                    takenItems.add(item);
                }
                latch.countDown();
        }));
        latch.await();
        executor.shutdownNow();


        assertThat(producedCount.get()).isEqualTo(producerThreads * itemsPerProducer);
        assertThat(takenItems).hasSize(consumerThreads * itemsPerProducer);
        for (int i = 0; i < producedCount.get(); i++) {
            assertThat(takenItems).contains(i);
        }
        assertThat(ringBuffer.size()).isBetween(0, CAPACITY);
    }

    @Test
    @DisplayName("Stress test with multiple threads")
    public void testStress() throws InterruptedException {
        int producerThreads = 10;
        int consumerThreads = 10;
        int itemsPerProducer = 1000;
        @SuppressWarnings("all")
        ExecutorService executor = Executors.newFixedThreadPool(producerThreads + consumerThreads);
        CountDownLatch latch = new CountDownLatch(producerThreads + consumerThreads);
        ConcurrentLinkedQueue<Integer> takenItems = new ConcurrentLinkedQueue<>();
        AtomicInteger producedCount = new AtomicInteger(0);


        IntStream.range(0, producerThreads).forEach(_ -> executor.execute(() -> {
                for (int i = 0; i < itemsPerProducer; i++) {
                    int item = producedCount.getAndIncrement();
                    ringBuffer.put(item);
                }
                latch.countDown();
        }));
        IntStream.range(0, consumerThreads).forEach(_ -> executor.execute(() -> {

                for (int i = 0; i < itemsPerProducer; i++) {
                    int item = ringBuffer.take();
                    takenItems.add(item);
                }
                latch.countDown();
        }));
        latch.await();
        executor.shutdownNow();


        assertThat(producedCount.get()).isEqualTo(producerThreads * itemsPerProducer);
        assertThat(takenItems).hasSize(consumerThreads * itemsPerProducer);
        for (int i = 0; i < producedCount.get(); i++)
            assertThat(takenItems).contains(i);
        assertThat(ringBuffer.size()).isBetween(0, CAPACITY);
    }
}

