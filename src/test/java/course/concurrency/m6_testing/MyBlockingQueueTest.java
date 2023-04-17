package course.concurrency.m6_testing;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class MyBlockingQueueTest {

    public static final int SIZE = 5;
    private final MyBlockingQueue<String> queue = new MyBlockingQueue<>(SIZE);

    @Test
    void insertAndGet() {
        queue.enqueue("element");
        assertEquals("element", queue.dequeue());
    }

    @Test
    void returnElementsInFifoOrder() {
        queue.enqueue("1");
        queue.enqueue("2");
        queue.enqueue("3");
        queue.enqueue("4");
        queue.enqueue("5");

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
            IntStream.range(0, 5).forEach(j -> queue.dequeue());
        }
    }
}
