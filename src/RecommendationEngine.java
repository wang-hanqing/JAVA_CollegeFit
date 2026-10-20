import java.util.*;

/**
 * Runs every lens on every college and ranks the results without an overall score.
 * The student can choose how results are ordered (SortOrder).
 */
public class RecommendationEngine {
    // Polymorphism: the engine only knows the LensEvaluator interface.
    private final List<LensEvaluator> lenses = List.of(
        new AcademicEvaluator(), new FinancialEvaluator(), new SocialEvaluator());

    public enum SortOrder { BEST_FIT, EASIER_ADMISSION, LOWEST_COST, NAME;
        @Override public String toString() { return Messages.get("SortOrder." + name()); } }

    /**
     * BEST_FIT (default, neutral): program evidence, most of her programs covered, fewest mismatches, most social preferences matched,
     * preferred state first, then name. It never ranks by admission difficulty or price.
     * The student can choose EASIER_ADMISSION or LOWEST_COST herself; both still put program evidence first.
     */
    public static Comparator<CollegeRecommendation> comparator(SortOrder order, StudentProfile s) {
        Comparator<CollegeRecommendation> byName = Comparator.comparing(r -> r.college.name, String.CASE_INSENSITIVE_ORDER);
        Comparator<CollegeRecommendation> byProgram = Comparator.comparingInt(r -> r.programPriority);
        switch (order) {
            case NAME:
                return byName;
            case LOWEST_COST:
                return byProgram
                    .thenComparing((CollegeRecommendation r) -> referenceCost(s, r.college),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(byName);
            case EASIER_ADMISSION:
                return byProgram
                    .thenComparing((CollegeRecommendation r) -> admissionAccess(r.college), Comparator.reverseOrder())
                    .thenComparing(byName);
            default:
                return byProgram
                    .thenComparing(r -> r.programsAwarded, Comparator.reverseOrder())
                    .thenComparingInt(CollegeRecommendation::mismatches)
                    .thenComparing(r -> r.social.matched, Comparator.reverseOrder())
                    .thenComparingInt(r -> s.prefersState(r.college.state) ? 0 : 1)
                    .thenComparing(byName);
        }
    }

    /** Institution-level access only: open admission first, then higher admission rate; unknown last. */
    static double admissionAccess(College c) {
        if (Boolean.TRUE.equals(c.openAdmission)) return 2.0;
        if (c.admissionRate != null && c.admissionRate > 0) return c.admissionRate;
        return -1.0;
    }

    public List<CollegeRecommendation> recommend(StudentProfile s, List<College> colleges) {
        List<CollegeRecommendation> result = new ArrayList<>();
        for (College c : colleges) {
            if (!offersPathway(s.getPathway(), c)) continue;
            result.add(evaluate(s, c));
        }
        result.sort(comparator(SortOrder.BEST_FIT, s));
        return result;
    }

    /** Evaluates the given colleges in their current order, without filtering or sorting (used by Shortlist). */
    public List<CollegeRecommendation> recommendAll(StudentProfile s, List<College> colleges) {
        List<CollegeRecommendation> result = new ArrayList<>();
        for (College c : colleges) result.add(evaluate(s, c));
        return result;
    }

    private CollegeRecommendation evaluate(StudentProfile s, College c) {
        EvaluationResult[] r = new EvaluationResult[lenses.size()];
        for (int i = 0; i < lenses.size(); i++) r[i] = lenses.get(i).evaluate(s, c);
        return new CollegeRecommendation(s, c, r[0], r[1], r[2]);
    }

    /**
     * Discovery list for students who chose specific states: colleges elsewhere with program evidence,
     * lowest labeled reference cost first. Supports the "beyond the colleges she knows" idea.
     */
    public List<CollegeRecommendation> worthALookElsewhere(StudentProfile s, List<CollegeRecommendation> ranked, int limit) {
        if (s.getLocationScope() != StudentProfile.LocationScope.PREFERRED_STATE) return List.of();
        List<CollegeRecommendation> out = new ArrayList<>();
        for (CollegeRecommendation r : ranked)
            if (!s.prefersState(r.college.state) && r.hasProgramEvidence()) out.add(r);
        out.sort(Comparator.comparing((CollegeRecommendation r) -> referenceCost(s, r.college),
                Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(r -> r.college.name, String.CASE_INSENSITIVE_ORDER));
        return out.subList(0, Math.min(limit, out.size()));
    }

    /**
     * Labeled reference cost used only for the optional cost sort and the discovery list:
     * net price for her income range (or all aid recipients) where it applies, otherwise the published cost.
     */
    static Integer referenceCost(StudentProfile s, College c) {
        if (c.isPublic() && s.hasHomeState() && !s.getHomeState().equals(c.state)) return c.outOfStateCostEstimate();
        Integer net = s.getIncomeBracket().isKnown() ? c.netPrice(s.getIncomeBracket()) : c.netPriceAll;
        return net != null ? net : c.costOfAttendance; // negative averages stay as reported
    }

    /** Credential levels to check: 2 = associate, 3 = bachelor's. */
    static int[] levels(StudentProfile.Pathway p) {
        switch (p) {
            case ASSOCIATE: return new int[]{2};
            case BACHELOR: return new int[]{3};
            default: return new int[]{2, 3};
        }
    }

    /**
     * Keeps a college unless the data shows it awards only the other degree level.
     * Colleges with no award records at either level stay in, flagged by the program check.
     */
    static boolean offersPathway(StudentProfile.Pathway p, College c) {
        for (int level : levels(p)) if (c.offersLevel(level)) return true;
        boolean awardsOtherLevel = c.offersLevel(2) || c.offersLevel(3);
        return !awardsOtherLevel;
    }
}
