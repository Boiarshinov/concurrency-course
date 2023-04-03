package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private final Notifier notifier;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private static final boolean RUNNING = false;
    private static final boolean STOPPED = true;

    private final AtomicMarkableReference<Bid> bidRef = new AtomicMarkableReference<>(Bid.DUMMY, RUNNING);

    public boolean propose(Bid bid) {
        if (bidRef.isMarked()) { //fast check without memory allocation
            return false;
        }

        Bid expected;
        do {
            expected = bidRef.getReference();
            if (bid.getPrice() <= expected.getPrice() || bidRef.isMarked()) {
                return false;
            }
        } while (!bidRef.compareAndSet(expected, bid, RUNNING, RUNNING));
        notifier.sendOutdatedMessage(expected);
        return true;
    }

    public Bid getLatestBid() {
        return bidRef.getReference();
    }

    public Bid stopAuction() {
        Bid expected;
        do {
            expected = bidRef.getReference();
        } while (!bidRef.attemptMark(expected, STOPPED));
        return bidRef.getReference();
    }
}
