package course.concurrency.exams.auction;

public class Bid {
    private final long id; // ID заявки
    private final long participantId; // ID участника
    private final long price; // предложенная цена

    public Bid(long id, long participantId, long price) {
        this.id = id;
        this.participantId = participantId;
        this.price = price;
    }

    public long getId() {
        return id;
    }

    public long getParticipantId() {
        return participantId;
    }

    public long getPrice() {
        return price;
    }
}
