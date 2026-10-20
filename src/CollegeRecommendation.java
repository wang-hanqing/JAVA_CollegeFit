import java.util.ArrayList;
import java.util.List;

/** One college as seen through the program check and all three lenses. */
public class CollegeRecommendation {
    public final College college;
    public final EvaluationResult academic, financial, social, program;
    /** 0 = a chosen program has a positive award count (or no program chosen); 1 = no positive award evidence. */
    public final int programPriority;
    /** How many of her chosen programs have positive award evidence here. */
    public final int programsAwarded;

    public CollegeRecommendation(StudentProfile s, College c,
                                 EvaluationResult academic, EvaluationResult financial, EvaluationResult social) {
        this.college = c;
        this.academic = academic;
        this.financial = financial;
        this.social = social;

        if (!s.hasPrograms()) {
            programPriority = 0;
            programsAwarded = 0;
            program = new EvaluationResult(FitStatus.UNKNOWN, Messages.get("prog.label.none"),
                List.of(new Fact(Messages.get("lens.program"), Messages.get("prog.none"), Messages.get("prog.none.note"), null)),
                "", Messages.get("prog.next.none"));
            return;
        }

        List<Fact> facts = new ArrayList<>();
        List<String> steps = new ArrayList<>();
        List<String> awardedNames = new ArrayList<>(), otherNames = new ArrayList<>();
        boolean nursingBachelor = false;
        int awarded = 0;
        String singleLabel = "";
        for (Program p : s.getPrograms()) {
            College.ProgramEvidence best = College.ProgramEvidence.NOT_CONFIRMED;
            List<String> parts = new ArrayList<>();
            boolean bachelorAwarded = false;
            for (int level : RecommendationEngine.levels(s.getPathway())) {
                College.ProgramEvidence e = c.evidence(p.cipCode, level);
                Integer[] n = c.awardCounts(p.cipCode, level);
                String levelName = Messages.get("prog.level." + level);
                if (p.isFamily()) {
                    int k = c.familyProgramsAwarded(p.cipCode, level);
                    parts.add(k == 0 ? Messages.get("prog.part.family.none", levelName)
                        : k == 1 ? Messages.get("prog.part.family.one", levelName) : Messages.get("prog.part.family", levelName, k));
                } else if (e == College.ProgramEvidence.AWARDED) {
                    parts.add(Messages.get("prog.part.awarded", levelName, count(n[0]), count(n[1])));
                    if (level == 3) bachelorAwarded = true;
                } else if (e == College.ProgramEvidence.ZERO_AWARDS) {
                    parts.add(Messages.get("prog.part.zero", levelName));
                } else if (n != null) {
                    parts.add(Messages.get("prog.part.missing", levelName));
                } else {
                    parts.add(Messages.get("prog.part.noRecord", levelName));
                }
                if (e.ordinal() < best.ordinal()) best = e; // AWARDED < ZERO_AWARDS < NOT_CONFIRMED
            }
            boolean isAwarded = best == College.ProgramEvidence.AWARDED;
            // With many programs, list the awarded ones in detail and summarize the rest
            if (s.getPrograms().size() <= MAX_LISTED || (isAwarded && awardedNames.size() < MAX_LISTED))
                facts.add(new Fact(p.shortName(), Messages.get("prog.value." + best.name()), String.join("; ", parts), isAwarded));
            if (isAwarded) {
                awarded++;
                awardedNames.add(p.shortName());
                // CIP 5138 also covers RN-to-BSN programs for nurses who are already licensed.
                if ("5138".equals(p.cipCode) && bachelorAwarded) nursingBachelor = true;
            } else {
                otherNames.add(p.shortName());
                if (s.getPrograms().size() <= MAX_LISTED)
                    steps.add(Messages.get(best == College.ProgramEvidence.ZERO_AWARDS ? "prog.next.zero" : "prog.next.confirm", p.shortName()));
            }
            singleLabel = Messages.get("prog.label.one." + best.name(), p.shortName());
        }
        int total = s.getPrograms().size();
        if (total > MAX_LISTED) {
            if (awardedNames.size() > MAX_LISTED)
                facts.add(new Fact(Messages.get("prog.moreAwarded"), String.valueOf(awardedNames.size() - MAX_LISTED), "", true));
            if (!otherNames.isEmpty())
                facts.add(new Fact(Messages.get("prog.others"), Messages.get("prog.others.value", otherNames.size()),
                    Messages.get("prog.others.note"), null));
            if (!otherNames.isEmpty()) steps.add(0, Messages.get("prog.next.others"));
        }
        if (!awardedNames.isEmpty())
            steps.add(0, awardedNames.size() == 1 ? Messages.get("prog.next.verify.one", awardedNames.get(0))
                : Messages.get("prog.next.verify", names(awardedNames)));
        if (nursingBachelor) steps.add(1, Messages.get("prog.next.license"));

        programPriority = awarded > 0 ? 0 : 1;
        programsAwarded = awarded;
        String label = total == 1 ? singleLabel : Messages.get("prog.label.many", awarded, total);
        FitStatus status = awarded > 0 ? FitStatus.INFO : FitStatus.NEEDS_VERIFICATION;
        String reason = awardedNames.isEmpty() ? "" : Messages.get("prog.reason", names(awardedNames));
        program = new EvaluationResult(status, label, facts, reason, String.join("\n", steps));
    }

    static final int MAX_LISTED = 6; // programs listed one by one on the details page

    /** "A", "A and B", or "A, B, C and 4 more". */
    private static String names(List<String> list) {
        if (list.size() == 1) return list.get(0);
        if (list.size() <= 3) return String.join(", ", list.subList(0, list.size() - 1)) + " " + Messages.get("and") + " " + list.get(list.size() - 1);
        return String.join(", ", list.subList(0, 3)) + " " + Messages.get("prog.andMore", list.size() - 3);
    }

    private static String count(Integer n) { return n == null ? Messages.get("notReported") : String.valueOf(n); }

    public boolean hasProgramEvidence() { return programPriority == 0; }

    public int mismatches() {
        int n = 0;
        for (EvaluationResult r : new EvaluationResult[]{academic, financial, social})
            if (r.status == FitStatus.MISMATCH) n++;
        return n;
    }

    /** Positive reasons from every lens, for the "Why explore" box. */
    public List<String> reasons() {
        List<String> out = new ArrayList<>();
        for (EvaluationResult r : new EvaluationResult[]{program, academic, financial, social})
            for (String line : r.reason.split("\n")) if (!line.isBlank()) out.add(line);
        return out;
    }
}
