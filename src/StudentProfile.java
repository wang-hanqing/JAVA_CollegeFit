import java.util.*;

/**
 * A student's stated answers. Not an admission or aid decision.
 * No name, immigration status or identifying information is stored.
 * Every question allows an "unknown" answer, which is also the default.
 * Programs, preferred states, settings and sizes allow several choices; an empty choice means "not sure".
 * Programs have no limit, and a whole field (such as all Engineering programs) counts as one choice.
 * There is no limit on programs: one field such as engineering can span many program codes.
 */
public class StudentProfile {

    // Each enum shows readable text in menus through toString(), using "<EnumName>.<VALUE>" in messages_en.properties.
    public enum Pathway { NOT_SURE, ASSOCIATE, BACHELOR, BOTH;
        @Override public String toString() { return Messages.get("Pathway." + name()); } }

    public enum SatStatus { NOT_SURE, NOT_YET, PROVIDED, PREFER_NOT_TO_SAY;
        @Override public String toString() { return Messages.get("SatStatus." + name()); } }

    public enum ChoiceStatus { NOT_SURE, KNOWN, PREFER_NOT_TO_SAY;
        @Override public String toString() { return Messages.get("ChoiceStatus." + name()); } }

    public enum LocationScope { NOT_SURE, PREFERRED_STATE, ANY_STATE;
        @Override public String toString() { return Messages.get("LocationScope." + name()); } }

    /** NOT_SURE is used only for colleges whose locale is missing. */
    public enum CampusSetting { NOT_SURE, URBAN, SUBURBAN, TOWN, RURAL;
        @Override public String toString() { return Messages.get("CampusSetting." + name()); } }

    public enum CollegeSize { SMALL, MEDIUM, LARGE;
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
    private final List<Program> programs = new ArrayList<>();           // any number; empty = not sure yet
    private SatStatus satStatus = SatStatus.NOT_SURE;
    private Integer satReadingWriting, satMath;                        // 200-800 if PROVIDED, otherwise null
    private String homeState = "";                                     // where she lives; in-state tuition context
    private LocationScope locationScope = LocationScope.NOT_SURE;
    private final Set<String> preferredStates = new TreeSet<>();       // only if PREFERRED_STATE
    private final Set<CampusSetting> settings = EnumSet.noneOf(CampusSetting.class); // empty = not sure
    private final Set<CollegeSize> sizes = EnumSet.noneOf(CollegeSize.class);         // empty = not sure
    private ChoiceStatus budgetStatus = ChoiceStatus.NOT_SURE;
    private Integer annualBudget;                                      // nonnegative USD if KNOWN
    private IncomeBracket incomeBracket = IncomeBracket.NOT_SURE;
    private AidEligibility aidEligibility = AidEligibility.NOT_SURE;

    public Pathway getPathway() { return pathway; }
    public void setPathway(Pathway value) { pathway = Objects.requireNonNull(value); }

    public List<Program> getPrograms() { return Collections.unmodifiableList(programs); }
    public boolean hasPrograms() { return !programs.isEmpty(); }
    public void setPrograms(List<Program> values) {
        List<Program> clean = new ArrayList<>();
        for (Program p : values) if (!p.isNotSure() && !clean.contains(p)) clean.add(p);
        programs.clear();
        programs.addAll(clean);
    }

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
        if (value == SatStatus.PROVIDED) throw new IllegalArgumentException("Use setSatScores() to provide scores.");
        satStatus = value;
        satReadingWriting = null;
        satMath = null;
    }

    public String getHomeState() { return homeState; }
    public boolean hasHomeState() { return !homeState.isEmpty(); }
    public void setHomeState(String stateCode) { homeState = stateCode == null ? "" : checkState(stateCode); }

    public LocationScope getLocationScope() { return locationScope; }
    public Set<String> getPreferredStates() { return Collections.unmodifiableSet(preferredStates); }
    public void setPreferredStates(Collection<String> states) {
        if (states.isEmpty()) throw new IllegalArgumentException(Messages.get("error.preferredState"));
        preferredStates.clear();
        for (String s : states) preferredStates.add(checkState(s));
        locationScope = LocationScope.PREFERRED_STATE;
    }
    public void setLocationScope(LocationScope value) {
        Objects.requireNonNull(value);
        if (value == LocationScope.PREFERRED_STATE) throw new IllegalArgumentException("Use setPreferredStates().");
        locationScope = value;
        preferredStates.clear();
    }
    public boolean prefersState(String state) {
        return locationScope == LocationScope.PREFERRED_STATE && preferredStates.contains(state);
    }

    public Set<CampusSetting> getSettings() { return Collections.unmodifiableSet(settings); }
    public void setSettings(Collection<CampusSetting> values) {
        settings.clear();
        for (CampusSetting c : values) if (c != CampusSetting.NOT_SURE) settings.add(c);
    }
    public Set<CollegeSize> getSizes() { return Collections.unmodifiableSet(sizes); }
    public void setSizes(Collection<CollegeSize> values) { sizes.clear(); sizes.addAll(values); }

    public ChoiceStatus getBudgetStatus() { return budgetStatus; }
    public Integer getAnnualBudget() { return annualBudget; }
    public void setAnnualBudget(int dollars) {
        if (dollars < 0) throw new IllegalArgumentException(Messages.get("error.budget"));
        annualBudget = dollars;
        budgetStatus = ChoiceStatus.KNOWN;
    }
    public void setBudgetStatus(ChoiceStatus value) {
        Objects.requireNonNull(value);
        if (value == ChoiceStatus.KNOWN) throw new IllegalArgumentException("Use setAnnualBudget().");
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

    /** A copy of these answers with a different program list; used to focus results on one interest. */
    public StudentProfile withPrograms(List<Program> focus) {
        StudentProfile c = new StudentProfile();
        c.pathway = pathway;
        c.setPrograms(focus);
        c.satStatus = satStatus; c.satReadingWriting = satReadingWriting; c.satMath = satMath;
        c.homeState = homeState;
        c.locationScope = locationScope; c.preferredStates.addAll(preferredStates);
        c.settings.addAll(settings); c.sizes.addAll(sizes);
        c.budgetStatus = budgetStatus; c.annualBudget = annualBudget;
        c.incomeBracket = incomeBracket; c.aidEligibility = aidEligibility;
        return c;
    }

    /** The fictional student used in the proposal and in ScenarioTests. */
    public static StudentProfile amina() {
        StudentProfile s = new StudentProfile();
        s.setPrograms(List.of(new Program("5138",
            "Registered Nursing, Nursing Administration, Nursing Research and Clinical Nursing", 0)));
        s.setSatStatus(SatStatus.NOT_YET);
        s.setHomeState("NY");
        s.setPreferredStates(List.of("NY"));
        s.setSettings(List.of(CampusSetting.URBAN));
        s.setIncomeBracket(IncomeBracket.UNDER_30K);
        return s; // pathway, size, budget and aid eligibility stay "not sure"
    }

    @Override public String toString() {
        List<String> cips = new ArrayList<>();
        for (Program p : programs) cips.add(p.cipCode);
        return "StudentProfile{pathway=" + pathway.name() + ", programs=" + cips + ", sat=" + satStatus.name()
            + ", home=" + homeState + ", location=" + locationScope.name() + " " + preferredStates
            + ", settings=" + settings + ", sizes=" + sizes + ", budget=" + budgetStatus.name()
            + ", income=" + incomeBracket.name() + ", aid=" + aidEligibility.name() + "}";
    }
}
