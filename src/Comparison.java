import java.util.*;

/**
 * Compare and decide: which differences are large enough to mention, on the same measure,
 * and a short trade-off summary in the order of the student's priorities.
 * It never calls one college "better", and never compares numbers measured differently.
 */
public class Comparison {
    /** Rows of the comparison table. */
    public enum Row { DEGREE, COSTS, GRADUATION, ADMISSION, STUDY, CAMPUS }

    static final int COST_GAP = 2000;     // dollars
    static final int RATE_GAP = 5;        // completion, percentage points
    static final int ADMIT_GAP = 10;      // admission rate, percentage points
    static final double STUDY_RATIO = 2;  // recent graduates, same degree level

    private final StudentProfile s;
    private final List<CollegeRecommendation> recs;
    private final Map<CollegeRecommendation, Map<Row, String>> markers = new HashMap<>();
    private final List<String> summary = new ArrayList<>();

    public Comparison(StudentProfile s, List<CollegeRecommendation> recs) {
        this.s = s;
        this.recs = recs;
        for (CollegeRecommendation r : recs) markers.put(r, new EnumMap<>(Row.class));
        if (recs.size() < 2) return;
        Map<Row, String> sentences = new EnumMap<>(Row.class);
        sentences.put(Row.DEGREE, degree());
        sentences.put(Row.COSTS, costs());
        sentences.put(Row.GRADUATION, graduation());
        sentences.put(Row.ADMISSION, admission());
        sentences.put(Row.STUDY, study());
        sentences.put(Row.CAMPUS, campus());
        for (Row row : rowOrder(s)) if (sentences.get(row) != null) summary.add(sentences.get(row));
        summary.add(Messages.get("cmp.closing"));
    }

    /** Degree level first (it changes the path), then her priorities, then the remaining rows. */
    public static List<Row> rowOrder(StudentProfile s) {
        List<Row> out = new ArrayList<>(List.of(Row.DEGREE));
        for (StudentProfile.Priority p : s.getPriorities()) out.add(Row.valueOf(p == StudentProfile.Priority.COSTS ? "COSTS" : p.name()));
        for (Row r : Row.values()) if (!out.contains(r)) out.add(r);
        return out;
    }

    public List<String> summary() { return summary; }
    public String marker(CollegeRecommendation r, Row row) { return markers.get(r).get(row); }
    public boolean differentDegrees() { return degreeSets().size() > 1; }

    // ---------- each row ----------

    /** "associate", "bachelor's" or "associate and bachelor's" for her programs (or for the college if none chosen). */
    public static String degreeText(StudentProfile s, CollegeRecommendation r) {
        boolean a = s.hasPrograms() ? r.associateAwarded : r.college.offersLevel(2);
        boolean b = s.hasPrograms() ? r.bachelorAwarded : r.college.offersLevel(3);
        if (a && b) return Messages.get("level.both");
        if (b) return Messages.get("level.bachelor");
        if (a) return Messages.get("level.associate");
        return Messages.get("notReported");
    }

    private Map<String, List<String>> degreeSets() {
        Map<String, List<String>> by = new LinkedHashMap<>();
        for (CollegeRecommendation r : recs) by.computeIfAbsent(degreeText(s, r), k -> new ArrayList<>()).add(shortName(r));
        return by;
    }

    private String degree() {
        Map<String, List<String>> by = degreeSets();
        if (by.size() < 2) return null;
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : by.entrySet()) parts.add(Messages.get("cmp.degree.part", e.getKey(), names(e.getValue())));
        for (CollegeRecommendation r : recs) markers.get(r).put(Row.DEGREE, Messages.get("cmp.mark.differentPath"));
        return Messages.get("cmp.degree", String.join("; ", parts));
    }

    private String costs() {
        List<CollegeRecommendation> ok = new ArrayList<>();
        List<String> notComparable = new ArrayList<>();
        for (CollegeRecommendation r : recs) {
            if (RecommendationEngine.comparableCost(s, r.college) != null) ok.add(r);
            else { notComparable.add(shortName(r)); markers.get(r).put(Row.COSTS, Messages.get("cmp.mark.notComparable")); }
        }
        String extra = notComparable.isEmpty() ? "" : " " + Messages.get("cmp.cost.notComparable", names(notComparable));
        if (ok.size() < 2) return notComparable.isEmpty() ? null : Messages.get("cmp.cost.none") + extra;
        ok.sort(Comparator.comparing(r -> RecommendationEngine.comparableCost(s, r.college)));
        int low = RecommendationEngine.comparableCost(s, ok.get(0).college), high = RecommendationEngine.comparableCost(s, ok.get(ok.size() - 1).college);
        if (high - low < COST_GAP)
            return Messages.get("cmp.cost.similar", Messages.money(low), Messages.money(high)) + extra;
        markers.get(ok.get(0)).put(Row.COSTS, Messages.get("cmp.mark.lowerCost"));
        return Messages.get("cmp.cost.lower", shortName(ok.get(0)), shortName(ok.get(ok.size() - 1)), Messages.money(low), Messages.money(high)) + extra;
    }

    private String graduation() {
        Map<Integer, List<CollegeRecommendation>> byMeasure = new TreeMap<>(Comparator.reverseOrder()); // 6-year, then 3-year
        List<String> missing = new ArrayList<>();
        for (CollegeRecommendation r : recs) {
            if (r.college.completionRate == null) { missing.add(shortName(r)); continue; }
            byMeasure.computeIfAbsent(r.college.completionYears, k -> new ArrayList<>()).add(r);
        }
        List<String> sentences = new ArrayList<>();
        boolean mixed = byMeasure.size() > 1;
        for (Map.Entry<Integer, List<CollegeRecommendation>> e : byMeasure.entrySet()) {
            List<CollegeRecommendation> g = e.getValue();
            if (mixed) for (CollegeRecommendation r : g) markers.get(r).put(Row.GRADUATION, Messages.get("cmp.mark.measure." + e.getKey()));
            if (g.size() >= 2) {
                g.sort(Comparator.comparing((CollegeRecommendation r) -> r.college.completionRate).reversed());
                int hi = pct(g.get(0).college.completionRate), lo = pct(g.get(g.size() - 1).college.completionRate);
                if (hi - lo >= RATE_GAP) {
                    markers.get(g.get(0)).put(Row.GRADUATION, Messages.get("cmp.mark.higherRate"));
                    sentences.add(Messages.get("cmp.grad.higher." + e.getKey(), shortName(g.get(0)), shortName(g.get(g.size() - 1)), hi + "%", lo + "%"));
                } else {
                    sentences.add(Messages.get("cmp.grad.similar." + e.getKey(), names(namesOf(g))));
                }
            } else if (mixed) {
                sentences.add(Messages.get("cmp.grad.single." + e.getKey(), shortName(g.get(0)), pct(g.get(0).college.completionRate) + "%"));
            }
        }
        if (!missing.isEmpty()) sentences.add(Messages.get("cmp.grad.missing", names(missing)));
        if (sentences.isEmpty()) return null;
        return String.join(" ", sentences) + " " + Messages.get("cmp.grad.note");
    }

    private String admission() {
        List<String> open = new ArrayList<>();
        List<CollegeRecommendation> rated = new ArrayList<>();
        for (CollegeRecommendation r : recs) {
            if (Boolean.TRUE.equals(r.college.openAdmission)) { open.add(shortName(r)); markers.get(r).put(Row.ADMISSION, Messages.get("ac.open")); }
            else if (r.college.admissionRate != null && r.college.admissionRate > 0) rated.add(r);
        }
        List<String> sentences = new ArrayList<>();
        if (!open.isEmpty() && open.size() < recs.size()) sentences.add(Messages.get("cmp.adm.open", names(open)));
        if (rated.size() >= 2) {
            rated.sort(Comparator.comparing((CollegeRecommendation r) -> r.college.admissionRate).reversed());
            int hi = pct(rated.get(0).college.admissionRate), lo = pct(rated.get(rated.size() - 1).college.admissionRate);
            if (hi - lo >= ADMIT_GAP) {
                markers.get(rated.get(0)).put(Row.ADMISSION, Messages.get("cmp.mark.higherAdmit"));
                sentences.add(Messages.get("cmp.adm.higher", shortName(rated.get(0)), shortName(rated.get(rated.size() - 1)), hi + "%", lo + "%"));
            }
        }
        return sentences.isEmpty() ? null : String.join(" ", sentences);
    }

    private String study() {
        if (!s.hasPrograms()) return null;
        Map<String, List<CollegeRecommendation>> byLevel = new LinkedHashMap<>();
        for (CollegeRecommendation r : recs) byLevel.computeIfAbsent(degreeText(s, r), k -> new ArrayList<>()).add(r);
        for (List<CollegeRecommendation> g : byLevel.values()) {
            if (g.size() < 2) continue;
            g.sort(Comparator.comparingInt((CollegeRecommendation r) -> r.recentDegrees).reversed());
            int hi = g.get(0).recentDegrees, lo = g.get(g.size() - 1).recentDegrees;
            if (lo > 0 && hi >= lo * STUDY_RATIO) {
                markers.get(g.get(0)).put(Row.STUDY, Messages.get("cmp.mark.moreGraduates"));
                return Messages.get("cmp.study.more", shortName(g.get(0)), shortName(g.get(g.size() - 1)), Messages.number(hi), Messages.number(lo));
            }
        }
        return null;
    }

    private String campus() {
        List<String> match = new ArrayList<>(), not = new ArrayList<>();
        for (CollegeRecommendation r : recs) {
            if (r.social.considered == 0) return null;
            if (r.social.matched == r.social.considered) match.add(shortName(r)); else not.add(shortName(r));
        }
        if (match.isEmpty() || not.isEmpty()) return null;
        return Messages.get("cmp.campus", names(match));
    }

    // ---------- helpers ----------
    private static int pct(double v) { return (int) Math.round(v * 100); }

    /** "CUNY Hunter College" -> "Hunter"-style short names keep sentences readable. */
    static String shortName(CollegeRecommendation r) {
        String n = r.college.name;
        if (n.startsWith("CUNY ")) n = n.substring(5);
        if (n.startsWith("SUNY ")) n = n.substring(5);
        return n.replace(" Community College", " CC").replace(" College", "").trim();
    }

    private static List<String> namesOf(List<CollegeRecommendation> g) {
        List<String> out = new ArrayList<>();
        for (CollegeRecommendation r : g) out.add(shortName(r));
        return out;
    }

    private static String names(List<String> list) {
        if (list.size() == 1) return list.get(0);
        return String.join(", ", list.subList(0, list.size() - 1)) + " " + Messages.get("and") + " " + list.get(list.size() - 1);
    }
}
