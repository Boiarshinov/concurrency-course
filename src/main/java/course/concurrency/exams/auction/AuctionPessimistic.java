package course.concurrency.exams.auction;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionPessimistic implements Auction {

    private final Notifier notifier;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private Bid latestBid;

    private final Lock bidLock = new ReentrantLock();

    public boolean propose(Bid bid) {
        bidLock.lock();
        try {
            if (latestBid == null) {
                latestBid = bid;
                return true;
            }
            if (bid.getPrice() > latestBid.getPrice()) {
                notifier.sendOutdatedMessage(latestBid);
                latestBid = bid;
                return true;
            }
            return false;
        } finally {
            bidLock.unlock();
        }
    }

    public Bid getLatestBid() {
        bidLock.lock();
        try {
            return latestBid;
        } finally {
            bidLock.unlock();
        }
    }
}
