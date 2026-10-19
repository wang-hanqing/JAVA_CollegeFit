import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Reads data/processed/colleges.csv and programs.csv (made by notebooks/03_export.ipynb)
 * and joins them on UNITID. No external libraries: the CSV reader below handles quoted commas,
 * because 37% of program names contain commas.
 */
public class CollegeDataLoader {

    /** Where a CSV comes from: a file on disk, or a resource packed inside CollegeFit.jar. */
    public interface Source { BufferedReader open() throws IOException; }

    public static Source file(Path p) { return () -> Files.newBufferedReader(p, StandardCharsets.UTF_8); }

    public static Source resource(String name) {
        return () -> {
            InputStream in = CollegeDataLoader.class.getResourceAsStream(name);
            if (in == null) throw new FileNotFoundException("Not packed in the app: " + name);
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        };
    }

    public static List<College> load(Path collegesCsv, Path programsCsv) throws IOException {
        return load(file(collegesCsv), file(programsCsv));
    }

    public static List<Program> loadProgramList(Path programsCsv, List<College> colleges) throws IOException {
        return loadProgramList(file(programsCsv), colleges);
    }

    private static final StudentProfile.IncomeBracket[] BRACKETS = {
        StudentProfile.IncomeBracket.UNDER_30K, StudentProfile.IncomeBracket.FROM_30K_TO_48K,
        StudentProfile.IncomeBracket.FROM_48K_TO_75K, StudentProfile.IncomeBracket.FROM_75K_TO_110K,
        StudentProfile.IncomeBracket.ABOVE_110K };

    public static List<College> load(Source collegesCsv, Source programsCsv) throws IOException {
        Map<String, Set<String>> awards = loadAwards(programsCsv);
        List<College> colleges = new ArrayList<>();
        try (BufferedReader in = collegesCsv.open()) {
            Map<String, Integer> ix = header(in, "colleges CSV",
                "UNITID", "INSTNM", "STABBR", "CONTROL", "OPENADMP", "ADM_RATE", "ADMCON7", "COSTT4_A", "LOCALE", "UGDS");
            List<String> r;
            while ((r = readCsvRecord(in)) != null) {
                if (r.size() == 1 && r.get(0).isBlank()) continue; // skip empty lines
                String id = get(r, ix, "UNITID");
                Map<StudentProfile.IncomeBracket, Integer> net = new EnumMap<>(StudentProfile.IncomeBracket.class);
                for (int i = 1; i <= 5; i++) {
                    Integer v = integer(get(r, ix, "NPT4" + i));
                    if (v != null) net.put(BRACKETS[i - 1], v); // keep negatives; Messages.money() displays them
                }
                String open = get(r, ix, "OPENADMP");
                colleges.add(new College(
                    id, get(r, ix, "INSTNM"), get(r, ix, "CITY"), get(r, ix, "STABBR"),
                    get(r, ix, "INSTURL"), get(r, ix, "NPCURL"),
                    integer(get(r, ix, "CONTROL")) == null ? 0 : integer(get(r, ix, "CONTROL")),
                    open.equals("1") ? Boolean.TRUE : open.equals("2") ? Boolean.FALSE : null,
                    decimal(get(r, ix, "ADM_RATE")), integer(get(r, ix, "ADMCON7")),
                    integer(get(r, ix, "SATVR25")), integer(get(r, ix, "SATVR75")),
                    integer(get(r, ix, "SATMT25")), integer(get(r, ix, "SATMT75")),
                    integer(get(r, ix, "COSTT4_A")), integer(get(r, ix, "TUITIONFEE_IN")),
                    integer(get(r, ix, "TUITIONFEE_OUT")), net,
                    locale(get(r, ix, "LOCALE")), integer(get(r, ix, "UGDS")),
                    decimal(get(r, ix, "LATITUDE")), decimal(get(r, ix, "LONGITUDE")),
                    awards.getOrDefault(id, Collections.emptySet())));
            }
        }
        return colleges;
    }

    /** All programs with recent associate or bachelor's awards at the given colleges, sorted by name. */
    public static List<Program> loadProgramList(Source programsCsv, List<College> colleges) throws IOException {
        Set<String> inScope = new HashSet<>();
        for (College c : colleges) inScope.add(c.unitId);
        Map<String, String> names = new HashMap<>();
        Map<String, Set<String>> collegesByCip = new HashMap<>();
        try (BufferedReader in = programsCsv.open()) {
            Map<String, Integer> ix = header(in, "programs CSV", "UNITID", "CIPCODE", "CIPDESC", "CREDLEV", "IPEDSCOUNT1", "IPEDSCOUNT2");
            List<String> r;
            while ((r = readCsvRecord(in)) != null) {
                String id = get(r, ix, "UNITID"), level = get(r, ix, "CREDLEV");
                if (!inScope.contains(id) || (!level.equals("2") && !level.equals("3"))) continue;
                Integer a = integer(get(r, ix, "IPEDSCOUNT1")), b = integer(get(r, ix, "IPEDSCOUNT2"));
                if ((a == null || a <= 0) && (b == null || b <= 0)) continue;
                String cip = get(r, ix, "CIPCODE");
                if (cip.matches("[0-9]{1,3}")) cip = String.format("%04d", Integer.parseInt(cip));
                names.putIfAbsent(cip, get(r, ix, "CIPDESC"));
                collegesByCip.computeIfAbsent(cip, k -> new HashSet<>()).add(id);
            }
        }
        List<Program> list = new ArrayList<>();
        for (String cip : names.keySet()) list.add(new Program(cip, names.get(cip), collegesByCip.get(cip).size()));
        Collections.sort(list);
        return list;
    }

    /** UNITID -> {"CIP:level"} for associate and bachelor's programs with at least one recent award. */
    private static Map<String, Set<String>> loadAwards(Source source) throws IOException {
        Map<String, Set<String>> map = new HashMap<>();
        try (BufferedReader in = source.open()) {
            Map<String, Integer> ix = header(in, "programs CSV", "UNITID", "CIPCODE", "CREDLEV", "IPEDSCOUNT1", "IPEDSCOUNT2");
            List<String> r;
            while ((r = readCsvRecord(in)) != null) {
                String level = get(r, ix, "CREDLEV");
                if (!level.equals("2") && !level.equals("3")) continue;
                Integer a = integer(get(r, ix, "IPEDSCOUNT1")), b = integer(get(r, ix, "IPEDSCOUNT2"));
                if ((a == null || a <= 0) && (b == null || b <= 0)) continue;
                String cip = get(r, ix, "CIPCODE");
                if (cip.matches("[0-9]{1,3}")) cip = String.format("%04d", Integer.parseInt(cip)); // restore leading zero
                if (!cip.matches("[0-9]{4}")) continue;
                map.computeIfAbsent(get(r, ix, "UNITID"), k -> new HashSet<>()).add(cip + ":" + level);
            }
        }
        return map;
    }

    private static Map<String, Integer> header(BufferedReader in, String path, String... required) throws IOException {
        List<String> cols = readCsvRecord(in);
        if (cols == null) throw new IOException("Empty CSV: " + path);
        Map<String, Integer> ix = new HashMap<>();
        for (int i = 0; i < cols.size(); i++) ix.put(cols.get(i).replace("\ufeff", "").trim(), i);
        for (String col : required)
            if (!ix.containsKey(col)) throw new IOException("Missing column " + col + " in " + path);
        return ix;
    }

    /** LOCALE tens digit: 1 city, 2 suburb, 3 town, 4 rural. */
    private static StudentProfile.CampusSetting locale(String value) {
        Integer code = integer(value);
        if (code == null) return StudentProfile.CampusSetting.NOT_SURE;
        switch (code / 10) {
            case 1: return StudentProfile.CampusSetting.URBAN;
            case 2: return StudentProfile.CampusSetting.SUBURBAN;
            case 3: return StudentProfile.CampusSetting.TOWN;
            case 4: return StudentProfile.CampusSetting.RURAL;
            default: return StudentProfile.CampusSetting.NOT_SURE;
        }
    }

    /** Empty text means missing, so it becomes null rather than zero. */
    private static Integer integer(String s) {
        if (s.isBlank()) return null;
        try { return (int) Math.round(Double.parseDouble(s)); }
        catch (NumberFormatException e) { return null; }
    }

    private static Double decimal(String s) {
        if (s.isBlank()) return null;
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return null; }
    }

    private static String get(List<String> row, Map<String, Integer> ix, String key) {
        Integer p = ix.get(key);
        return p == null || p >= row.size() ? "" : row.get(p).trim();
    }

    /** Reads one CSV record. Handles quoted commas, escaped quotes ("") and quoted line breaks. */
    static List<String> readCsvRecord(BufferedReader in) throws IOException {
        StringBuilder field = new StringBuilder();
        List<String> result = new ArrayList<>();
        boolean quoted = false, seen = false;
        int ch;
        while ((ch = in.read()) != -1) {
            seen = true;
            char c = (char) ch;
            if (c == '"') {
                if (quoted) {
                    in.mark(1);
                    int next = in.read();
                    if (next == '"') field.append('"');          // "" inside quotes = one quote
                    else { quoted = false; if (next != -1) in.reset(); }
                } else if (field.length() == 0) quoted = true;
                else field.append(c);
            } else if (c == ',' && !quoted) {
                result.add(field.toString());
                field.setLength(0);
            } else if ((c == '\n' || c == '\r') && !quoted) {
                if (c == '\r') { in.mark(1); int next = in.read(); if (next != '\n' && next != -1) in.reset(); }
                result.add(field.toString());
                return result;
            } else field.append(c);
        }
        if (!seen) return null;
        result.add(field.toString());
        return result;
    }
}
