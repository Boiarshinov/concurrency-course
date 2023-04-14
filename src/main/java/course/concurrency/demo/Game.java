package course.concurrency.demo;

import java.time.Duration;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntSupplier;

public class Game {

    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    private final Duration diceRollInterval;
    private final Duration blockingInterval;
    private final IntSupplier diceRoll;

    private final ReentrantLock betLock = new ReentrantLock();

    private final Set<Result> resultSet = new HashSet<>(); //todo подумать над многопоточкой

    public Game(Duration diceRollInterval, Duration blockingInterval, IntSupplier diceRoll) {
        this.diceRollInterval = diceRollInterval;
        this.blockingInterval = blockingInterval;
        this.diceRoll = diceRoll;
    }

    public void start() {
        executor.scheduleAtFixedRate(() -> {
                    int diceRollResult = diceRoll.getAsInt();
                    resultSet.forEach(result -> result.complete(diceRollResult));
                    resultSet.clear();
                    betLock.unlock();
                },
                diceRollInterval.toMillis(), diceRollInterval.toMillis(), TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(() -> betLock.lock(),
                diceRollInterval.toMillis() - blockingInterval.toMillis(), diceRollInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    public Result postBet(Bet bet) {
        if (betLock.isLocked()) {
            throw new IllegalArgumentException("Wait for the new game round to make a bet");
        }
        Result result = new Result(bet);
        resultSet.add(result);
        return result;
    }

    public static class Bet {
        private final int betNumber;
        private final int amount;

        public Bet(int betNumber, int amount) {
            this.betNumber = betNumber;
            this.amount = amount;
        }
    }

    public static class Result {
        private volatile int amount;
        private volatile boolean isComplete = false;

        private final Bet bet;

        Result(Bet bet) {
            this.bet = bet;
        }

        public boolean isComplete() {
            return isComplete;
        }

        public int get() {
            if (isComplete()) {
                return amount;
            }
            throw new IllegalStateException("result is not complete");
        }

        void complete(int diceRollResult) {
            amount = bet.betNumber == diceRollResult
                    ? bet.amount * 6
                    : 0;
            isComplete = true;
        }
    }

    public static class RandomDiceRoller implements IntSupplier {
        private final Random random = new Random();

        @Override
        public int getAsInt() {
            return random.nextInt(6) + 1;
        }
    }
}
