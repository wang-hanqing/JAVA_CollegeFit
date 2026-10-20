import java.util.HashMap;
import java.util.Map;

/** Short names for 2-digit CIP families, so a student can choose a whole field such as Engineering. */
final class CipFamilies {
    private static final Map<String, String> NAMES = new HashMap<>();
    static {
        String[][] rows = {
            {"01", "Agriculture"}, {"03", "Natural Resources and Conservation"}, {"04", "Architecture"},
            {"05", "Area, Ethnic and Gender Studies"}, {"09", "Communication and Journalism"},
            {"10", "Communications Technologies"}, {"11", "Computer and Information Sciences"},
            {"12", "Culinary and Personal Services"}, {"13", "Education"}, {"14", "Engineering"},
            {"15", "Engineering Technologies"}, {"16", "Languages and Linguistics"},
            {"19", "Family and Consumer Sciences"}, {"22", "Legal Studies"}, {"23", "English"},
            {"24", "Liberal Arts and General Studies"}, {"25", "Library Science"},
            {"26", "Biological and Biomedical Sciences"}, {"27", "Mathematics and Statistics"},
            {"28", "Military Science"}, {"29", "Military Technologies"}, {"30", "Interdisciplinary Studies"},
            {"31", "Parks, Recreation and Kinesiology"}, {"38", "Philosophy and Religious Studies"},
            {"39", "Theology"}, {"40", "Physical Sciences"}, {"41", "Science Technologies"},
            {"42", "Psychology"}, {"43", "Public Safety and Law Enforcement"},
            {"44", "Public Administration and Social Work"}, {"45", "Social Sciences"},
            {"46", "Construction Trades"}, {"47", "Mechanic and Repair Technologies"},
            {"48", "Precision Production"}, {"49", "Transportation"}, {"50", "Visual and Performing Arts"},
            {"51", "Health Professions"}, {"52", "Business"}, {"54", "History"} };
        for (String[] r : rows) NAMES.put(r[0], r[1]);
    }

    private CipFamilies() { }

    static String name(String twoDigit) { return NAMES.getOrDefault(twoDigit, "Field " + twoDigit); }
}
