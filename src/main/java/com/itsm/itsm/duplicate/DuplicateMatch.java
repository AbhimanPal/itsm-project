package com.itsm.itsm.duplicate;

import com.itsm.itsm.model.Incident;

// "This new report looks like that existing incident, this similar (0.0 - 1.0)"
public record DuplicateMatch(Incident original, double similarity) {

    public int percent() {
        return (int) Math.round(similarity * 100);
    }
}
