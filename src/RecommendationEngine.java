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

    /** How colleges are ordered inside each group. Program evidence always comes first. */
    public enum Order { RECENT_DEGREES, EASIER_ADMISSION, LOWEST_COST, NAME;
        @Override public String toString() { return Messages.get("Order." + name()); } }

    public static Comparator<CollegeRecommendation> comparator(Order order, StudentProfile s) {
        Comparator<CollegeRecommendation> byName = Comparator.comparing(r -> r.college.name, String.CASE_INSENSITIVE_ORDER);
        Comparator<CollegeRecommendation> base = Comparator.<CollegeRecommendation>comparingInt(r -> r.programPriority)
            .thenComparing(r -> r.social.matched, Comparator.reverseOrder()); // stated preferences shape the order
        switch (order) {
            case NAME:
                return byName;
            case LOWEST_COST:
                return base.thenComparing((CollegeRecommendation r) -> referenceCost(s, r.college),
                    Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(byName);
            case EASIER_ADMISSION:
                return base.thenComparing((CollegeRecommendation r) -> admissionAccess(r.college), Comparator.reverseOrder())
                    .thenComparing(byName);
            default: // RECENT_DEGREES: larger recent programs in her fields first
                return base.thenComparing(r -> r.recentDegrees, Comparator.reverseOrder()).thenComparing(byName);
        }
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
        result.sort(comparator(Order.RECENT_DEGREES, s));
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
