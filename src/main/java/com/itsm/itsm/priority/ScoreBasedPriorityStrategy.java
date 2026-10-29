package com.itsm.itsm.priority;

import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.model.Priority;
import org.springframework.stereotype.Component;

// score = impact x urgency x criticality (each 1-3, so the score is 1-27)
//   score >= 18 -> P1,  >= 9 -> P2,  >= 4 -> P3,  otherwise P4
// Then, if 100 or more users are affected, raise the priority by one level.
@Component
public class ScoreBasedPriorityStrategy implements PriorityStrategy {

    public static final int MANY_USERS = 100;

    @Override
    public Priority calculate(int impact, int urgency, int criticality, int usersAffected) {
        checkRange("Impact", impact);
        checkRange("Urgency", urgency);
        checkRange("Criticality", criticality);
        if (usersAffected < 1) {
            throw new InvalidInputException("Users affected must be at least 1.");
        }

        int score = impact * urgency * criticality;
        Priority priority;
        if (score >= 18) {
            priority = Priority.P1;
        } else if (score >= 9) {
            priority = Priority.P2;
        } else if (score >= 4) {
            priority = Priority.P3;
        } else {
            priority = Priority.P4;
        }

        if (usersAffected >= MANY_USERS) {
            priority = priority.oneLevelHigher();
        }
        return priority;
    }

    private void checkRange(String name, int value) {
        if (value < 1 || value > 3) {
            throw new InvalidInputException(name + " must be between 1 and 3, but was " + value + ".");
        }
    }
}
