/**
 * Social lens (appendix A2). Only preferences she stated are compared;
 * "not sure" is left out, never counted as a mismatch. Missing college data is also left out.
 */
public class SocialEvaluator implements LensEvaluator {
    // Illustrative size bands based on undergraduate enrollment
    static final int SMALL_MAX = 1999, MEDIUM_MAX = 9999;

    @Override
    public EvaluationResult evaluate(StudentProfile s, College c) {
        int considered = 0, matched = 0;
        StringBuilder details = new StringBuilder();

        if (s.getLocationScope() == StudentProfile.LocationScope.PREFERRED_STATE) {
            considered++;
            boolean ok = s.getPreferredState().equals(c.state);
            if (ok) matched++;
            details.append(Messages.get(ok ? "pref.state.yes" : "pref.state.no", c.state)).append(' ');
        }
        if (s.getCampusSetting() != StudentProfile.CampusSetting.NOT_SURE
                && c.setting != StudentProfile.CampusSetting.NOT_SURE) {
            considered++;
            boolean ok = s.getCampusSetting() == c.setting;
            if (ok) matched++;
            details.append(Messages.get(ok ? "pref.setting.yes" : "pref.setting.no", c.setting)).append(' ');
        }
        if (s.getCollegeSize() != StudentProfile.CollegeSize.NOT_SURE && c.undergraduates != null) {
            considered++;
            StudentProfile.CollegeSize size = c.undergraduates <= SMALL_MAX ? StudentProfile.CollegeSize.SMALL
                : c.undergraduates <= MEDIUM_MAX ? StudentProfile.CollegeSize.MEDIUM : StudentProfile.CollegeSize.LARGE;
            boolean ok = size == s.getCollegeSize();
            if (ok) matched++;
            details.append(Messages.get(ok ? "pref.size.yes" : "pref.size.no",
                String.format("%,d", c.undergraduates))).append(' ').append(Messages.get("pref.size.online")).append(' ');
        }

        if (considered == 0) {
            return new EvaluationResult(FitStatus.UNKNOWN, Messages.get("pref.none.label"),
                Messages.get("pref.none.explain"), Messages.get("pref.none.next"));
        }
        String label = Messages.get("pref.label", matched, considered);
        boolean all = matched == considered;
        return new EvaluationResult(all ? FitStatus.MATCH : FitStatus.MISMATCH, label,
            details.toString().trim(), all ? "" : Messages.get("pref.flexible.next"), matched, considered);
    }
}
