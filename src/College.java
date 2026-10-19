import java.util.*;

/**
 * One row of data/processed/colleges.csv plus its program awards.
 * Values are historical school-level statistics, not personal predictions.
 * A null value always means "missing in the data" (NA or PS), never zero.
 */
public class College {
    public final String unitId, name, city, state, schoolUrl, netPriceCalculatorUrl;
    public final int control;                  // 1 = public, 2 = private nonprofit
    public final Boolean openAdmission;        // OPENADMP: true, false, or null if not reported
    public final Double admissionRate;         // ADM_RATE, 0.0 to 1.0
    public final Integer testPolicy;           // ADMCON7: 1 required, 2 recommended, 3 neither, 5 considered not required
    public final Integer reading25, reading75, math25, math75;
    public final Integer costOfAttendance;     // COSTT4_A; publics use in-state tuition
    public final Integer tuitionIn, tuitionOut;
    public final Map<StudentProfile.IncomeBracket, Integer> netPrices; // may be negative (data issue 8)
    public final StudentProfile.CampusSetting setting;
    public final Integer undergraduates;       // UGDS; may include online students (data issue 7)
    public final Double latitude, longitude;   // kept for a future distance feature
    private final Set<String> awards;          // "CIP:level", e.g. "5138:2" = nursing associate awarded

    public College(String unitId, String name, String city, String state, String schoolUrl, String npcUrl,
                   int control, Boolean openAdmission, Double admissionRate, Integer testPolicy,
                   Integer reading25, Integer reading75, Integer math25, Integer math75,
                   Integer costOfAttendance, Integer tuitionIn, Integer tuitionOut,
                   Map<StudentProfile.IncomeBracket, Integer> netPrices,
                   StudentProfile.CampusSetting setting, Integer undergraduates,
                   Double latitude, Double longitude, Set<String> awards) {
        this.unitId = unitId; this.name = name; this.city = city; this.state = state;
        this.schoolUrl = schoolUrl; this.netPriceCalculatorUrl = npcUrl;
        this.control = control; this.openAdmission = openAdmission;
        this.admissionRate = admissionRate; this.testPolicy = testPolicy;
        this.reading25 = reading25; this.reading75 = reading75; this.math25 = math25; this.math75 = math75;
        this.costOfAttendance = costOfAttendance; this.tuitionIn = tuitionIn; this.tuitionOut = tuitionOut;
        this.netPrices = Collections.unmodifiableMap(new EnumMap<>(netPrices));
        this.setting = setting; this.undergraduates = undergraduates;
        this.latitude = latitude; this.longitude = longitude;
        this.awards = Collections.unmodifiableSet(new HashSet<>(awards));
    }

    public boolean isPublic() { return control == 1; }

    /** True if the program file shows at least one award at this level in 2021-22 or 2022-23. */
    public boolean awarded(String cipCode, int credentialLevel) {
        return cipCode != null && awards.contains(cipCode + ":" + credentialLevel);
    }

    /** Any associate (level 2) or bachelor's (level 3) award in any program. */
    public boolean offersLevel(int credentialLevel) {
        for (String key : awards) if (key.endsWith(":" + credentialLevel)) return true;
        return false;
    }

    /** Out-of-state cost estimate for publics: in-state cost of attendance with the tuition difference added. */
    public Integer outOfStateCostEstimate() {
        if (costOfAttendance == null || tuitionIn == null || tuitionOut == null) return null;
        return costOfAttendance - tuitionIn + tuitionOut;
    }

    public Integer netPrice(StudentProfile.IncomeBracket bracket) { return netPrices.get(bracket); }

    @Override public String toString() { return name + " (" + city + ", " + state + ")"; }
}
