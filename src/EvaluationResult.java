import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What one lens says about one college: a status, a short label for the card,
 * itemized facts for the details page, a "why explore" reason and a next step.
 */
public class EvaluationResult {
    public final FitStatus status;
    public final String label;      // short card text, e.g. "Open admission"
    public final List<Fact> facts;  // itemized details, e.g. "Test policy: considered, not required"
    public final String reason;     // a positive reason to explore; empty if none
    public final String nextStep;   // what to verify; empty if nothing
    public final int matched, considered; // social lens only

    public EvaluationResult(FitStatus status, String label, List<Fact> facts, String reason, String nextStep) {
        this(status, label, facts, reason, nextStep, 0, 0);
    }

    public EvaluationResult(FitStatus status, String label, List<Fact> facts, String reason, String nextStep,
                            int matched, int considered) {
        this.status = status;
        this.label = label;
        this.facts = Collections.unmodifiableList(new ArrayList<>(facts));
        this.reason = reason == null ? "" : reason;
        this.nextStep = nextStep == null ? "" : nextStep;
        this.matched = matched;
        this.considered = considered;
    }

    /** All facts joined into one sentence, used in tests and the CSV export. */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        for (Fact f : facts) sb.append(f.label).append(": ").append(f.value)
            .append(f.note.isEmpty() ? "" : " (" + f.note + ")").append(". ");
        return sb.toString().trim();
    }
}
