/** What one lens says about one college: a status, a short label, a reason and a next step. */
public class EvaluationResult {
    public final FitStatus status;
    public final String label;        // short text for the result card, e.g. "Open admission"
    public final String explanation;  // plain-English reason for the details page
    public final String nextStep;     // empty if no action is needed
    public final int matched;         // used by the social lens only
    public final int considered;      // used by the social lens only

    public EvaluationResult(FitStatus status, String label, String explanation, String nextStep) {
        this(status, label, explanation, nextStep, 0, 0);
    }

    public EvaluationResult(FitStatus status, String label, String explanation, String nextStep,
                            int matched, int considered) {
        this.status = status;
        this.label = label;
        this.explanation = explanation;
        this.nextStep = nextStep == null ? "" : nextStep;
        this.matched = matched;
        this.considered = considered;
    }
}
