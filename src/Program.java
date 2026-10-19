import java.util.Objects;

/** A field of study from the Scorecard program file (4-digit CIP code). */
public class Program implements Comparable<Program> {
    public static final Program NOT_SURE = new Program(null, null, 0);

    public final String cipCode;     // e.g. "5138"; null means "not sure yet"
    public final String fullName;    // e.g. "Registered Nursing, Nursing Administration, ..."
    public final int collegeCount;   // colleges in scope with recent awards

    public Program(String cipCode, String fullName, int collegeCount) {
        this.cipCode = cipCode;
        this.fullName = fullName == null ? null : fullName.replaceAll("\\.$", "").trim();
        this.collegeCount = collegeCount;
    }

    public boolean isNotSure() { return cipCode == null; }

    /** Text before the first comma, e.g. "Registered Nursing". Used on result cards. */
    public String shortName() {
        if (isNotSure()) return Messages.get("program.notSure");
        int comma = fullName.indexOf(',');
        return comma > 0 ? fullName.substring(0, comma) : fullName;
    }

    /** Shown in the drop-down menu. */
    @Override public String toString() {
        return isNotSure() ? Messages.get("program.notSure") : fullName + "  (" + collegeCount + ")";
    }

    @Override public int compareTo(Program o) {
        if (isNotSure()) return o.isNotSure() ? 0 : -1;
        if (o.isNotSure()) return 1;
        return fullName.compareToIgnoreCase(o.fullName);
    }

    @Override public boolean equals(Object o) {
        return o instanceof Program && Objects.equals(cipCode, ((Program) o).cipCode);
    }
    @Override public int hashCode() { return Objects.hashCode(cipCode); }
}
