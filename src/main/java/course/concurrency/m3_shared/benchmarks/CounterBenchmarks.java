package course.concurrency.m3_shared.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.*;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class CounterBenchmarks {

    public static final int WRITERS = 7;
    public static final int READERS = 1;

    private final AtomicLong atomicLongCounter = new AtomicLong();
    private final LongAdder longAdderCounter = new LongAdder();

    private final Lock lock = new ReentrantLock();
    private final Lock lockFair = new ReentrantLock(true);

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReadWriteLock readWriteLockFair = new ReentrantReadWriteLock(true);

    private final Semaphore semaphore = new Semaphore(1);
    private final Semaphore semaphoreFair = new Semaphore(1, true);

    private final StampedLock stampedLock = new StampedLock();

    @State(Scope.Group)
    public static class ValueState {
        long value = 0;
    }

    @State(Scope.Group)
    public static class VolatileValueState {
        volatile long value = 0;

    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(CounterBenchmarks.class.getName())
                .forks(1)
//                .resultFormat(ResultFormatType.JSON)
//                .result("benchmark-result.json")
                .build();

        new Runner(options).run();
    }

    @Benchmark
    @Group("AtomicLong")
    @GroupThreads(WRITERS)
    public long writeAtomicLong() {
        return atomicLongCounter.incrementAndGet();
    }

    @Benchmark
    @Group("AtomicLong")
    @GroupThreads(READERS)
    public long readAtomicLong() {
        return atomicLongCounter.get();
    }

    @Benchmark
    @Group("LongAdder")
    @GroupThreads(WRITERS)
    public void writeLongAdder() {
        longAdderCounter.increment();
    }

    @Benchmark
    @Group("LongAdder")
    @GroupThreads(READERS)
    public long readLongAdder() {
        return longAdderCounter.sum();
    }

    @Benchmark
    @Group("ReentrantLock")
    @GroupThreads(WRITERS)
    public void writeReentrantLock(ValueState state) {
        try {
            lock.lock();
            state.value++;
        } finally {
            lock.unlock();
        }
    }

    @Benchmark
    @Group("ReentrantLock")
    @GroupThreads(READERS)
    public long readReentrantLock(ValueState state) {
        try {
            lock.lock();
            return state.value;
        } finally {
            lock.unlock();
        }
    }

    @Benchmark
    @Group("ReentrantLock_fair")
    @GroupThreads(WRITERS)
    public void writeReentrantLockFair(ValueState state) {
        try {
            lockFair.lock();
            state.value++;
        } finally {
            lockFair.unlock();
        }
    }

    @Benchmark
    @Group("ReentrantLock_fair")
    @GroupThreads(READERS)
    public long readReentrantLockFair(ValueState state) {
        try {
            lockFair.lock();
            return state.value;
        } finally {
            lockFair.unlock();
        }
    }

    @Benchmark
    @Group("ReadWriteLock")
    @GroupThreads(WRITERS)
    public void writeReadWriteLock(ValueState state) {
        try {
            readWriteLock.writeLock().lock();
            state.value++;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Benchmark
    @Group("ReadWriteLock")
    @GroupThreads(READERS)
    public long readReadWriteLock(ValueState state) {
        try {
            readWriteLock.readLock().lock();
            return state.value;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Benchmark
    @Group("ReadWriteLock_fair")
    @GroupThreads(WRITERS)
    public void writeReadWriteLockFair(ValueState state) {
        try {
            readWriteLockFair.writeLock().lock();
            state.value++;
        } finally {
            readWriteLockFair.writeLock().unlock();
        }
    }

    @Benchmark
    @Group("ReadWriteLock_fair")
    @GroupThreads(READERS)
    public long readReadWriteLockFair(ValueState state) {
        try {
            readWriteLockFair.readLock().lock();
            return state.value;
        } finally {
            readWriteLockFair.readLock().unlock();
        }
    }

    @Benchmark
    @Group("Semaphore")
    @GroupThreads(WRITERS)
    public void writeSemaphore(ValueState state) throws InterruptedException {
        try {
            semaphore.acquire();
            state.value++;
        } finally {
            semaphore.release();
        }
    }

    @Benchmark
    @Group("Semaphore")
    @GroupThreads(READERS)
    public long readSemaphore(ValueState state) throws InterruptedException {
        try {
            semaphore.acquire();
            return state.value;
        } finally {
            semaphore.release();
        }
    }

    @Benchmark
    @Group("Semaphore_fair")
    @GroupThreads(WRITERS)
    public void writeSemaphoreFair(ValueState state) throws InterruptedException {
        try {
            semaphoreFair.acquire();
            state.value++;
        } finally {
            semaphoreFair.release();
        }
    }

    @Benchmark
    @Group("Semaphore_fair")
    @GroupThreads(READERS)
    public long readSemaphoreFair(ValueState state) throws InterruptedException {
        try {
            semaphoreFair.acquire();
            return state.value;
        } finally {
            semaphoreFair.release();
        }
    }

    @Benchmark
    @Group("StampedLock_readwrite")
    @GroupThreads(WRITERS)
    public void writeStampedLockRW(ValueState state) {
        long stamp = stampedLock.writeLock();
        try {
            state.value++;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Benchmark
    @Group("StampedLock_readwrite")
    @GroupThreads(READERS)
    public long readStampedLockRW(ValueState state) {
        long stamp = stampedLock.readLock();
        try {
            return state.value;
        } finally {
            stampedLock.unlockRead(stamp);
        }
    }

    @Benchmark
    @Group("StampedLock_optimistic")
    @GroupThreads(WRITERS)
    public void writeStampedLockOptimistic(ValueState state) {
        long stamp = stampedLock.writeLock();
        try {
            state.value++;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Benchmark
    @Group("StampedLock_optimistic")
    @GroupThreads(READERS)
    public long readStampedLockOptimistic(ValueState state) {
        long stamp, value;
        do {
            stamp = stampedLock.tryOptimisticRead();
            value = state.value;
        } while (!stampedLock.validate(stamp));
        return value;
    }

    @Benchmark
    @Group("synchronized")
    @GroupThreads(WRITERS)
    public void writeSynchronized(ValueState state) {
        synchronized (state) {
            state.value++;
        }
    }

    @Benchmark
    @Group("synchronized")
    @GroupThreads(READERS)
    public long readSynchronized(ValueState state) {
        synchronized (state) {
            return state.value;
        }
    }

    @Benchmark
    @Group("volatile_synchronized")
    @GroupThreads(WRITERS)
    public void writeSynchronizedVolatile(VolatileValueState state) {
        synchronized (state) {
            state.value++;
        }
    }

    @Benchmark
    @Group("volatile_synchronized")
    @GroupThreads(READERS)
    public long readSynchronizedVolatile(VolatileValueState state) {
        return state.value;
    }

    @Benchmark
    @Group("volatile")
    @GroupThreads(WRITERS)
    public void writeVolatile(VolatileValueState state) {
        state.value = System.currentTimeMillis();
    }

    @Benchmark
    @Group("volatile")
    @GroupThreads(READERS)
    public long readVolatile(VolatileValueState state) {
        return state.value;
    }
}
