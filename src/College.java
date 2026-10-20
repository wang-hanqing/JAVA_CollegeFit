import java.util.*;

/**
 * One row of data/processed/colleges.csv plus its program records.
 * Values are historical school-level statistics, not personal predictions.
 * A null value always means "missing in the data" (NA or PS), never zero.
 */
public class College {
    /** What the program file says about one program at one credential level. */
    public enum ProgramEvidence { AWARDED, ZERO_AWARDS, NOT_CONFIRMED }

    public final String unitId, name, city, state, schoolUrl, netPriceCalculatorUrl;
    public final int control;                  // 1 = public, 2 = private nonprofit
    public final Boolean openAdmission;        // OPENADMP: true, false, or null if not reported
    public final Double admissionRate;         // ADM_RATE, 0.0 to 1.0
    public final Integer testPolicy;           // ADMCON7: 1 required, 2 recommended, 3 neither, 5 considered not required
    public final Integer reading25, reading75, math25, math75;
    public final Integer costOfAttendance;     // COSTT4_A; publics use in-state tuition
    public final Integer tuitionIn, tuitionOut;
    public final Integer netPriceAll;          // NPT4: all aid recipients; may be negative
    public final Map<StudentProfile.IncomeBracket, Integer> netPrices; // NPT41-45; may be negative
    public final StudentProfile.CampusSetting setting;
    public final Integer undergraduates;       // UGDS; may include online students
    public final Double latitude, longitude;   // kept for a future distance feature
    public final Double completionRate;        // C150_4 (4-year, within 6 years) or C150_L4 (2-year, within 3 years)
    public final int completionYears;          // 6 or 3; 0 if no rate
    private final Map<String, Integer[]> programCounts; // "CIP:level" -> {IPEDSCOUNT1, IPEDSCOUNT2}, null = missing

    public College(String unitId, String name, String city, String state, String schoolUrl, String npcUrl,
                   int control, Boolean openAdmission, Double admissionRate, Integer testPolicy,
                   Integer reading25, Integer reading75, Integer math25, Integer math75,
                   Integer costOfAttendance, Integer tuitionIn, Integer tuitionOut, Integer netPriceAll,
                   Map<StudentProfile.IncomeBracket, Integer> netPrices,
                   StudentProfile.CampusSetting setting, Integer undergraduates,
                   Double latitude, Double longitude, Double completionRate, int completionYears,
                   Map<String, Integer[]> programCounts) {
        this.unitId = unitId; this.name = name; this.city = city; this.state = state;
        this.schoolUrl = schoolUrl; this.netPriceCalculatorUrl = npcUrl;
        this.control = control; this.openAdmission = openAdmission;
        this.admissionRate = admissionRate; this.testPolicy = testPolicy;
        this.reading25 = reading25; this.reading75 = reading75; this.math25 = math25; this.math75 = math75;
        this.costOfAttendance = costOfAttendance; this.tuitionIn = tuitionIn; this.tuitionOut = tuitionOut;
        this.netPriceAll = netPriceAll;
        this.netPrices = Collections.unmodifiableMap(new EnumMap<>(netPrices));
        this.setting = setting; this.undergraduates = undergraduates;
        this.latitude = latitude; this.longitude = longitude;
        this.completionRate = completionRate; this.completionYears = completionYears;
        this.programCounts = Collections.unmodifiableMap(new HashMap<>(programCounts));
    }

    public boolean isPublic() { return control == 1; }

    /**
     * Positive count in either year = awarded; both reported as 0 = zero awards; anything else = not confirmed.
     * For a 2-digit family, the best evidence among all its programs at this level is used.
     */
    public ProgramEvidence evidence(String cipCode, int level) {
        if (cipCode.length() == 2) {
            ProgramEvidence best = ProgramEvidence.NOT_CONFIRMED;
            for (String key : programCounts.keySet()) {
                if (!key.startsWith(cipCode) || !key.endsWith(":" + level)) continue;
                ProgramEvidence e = evidence(key.substring(0, 4), level);
                if (e.ordinal() < best.ordinal()) best = e;
            }
            return best;
        }
        Integer[] c = programCounts.get(cipCode + ":" + level);
        if (c == null) return ProgramEvidence.NOT_CONFIRMED;
        boolean positive = (c[0] != null && c[0] > 0) || (c[1] != null && c[1] > 0);
        if (positive) return ProgramEvidence.AWARDED;
        if (c[0] != null && c[1] != null) return ProgramEvidence.ZERO_AWARDS;
        return ProgramEvidence.NOT_CONFIRMED;
    }

    /** Groups for starting options: public two-year (community) colleges, four-year public colleges, private nonprofit colleges. */
    public enum Type { COMMUNITY_COLLEGE, PUBLIC_FOUR_YEAR, PRIVATE }

    /** Two-year if every recent award is an associate degree. */
    public boolean isTwoYear() { return offersLevel(2) && !offersLevel(3); }

    /** Private two-year colleges (for example nursing schools) go to the private group, not "community colleges". */
    public Type type() {
        if (!isPublic()) return Type.PRIVATE;
        return isTwoYear() ? Type.COMMUNITY_COLLEGE : Type.PUBLIC_FOUR_YEAR;
    }

    /** 4-digit codes of programs with a positive award count at associate or bachelor's level. */
    public Set<String> awardedPrograms() {
        Set<String> out = new HashSet<>();
        for (Map.Entry<String, Integer[]> e : programCounts.entrySet()) {
            Integer[] c = e.getValue();
            if ((c[0] != null && c[0] > 0) || (c[1] != null && c[1] > 0)) out.add(e.getKey().substring(0, 4));
        }
        return out;
    }

    /**
     * Degrees awarded in 2022-23 for a program (4-digit) or a whole field (2-digit) at one level.
     * Falls back to 2021-22 if 2022-23 is missing. Returns 0 when there is no positive count.
     */
    public int recentDegrees(String cipCode, int level) {
        int total = 0;
        for (Map.Entry<String, Integer[]> e : programCounts.entrySet()) {
            String key = e.getKey();
            if (!key.endsWith(":" + level) || !key.startsWith(cipCode)) continue;
            Integer[] n = e.getValue();
            Integer v = n[1] != null ? n[1] : n[0];
            if (v != null && v > 0) total += v;
        }
        return total;
    }

    /** All degrees awarded at this level in 2022-23 (used when no program is chosen). */
    public int recentDegreesAll(int level) { return recentDegrees("", level); }

    /** Number of programs in a 2-digit family with a positive award count at this level. */
    public int familyProgramsAwarded(String twoDigit, int level) {
        int n = 0;
        for (String key : programCounts.keySet())
            if (key.startsWith(twoDigit) && key.endsWith(":" + level) && evidence(key.substring(0, 4), level) == ProgramEvidence.AWARDED) n++;
        return n;
    }

    /** {2021-22 count, 2022-23 count}, or null if there is no record. */
    public Integer[] awardCounts(String cipCode, int level) { return programCounts.get(cipCode + ":" + level); }

    /** True if any program at this level has a positive award count. */
    public boolean offersLevel(int level) {
        for (Map.Entry<String, Integer[]> e : programCounts.entrySet()) {
            if (!e.getKey().endsWith(":" + level)) continue;
            Integer[] c = e.getValue();
            if ((c[0] != null && c[0] > 0) || (c[1] != null && c[1] > 0)) return true;
        }
        return false;
    }

    /** Out-of-state cost estimate for publics: in-state cost of attendance plus the tuition difference. */
    public Integer outOfStateCostEstimate() {
        if (costOfAttendance == null || tuitionIn == null || tuitionOut == null) return null;
        return costOfAttendance - tuitionIn + tuitionOut;
    }

    public Integer netPrice(StudentProfile.IncomeBracket bracket) { return netPrices.get(bracket); }

    @Override public String toString() { return name + " (" + city + ", " + state + ")"; }
}
