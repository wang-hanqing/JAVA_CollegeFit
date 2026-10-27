import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * Links from what a student enjoys doing to fields of study, read from data/processed/interest_links.csv
 * (made by notebooks/04_interests.ipynb).
 *
 * Interest -> career: each career's first career interest type in O*NET (U.S. Department of Labor).
 * Career -> program: the NCES and BLS CIP-SOC crosswalk. A link says a program prepares students for a career;
 * it is not a measure of quality or a promise about jobs. Only careers that usually need training,
 * an associate degree, a bachelor's degree or graduate school (O*NET Job Zones 3 to 5) are kept.
 */
public class InterestLinks {
    /** The six O*NET career interest types (RIASEC). The number of cards comes from the data. */
    public enum Interest { R, I, A, S, E, C }

    /** One career linked to one field, with the programs in that field it is linked to. */
    static final class CareerLink {
        final String soc, title;
        final int jobZone, cip6Links;  // cip6Links: how many detailed programs in the field link to this career
        final Set<String> cip4 = new TreeSet<>();
        CareerLink(String soc, String title, int jobZone, int cip6Links) {
            this.soc = soc; this.title = title; this.jobZone = jobZone; this.cip6Links = cip6Links;
        }
    }

    /** A field (2-digit CIP family) shown as a result card. */
    public static final class FieldMatch {
        public final String family;
        public final int linkedCareers;          // careers linked to the chosen interests in this field
        public final int colleges;               // colleges in scope that recently awarded degrees in the field
        public final List<CareerLink> careers;   // up to 3, most closely tied first
        public final List<String[]> programs;    // up to 3 linked programs: {name, college count}, most colleges first
        FieldMatch(String family, int linkedCareers, int colleges, List<CareerLink> careers, List<String[]> programs) {
            this.family = family; this.linkedCareers = linkedCareers; this.colleges = colleges;
            this.careers = careers; this.programs = programs;
        }
    }

    /** interest -> family -> SOC -> link */
    private final Map<Interest, Map<String, Map<String, CareerLink>>> links = new EnumMap<>(Interest.class);

    public static InterestLinks empty() { return new InterestLinks(); }

    public boolean isEmpty() { return links.isEmpty(); }

    /** Every career link, for tests. */
    List<CareerLink> allCareers() {
        List<CareerLink> out = new ArrayList<>();
        for (Map<String, Map<String, CareerLink>> fams : links.values())
            for (Map<String, CareerLink> socs : fams.values()) out.addAll(socs.values());
        return out;
    }

    /** True if the interest has a link to the field at all, in any state. */
    boolean links(Interest t, String family) { return links.getOrDefault(t, Map.of()).containsKey(family); }

    public static InterestLinks load(CollegeDataLoader.Source source) throws IOException {
        InterestLinks out = new InterestLinks();
        try (BufferedReader in = source.open()) {
            List<String> head = CollegeDataLoader.readCsvRecord(in);
            if (head == null) throw new IOException("interest links CSV is empty");
            Map<String, Integer> ix = new HashMap<>();
            for (int i = 0; i < head.size(); i++) ix.put(head.get(i).replace("\uFEFF", "").trim(), i);
            for (String col : new String[] {"INTEREST", "FAMILY", "SOC", "TITLE", "JOB_ZONE", "CIP6_LINKS", "CIP4"})
                if (!ix.containsKey(col)) throw new IOException("interest links CSV is missing column " + col);
            List<String> r;
            while ((r = CollegeDataLoader.readCsvRecord(in)) != null) {
                if (r.size() < ix.size()) continue;
                Interest t = Interest.valueOf(r.get(ix.get("INTEREST")).trim());
                String family = pad(r.get(ix.get("FAMILY")), 2), soc = r.get(ix.get("SOC")).trim();
                String title = r.get(ix.get("TITLE")).trim();
                int zone = Integer.parseInt(r.get(ix.get("JOB_ZONE")).trim()), cip6 = Integer.parseInt(r.get(ix.get("CIP6_LINKS")).trim());
                CareerLink link = out.links.computeIfAbsent(t, k -> new TreeMap<>())
                    .computeIfAbsent(family, k -> new TreeMap<>())
                    .computeIfAbsent(soc, k -> new CareerLink(soc, title, zone, cip6));
                link.cip4.add(pad(r.get(ix.get("CIP4")), 4));
            }
        }
        return out;
    }

    private static String pad(String s, int n) {
        StringBuilder b = new StringBuilder(s.trim());
        while (b.length() < n) b.insert(0, '0');
        return b.toString();
    }

    /**
     * Fields linked to the chosen interests, for colleges in the given states (empty = all states).
     * Order: more linked careers first, then more colleges, then the field code. Fields no college in scope offers are left out.
     */
    public List<FieldMatch> fields(Set<Interest> interests, List<College> colleges, Set<String> states) {
        Map<String, Map<String, CareerLink>> byFamily = new TreeMap<>();
        for (Interest t : interests)
            for (Map.Entry<String, Map<String, CareerLink>> e : links.getOrDefault(t, Map.of()).entrySet())
                byFamily.computeIfAbsent(e.getKey(), k -> new TreeMap<>()).putAll(e.getValue());

        List<FieldMatch> out = new ArrayList<>();
        for (Map.Entry<String, Map<String, CareerLink>> e : byFamily.entrySet()) {
            String family = e.getKey();
            int count = RecommendationEngine.fieldCount(colleges, states, family);
            if (count == 0) continue;
            List<CareerLink> careers = new ArrayList<>(e.getValue().values());
            careers.sort(Comparator.comparingInt((CareerLink c) -> -c.cip6Links)
                .thenComparingInt(c -> c.jobZone).thenComparing(c -> c.title));
            Set<String> cips = new TreeSet<>();
            for (CareerLink c : careers) cips.addAll(c.cip4);
            List<String[]> programs = new ArrayList<>();
            for (String cip : cips) {
                int n = RecommendationEngine.fieldCount(colleges, states, cip);
                if (n > 0) programs.add(new String[] {cip, String.valueOf(n)});
            }
            programs.sort((a, b) -> Integer.parseInt(b[1]) != Integer.parseInt(a[1])
                ? Integer.parseInt(b[1]) - Integer.parseInt(a[1]) : a[0].compareTo(b[0]));
            List<String[]> top = new ArrayList<>();
            for (String[] p : programs.subList(0, Math.min(3, programs.size()))) top.add(new String[] {Program.nameOf(p[0]), p[1]});
            out.add(new FieldMatch(family, careers.size(), count, careers.subList(0, Math.min(3, careers.size())), top));
        }
        out.sort(Comparator.comparingInt((FieldMatch f) -> -f.linkedCareers)
            .thenComparingInt(f -> -f.colleges).thenComparing(f -> f.family));
        return out;
    }
}
