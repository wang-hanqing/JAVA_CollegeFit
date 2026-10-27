import java.nio.file.*;
import java.util.*;

/**
 * Phase 1 success check: fictional profiles must return the expected statuses, facts and next steps.
 * Uses data/processed/colleges_sample.csv, whose colleges were picked to cover each rule.
 * Run from the CollegeFit root folder:  java -cp out ScenarioTests
 */
public class ScenarioTests {
    private static int passed = 0, failed = 0;
    private static final Map<String, College> byName = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Path data = Paths.get("data", "processed");
        for (College c : CollegeDataLoader.load(data.resolve("colleges_sample.csv"), data.resolve("programs_sample.csv")))
            byName.put(c.name, c);
        List<College> sample = new ArrayList<>(byName.values());
        List<Program> programs = CollegeDataLoader.loadProgramList(data.resolve("programs_sample.csv"), sample);
        check("sample has 30 colleges", byName.size() == 30);
        check("program list is read from the data", programs.size() > 50);
        check("program short name", new Program("5138", "Registered Nursing, Nursing Administration.", 1).shortName().equals("Registered Nursing"));

        LensEvaluator academic = new AcademicEvaluator(), financial = new FinancialEvaluator(), social = new SocialEvaluator();
        RecommendationEngine engine = new RecommendationEngine();

        // ---------- Profile 1: Amina ----------
        StudentProfile amina = StudentProfile.amina();
        College bronx = college("CUNY Bronx Community College"), lehman = college("CUNY Lehman College"), tulsa = college("University of Tulsa");

        EvaluationResult r = academic.evaluate(amina, bronx);
        check("Bronx CC: open admission is context (INFO), not a match", r.status == FitStatus.INFO && r.label.equals("Open admission"));
        r = financial.evaluate(amina, bronx);
        check("Bronx CC: verify costs with a residency step", r.status == FitStatus.NEEDS_VERIFICATION && r.nextStep.contains("in-state"));
        check("Bronx CC: in-state and out-of-state tuition listed separately",
            hasFact(r, "In-state tuition and fees") && hasFact(r, "Out-of-state tuition and fees"));
        r = social.evaluate(amina, bronx);
        check("Bronx CC: 2 of 2 social preferences", r.status == FitStatus.MATCH && r.label.equals("2 of 2 matched"));

        r = academic.evaluate(amina, lehman);
        check("Lehman: admission rate 57%, test policy 5, no SAT row without scores", r.status == FitStatus.INFO
            && r.label.equals("Admission rate 57%") && r.summary().contains("Considered, not required") && !r.summary().contains("Your SAT"));
        CollegeRecommendation rec = new CollegeRecommendation(amina, lehman, r, financial.evaluate(amina, lehman), social.evaluate(amina, lehman));
        check("Lehman: nursing awarded with counts and a license step", rec.programPriority == 0
            && rec.program.label.equals("Registered Nursing: awarded") && rec.program.summary().contains("145 degrees awarded (historical)")
            && rec.program.nextStep.contains("nursing license"));

        r = social.evaluate(amina, tulsa);
        check("Tulsa: 1 of 2 matched, all three items listed", r.status == FitStatus.MISMATCH && r.label.equals("1 of 2 matched") && r.facts.size() == 3);
        r = financial.evaluate(amina, tulsa);
        check("Tulsa: $5,578 net price for income under $30k", r.summary().contains("$5,578"));

        // ---------- Profile 2: NJ student with SAT scores ----------
        StudentProfile nj = new StudentProfile();
        nj.setSatScores(770, 790); // Harvard range: reading 740-780, math 770-800
        nj.setHomeState("NJ");
        nj.setLocationScope(StudentProfile.LocationScope.ANY_STATE);
        r = academic.evaluate(nj, college("Harvard University"));
        check("Harvard: highly selective even with strong scores", r.status == FitStatus.NEEDS_VERIFICATION
            && r.label.startsWith("Admission rate") && r.label.contains("highly selective") && r.summary().contains("Required"));
        r = financial.evaluate(nj, college("CUNY Hunter College"));
        check("Hunter for NJ student: out-of-state estimate, net price not shown",
            hasFact(r, "Estimated cost at out-of-state rates") && r.summary().contains("Not shown"));

        College ranged = null;
        for (College c : sample)
            if (c.reading25 != null && c.math25 != null && c.reading75 != null && c.math75 != null
                && c.admissionRate != null && c.admissionRate >= 0.15) { ranged = c; break; }
        StudentProfile mixed = new StudentProfile();
        mixed.setSatScores(ranged.reading25, Math.max(200, ranged.math25 - 10));
        r = academic.evaluate(mixed, ranged);
        check("SAT compared by section: mixed result needs checking", r.label.equals("SAT mixed") && r.status == FitStatus.NEEDS_VERIFICATION);
        mixed.setSatScores(Math.min(800, ranged.reading75 + 10), Math.min(800, ranged.math75 + 10));
        r = academic.evaluate(mixed, ranged);
        check("SAT above range is context, never a match", r.label.equals("SAT above range") && r.status == FitStatus.INFO);

        // ---------- Profile 3: data problems ----------
        StudentProfile ca = new StudentProfile();
        ca.setHomeState("CA");
        ca.setIncomeBracket(StudentProfile.IncomeBracket.UNDER_30K);
        r = financial.evaluate(ca, college("Moorpark College"));
        check("Moorpark: negative net price shown as reported, not a refund", r.summary().contains("-$") && r.summary().contains("not a refund"));
        r = academic.evaluate(ca, college("Belanger School of Nursing"));
        check("Belanger: 0% admission rate flagged as not usable", r.summary().contains("not usable"));
        r = financial.evaluate(ca, college("PENSACOLA CHRISTIAN COLLEGE"));
        check("Pensacola: insufficient information", r.status == FitStatus.UNKNOWN);
        r = financial.evaluate(new StudentProfile(), tulsa);
        check("Income not given: overall average net price (NPT4) shown", hasFact(r, "Average paid after grants, all incomes"));

        // ---------- Program evidence: three states, and a missing net price calculator ----------
        Map<String, Integer[]> counts = new HashMap<>();
        counts.put("5138:3", new Integer[]{5, 0});
        counts.put("1107:3", new Integer[]{0, 0});
        counts.put("4506:3", new Integer[]{null, null});
        College test = new College("999999", "Test College", "Town", "NY", "https://test.edu", "", 2, false, 0.6, 5,
            null, null, null, null, 30000, 20000, 20000, 15000, new EnumMap<>(StudentProfile.IncomeBracket.class),
            StudentProfile.CampusSetting.URBAN, 3000, null, null, null, 0, null, 0, null, counts, Map.of());
        check("Evidence: positive count = awarded", test.evidence("5138", 3) == College.ProgramEvidence.AWARDED);
        check("Evidence: 0 in both years = zero awards", test.evidence("1107", 3) == College.ProgramEvidence.ZERO_AWARDS);
        check("Evidence: missing counts = not confirmed", test.evidence("4506", 3) == College.ProgramEvidence.NOT_CONFIRMED);
        check("Evidence: no record = not confirmed", test.evidence("2601", 3) == College.ProgramEvidence.NOT_CONFIRMED);
        r = financial.evaluate(new StudentProfile(), test);
        check("No net price calculator: contact the financial aid office", r.nextStep.contains("financial aid office"));

        // ---------- Profile 4: budget comparison ----------
        StudentProfile budget = new StudentProfile();
        budget.setAnnualBudget(10000);
        budget.setAidEligibility(StudentProfile.AidEligibility.YES);
        budget.setIncomeBracket(StudentProfile.IncomeBracket.UNDER_30K);
        EvaluationResult within = financial.evaluate(budget, tulsa);
        check("Tulsa: reference cost within budget is INFO, never an affordability match",
            within.status == FitStatus.INFO && within.label.equals("Reference cost within budget"));
        budget.setAidEligibility(StudentProfile.AidEligibility.NOT_SURE);
        check("Tulsa above budget before aid when aid is unsure", financial.evaluate(budget, tulsa).label.equals("Above budget before aid"));

        // ---------- Profile 5: everything "not sure" ----------
        StudentProfile unsure = new StudentProfile();
        List<CollegeRecommendation> ranked = engine.recommend(unsure, sample);
        check("All not sure: still returns colleges", !ranked.isEmpty());
        check("All not sure: social unknown, never mismatch", ranked.get(0).social.status == FitStatus.UNKNOWN);
        check("All not sure: 7 next steps", new NextStepGenerator().profileSteps(unsure).size() == 7);

        // ---------- Multi-select ----------
        StudentProfile multi = new StudentProfile();
        multi.setPreferredStates(List.of("NY", "OK"));
        multi.setSettings(List.of(StudentProfile.CampusSetting.URBAN, StudentProfile.CampusSetting.SUBURBAN));
        check("Two preferred states: Tulsa (OK) matches", social.evaluate(multi, tulsa).label.equals("2 of 2 matched"));
        List<Program> many = new ArrayList<>(programs.subList(0, 10));
        many.add(amina.getPrograms().get(0));
        multi.setPrograms(many);
        check("No program limit: 11 programs accepted", multi.getPrograms().size() == 11);
        rec = engine.recommendAll(multi, List.of(lehman)).get(0);
        check("Many programs: label counts awarded programs", rec.program.label.matches("\\d+ of 11 programs awarded"));
        check("Many programs: details stay short", rec.program.facts.size() <= CollegeRecommendation.MAX_LISTED + 2);

        // ---------- Ranking ----------
        ranked = engine.recommend(amina, sample);
        check("Amina: first result has program evidence", ranked.get(0).hasProgramEvidence());
        check("Amina: chosen state is the scope (only New York colleges)", ranked.stream().allMatch(x -> x.college.state.equals("NY")));
        boolean byDegrees = true;
        for (int i = 1; i < ranked.size(); i++)
            if (ranked.get(i).social.matched == ranked.get(i - 1).social.matched && ranked.get(i).recentDegrees > ranked.get(i - 1).recentDegrees)
                byDegrees = false;
        check("Default order: larger recent programs first (after preferences)", byDegrees);
        Map<College.Type, List<CollegeRecommendation>> groups = RecommendationEngine.groups(ranked, amina.getPathway());
        check("Groups: Bronx CC is two-year, Lehman is four-year public",
            groups.get(College.Type.COMMUNITY_COLLEGE).stream().anyMatch(x -> x.college == bronx)
            && groups.get(College.Type.PUBLIC_FOUR_YEAR).stream().anyMatch(x -> x.college == lehman));
        CollegeRecommendation lr = engine.evaluate(amina, lehman);
        check("Card line: degrees awarded, labeled historical", lr.programLine(amina).equals("Registered Nursing: 145 bachelor's degrees awarded (historical)"));
        check("Card line: Lehman costs in plain words", lr.financialLine(amina).equals("Cost before aid $14,121/yr. Average paid after grants $1,589/yr*"));
        check("Reason is built from data", lr.cardWhy().startsWith("145 degrees awarded in Registered Nursing (bachelor's, historical)."));
        check("Completion rate loaded (Lehman 51%, 6 years)", Math.round(lehman.completionRate * 100) == 51 && lehman.completionYears == 6);
        Map<RecommendationEngine.PathwayKind, int[]> summary = RecommendationEngine.pathwaySummary(sample, Set.of("NY"));
        check("Pathway facts: counts per college type", summary.get(RecommendationEngine.PathwayKind.TWO_YEAR)[0] >= 1);
        check("Private two-year colleges go to the private group", sample.stream().noneMatch(x -> !x.isPublic() && x.type() == College.Type.COMMUNITY_COLLEGE));
        Map<String, Integer> nyCounts = RecommendationEngine.programCounts(sample, Set.of("NY")), allCounts = RecommendationEngine.programCounts(sample, Set.of());
        check("Program counts follow the chosen state", nyCounts.getOrDefault("5138", 0) < allCounts.getOrDefault("5138", 0));
        StudentProfile econ = new StudentProfile();
        for (Program p : programs) if ("4506".equals(p.cipCode)) econ.setPrograms(List.of(p));
        econ.setLocationScope(StudentProfile.LocationScope.ANY_STATE);
        econ.setPriorities(List.of(StudentProfile.Priority.ADMISSION));
        List<CollegeRecommendation> eco = engine.recommend(econ, sample);
        check("Priority admission: open admission and higher rates first", ordered(eco, StudentProfile.Priority.ADMISSION, econ));

        // ---------- v0.4: priorities, comparability, trade-offs ----------
        StudentProfile cost = StudentProfile.amina();
        cost.setPriorities(List.of(StudentProfile.Priority.COSTS, StudentProfile.Priority.GRADUATION));
        List<CollegeRecommendation> byCost = engine.recommend(cost, sample);
        boolean groupsOrdered = true;
        for (List<CollegeRecommendation> g : RecommendationEngine.groups(byCost, cost.getPathway()).values())
            if (!ordered(g, StudentProfile.Priority.COSTS, cost)) groupsOrdered = false;
        check("Priority cost: lower cost after grants first in each group, missing last", groupsOrdered);
        boolean tooMany = false;
        try { cost.setPriorities(List.of(StudentProfile.Priority.values())); } catch (IllegalArgumentException e) { tooMany = true; }
        check("At most 3 priorities", tooMany);
        StudentProfile njHome = StudentProfile.amina();
        njHome.setHomeState("NJ");
        check("Out-of-state public cost is not comparable", RecommendationEngine.comparableCost(njHome, college("CUNY Hunter College")) == null);
        check("Private two-year completion is not compared with four-year", sample.stream()
            .filter(x -> !x.isPublic() && x.isTwoYear()).allMatch(x -> RecommendationEngine.comparableCompletion(x) == null));

        Comparison cmp = new Comparison(cost, engine.recommendAll(cost, List.of(bronx, college("CUNY Hunter College"), lehman)));
        check("Compare: associate and bachelor's are flagged as different paths", cmp.differentDegrees()
            && cmp.summary().get(0).contains("different degrees"));
        String all = String.join(" ", cmp.summary());
        check("Compare: completion compared only on the same measure", all.contains("Hunter reports a higher six-year completion rate than Lehman")
            && all.contains("measured differently") && !all.contains("Bronx CC reports a higher"));
        check("Compare: never says 'better'", !all.toLowerCase().contains("better"));
        check("Compare: closing reminder", cmp.summary().get(cmp.summary().size() - 1).contains("net price calculator"));

        // ---------- Shortlist ----------
        Shortlist list = new Shortlist();
        list.add(bronx); list.add(lehman); list.add(bronx);
        check("Shortlist ignores duplicates", list.colleges().size() == 2);
        Path tmp = Files.createTempFile("collegefit", ".csv");
        list.exportCsv(tmp, amina, engine);
        List<String> lines = Files.readAllLines(tmp);
        check("Export: header, 2 colleges and a note", lines.size() == 4);
        check("Export keeps commas inside quotes", CollegeDataLoader.readCsvRecord(
            new java.io.BufferedReader(new java.io.StringReader(lines.get(1)))).size() == 11);
        Files.delete(tmp);
        list.clear();
        check("Start over clears the list", list.colleges().isEmpty());

        // Whole fields: "All Engineering programs" covers every 14xx program
        Program engineering = null;
        for (Program p : programs) if ("14".equals(p.cipCode)) engineering = p;
        check("Program list offers whole fields such as Engineering", engineering != null
            && engineering.isFamily() && engineering.shortName().equals("Engineering (all)"));
        if (engineering != null) {
            College harvard = college("Harvard University");
            check("Engineering field: evidence from any 14xx program", harvard.evidence("14", 3) == College.ProgramEvidence.AWARDED
                && harvard.familyProgramsAwarded("14", 3) >= 1);
            StudentProfile eng = new StudentProfile();
            eng.setPrograms(List.of(engineering));
            CollegeRecommendation er = new RecommendationEngine().recommendAll(eng, List.of(harvard)).get(0);
            check("Engineering field: card label and field count", er.program.label.startsWith("Engineering:")
                && er.program.facts.get(0).value.contains("program"));
        }

        // Review fixes (trust and accuracy)
        StudentProfile allUnsure = new StudentProfile();
        List<CollegeRecommendation> everyone = new RecommendationEngine().recommend(allUnsure, new ArrayList<>(byName.values()));
        boolean pensacolaKept = false;
        for (CollegeRecommendation x : everyone) if (x.college.name.equals("PENSACOLA CHRISTIAN COLLEGE")) pensacolaKept = true;
        check("Missing award data does not remove a college", pensacolaKept);
        StudentProfile assocOnly = new StudentProfile();
        assocOnly.setPathway(StudentProfile.Pathway.ASSOCIATE);
        check("Pathway filter: a bachelor's-only college is left out for an associate pathway",
            !RecommendationEngine.offersPathway(StudentProfile.Pathway.ASSOCIATE, college("CUNY Lehman College")));
        boolean anyFinancialMatch = false;
        for (CollegeRecommendation x : everyone) if (x.financial.status == FitStatus.MATCH) anyFinancialMatch = true;
        check("Financial lens never returns MATCH", !anyFinancialMatch);
        StudentProfile focused = amina.withPrograms(List.of());
        check("Focus copy keeps answers but changes programs", !focused.hasPrograms()
            && focused.getPreferredStates().equals(amina.getPreferredStates()) && focused.getIncomeBracket() == amina.getIncomeBracket());

        // ---------- added data: program earnings, transfer rate, students per faculty ----------
        CollegeRecommendation lrec = engine.evaluate(amina, lehman);
        check("Program earnings shown when reported (Lehman nursing)", lrec.program.summary().contains("$101,531")
            && lrec.program.summary().contains("Not a measure of program quality"));
        check("Students per faculty and transfer rate loaded (Lehman)", lehman.studentFacultyRatio == 15
            && Math.round(lehman.transferRate * 100) == 21 && lehman.transferYears == 6);
        check("Bronx CC transfer rate uses the two-year measure", bronx.transferYears == 3);
        // "My area of study": relevance first, not program size
        StudentProfile two = new StudentProfile();
        Program nursing = null, biology = null;
        for (Program p : programs) { if ("5138".equals(p.cipCode)) nursing = p; if ("26".equals(p.cipCode)) biology = p; }
        two.setPrograms(List.of(nursing, biology));
        two.setPriorities(List.of(StudentProfile.Priority.STUDY));
        List<CollegeRecommendation> rel = engine.recommend(two, sample);
        boolean relevanceFirst = true;
        for (int i = 1; i < rel.size(); i++)
            if (rel.get(i).programPriority == rel.get(i - 1).programPriority && rel.get(i).programsAwarded > rel.get(i - 1).programsAwarded) relevanceFirst = false;
        check("Area of study orders by relevance (programs covered) before program size", relevanceFirst);
        List<CollegeRecommendation> pair = engine.recommendAll(cost, List.of(college("CUNY Hunter College"), lehman));
        Comparison cmp2 = new Comparison(cost, pair);
        check("No 'more graduates' marker in Compare", cmp2.marker(pair.get(0), Comparison.Row.STUDY) == null && cmp2.marker(pair.get(1), Comparison.Row.STUDY) == null);

        // Earnings for a whole field: the largest programs with reported earnings
        StudentProfile engField = new StudentProfile();
        for (Program p : programs) if ("14".equals(p.cipCode)) engField.setPrograms(List.of(p));
        engField.setPathway(StudentProfile.Pathway.BACHELOR);
        CollegeRecommendation hv = engine.evaluate(engField, college("Harvard University"));
        check("Whole field: program earnings listed when reported", !college("Harvard University").fieldEarnings("14", 3, 3).isEmpty() == hv.program.summary().contains("Median earnings 1 year after"));

        // ---------- "Not sure" path: interests -> careers -> fields (O*NET + CIP-SOC crosswalk) ----------
        InterestLinks interestLinks = InterestLinks.load(CollegeDataLoader.file(data.resolve("interest_links.csv")));
        check("Interest links are read from the data", !interestLinks.isEmpty());
        boolean everyType = true;
        for (InterestLinks.Interest t : InterestLinks.Interest.values())
            if (interestLinks.fields(EnumSet.of(t), sample, Set.of()).isEmpty()) everyType = false;
        check("Each of the six interests reaches at least one field", everyType);
        boolean zonesOk = true, noPostsecondary = true;
        for (InterestLinks.CareerLink k : interestLinks.allCareers()) {
            if (k.jobZone < 3 || k.jobZone > 5) zonesOk = false;
            if (k.soc.startsWith("25-1")) noPostsecondary = false;
        }
        check("Careers need training, a degree or graduate school (Job Zones 3-5)", zonesOk);
        check("Postsecondary teachers are left out (they link to almost every field)", noPostsecondary);
        boolean noLiberal = true;
        for (InterestLinks.Interest t : InterestLinks.Interest.values()) if (interestLinks.links(t, "24")) noLiberal = false;
        check("Liberal Arts has no career links, so it is shown separately", noLiberal);

        List<InterestLinks.FieldMatch> socialFields = interestLinks.fields(EnumSet.of(InterestLinks.Interest.S), sample, Set.of());
        InterestLinks.FieldMatch healthField = null;
        for (InterestLinks.FieldMatch f : socialFields) if (f.family.equals("51")) healthField = f;
        check("Helping people: Health Professions, Registered Nurses first", healthField != null
            && healthField.careers.get(0).title.equals("Registered Nurses") && healthField.careers.get(0).jobZone == 4);
        boolean sortedOk = true, countsMatch = true, programsOk = true;
        for (int i = 0; i < socialFields.size(); i++) {
            InterestLinks.FieldMatch f = socialFields.get(i);
            if (i > 0 && f.linkedCareers > socialFields.get(i - 1).linkedCareers) sortedOk = false;
            if (f.colleges != RecommendationEngine.fieldCount(sample, Set.of(), f.family)) countsMatch = false;
            if (f.careers.size() > 3 || f.programs.size() > 3) programsOk = false;
            for (int j = 1; j < f.programs.size(); j++)
                if (Integer.parseInt(f.programs.get(j)[1]) > Integer.parseInt(f.programs.get(j - 1)[1])) programsOk = false;
        }
        check("Fields: more linked careers first", sortedOk);
        check("Field counts match the college counts used elsewhere", countsMatch);
        check("At most 3 careers and 3 programs per field, most colleges first", programsOk);
        int businessE = 0, businessSE = 0;
        for (InterestLinks.FieldMatch f : interestLinks.fields(EnumSet.of(InterestLinks.Interest.E), sample, Set.of())) if (f.family.equals("52")) businessE = f.linkedCareers;
        for (InterestLinks.FieldMatch f : interestLinks.fields(EnumSet.of(InterestLinks.Interest.S, InterestLinks.Interest.E), sample, Set.of())) if (f.family.equals("52")) businessSE = f.linkedCareers;
        check("Two interests combine their careers", businessE > 0 && businessSE >= businessE);
        check("A state with no colleges in scope shows no fields", interestLinks.fields(EnumSet.of(InterestLinks.Interest.S), sample, Set.of("ZZ")).isEmpty());

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static boolean hasFact(EvaluationResult r, String label) {
        for (Fact f : r.facts) if (f.label.equals(label)) return true;
        return false;
    }

    /** True if the list follows one priority's sort key (ties allowed, missing values last). */
    private static boolean ordered(List<CollegeRecommendation> list, StudentProfile.Priority p, StudentProfile s) {
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i).programPriority != list.get(i - 1).programPriority) continue;
            Integer a = RecommendationEngine.sortKey(p, s, list.get(i - 1)), b = RecommendationEngine.sortKey(p, s, list.get(i));
            if (a == null && b != null) return false;
            if (a != null && b != null && b < a) return false;
        }
        return true;
    }

    private static College college(String name) {
        College c = byName.get(name);
        if (c == null) throw new IllegalStateException("Not in sample: " + name);
        return c;
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
    }
}
