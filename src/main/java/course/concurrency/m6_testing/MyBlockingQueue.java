package course.concurrency.m6_testing;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MyBlockingQueue<E> implements IBlockingQueue<E> {

    private final Object[] arr;
    private int head = 0;
    private int tail = 0;
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition enqueueCondition = lock.newCondition();
    private final Condition dequeueCondition = lock.newCondition();

    public MyBlockingQueue(int size) {
        arr = new Object[size];
    }

    @Override
    public void enqueue(E e) {
        lock.lock();
        try {
            while (count == arr.length) enqueueCondition.await();

            arr[head++] = e;
            if (head == arr.length) head = 0;
            ++count;
            dequeueCondition.signal();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E dequeue() {
        lock.lock();
        try {
            while (count == 0) dequeueCondition.await();
            E e = (E) arr[tail++];
            if (tail == arr.length) tail = 0;
            enqueueCondition.signal();
            --count;
            return e;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}

interface IBlockingQueue<E> {
    void enqueue(E e);
    E dequeue();
}
