import java.util.Objects;

/**
 * A student's stated answers. Not an admission or aid decision.
 * No name, immigration status or identifying information is stored.
 * Every question allows an "unknown" answer, which is also the default.
 */
public class StudentProfile {

    // Each enum shows readable text in drop-down menus through toString(),
    // using the key "<EnumName>.<VALUE>" in messages_en.properties.
    public enum Pathway { NOT_SURE, ASSOCIATE, BACHELOR, BOTH;
        @Override public String toString() { return Messages.get("Pathway." + name()); } }

    public enum SatStatus { NOT_SURE, NOT_YET, PROVIDED, PREFER_NOT_TO_SAY;
        @Override public String toString() { return Messages.get("SatStatus." + name()); } }

    public enum ChoiceStatus { NOT_SURE, KNOWN, PREFER_NOT_TO_SAY;
        @Override public String toString() { return Messages.get("ChoiceStatus." + name()); } }

    public enum LocationScope { NOT_SURE, PREFERRED_STATE, ANY_STATE;
        @Override public String toString() { return Messages.get("LocationScope." + name()); } }

    public enum CampusSetting { NOT_SURE, URBAN, SUBURBAN, TOWN, RURAL;
        @Override public String toString() { return Messages.get("CampusSetting." + name()); } }

    public enum CollegeSize { NOT_SURE, SMALL, MEDIUM, LARGE;
        @Override public String toString() { return Messages.get("CollegeSize." + name()); } }

    /** Matches the five Scorecard net price brackets (NPT41 to NPT45). */
    public enum IncomeBracket { NOT_SURE, UNDER_30K, FROM_30K_TO_48K, FROM_48K_TO_75K,
        FROM_75K_TO_110K, ABOVE_110K, PREFER_NOT_TO_SAY;
        public boolean isKnown() { return this != NOT_SURE && this != PREFER_NOT_TO_SAY; }
        @Override public String toString() { return Messages.get("IncomeBracket." + name()); } }

    /** "Can you apply for federal financial aid (FAFSA)?" Never asks about immigration status. */
    public enum AidEligibility { NOT_SURE, YES, NO, PREFER_NOT_TO_SAY;
        @Override public String toString() { return Messages.get("AidEligibility." + name()); } }

    private Pathway pathway = Pathway.NOT_SURE;
    private Program program = Program.NOT_SURE;
    private SatStatus satStatus = SatStatus.NOT_SURE;
    private Integer satReadingWriting; // 200-800 if PROVIDED, otherwise null
    private Integer satMath;           // 200-800 if PROVIDED, otherwise null
    private String homeState = "";     // where she lives; used only to pick in-state or out-of-state costs
    private LocationScope locationScope = LocationScope.NOT_SURE;
    private String preferredState = ""; // where she would like to study, only if PREFERRED_STATE
    private CampusSetting campusSetting = CampusSetting.NOT_SURE;
    private CollegeSize collegeSize = CollegeSize.NOT_SURE;
    private ChoiceStatus budgetStatus = ChoiceStatus.NOT_SURE;
    private Integer annualBudget;      // nonnegative USD if KNOWN, otherwise null
    private IncomeBracket incomeBracket = IncomeBracket.NOT_SURE;
    private AidEligibility aidEligibility = AidEligibility.NOT_SURE;

    public Pathway getPathway() { return pathway; }
    public void setPathway(Pathway value) { pathway = Objects.requireNonNull(value); }

    public Program getProgram() { return program; }
    public void setProgram(Program value) { program = Objects.requireNonNull(value); }

    public SatStatus getSatStatus() { return satStatus; }
    public Integer getSatReadingWriting() { return satReadingWriting; }
    public Integer getSatMath() { return satMath; }
    public void setSatScores(int readingWriting, int math) {
        if (readingWriting < 200 || readingWriting > 800 || math < 200 || math > 800)
            throw new IllegalArgumentException(Messages.get("error.sat"));
        satReadingWriting = readingWriting;
        satMath = math;
        satStatus = SatStatus.PROVIDED;
    }
    public void setSatStatus(SatStatus value) {
        Objects.requireNonNull(value);
        if (value == SatStatus.PROVIDED)
            throw new IllegalArgumentException("Use setSatScores() to provide both section scores.");
        satStatus = value;
        satReadingWriting = null;
        satMath = null;
    }

    public String getHomeState() { return homeState; }
    public boolean hasHomeState() { return !homeState.isEmpty(); }
    public void setHomeState(String stateCode) { homeState = stateCode == null ? "" : checkState(stateCode); }

    public LocationScope getLocationScope() { return locationScope; }
    public String getPreferredState() { return preferredState; }
    public void setPreferredState(String stateCode) {
        preferredState = checkState(stateCode);
        locationScope = LocationScope.PREFERRED_STATE;
    }
    public void setLocationScope(LocationScope value) {
        Objects.requireNonNull(value);
        if (value == LocationScope.PREFERRED_STATE)
            throw new IllegalArgumentException("Use setPreferredState() to specify the state.");
        locationScope = value;
        preferredState = "";
    }

    public CampusSetting getCampusSetting() { return campusSetting; }
    public void setCampusSetting(CampusSetting value) { campusSetting = Objects.requireNonNull(value); }
    public CollegeSize getCollegeSize() { return collegeSize; }
    public void setCollegeSize(CollegeSize value) { collegeSize = Objects.requireNonNull(value); }

    public ChoiceStatus getBudgetStatus() { return budgetStatus; }
    public Integer getAnnualBudget() { return annualBudget; }
    public void setAnnualBudget(int dollars) {
        if (dollars < 0) throw new IllegalArgumentException(Messages.get("error.budget"));
        annualBudget = dollars;
        budgetStatus = ChoiceStatus.KNOWN;
    }
    public void setBudgetStatus(ChoiceStatus value) {
        Objects.requireNonNull(value);
        if (value == ChoiceStatus.KNOWN)
            throw new IllegalArgumentException("Use setAnnualBudget() to provide a budget.");
        budgetStatus = value;
        annualBudget = null;
    }

    public IncomeBracket getIncomeBracket() { return incomeBracket; }
    public void setIncomeBracket(IncomeBracket value) { incomeBracket = Objects.requireNonNull(value); }
    public AidEligibility getAidEligibility() { return aidEligibility; }
    public void setAidEligibility(AidEligibility value) { aidEligibility = Objects.requireNonNull(value); }

    private static String checkState(String stateCode) {
        if (stateCode == null || !stateCode.trim().matches("[A-Za-z]{2}"))
            throw new IllegalArgumentException(Messages.get("error.state"));
        return stateCode.trim().toUpperCase();
    }

    /** The fictional student used in the proposal and in ScenarioTests. */
    public static StudentProfile amina() {
        StudentProfile s = new StudentProfile();
        s.setProgram(new Program("5138", "Registered Nursing, Nursing Administration, Nursing Research and Clinical Nursing", 0));
        s.setSatStatus(SatStatus.NOT_YET);
        s.setHomeState("NY");
        s.setPreferredState("NY");
        s.setCampusSetting(CampusSetting.URBAN);
        s.setIncomeBracket(IncomeBracket.UNDER_30K);
        return s; // pathway, size, budget and aid eligibility stay "not sure"
    }

    @Override
    public String toString() {
        return "StudentProfile{pathway=" + pathway.name() + ", program=" + (program.isNotSure() ? "NOT_SURE" : program.cipCode)
            + ", sat=" + satStatus.name() + ", home=" + homeState
            + ", location=" + locationScope.name() + " " + preferredState
            + ", setting=" + campusSetting.name() + ", size=" + collegeSize.name()
            + ", budget=" + budgetStatus.name() + (annualBudget == null ? "" : " " + annualBudget)
            + ", income=" + incomeBracket.name() + ", aid=" + aidEligibility.name() + "}";
    }
}
