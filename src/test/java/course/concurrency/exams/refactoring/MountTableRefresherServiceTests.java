package course.concurrency.exams.refactoring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class MountTableRefresherServiceTests {

    public static final int CACHE_UPDATE_TIMEOUT = 1000;
    private MountTableRefresherService service;

    private Others.RouterStore routerStore;
    private Others.MountTableManager manager;
    private Others.LoadingCache routerClientsCache;

    @BeforeEach
    public void setUpStreams() {
        service = new MountTableRefresherService();
        service.setCacheUpdateTimeout(CACHE_UPDATE_TIMEOUT);
        routerStore = mock(Others.RouterStore.class);
        manager = mock(Others.MountTableManager.class);
        routerClientsCache = mock(Others.LoadingCache.class);

        service.setRouterStore(routerStore);
        service.setManagerFactory(ignored -> manager);
        service.setRouterClientsCache(routerClientsCache);

        service = spy(service);
        // service.serviceInit(); // needed for complex class testing, not for now
    }

    @AfterEach
    public void restoreStreams() {
        // service.serviceStop();
    }

    @Test
    @DisplayName("All tasks are completed successfully")
    public void allDone() {
        prepareInitialState();
        when(manager.refresh()).thenReturn(true);

        service.refresh();

        verify(service).log("Mount table entries cache refresh successCount=4,failureCount=0");
        verify(routerClientsCache, never()).invalidate(anyString());
    }

    @Test
    @DisplayName("All tasks failed")
    public void noSuccessfulTasks() {
        prepareInitialState();
        when(manager.refresh()).thenReturn(false);

        service.refresh();

        verify(service).log("Mount table entries cache refresh successCount=0,failureCount=4");
        verify(routerClientsCache, times(4)).invalidate(anyString());
    }

    @Test
    @DisplayName("Some tasks failed")
    public void halfSuccessedTasks() {
        prepareInitialState();
        when(manager.refresh()).thenReturn(true, true, false, false);

        service.refresh();

        verify(service).log("Mount table entries cache refresh successCount=2,failureCount=2");
        verify(routerClientsCache, times(2)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task completed with exception")
    public void exceptionInOneTask() {
        prepareInitialState();
        AnswerSupplier answerSupplier = AnswerSupplier.of(
                () -> true,
                () -> true,
                () -> true,
                () -> {throw new RuntimeException();});
        when(manager.refresh()).thenAnswer(answerSupplier.getAnswer());

        service.refresh();

        verify(service).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task exceeds timeout")
    public void oneTaskExceedTimeout() {
        prepareInitialState();
        AnswerSupplier answerSupplier = AnswerSupplier.of(
                () -> true,
                () -> true,
                () -> true,
                () -> {
                    try {
                        Thread.sleep(CACHE_UPDATE_TIMEOUT + 100);
                        return true;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
        when(manager.refresh()).thenAnswer(answerSupplier.getAnswer());

        service.refresh();

        verify(service).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    @Test
    @DisplayName("Log on refreshing interrupted")
    public void interruptRefreshing() throws InterruptedException {
        prepareInitialState();
        when(manager.refresh()).thenAnswer(invocation -> {
            try {
                Thread.sleep(500);
                return true;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        Thread thread = new Thread(() -> service.refresh());
        thread.start();
        Thread.sleep(100);
        thread.interrupt();

        Thread.sleep(2000);

        verify(service).log("Mount table cache refresher was interrupted.");
        verify(service).log("Mount table entries cache refresh successCount=0,failureCount=4");
    }

    private void prepareInitialState() {
        List<Others.RouterState> states = Stream.of("123", "local6", "789", "local")
                .map(a -> new Others.RouterState(a))
                .collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);
    }

    private static class AnswerSupplier {

        AtomicInteger counter = new AtomicInteger(0);

        Supplier<Boolean>[] suppliers;

        public static AnswerSupplier of(Supplier<Boolean>... suppliers) {
            AnswerSupplier answerSupplier = new AnswerSupplier();
            answerSupplier.suppliers = suppliers;
            return answerSupplier;
        }

        public Answer<Boolean> getAnswer() {
            return invocation -> suppliers[counter.getAndIncrement()].get();
        }
    }
}
