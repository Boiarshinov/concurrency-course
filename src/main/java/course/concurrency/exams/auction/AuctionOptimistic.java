package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private final Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private static final Bid BID_DUMMY = new Bid(0, 0, 0);
    private final AtomicReference<Bid> bidRef = new AtomicReference<>(BID_DUMMY);

    public boolean propose(Bid bid) {
        Bid expected;
        do {
            expected = bidRef.get();
            if (bid.getPrice() <= expected.getPrice()) {
                return false;
            }
        } while (bidRef.compareAndSet(expected, bid));
        notifier.sendOutdatedMessage(expected);
        return true;
    }

    public Bid getLatestBid() {
        return bidRef.get();
    }
}
