package net.diveon.backend.domain.grade.service;

import com.fasterxml.jackson.databind.JsonNode;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemRevision;
import net.diveon.backend.domain.problem.repository.ProblemCodingRepository;
import net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository;
import org.springframework.stereotype.Component;

/** Board metadata only; explanation contents remain behind the disclosure endpoints. */
@Component
public class SubmissionOboPresence {
    private final ProblemCodingRepository coding;
    private final ProblemObjectiveRepository objective;

    public SubmissionOboPresence(ProblemCodingRepository coding, ProblemObjectiveRepository objective) {
        this.coding = coding;
        this.objective = objective;
    }

    public boolean hasObo(Problem problem, ProblemRevision revision) {
        if (!"coding".equals(problem.getType()) && !"objective".equals(problem.getType())) return false;
        // A present revision with no OBO must never inherit a later problem edit.
        if (revision != null) return containsDocument(revision.getSnapshot().get("obo"));
        // Legacy submissions have no immutable snapshot, matching the explanation endpoint's fallback.
        JsonNode document = "coding".equals(problem.getType())
                ? coding.findById(problem.getId()).map(p -> p.getOboJsonData()).orElse(null)
                : objective.findById(problem.getId()).map(p -> p.getOboJsonData()).orElse(null);
        return containsDocument(document);
    }

    private static boolean containsDocument(JsonNode document) {
        return document != null && document.isObject() && !document.isEmpty();
    }
}
