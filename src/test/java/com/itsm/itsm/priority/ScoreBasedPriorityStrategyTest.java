package com.itsm.itsm.priority;

import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.model.Priority;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreBasedPriorityStrategyTest {

    private final PriorityStrategy strategy = new ScoreBasedPriorityStrategy();

    @Test
    void lowestScoreIsP4() {
        assertEquals(Priority.P4, strategy.calculate(1, 1, 1, 1));
    }

    @Test
    void score3IsP4AndScore4IsP3() {
        assertEquals(Priority.P4, strategy.calculate(3, 1, 1, 1)); // score 3
        assertEquals(Priority.P3, strategy.calculate(2, 2, 1, 1)); // score 4
    }

    @Test
    void score8IsP3AndScore9IsP2() {
        assertEquals(Priority.P3, strategy.calculate(2, 2, 2, 1)); // score 8
        assertEquals(Priority.P2, strategy.calculate(3, 3, 1, 1)); // score 9
    }

    @Test
    void score12IsP2AndScore18IsP1() {
        assertEquals(Priority.P2, strategy.calculate(3, 2, 2, 1)); // score 12
        assertEquals(Priority.P1, strategy.calculate(3, 3, 2, 1)); // score 18
    }

    @Test
    void manyUsersRaisesPriorityOneLevel() {
        assertEquals(Priority.P3, strategy.calculate(1, 1, 1, 100));  // P4 -> P3
        assertEquals(Priority.P4, strategy.calculate(1, 1, 1, 99));   // just below the limit
    }

    @Test
    void manyUsersNeverGoesAboveP1() {
        assertEquals(Priority.P1, strategy.calculate(3, 3, 3, 500));
    }

    @Test
    void impactOutOfRangeThrows() {
        assertThrows(InvalidInputException.class, () -> strategy.calculate(0, 2, 2, 1));
        assertThrows(InvalidInputException.class, () -> strategy.calculate(4, 2, 2, 1));
    }

    @Test
    void zeroUsersAffectedThrows() {
        assertThrows(InvalidInputException.class, () -> strategy.calculate(2, 2, 2, 0));
    }
}
