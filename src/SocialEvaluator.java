import java.util.ArrayList;
import java.util.List;

/**
 * Social lens: location, campus setting and size. Every item is listed with the college's actual value.
 * Only preferences she stated are compared; "not sure" and missing college data are left out of the count.
 */
public class SocialEvaluator implements LensEvaluator {
    static final int SMALL_MAX = 1999, MEDIUM_MAX = 9999; // illustrative size bands

    @Override
    public EvaluationResult evaluate(StudentProfile s, College c) {
        List<Fact> facts = new ArrayList<>();
        int considered = 0, matched = 0;

        // State
        if (s.getLocationScope() == StudentProfile.LocationScope.PREFERRED_STATE) {
            boolean ok = s.getPreferredStates().contains(c.state);
            considered++; if (ok) matched++;
            facts.add(new Fact(Messages.get("soc.state"), c.state,
                Messages.get("soc.youChose", String.join(", ", s.getPreferredStates())), ok));
        } else {
            facts.add(new Fact(Messages.get("soc.state"), c.state, Messages.get("soc.noPreference"), null));
        }

        // Campus setting
        String settingValue = c.setting == StudentProfile.CampusSetting.NOT_SURE ? Messages.get("unavailable") : c.setting.toString();
        if (s.getSettings().isEmpty()) {
            facts.add(new Fact(Messages.get("soc.setting"), settingValue, Messages.get("soc.noPreference"), null));
        } else if (c.setting == StudentProfile.CampusSetting.NOT_SURE) {
            facts.add(new Fact(Messages.get("soc.setting"), settingValue, Messages.get("soc.dataMissing"), null));
        } else {
            boolean ok = s.getSettings().contains(c.setting);
            considered++; if (ok) matched++;
            facts.add(new Fact(Messages.get("soc.setting"), settingValue, Messages.get("soc.youChose", join(s.getSettings())), ok));
        }

        // Size
        StudentProfile.CollegeSize size = c.undergraduates == null ? null
            : c.undergraduates <= SMALL_MAX ? StudentProfile.CollegeSize.SMALL
            : c.undergraduates <= MEDIUM_MAX ? StudentProfile.CollegeSize.MEDIUM : StudentProfile.CollegeSize.LARGE;
        String sizeValue = size == null ? Messages.get("unavailable")
            : Messages.get("soc.size.value", Messages.number(c.undergraduates), Messages.get("CollegeSize.short." + size.name()));
        String online = Messages.get("soc.size.online");
        if (s.getSizes().isEmpty()) {
            facts.add(new Fact(Messages.get("soc.size"), sizeValue, Messages.get("soc.noPreference") + " " + online, null));
        } else if (size == null) {
            facts.add(new Fact(Messages.get("soc.size"), sizeValue, Messages.get("soc.dataMissing"), null));
        } else {
            boolean ok = s.getSizes().contains(size);
            considered++; if (ok) matched++;
            facts.add(new Fact(Messages.get("soc.size"), sizeValue, Messages.get("soc.youChose", join(s.getSizes())) + " " + online, ok));
        }

        if (considered == 0)
            return new EvaluationResult(FitStatus.UNKNOWN, Messages.get("soc.label.none"), facts, "", Messages.get("soc.next.none"));
        boolean all = matched == considered;
        return new EvaluationResult(all ? FitStatus.MATCH : FitStatus.MISMATCH, Messages.get("soc.label", matched, considered),
            facts, all ? Messages.get("soc.reason", matched, considered) : "", all ? "" : Messages.get("soc.next.flexible"),
            matched, considered);
    }

    private static String join(Iterable<?> values) {
        List<String> out = new ArrayList<>();
        for (Object v : values) out.add(v.toString());
        return String.join(", ", out);
    }
}
