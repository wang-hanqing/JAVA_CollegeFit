import java.util.ArrayList;
import java.util.List;

/** One college as seen through all three lenses, plus the program check. */
public class CollegeRecommendation {
    public final College college;
    public final EvaluationResult academic, financial, social, program;
    public final int programPriority; // 0 = awarded at her level, 1 = no program chosen, 2 = no recent awards

    public CollegeRecommendation(StudentProfile s, College c,
                                 EvaluationResult academic, EvaluationResult financial, EvaluationResult social) {
        this.college = c;
        this.academic = academic;
        this.financial = financial;
        this.social = social;

        Program p = s.getProgram();
        if (p.isNotSure()) {
            programPriority = 1;
            program = new EvaluationResult(FitStatus.UNKNOWN, Messages.get("program.none.label"),
                Messages.get("program.none.explain"), Messages.get("program.none.next"));
            return;
        }
        List<String> levels = new ArrayList<>();
        boolean bachelorAwarded = false;
        for (int level : RecommendationEngine.levels(s.getPathway())) {
            if (c.awarded(p.cipCode, level)) {
                levels.add(Messages.get(level == 2 ? "program.level.2" : "program.level.3"));
                if (level == 3) bachelorAwarded = true;
            }
        }
        if (levels.isEmpty()) {
            programPriority = 2;
            program = new EvaluationResult(FitStatus.UNKNOWN, Messages.get("program.noAwards.label"),
                Messages.get("program.noAwards.explain", p.shortName()), Messages.get("program.noAwards.next", p.shortName()));
            return;
        }
        programPriority = 0;
        String next = Messages.get("program.verify.next", p.shortName());
        // CIP 5138 also covers RN-to-BSN programs for licensed nurses (data issue 11).
        if ("5138".equals(p.cipCode) && bachelorAwarded) next += " " + Messages.get("program.nursingLicense.next");
        program = new EvaluationResult(FitStatus.NEEDS_VERIFICATION,
            Messages.get("program.awarded.label", p.shortName(), String.join(" " + Messages.get("program.and") + " ", levels)),
            Messages.get("program.awarded.explain"), next);
    }

    public int mismatches() {
        int n = 0;
        for (EvaluationResult r : new EvaluationResult[]{academic, financial, social})
            if (r.status == FitStatus.MISMATCH) n++;
        return n;
    }
}
