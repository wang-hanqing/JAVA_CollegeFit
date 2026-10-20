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

    private static final StudentProfile.IncomeBracket[] BRACKETS = {
        StudentProfile.IncomeBracket.UNDER_30K, StudentProfile.IncomeBracket.FROM_30K_TO_48K,
        StudentProfile.IncomeBracket.FROM_48K_TO_75K, StudentProfile.IncomeBracket.FROM_75K_TO_110K,
        StudentProfile.IncomeBracket.ABOVE_110K };

    public static List<College> load(Path collegesCsv, Path programsCsv) throws IOException {
        return load(file(collegesCsv), file(programsCsv));
    }

    public static List<Program> loadProgramList(Path programsCsv, List<College> colleges) throws IOException {
        return loadProgramList(file(programsCsv), colleges);
    }

    public static List<College> load(Source collegesCsv, Source programsCsv) throws IOException {
        Map<String, Map<String, Integer[]>> programs = loadProgramCounts(programsCsv);
        List<College> colleges = new ArrayList<>();
        try (BufferedReader in = collegesCsv.open()) {
            Map<String, Integer> ix = header(in, "colleges CSV",
                "UNITID", "INSTNM", "STABBR", "CONTROL", "OPENADMP", "ADM_RATE", "ADMCON7", "COSTT4_A", "LOCALE", "UGDS");
            List<String> r;
            while ((r = readCsvRecord(in)) != null) {
                if (r.size() == 1 && r.get(0).isBlank()) continue;
                String id = get(r, ix, "UNITID");
                Map<StudentProfile.IncomeBracket, Integer> net = new EnumMap<>(StudentProfile.IncomeBracket.class);
                for (int i = 1; i <= 5; i++) {
                    Integer v = integer(get(r, ix, "NPT4" + i));
                    if (v != null) net.put(BRACKETS[i - 1], v); // negatives are kept as reported
                }
                String open = get(r, ix, "OPENADMP");
                Integer control = integer(get(r, ix, "CONTROL"));
                colleges.add(new College(
                    id, get(r, ix, "INSTNM"), get(r, ix, "CITY"), get(r, ix, "STABBR"),
                    get(r, ix, "INSTURL"), get(r, ix, "NPCURL"), control == null ? 0 : control,
                    open.equals("1") ? Boolean.TRUE : open.equals("2") ? Boolean.FALSE : null,
                    decimal(get(r, ix, "ADM_RATE")), integer(get(r, ix, "ADMCON7")),
                    integer(get(r, ix, "SATVR25")), integer(get(r, ix, "SATVR75")),
                    integer(get(r, ix, "SATMT25")), integer(get(r, ix, "SATMT75")),
                    integer(get(r, ix, "COSTT4_A")), integer(get(r, ix, "TUITIONFEE_IN")),
                    integer(get(r, ix, "TUITIONFEE_OUT")), integer(get(r, ix, "NPT4")), net,
                    locale(get(r, ix, "LOCALE")), integer(get(r, ix, "UGDS")),
                    decimal(get(r, ix, "LATITUDE")), decimal(get(r, ix, "LONGITUDE")),
                    programs.getOrDefault(id, Collections.emptyMap())));
            }
        }
        return colleges;
    }

    /** UNITID -> ("CIP:level" -> {count1, count2}) for every associate and bachelor's row, including zeros. */
    private static Map<String, Map<String, Integer[]>> loadProgramCounts(Source source) throws IOException {
        Map<String, Map<String, Integer[]>> map = new HashMap<>();
        try (BufferedReader in = source.open()) {
            Map<String, Integer> ix = header(in, "programs CSV", "UNITID", "CIPCODE", "CREDLEV", "IPEDSCOUNT1", "IPEDSCOUNT2");
            List<String> r;
            while ((r = readCsvRecord(in)) != null) {
                String level = get(r, ix, "CREDLEV");
                if (!level.equals("2") && !level.equals("3")) continue;
                String cip = cip(get(r, ix, "CIPCODE"));
                if (cip == null) continue;
                Integer[] counts = { integer(get(r, ix, "IPEDSCOUNT1")), integer(get(r, ix, "IPEDSCOUNT2")) };
                map.computeIfAbsent(get(r, ix, "UNITID"), k -> new HashMap<>()).put(cip + ":" + level, counts);
            }
        }
        return map;
    }

    /** Programs and whole fields with a positive associate or bachelor's award count at the given colleges. */
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
                String cip = cip(get(r, ix, "CIPCODE"));
                if (cip == null) continue;
                names.putIfAbsent(cip, get(r, ix, "CIPDESC"));
                collegesByCip.computeIfAbsent(cip, k -> new HashSet<>()).add(id);
            }
        }
        List<Program> list = new ArrayList<>();
        Map<String, Set<String>> collegesByFamily = new TreeMap<>();
        for (String cip : names.keySet()) {
            list.add(new Program(cip, names.get(cip), collegesByCip.get(cip).size()));
            collegesByFamily.computeIfAbsent(cip.substring(0, 2), k -> new HashSet<>()).addAll(collegesByCip.get(cip));
        }
        // A family is offered only when it contains more than one program; otherwise it would duplicate the program.
        Map<String, Integer> programsPerFamily = new HashMap<>();
        for (String cip : names.keySet()) programsPerFamily.merge(cip.substring(0, 2), 1, Integer::sum);
        for (Map.Entry<String, Set<String>> e : collegesByFamily.entrySet())
            if (programsPerFamily.get(e.getKey()) > 1) list.add(Program.family(e.getKey(), e.getValue().size()));
        Collections.sort(list);
        return list;
    }

    /** Restores leading zeros, e.g. "901" -> "0901". Returns null for anything that is not a CIP code. */
    private static String cip(String raw) {
        if (raw.matches("[0-9]{1,3}")) raw = String.format("%04d", Integer.parseInt(raw));
        return raw.matches("[0-9]{4}") ? raw : null;
    }

    private static Map<String, Integer> header(BufferedReader in, String what, String... required) throws IOException {
        List<String> cols = readCsvRecord(in);
        if (cols == null) throw new IOException("Empty " + what);
        Map<String, Integer> ix = new HashMap<>();
        for (int i = 0; i < cols.size(); i++) ix.put(cols.get(i).replace("\ufeff", "").trim(), i);
        for (String col : required)
            if (!ix.containsKey(col)) throw new IOException("Missing column " + col + " in " + what);
        return ix;
    }

    /** LOCALE tens digit: 1 city, 2 suburb, 3 town, 4 rural. Anything else stays unknown. */
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

    /** Empty text means missing, so it becomes null rather than zero. A reported 0 stays 0. */
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
                    if (next == '"') field.append('"');
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
