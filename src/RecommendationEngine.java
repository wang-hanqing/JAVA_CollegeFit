import java.util.*;

/**
 * Runs every lens on every college and builds the starting options without an overall score:
 * colleges are grouped by type (two-year, four-year public, four-year private nonprofit),
 * and each group is ordered by a rule the student can see and change.
 */
public class RecommendationEngine {
    // Polymorphism: the engine only knows the LensEvaluator interface.
    private final List<LensEvaluator> lenses = List.of(
        new AcademicEvaluator(), new FinancialEvaluator(), new SocialEvaluator());

    /**
     * Priority-based ordering inside each college group. No weights and no overall score:
     * program evidence first, then the student's #1 priority, then #2 and #3, then name.
     * Values that are close count as a tie (so the next priority decides), and missing or
     * not-comparable values go after comparable ones instead of getting a false advantage.
     * Without priorities: campus matches, then more recent graduates in her fields.
     */
    public static Comparator<CollegeRecommendation> comparator(StudentProfile s) {
        Comparator<CollegeRecommendation> c = Comparator.comparingInt(r -> r.programPriority);
        List<StudentProfile.Priority> order = s.hasPriorities() ? s.getPriorities()
            : List.of(StudentProfile.Priority.CAMPUS, StudentProfile.Priority.STUDY);
        for (StudentProfile.Priority p : order) c = c.thenComparing(r -> sortKey(p, s, r), Comparator.nullsLast(Comparator.naturalOrder()));
        // Historical degree activity breaks remaining ties only when "My area of study" is used; it is size, not quality.
        if (order.contains(StudentProfile.Priority.STUDY)) c = c.thenComparing(r -> r.recentDegrees, Comparator.reverseOrder());
        return c.thenComparing(r -> r.college.name, String.CASE_INSENSITIVE_ORDER);
    }

    /** Smaller key = earlier. Close values share a key so the next priority can decide. */
    static Integer sortKey(StudentProfile.Priority p, StudentProfile s, CollegeRecommendation r) {
        College c = r.college;
        switch (p) {
            case COSTS: {
                Integer cost = comparableCost(s, c);
                return cost == null ? null : Math.floorDiv(cost, COST_STEP);          // $1,000 steps
            }
            case GRADUATION: {
                Double rate = comparableCompletion(c);
                return rate == null ? null : -(int) Math.floor(rate * 100 / RATE_STEP); // 5-point steps, higher first
            }
            case ADMISSION: {
                double a = admissionAccess(c);
                return a < 0 ? null : -(int) Math.floor(a * 100 / ADMIT_STEP);         // open admission first, then 10-point steps
            }
            case STUDY:
                return -studyRelevance(s, r);                                          // relevance, not program size
            default:
                return -r.social.matched;                                              // more campus matches first
        }
    }

    static final int COST_STEP = 1000, RATE_STEP = 5, ADMIT_STEP = 10;

    /**
     * How related a college is to her study plans: more of her chosen programs offered first,
     * then whether the degree levels fit her pathway (with "Not sure", colleges offering both levels come first).
     */
    static int studyRelevance(StudentProfile s, CollegeRecommendation r) {
        if (!s.hasPrograms()) return 0;
        int levelFit;
        switch (s.getPathway()) {
            case ASSOCIATE: levelFit = r.associateAwarded ? 1 : 0; break;
            case BACHELOR: levelFit = r.bachelorAwarded ? 1 : 0; break;
            default: levelFit = r.associateAwarded && r.bachelorAwarded ? 1 : 0;
        }
        return r.programsAwarded * 2 + levelFit;
    }

    /**
     * Average cost after grants that can be compared across colleges: her income range if given, otherwise all incomes.
     * Public colleges outside her home state are not comparable (their averages use in-state rates), so null.
     */
    static Integer comparableCost(StudentProfile s, College c) {
        if (c.isPublic() && s.hasHomeState() && !s.getHomeState().equals(c.state)) return null;
        return s.getIncomeBracket().isKnown() ? c.netPrice(s.getIncomeBracket()) : c.netPriceAll;
    }

    /** Completion rate for ordering. Private two-year rates use a different measure than the four-year colleges they are grouped with. */
    static Double comparableCompletion(College c) {
        if (!c.isPublic() && c.isTwoYear()) return null;
        return c.completionRate;
    }

    /** Institution-level access only: open admission first, then higher admission rate; unknown last. */
    static double admissionAccess(College c) {
        if (Boolean.TRUE.equals(c.openAdmission)) return 2.0;
        if (c.admissionRate != null && c.admissionRate > 0) return c.admissionRate;
        return -1.0;
    }

    /** Scope: degree pathway, chosen states (if any) and program evidence (if programs are chosen). */
    public List<CollegeRecommendation> recommend(StudentProfile s, List<College> colleges) {
        List<CollegeRecommendation> result = new ArrayList<>();
        for (College c : colleges) {
            if (!offersPathway(s.getPathway(), c)) continue;
            if (s.getLocationScope() == StudentProfile.LocationScope.PREFERRED_STATE && !s.prefersState(c.state)) continue;
            CollegeRecommendation r = evaluate(s, c);
            if (s.hasPrograms() && !r.hasProgramEvidence()) continue;
            result.add(r);
        }
        result.sort(comparator(s));
        return result;
    }

    /** Evaluates the given colleges in their current order, without filtering or sorting (used by Shortlist). */
    public List<CollegeRecommendation> recommendAll(StudentProfile s, List<College> colleges) {
        List<CollegeRecommendation> result = new ArrayList<>();
        for (College c : colleges) result.add(evaluate(s, c));
        return result;
    }

    public CollegeRecommendation evaluate(StudentProfile s, College c) {
        EvaluationResult[] r = new EvaluationResult[lenses.size()];
        for (int i = 0; i < lenses.size(); i++) r[i] = lenses.get(i).evaluate(s, c);
        return new CollegeRecommendation(s, c, r[0], r[1], r[2]);
    }

    /** Splits ranked results into the three college types, keeping the order inside each group. */
    public static Map<College.Type, List<CollegeRecommendation>> groups(List<CollegeRecommendation> ranked, StudentProfile.Pathway p) {
        Map<College.Type, List<CollegeRecommendation>> out = new EnumMap<>(College.Type.class);
        for (College.Type t : College.Type.values()) {
            out.put(t, new ArrayList<>());
        }
        for (CollegeRecommendation r : ranked) out.get(r.college.type()).add(r);
        return out;
    }

    /** Labeled reference cost for the cost order: net price where it applies, otherwise the published cost. */
    static Integer referenceCost(StudentProfile s, College c) {
        if (c.isPublic() && s.hasHomeState() && !s.getHomeState().equals(c.state)) return c.outOfStateCostEstimate();
        Integer net = s.getIncomeBracket().isKnown() ? c.netPrice(s.getIncomeBracket()) : c.netPriceAll;
        return net != null ? net : c.costOfAttendance;
    }

    /** Credential levels to check: 2 = associate, 3 = bachelor's. */
    static int[] levels(StudentProfile.Pathway p) {
        switch (p) {
            case ASSOCIATE: return new int[]{2};
            case BACHELOR: return new int[]{3};
            default: return new int[]{2, 3};
        }
    }

    /** Keeps a college unless the data shows it awards only the other degree level. */
    static boolean offersPathway(StudentProfile.Pathway p, College c) {
        for (int level : levels(p)) if (c.offersLevel(level)) return true;
        return !(c.offersLevel(2) || c.offersLevel(3));
    }

    // ---------- facts for the guided "not sure" page ----------

    /** For the pathway explainer: two-year, four-year public and four-year private colleges. */
    public enum PathwayKind { TWO_YEAR, FOUR_YEAR_PUBLIC, FOUR_YEAR_PRIVATE }

    /** Number of colleges and median published cost per pathway kind, in the given states (all if empty). */
    public static Map<PathwayKind, int[]> pathwaySummary(List<College> colleges, Set<String> states) {
        Map<PathwayKind, List<Integer>> costs = new EnumMap<>(PathwayKind.class);
        Map<PathwayKind, int[]> out = new EnumMap<>(PathwayKind.class);
        for (PathwayKind k : PathwayKind.values()) { costs.put(k, new ArrayList<>()); out.put(k, new int[]{0, 0}); }
        for (College c : colleges) {
            if (!states.isEmpty() && !states.contains(c.state)) continue;
            PathwayKind k = c.isTwoYear() ? PathwayKind.TWO_YEAR : c.isPublic() ? PathwayKind.FOUR_YEAR_PUBLIC : PathwayKind.FOUR_YEAR_PRIVATE;
            out.get(k)[0]++;
            if (c.costOfAttendance != null) costs.get(k).add(c.costOfAttendance);
        }
        for (PathwayKind k : PathwayKind.values()) out.get(k)[1] = median(costs.get(k));
        return out;
    }

    /** Colleges per program code and per 2-digit field, in the given states (all if empty). */
    public static Map<String, Integer> programCounts(List<College> colleges, Set<String> states) {
        Map<String, Integer> out = new HashMap<>();
        for (College c : colleges) {
            if (!states.isEmpty() && !states.contains(c.state)) continue;
            Set<String> fields = new HashSet<>();
            for (String cip : c.awardedPrograms()) { out.merge(cip, 1, Integer::sum); fields.add(cip.substring(0, 2)); }
            for (String f : fields) out.merge(f, 1, Integer::sum);
        }
        return out;
    }

    private static int median(List<Integer> v) {
        if (v.isEmpty()) return 0;
        Collections.sort(v);
        int n = v.size();
        return n % 2 == 1 ? v.get(n / 2) : (v.get(n / 2 - 1) + v.get(n / 2)) / 2;
    }

    /** Colleges in the given states (all if empty) with recent awards in a 2-digit field. */
    public static int fieldCount(List<College> colleges, Set<String> states, String family) {
        int n = 0;
        for (College c : colleges) {
            if (!states.isEmpty() && !states.contains(c.state)) continue;
            if (c.evidence(family, 2) == College.ProgramEvidence.AWARDED || c.evidence(family, 3) == College.ProgramEvidence.AWARDED) n++;
        }
        return n;
    }
}
