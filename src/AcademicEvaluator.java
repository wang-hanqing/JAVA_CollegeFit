import java.util.ArrayList;
import java.util.List;

/**
 * Academic lens. Describes how a college admits students in three parts: admission policy,
 * SAT comparison by section, and program admission. It never predicts this student's admission,
 * so it never returns MATCH: school statistics are context (INFO) or something to verify.
 */
public class AcademicEvaluator implements LensEvaluator {
    static final double HIGHLY_SELECTIVE = 0.15; // admits under 15%

    private enum Band { BELOW, WITHIN, ABOVE }

    @Override
    public EvaluationResult evaluate(StudentProfile s, College c) {
        List<Fact> facts = new ArrayList<>();

        // 1. Admission policy. Open admission is checked first: those colleges have no admission rate.
        Double rate = c.admissionRate;
        boolean open = Boolean.TRUE.equals(c.openAdmission);
        if (open) {
            facts.add(new Fact(Messages.get("ac.policy"), Messages.get("ac.open"), Messages.get("ac.open.note"), null));
        } else if (rate != null && rate <= 0) {
            rate = null; // a 0% rate is not believable for a college with students
            facts.add(new Fact(Messages.get("ac.policy"), Messages.get("ac.rate.unusable"), Messages.get("ac.rate.unusable.note"), null));
        } else if (rate != null) {
            facts.add(new Fact(Messages.get("ac.policy"), Messages.get("ac.rate", pct(rate)), Messages.get("ac.rate.note"), null));
        } else {
            facts.add(new Fact(Messages.get("ac.policy"), Messages.get("notReported"), "", null));
        }
        facts.add(new Fact(Messages.get("ac.test"), testPolicy(c.testPolicy), Messages.get("ac.test.note"), null));

        // 2. SAT comparison, one section at a time. Section percentiles are never added together.
        Band reading = null, math = null;
        boolean hasScores = s.getSatStatus() == StudentProfile.SatStatus.PROVIDED;
        if (!hasScores) {
            facts.add(new Fact(Messages.get("ac.sat"), Messages.get("ac.sat.notAssessed"), Messages.get("ac.sat.notAssessed.note"), null));
        } else if (c.reading25 == null || c.reading75 == null || c.math25 == null || c.math75 == null) {
            facts.add(new Fact(Messages.get("ac.sat"), Messages.get("ac.sat.noRanges"), Messages.get("ac.sat.noRanges.note"), null));
        } else {
            reading = band(s.getSatReadingWriting(), c.reading25, c.reading75);
            math = band(s.getSatMath(), c.math25, c.math75);
            facts.add(new Fact(Messages.get("ac.sat.reading"), Messages.get("ac.band." + reading, s.getSatReadingWriting(),
                c.reading25 + "-" + c.reading75), Messages.get("ac.sat.range.note"), null));
            facts.add(new Fact(Messages.get("ac.sat.math"), Messages.get("ac.band." + math, s.getSatMath(),
                c.math25 + "-" + c.math75), Messages.get("ac.sat.range.note"), null));
        }

        // 3. Program admission cannot be confirmed from this data.
        facts.add(new Fact(Messages.get("ac.program"), Messages.get("ac.program.value"), Messages.get("ac.program.note"), null));

        String next = Messages.get("ac.next");
        boolean selective = rate != null && rate < HIGHLY_SELECTIVE;
        if (selective)
            return new EvaluationResult(FitStatus.NEEDS_VERIFICATION, Messages.get("ac.label.selective", pct(rate)), facts, "", next);
        if (reading != null) {
            String overall = reading == math ? reading.name() : "MIXED";
            FitStatus status = overall.equals("BELOW") || overall.equals("MIXED") ? FitStatus.NEEDS_VERIFICATION : FitStatus.INFO;
            String reason = overall.equals("WITHIN") || overall.equals("ABOVE") ? Messages.get("ac.reason.sat") : "";
            return new EvaluationResult(status, Messages.get("ac.label.sat." + overall), facts, reason, next);
        }
        if (open)
            return new EvaluationResult(FitStatus.INFO, Messages.get("ac.open"), facts, Messages.get("ac.reason.open"), "");
        if (rate != null)
            return new EvaluationResult(FitStatus.INFO, Messages.get("ac.label.rate", pct(rate)), facts,
                rate >= 0.5 ? Messages.get("ac.reason.rate", pct(rate)) : "", next);
        return new EvaluationResult(FitStatus.UNKNOWN, Messages.get("ac.label.unknown"), facts, "", next);
    }

    private static Band band(int score, int p25, int p75) {
        return score < p25 ? Band.BELOW : score > p75 ? Band.ABOVE : Band.WITHIN;
    }

    private static String pct(double rate) { return Math.round(rate * 100) + "%"; }

    /** ADMCON7 from IPEDS, fall 2024. A missing SAT range does NOT mean test-optional. */
    private static String testPolicy(Integer code) {
        if (code == null || (code != 1 && code != 2 && code != 3 && code != 5)) return Messages.get("notReported");
        return Messages.get("ac.test." + code);
    }
}
