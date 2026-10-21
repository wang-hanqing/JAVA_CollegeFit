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
    /** Degrees awarded in 2022-23 in her chosen programs (or in all programs if none chosen), at her pathway levels. */
    public final int recentDegrees;
    static final int FIELD_EARNINGS_SHOWN = 3; // for a whole field, show the 3 largest programs with reported earnings
    /** Which levels have awards in her programs: used for "associate", "bachelor's" or both. */
    public final boolean associateAwarded, bachelorAwarded;
    /** A nursing bachelor's program may be RN-to-BSN only, so the card shows a license check. */
    public final boolean nursingBachelor;

    public CollegeRecommendation(StudentProfile s, College c,
                                 EvaluationResult academic, EvaluationResult financial, EvaluationResult social) {
        this.college = c;
        this.academic = academic;
        this.financial = financial;
        this.social = social;

        int[] levels = RecommendationEngine.levels(s.getPathway());
        if (!s.hasPrograms()) {
            programPriority = 0;
            programsAwarded = 0;
            int d = 0;
            for (int level : levels) d += c.recentDegreesAll(level);
            recentDegrees = d;
            associateAwarded = c.offersLevel(2) && contains(levels, 2);
            bachelorAwarded = c.offersLevel(3) && contains(levels, 3);
            nursingBachelor = false;
            program = new EvaluationResult(FitStatus.UNKNOWN, Messages.get("prog.label.none"),
                List.of(new Fact(Messages.get("lens.program"), Messages.get("prog.none"), Messages.get("prog.none.note"), null)),
                "", Messages.get("prog.next.none"));
            return;
        }

        List<Fact> facts = new ArrayList<>();
        List<String> steps = new ArrayList<>();
        List<String> awardedNames = new ArrayList<>(), otherNames = new ArrayList<>();
        boolean nursing = false, anyAssociate = false, anyBachelor = false;
        int awarded = 0, degrees = 0;
        java.util.Set<String> families = new java.util.HashSet<>();
        for (Program p : s.getPrograms()) if (p.isFamily()) families.add(p.cipCode);
        String singleLabel = "";
        for (Program p : s.getPrograms()) {
            College.ProgramEvidence best = College.ProgramEvidence.NOT_CONFIRMED;
            String nm = p.isFamily() ? CipFamilies.name(p.cipCode) : p.shortName(); // "Agriculture", not "Agriculture (all)"
            List<String> parts = new ArrayList<>();
            boolean bachelorAwarded = false;
            List<Integer> awardedLevels = new ArrayList<>();
            for (int level : RecommendationEngine.levels(s.getPathway())) {
                College.ProgramEvidence e = c.evidence(p.cipCode, level);
                if (e == College.ProgramEvidence.AWARDED) {
                    String levelName = Messages.get("prog.level." + level);
                    int graduates = c.recentDegrees(p.cipCode, level);
                    if (p.isFamily()) {
                        int k = c.familyProgramsAwarded(p.cipCode, level);
                        parts.add(Messages.get(k == 1 ? "prog.part.family.one" : "prog.part.family", levelName, k, Messages.number(graduates)));
                    } else {
                        parts.add(Messages.get("prog.part.awarded", levelName, Messages.number(graduates)));
                    }
                    if (level == 2) anyAssociate = true; else { anyBachelor = true; bachelorAwarded = true; }
                    awardedLevels.add(level);
                    // a program inside a chosen field is already counted by the field
                    if (p.isFamily() || !families.contains(p.cipCode.substring(0, 2))) degrees += graduates;
                }
                if (e.ordinal() < best.ordinal()) best = e; // AWARDED < ZERO_AWARDS < NOT_CONFIRMED
            }
            boolean isAwarded = best == College.ProgramEvidence.AWARDED;
            String value = isAwarded ? String.join(". ", parts) : Messages.get("prog.value." + best.name());
            String note = isAwarded ? "" : Messages.get("prog.note." + best.name());
            // With many programs, list the awarded ones in detail and summarize the rest
            if (s.getPrograms().size() <= MAX_LISTED || (isAwarded && awardedNames.size() < MAX_LISTED))
            {
                facts.add(new Fact(nm, value, note, isAwarded));
                // Program-level earnings when reported. For a whole field, the largest programs with reported earnings.
                // Skipped when many programs are chosen, so the details stay short.
                if (s.getPrograms().size() <= MAX_LISTED) for (int level : awardedLevels) {
                    String lv = Messages.get("prog.level." + level).toLowerCase();
                    if (!p.isFamily()) {
                        Integer pay = c.programEarnings(p.cipCode, level);
                        if (pay != null) facts.add(new Fact(Messages.get("prog.earnings", nm, lv), Messages.money(pay), Messages.get("prog.earnings.note"), null));
                    } else {
                        List<String[]> rows = c.fieldEarnings(p.cipCode, level, FIELD_EARNINGS_SHOWN);
                        for (int i = 0; i < rows.size(); i++)
                            facts.add(new Fact(Messages.get("prog.earnings", Program.nameOf(rows.get(i)[0]), lv),
                                Messages.money(Integer.valueOf(rows.get(i)[1])), i == rows.size() - 1 ? Messages.get("prog.earnings.note") : "", null));
                    }
                }
            }
            if (isAwarded) {
                awarded++;
                awardedNames.add(nm);
                // CIP 5138 also covers RN-to-BSN programs for nurses who are already licensed.
                if ("5138".equals(p.cipCode) && bachelorAwarded) nursing = true;
            } else {
                otherNames.add(nm);
                if (s.getPrograms().size() <= MAX_LISTED)
                    steps.add(Messages.get(best == College.ProgramEvidence.ZERO_AWARDS ? "prog.next.zero" : "prog.next.confirm", nm));
            }
            singleLabel = Messages.get("prog.label.one." + best.name(), nm);
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
        if (nursing) steps.add(1, Messages.get("prog.next.license"));

        programPriority = awarded > 0 ? 0 : 1;
        programsAwarded = awarded;
        recentDegrees = degrees;
        associateAwarded = anyAssociate;
        bachelorAwarded = anyBachelor;
        nursingBachelor = nursing;
        String label = total == 1 ? singleLabel : Messages.get("prog.label.many", awarded, total);
        FitStatus status = awarded > 0 ? FitStatus.INFO : FitStatus.NEEDS_VERIFICATION;
        String reason = awardedNames.isEmpty() ? ""
            : awardedNames.size() == 1 && degrees > 0 ? Messages.get("prog.reason.count", Messages.number(degrees), awardedNames.get(0), levelText())
            : Messages.get("prog.reason", names(awardedNames));
        program = new EvaluationResult(status, label, facts, reason, String.join("\n", steps));
    }

    static final int MAX_LISTED = 6; // programs listed one by one on the details page

    /** "A", "A and B", or "A, B, C and 4 more". */
    private static String names(List<String> list) {
        if (list.size() == 1) return list.get(0);
        if (list.size() <= 3) return String.join(", ", list.subList(0, list.size() - 1)) + " " + Messages.get("and") + " " + list.get(list.size() - 1);
        return String.join(", ", list.subList(0, 3)) + " " + Messages.get("prog.andMore", list.size() - 3);
    }


    public boolean hasProgramEvidence() { return programPriority == 0; }

    private static boolean contains(int[] a, int v) { for (int x : a) if (x == v) return true; return false; }

    /** "associate", "bachelor's" or "associate and bachelor's". */
    public String levelText() {
        if (associateAwarded && bachelorAwarded) return Messages.get("level.both");
        return Messages.get(bachelorAwarded ? "level.bachelor" : "level.associate");
    }

    // ---------- short lines for result cards ----------

    /** e.g. "Agriculture: 25 graduates last year (associate)". Graduates = degrees awarded in the latest year. */
    public String programLine(StudentProfile s) {
        if (!s.hasPrograms()) return Messages.get("card.program.all", Messages.number(recentDegrees));
        if (programsAwarded == 0) return program.label;
        if (s.getPrograms().size() == 1) {
            String name = s.getPrograms().get(0).isFamily() ? CipFamilies.name(s.getPrograms().get(0).cipCode) : s.getPrograms().get(0).shortName();
            return recentDegrees > 0 ? Messages.get("card.program.one", name, Messages.number(recentDegrees), levelText())
                : Messages.get("card.program.awarded", name, levelText());
        }
        return Messages.get("card.program.many", programsAwarded, s.getPrograms().size(), Messages.number(recentDegrees));
    }

    /** The published cost that applies to her, and the average net price with a footnote mark. */
    public String financialLine(StudentProfile s) {
        boolean oos = college.isPublic() && s.hasHomeState() && !s.getHomeState().equals(college.state);
        Integer published = oos ? college.outOfStateCostEstimate() : college.costOfAttendance;
        Integer avg = oos ? null : s.getIncomeBracket().isKnown() ? college.netPrice(s.getIncomeBracket()) : college.netPriceAll;
        if (published == null && avg == null) return Messages.get("card.cost.none");
        if (published == null) return Messages.get("card.cost.avgOnly", Messages.money(avg));
        if (avg == null) return Messages.get(oos ? "card.cost.oos" : "card.cost.published", Messages.money(published));
        return Messages.get("card.cost.both", Messages.money(published), Messages.money(avg));
        // "Cost before aid" = published cost of attendance; "average paid after grants" = historical average net price
    }

    /** e.g. "NY ✓  City ✓  Large ✗" as HTML; only preferences she stated. */
    public String socialHtml() {
        StringBuilder sb = new StringBuilder();
        for (Fact f : social.facts) {
            if (f.match == null) continue;
            String v = f.label.equals(Messages.get("soc.size")) ? Messages.get("card.size." + sizeKey()) : f.value;
            sb.append(Ui.escape(v)).append(' ')
              .append(f.match ? "<span style='color:#1f9d74'>&#10003;</span>" : "<span style='color:#b4475a'>&#10007;</span>")
              .append("&nbsp;&nbsp; ");
        }
        return sb.length() == 0 ? Ui.escape(Messages.get("card.social.none")) : sb.toString().trim();
    }

    private String sizeKey() {
        Integer u = college.undergraduates;
        if (u == null) return "UNKNOWN";
        return u <= SocialEvaluator.SMALL_MAX ? "SMALL" : u <= SocialEvaluator.MEDIUM_MAX ? "MEDIUM" : "LARGE";
    }

    /** Up to two data-based reasons for the card. */
    public String cardWhy() {
        List<String> r = reasons();
        if (r.isEmpty()) return "";
        return r.size() == 1 ? r.get(0) : r.get(0) + " " + r.get(1);
    }

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
