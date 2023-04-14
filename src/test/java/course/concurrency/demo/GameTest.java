package course.concurrency.demo;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GameTest {

    public static final int WINNER_BET = 4;

    @Test
    void postBetAndWin() {
        Game game = new Game(Duration.ofSeconds(1), Duration.ofMillis(200), () -> WINNER_BET);
        game.start();
        int betAmount = 1;

        Game.Result result = game.postBet(new Game.Bet(WINNER_BET, betAmount));

        //todo rewrite to awaitility
        while (!result.isComplete()) {
        }

        assertEquals(betAmount * 6, result.get());
    }

    @Test
    void postBetAndLose() throws InterruptedException {
        Game game = new Game(Duration.ofSeconds(1), Duration.ofMillis(200), () -> WINNER_BET);
        game.start();
        int loserNum = 1;

        Game.Result result = game.postBet(new Game.Bet(loserNum, 1));

        //todo rewrite to awaitility
        while (!result.isComplete()) {
        }

        assertEquals(0, result.get());
    }

    @Test
    void cannotBetOnBlocked() throws InterruptedException {
        Game game = new Game(Duration.ofSeconds(1), Duration.ofMillis(500), () -> WINNER_BET);
        game.start();

        Thread.sleep(500);

        assertThrows(IllegalArgumentException.class, () ->
                game.postBet(new Game.Bet(WINNER_BET, 1)));
    }
}
