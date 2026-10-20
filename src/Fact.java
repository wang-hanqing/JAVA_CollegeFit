/** One line on the details page: a label, a value, an optional note, and an optional match mark. */
public class Fact {
    public final String label;
    public final String value;
    public final String note;   // small grey text under the value; may be empty
    public final Boolean match; // true = matches her answer, false = does not, null = no comparison

    public Fact(String label, String value, String note, Boolean match) {
        this.label = label;
        this.value = value;
        this.note = note == null ? "" : note;
        this.match = match;
    }

    public Fact(String label, String value) { this(label, value, "", null); }
}
