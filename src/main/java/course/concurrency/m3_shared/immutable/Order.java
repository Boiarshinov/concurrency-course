package course.concurrency.m3_shared.immutable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Order {

    public enum Status { NEW, IN_PROGRESS, DELIVERED }

    private final long id;
    private final List<Item> items;
    private final PaymentInfo paymentInfo;
    private final boolean isPacked;
    private final Status status;

    private Order(long id, List<Item> items, PaymentInfo paymentInfo, boolean isPacked, Status status) {
        this.id = id;
        this.items = items != null
                ? new ArrayList<>(items)
                : Collections.emptyList();
        this.paymentInfo = paymentInfo;
        this.isPacked = isPacked;
        this.status = status;
    }

    public static Order create(long id, List<Item> items) {
        return new Order(id, items, null, false, Status.NEW);
    }

    public boolean isReadyToDeliver() {
        return !items.isEmpty() && paymentInfo != null && isPacked;
    }

    public Order payed(PaymentInfo paymentInfo) {
        checkNotDelivered();
        return new Order(
                this.id,
                this.items,
                paymentInfo,
                this.isPacked,
                Status.IN_PROGRESS);
    }

    public Order packed() {
        checkNotDelivered();
        return new Order(
                this.id,
                this.items,
                this.paymentInfo,
                true,
                Status.IN_PROGRESS);
    }

    public Order delivered() {
        if (!isReadyToDeliver()) {
            throw new IllegalStateException("Order should not be delivered when it's not payed or not packed");
        }
        return new Order(
                this.id,
                this.items,
                this.paymentInfo,
                this.isPacked,
                Status.DELIVERED);
    }

    public boolean isDelivered() {
        return status == Status.DELIVERED;
    }

    private void checkNotDelivered() {
        if (status == Status.DELIVERED) {
            throw new IllegalStateException("Order already was delivered");
        }
    }


    //getters

    public Long getId() {
        return id;
    }

    public List<Item> getItems() {
        return new ArrayList<>(items);
    }

    public PaymentInfo getPaymentInfo() {
        return paymentInfo;
    }

    public boolean isPacked() {
        return isPacked;
    }

    public Status getStatus() {
        return status;
    }
}
