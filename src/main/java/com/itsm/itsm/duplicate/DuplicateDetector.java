package com.itsm.itsm.duplicate;

import com.itsm.itsm.model.ITService;
import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.IncidentStatus;
import com.itsm.itsm.repository.IncidentRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

// Intelligent feature: duplicate detection. A new report is compared with every
// open incident on the same IT service, using Jaccard similarity of their words:
//
//     similarity = (words both texts share) / (all different words in the two texts)
//
// e.g. {library, wifi, down} vs {library, wifi, connect} -> 2 shared / 4 total = 0.5
@Component
public class DuplicateDetector {

    // 0.4 = at least 40% of the words overlap. Chosen by trying typical short reports.
    public static final double THRESHOLD = 0.4;

    // Common words that say nothing about the problem itself
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "not", "but", "are", "was", "has", "have", "with", "from", "this",
            "that", "can", "cannot", "cant", "does", "doesnt", "dont", "any", "all", "our", "you",
            "its", "too", "very", "now", "since", "when", "what", "there", "again", "working",
            "please", "help", "issue", "problem", "today");

    private final IncidentRepository incidentRepository;

    public DuplicateDetector(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    // The most similar open incident on this service, if it is similar enough
    public Optional<DuplicateMatch> findDuplicate(ITService service, String title, String description) {
        Set<String> newWords = words(title + " " + description);
        DuplicateMatch best = null;
        for (Incident existing : incidentRepository.findByServiceAndStatusIn(service, IncidentStatus.OPEN)) {
            double similarity = jaccard(newWords, words(existing.getTitle() + " " + existing.getDescription()));
            if (similarity >= THRESHOLD && (best == null || similarity > best.similarity())) {
                best = new DuplicateMatch(existing, similarity);
            }
        }
        return Optional.ofNullable(best);
    }

    // "Wi-Fi is DOWN in the library!" -> {wifi, down, library}
    static Set<String> words(String text) {
        Set<String> result = new HashSet<>();
        String cleaned = text.toLowerCase().replace("-", "").replace("'", "");
        for (String word : cleaned.split("[^a-z0-9]+")) {
            if (word.length() >= 3 && !STOP_WORDS.contains(word)) {
                result.add(word);
            }
        }
        return result;
    }

    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        Set<String> shared = new HashSet<>(a);
        shared.retainAll(b);   // keep only words that are also in b
        Set<String> all = new HashSet<>(a);
        all.addAll(b);         // every word from either text, once
        return (double) shared.size() / all.size();
    }
}
