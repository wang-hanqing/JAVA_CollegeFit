import java.util.Objects;

/**
 * A field of study from the Scorecard program file: a 4-digit CIP code (one program, e.g. 5138 Nursing)
 * or a 2-digit family (all programs in a field, e.g. 14 Engineering).
 */
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

    /** Names of every 4-digit program in the data, filled when the program list is loaded. */
    private static final java.util.Map<String, String> NAMES = new java.util.HashMap<>();
    static void register(Program p) { if (!p.isNotSure() && !p.isFamily()) NAMES.put(p.cipCode, p.fullName); }
    static String nameOf(String cip) { return NAMES.getOrDefault(cip, cip); }

    /** A 2-digit family covers every 4-digit program that starts with it. */
    public boolean isFamily() { return cipCode != null && cipCode.length() == 2; }

    public static Program family(String twoDigit, int collegeCount) {
        return new Program(twoDigit, Messages.get("program.family", CipFamilies.name(twoDigit)), collegeCount);
    }

    /** Text before the first comma, e.g. "Registered Nursing". Used on result cards. */
    public String shortName() {
        if (isNotSure()) return Messages.get("program.notSure");
        if (isFamily()) return Messages.get("program.family.short", CipFamilies.name(cipCode));
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
        if (isFamily() != o.isFamily()) return isFamily() ? -1 : 1; // whole fields first
        return fullName.compareToIgnoreCase(o.fullName);
    }

    @Override public boolean equals(Object o) {
        return o instanceof Program && Objects.equals(cipCode, ((Program) o).cipCode);
    }
    @Override public int hashCode() { return Objects.hashCode(cipCode); }
}
