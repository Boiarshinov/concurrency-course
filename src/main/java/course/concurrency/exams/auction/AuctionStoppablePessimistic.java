package course.concurrency.exams.auction;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private final Notifier notifier;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private volatile Bid latestBid = Bid.DUMMY;
    private boolean isStopped = false;

    private final Lock bidLock = new ReentrantLock();

    public boolean propose(Bid bid) {
        if (isStopped || bid.getPrice() <= latestBid.getPrice()) { //fast check
            return false;
        }

        bidLock.lock();
        try {
            if (isStopped || bid.getPrice() <= latestBid.getPrice()) {
                return false;
            }
            notifier.sendOutdatedMessage(latestBid);
            latestBid = bid;
            return true;
        } finally {
            bidLock.unlock();
        }
    }

    public Bid getLatestBid() {
        return latestBid;
    }

    public Bid stopAuction() {
        bidLock.lock();
        try {
            isStopped = true;
            return latestBid;
        } finally {
            bidLock.unlock();
        }
    }
}
