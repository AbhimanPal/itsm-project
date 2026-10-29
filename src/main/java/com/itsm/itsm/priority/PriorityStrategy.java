package com.itsm.itsm.priority;

import com.itsm.itsm.model.Priority;

// Strategy pattern: any class that implements this interface can be plugged in
// as the way we calculate priority, without changing the code that uses it.
public interface PriorityStrategy {

    Priority calculate(int impact, int urgency, int criticality, int usersAffected);
}
