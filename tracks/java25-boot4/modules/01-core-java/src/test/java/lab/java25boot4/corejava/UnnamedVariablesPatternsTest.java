package lab.java25boot4.corejava;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnnamedVariablesPatternsTest {

    @Test
    @DisplayName("Unnamed pattern variables match and destructure records")
    void shouldMatchRecordPatternWithUnnamedVariable() {
        var c1 = new UnnamedVariablesPatterns.Coordinate(10, 20, 30);
        String desc = UnnamedVariablesPatterns.describeProjection(c1);

        assertThat(desc).isEqualTo("2D projection at x=10, y=20");
    }

    @Test
    @DisplayName("Unnamed loop variable counts elements correctly")
    void shouldCountElementsWithUnnamedLoopVariable() {
        var list =
                List.of(
                        new UnnamedVariablesPatterns.Coordinate(1, 2, 3),
                        new UnnamedVariablesPatterns.Coordinate(4, 5, 6));
        int count = UnnamedVariablesPatterns.countValidCoordinates(list);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Unnamed catch variable handles unused exception cleanly")
    void shouldCatchWithUnnamedVariable() {
        assertThat(UnnamedVariablesPatterns.parseOrIgnore("12345")).isTrue();
        assertThat(UnnamedVariablesPatterns.parseOrIgnore("not-a-number")).isFalse();
    }

    @Test
    @DisplayName("Map iteration with unnamed parameter aggregates values")
    void shouldAggregateValuesWithUnnamedParameter() {
        Map<String, Integer> map = Map.of("a", 10, "b", 20, "c", 30);
        int total = UnnamedVariablesPatterns.sumValues(map);

        assertThat(total).isEqualTo(60);
    }
}
