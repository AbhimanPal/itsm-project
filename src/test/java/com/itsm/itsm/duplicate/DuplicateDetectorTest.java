package com.itsm.itsm.duplicate;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Unit test for the word comparison only (no database)
class DuplicateDetectorTest {

    @Test
    void wordsAreLowercasedAndShortOrCommonWordsAreDropped() {
        assertEquals(Set.of("wifi", "down", "library"), DuplicateDetector.words("The Wi-Fi is DOWN in the library!"));
    }

    @Test
    void jaccardIsSharedWordsDividedByAllWords() {
        double similarity = DuplicateDetector.jaccard(Set.of("library", "wifi", "down"),
                Set.of("library", "wifi", "connect"));
        assertEquals(0.5, similarity, 0.0001); // 2 shared out of 4 different words
    }

    @Test
    void sameTextIsFullySimilarAndUnrelatedTextIsNot() {
        Set<String> a = DuplicateDetector.words("Printer on second floor is jammed");
        assertEquals(1.0, DuplicateDetector.jaccard(a, a), 0.0001);
        assertTrue(DuplicateDetector.jaccard(a, DuplicateDetector.words("Payment page shows error 500"))
                < DuplicateDetector.THRESHOLD);
    }

    @Test
    void emptyTextIsNeverADuplicate() {
        assertEquals(0.0, DuplicateDetector.jaccard(Set.of(), Set.of("wifi")), 0.0001);
    }
}
