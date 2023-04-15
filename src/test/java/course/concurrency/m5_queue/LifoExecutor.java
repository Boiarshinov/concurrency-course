package course.concurrency.m5_queue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.*;

public class LifoExecutor  {

    private static class ReversedBlockingQueue<E> extends LinkedBlockingDeque<E> {
        @Override
        public E take() throws InterruptedException {
            return super.takeLast();
        }
    }

    ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1,
            0, TimeUnit.MILLISECONDS,
            new ReversedBlockingQueue<>());

    @Test
    void lol() throws InterruptedException {
        var latch = new CountDownLatch(1);
        List<Integer> result = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 10; i++) {
            executor.execute(prepareTask(i, result, latch));
        }
        latch.countDown();

        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);
        System.out.println(result);
    }

    private Runnable prepareTask(int id, List<Integer> result, CountDownLatch latch) {
        return () -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            result.add(id);
        };
    }
}
