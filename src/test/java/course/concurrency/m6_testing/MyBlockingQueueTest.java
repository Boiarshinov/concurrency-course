package course.concurrency.m6_testing;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class MyBlockingQueueTest {

    public static final int SIZE = 5;
    private final MyBlockingQueue<String> queue = new MyBlockingQueue<>(SIZE);

    @Test
    void insertAndGet() {
        queue.enqueue("element");

        assertEquals(1, queue.size());
        assertEquals("element", queue.dequeue());
        assertEquals(0, queue.size());
    }

    @Test
    void returnElementsInFifoOrder() {
        queue.enqueue("1");
        queue.enqueue("2");
        queue.enqueue("3");
        queue.enqueue("4");
        queue.enqueue("5");

        assertEquals(5, queue.size());

        assertEquals("1", queue.dequeue());
        assertEquals("2", queue.dequeue());
        assertEquals("3", queue.dequeue());
        assertEquals("4", queue.dequeue());
        assertEquals("5", queue.dequeue());
    }

    @Test
    void blocksOnDequeueFromEmpty() throws InterruptedException {
        Thread thread = new Thread(() -> queue.dequeue());
        thread.start();

        Thread.sleep(100);
        assertEquals(Thread.State.WAITING, thread.getState());

        queue.enqueue("element");
        Thread.sleep(100);
        assertEquals(Thread.State.TERMINATED, thread.getState());
    }

    @Test
    void blocksOnEnqueueToFull() throws InterruptedException {
        IntStream.range(0, SIZE).forEach(i -> queue.enqueue(String.valueOf(i)));
        Thread thread = new Thread(() -> queue.enqueue("one more"));
        thread.start();

        Thread.sleep(100);
        assertEquals(Thread.State.WAITING, thread.getState());

        queue.dequeue();
        Thread.sleep(100);
        assertEquals(Thread.State.TERMINATED, thread.getState());
    }

    @Test
    void enqueueSeveralCycles() {
        for (int i = 0; i < 5; i++) {
            IntStream.range(0, 5).forEach(j -> queue.enqueue(String.valueOf(j)));
            assertEquals(5, queue.size());
            IntStream.range(0, 5).forEach(j -> queue.dequeue());
            assertEquals(0, queue.size());
        }
    }

    @Test
    void doNotMissElements() throws InterruptedException {
        LongAdder counter = new LongAdder();
        int countPerTask = 10_000;
        CountDownLatch latch = new CountDownLatch(1);
        Runnable pushTask = () -> {
            try {
                latch.await();
            } catch (InterruptedException ignored) {}
            for (int i = 0; i < countPerTask; i++) {
                queue.enqueue(String.valueOf(i));
            }
        };
        Runnable pullTask = () -> {
            try {
                latch.await();
            } catch (InterruptedException ignored) {}
            for (int i = 0; i < countPerTask; i++) {
                queue.dequeue();
                counter.increment();
            }
        };

        int tasksCount = 5;
        ExecutorService pushPool = Executors.newFixedThreadPool(tasksCount);
        ExecutorService pullPool = Executors.newFixedThreadPool(tasksCount);

        IntStream.range(0, tasksCount).forEach(j -> pushPool.submit(pushTask));
        IntStream.range(0, tasksCount).forEach(j -> pullPool.submit(pullTask));

        latch.countDown();

        pushPool.shutdown();
        pullPool.shutdown();
        pushPool.awaitTermination(3, TimeUnit.SECONDS);
        pullPool.awaitTermination(3, TimeUnit.SECONDS);

        assertEquals(countPerTask * tasksCount, counter.sum());
    }
}
