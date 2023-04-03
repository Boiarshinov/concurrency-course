package course.concurrency.exams.auction;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionPessimistic implements Auction {

    private final Notifier notifier;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private volatile Bid latestBid = Bid.DUMMY;

    private final Lock bidLock = new ReentrantLock();

    public boolean propose(Bid bid) {
        if (bid.getPrice() <= latestBid.getPrice()) { //fast check
            return false;
        }

        bidLock.lock();
        try {
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
        //return without lock, because it's volatile
        return latestBid;
    }
}
