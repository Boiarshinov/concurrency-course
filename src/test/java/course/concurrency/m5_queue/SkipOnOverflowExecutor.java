package course.concurrency.m5_queue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.*;

public class SkipOnOverflowExecutor {

    ExecutorService executor = new ThreadPoolExecutor(8, 8,
            0, TimeUnit.MILLISECONDS,
            new SynchronousQueue<>(), ((r, tp) -> {}));

    @Test
    void test() throws InterruptedException {
        var latch = new CountDownLatch(1);
        List<Integer> result = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 10; i++) {
            executor.execute(prepareTask(i, result, latch));
        }
        latch.countDown();

        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        System.out.println(result);
        Assertions.assertEquals(8, result.size());
    }

    private Runnable prepareTask(int id, List<Integer> result, CountDownLatch latch) {
        return () -> {
            try {
                latch.await();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            result.add(id);
        };
    }
}
