/**
 * Academic lens (appendix A2). Describes how a college admits students;
 * it never predicts whether this student will be admitted.
 * Order matters: open admission is checked first, because open-admission colleges have no admission rate.
 */
public class AcademicEvaluator implements LensEvaluator {
    static final double HIGHLY_SELECTIVE = 0.15; // admits under 15%: always shown as highly selective

    @Override
    public EvaluationResult evaluate(StudentProfile s, College c) {
        if (Boolean.TRUE.equals(c.openAdmission)) {
            return new EvaluationResult(FitStatus.MATCH, Messages.get("academic.open.label"),
                Messages.get("academic.open.explain"), Messages.get("academic.open.next"));
        }

        // An admission rate of 0 is not believable for a college with students (data issue 5).
        Double rate = c.admissionRate;
        String rateNote = "";
        if (rate != null && rate <= 0) { rate = null; rateNote = Messages.get("academic.rate.suspicious") + " "; }

        String context = rateNote + rateText(rate) + " " + testPolicyText(c.testPolicy);
        boolean highlySelective = rate != null && rate < HIGHLY_SELECTIVE;

        boolean hasScores = s.getSatStatus() == StudentProfile.SatStatus.PROVIDED;
        boolean hasRanges = c.reading25 != null && c.reading75 != null && c.math25 != null && c.math75 != null;

        if (hasScores && hasRanges) {
            boolean below = s.getSatReadingWriting() < c.reading25 || s.getSatMath() < c.math25;
            if (below) {
                return new EvaluationResult(FitStatus.NEEDS_VERIFICATION, Messages.get("academic.below.label"),
                    Messages.get("academic.below.explain") + " " + context, Messages.get("academic.next"));
            }
            if (highlySelective) {
                return new EvaluationResult(FitStatus.NEEDS_VERIFICATION, Messages.get("academic.selective.label"),
                    Messages.get("academic.selectiveWithScores.explain") + " " + context, Messages.get("academic.next"));
            }
            return new EvaluationResult(FitStatus.MATCH, Messages.get("academic.within.label"),
                Messages.get("academic.within.explain") + " " + context, Messages.get("academic.next"));
        }

        if (highlySelective) {
            return new EvaluationResult(FitStatus.NEEDS_VERIFICATION, Messages.get("academic.selective.label"),
                Messages.get("academic.selective.explain") + " " + context, Messages.get("academic.next"));
        }
        return new EvaluationResult(FitStatus.UNKNOWN, Messages.get("academic.moreInfo.label"),
            Messages.get("academic.moreInfo.explain") + " " + context, Messages.get("academic.next"));
    }

    private static String rateText(Double rate) {
        if (rate == null) return Messages.get("academic.rate.none");
        return Messages.get("academic.rate", Math.round(rate * 100) + "%");
    }

    /** ADMCON7 from IPEDS, fall 2024. A missing SAT range does NOT mean test-optional. */
    private static String testPolicyText(Integer code) {
        if (code == null) return Messages.get("academic.policy.none");
        switch (code) {
            case 1: return Messages.get("academic.policy.1");
            case 2: return Messages.get("academic.policy.2");
            case 3: return Messages.get("academic.policy.3");
            case 5: return Messages.get("academic.policy.5");
            default: return Messages.get("academic.policy.none");
        }
    }
}
