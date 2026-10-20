import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * The colleges a student saves. Stores colleges, not results, so the list is re-evaluated
 * with her latest answers whenever it is shown or exported.
 */
public class Shortlist {
    private final Map<String, College> saved = new LinkedHashMap<>(); // keeps the order she saved them

    public boolean contains(College c) { return saved.containsKey(c.unitId); }
    public void add(College c) { saved.put(c.unitId, c); }
    public void remove(College c) { saved.remove(c.unitId); }
    public int size() { return saved.size(); }
    public List<College> colleges() { return new ArrayList<>(saved.values()); }
    public void clear() { saved.clear(); }

    /** Evaluates every saved college for this student, in the order they were saved. */
    public List<CollegeRecommendation> evaluate(StudentProfile s, RecommendationEngine engine) {
        return engine.recommendAll(s, colleges());
    }

    /** Writes one row per saved college. Opens in Excel or Google Sheets. */
    public void exportCsv(Path file, StudentProfile s, RecommendationEngine engine) throws IOException {
        NextStepGenerator steps = new NextStepGenerator();
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write('\ufeff'); // byte order mark so Excel reads the file as UTF-8
            writeRow(out, Messages.get("export.college"), Messages.get("export.city"), Messages.get("export.state"),
                Messages.get("lens.program"), Messages.get("lens.academic"), Messages.get("lens.financial"),
                Messages.get("lens.social"), Messages.get("export.steps"),
                Messages.get("details.website"), Messages.get("details.npc"));
            for (CollegeRecommendation r : evaluate(s, engine)) {
                writeRow(out, r.college.name, r.college.city, r.college.state,
                    r.program.label, r.academic.label, r.financial.label, r.social.label,
                    String.join("; ", steps.collegeSteps(s, r)),
                    r.college.schoolUrl, r.college.netPriceCalculatorUrl);
            }
            writeRow(out, Messages.get("export.note"));
        }
    }

    private static void writeRow(Writer out, String... values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.write(',');
            out.write(csv(values[i]));
        }
        out.write("\r\n");
    }

    /** Quotes a value when it contains a comma, quote or line break; doubles any quotes inside. */
    static String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }
}
