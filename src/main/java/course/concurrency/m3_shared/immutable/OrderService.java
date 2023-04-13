package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class OrderService {

    private final Map<Long, Order> currentOrders = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(0L);

    public long createOrder(List<Item> items) {
        long id = nextId();
        if (currentOrders.containsKey(id)) {
            throw new IllegalStateException("Duplicated orderId: " + id);
        }
        Order order = Order.create(id, items);
        currentOrders.put(id, order);
        return id;
    }

    public void updatePaymentInfo(long orderId, PaymentInfo paymentInfo) {
        Order order = currentOrders.computeIfPresent(orderId, (id, o) -> o.payed(paymentInfo));
        checkOrderExists(orderId, order);

        if (order.isReadyToDeliver()) {
            deliver(order);
        }
    }

    public void setPacked(long orderId) {
        Order order = currentOrders.computeIfPresent(orderId, (id, o) -> o.packed());
        checkOrderExists(orderId, order);

        if (order.isReadyToDeliver()) {
            deliver(order);
        }
    }

    public boolean isDelivered(long orderId) {
        Order order = currentOrders.get(orderId);
        checkOrderExists(orderId, order);

        return order.isDelivered();
    }

    private static void checkOrderExists(long orderId, Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order not exists: " + orderId);
        }
    }

    private void deliver(Order order) {
        /* ... */
        currentOrders.computeIfPresent(order.getId(), (id, o) -> o.delivered());
    }

    private long nextId() {
        return idGen.incrementAndGet();
    }
}
