package lab.java25boot4.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RaceWinnerStructuredQueryTest {

    private final RaceWinnerStructuredQuery raceQuery = new RaceWinnerStructuredQuery();

    @Test
    @DisplayName("Should return the result of the fastest subtask")
    void getFastestQuote_picksFirstCompletedTask() throws Throwable {
        // Provider Alpha takes 50ms, Beta takes 200ms -> Alpha wins
        var quoteAlpha = raceQuery.getFastestQuote(50, 200);
        assertThat(quoteAlpha.provider()).isEqualTo("Alpha");
        assertThat(quoteAlpha.priceUsd()).isEqualTo(100);

        // Provider Beta takes 50ms, Alpha takes 200ms -> Beta wins
        var quoteBeta = raceQuery.getFastestQuote(200, 50);
        assertThat(quoteBeta.provider()).isEqualTo("Beta");
        assertThat(quoteBeta.priceUsd()).isEqualTo(102);
    }
}
