import java.nio.file.*;
import java.util.*;

/**
 * Phase 1 success check: fictional profiles must return the expected statuses and next steps.
 * Uses data/processed/colleges_sample.csv, whose colleges were picked to cover each rule.
 * Run from the CollegeFit root folder:  java -cp out ScenarioTests
 */
public class ScenarioTests {
    private static int passed = 0, failed = 0;
    private static Map<String, College> byName = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Path data = Paths.get("data", "processed");
        for (College c : CollegeDataLoader.load(data.resolve("colleges_sample.csv"), data.resolve("programs_sample.csv")))
            byName.put(c.name, c);
        check("sample has 30 colleges", byName.size() == 30);
        List<Program> programs = CollegeDataLoader.loadProgramList(data.resolve("programs_sample.csv"), new ArrayList<>(byName.values()));
        check("program list is read from the data, not hard-coded", programs.size() > 50);
        check("program short name", new Program("5138", "Registered Nursing, Nursing Administration.", 1).shortName().equals("Registered Nursing"));

        LensEvaluator academic = new AcademicEvaluator();
        LensEvaluator afford = new FinancialEvaluator();
        LensEvaluator pref = new SocialEvaluator();

        // ---------- Profile 1: Amina (proposal slides 2 and 6) ----------
        StudentProfile amina = StudentProfile.amina();
        College bronx = college("CUNY Bronx Community College");
        College lehman = college("CUNY Lehman College");
        College tulsa = college("University of Tulsa");

        EvaluationResult r = academic.evaluate(amina, bronx);
        check("Bronx CC: open admission", r.status == FitStatus.MATCH && r.label.equals("Open admission"));
        r = afford.evaluate(amina, bronx);
        check("Bronx CC: verify costs, residency step", r.status == FitStatus.NEEDS_VERIFICATION && r.nextStep.contains("in-state"));
        r = pref.evaluate(amina, bronx);
        check("Bronx CC: 2 of 2 preferences", r.status == FitStatus.MATCH && r.label.equals("2 of 2 matched"));

        r = academic.evaluate(amina, lehman);
        check("Lehman: more info needed, 57%, test policy 5", r.status == FitStatus.UNKNOWN
            && r.explanation.contains("57%") && r.explanation.contains("considered, not required"));
        CollegeRecommendation rec = new CollegeRecommendation(amina, lehman, r, afford.evaluate(amina, lehman), pref.evaluate(amina, lehman));
        check("Lehman: nursing bachelor's awarded + license step", rec.programPriority == 0
            && rec.program.label.contains("bachelor's") && rec.program.nextStep.contains("nursing license"));

        r = pref.evaluate(amina, tulsa);
        check("Tulsa: 1 of 2 preferences (not NY)", r.status == FitStatus.MISMATCH && r.label.equals("1 of 2 matched"));
        r = afford.evaluate(amina, tulsa);
        check("Tulsa: shows $5,578 net price for under $30k", r.explanation.contains("$5,578"));

        // ---------- Profile 2: NJ student with high SAT, budget and aid known ----------
        StudentProfile nj = new StudentProfile();
        nj.setSatScores(770, 790); // Harvard range: reading 740-780, math 770-800
        nj.setHomeState("NJ");
        nj.setLocationScope(StudentProfile.LocationScope.ANY_STATE);
        nj.setAnnualBudget(20000);
        nj.setAidEligibility(StudentProfile.AidEligibility.YES);
        nj.setIncomeBracket(StudentProfile.IncomeBracket.FROM_75K_TO_110K);
        r = academic.evaluate(nj, college("Harvard University"));
        check("Harvard: highly selective even with strong scores", r.status == FitStatus.NEEDS_VERIFICATION
            && r.label.equals("Highly selective") && r.explanation.contains("required"));
        r = afford.evaluate(nj, college("CUNY Hunter College"));
        check("Hunter for NJ student: out-of-state cost, no net price", r.explanation.contains("out-of-state")
            && !r.explanation.contains("paid"));

        // ---------- Profile 3: data problems ----------
        StudentProfile ca = new StudentProfile();
        ca.setHomeState("CA");
        ca.setIncomeBracket(StudentProfile.IncomeBracket.UNDER_30K);
        r = afford.evaluate(ca, college("Moorpark College"));
        check("Moorpark: negative net price shown as $0 or less", r.explanation.contains("$0 or less on average"));
        r = academic.evaluate(ca, college("Belanger School of Nursing"));
        check("Belanger: 0% admission rate treated as suspicious", r.explanation.contains("looks incorrect")
            && r.status != FitStatus.NEEDS_VERIFICATION);
        r = afford.evaluate(ca, college("PENSACOLA CHRISTIAN COLLEGE"));
        check("Pensacola: insufficient information", r.status == FitStatus.UNKNOWN);

        // ---------- Profile 4: budget comparison ----------
        StudentProfile budget = new StudentProfile();
        budget.setAnnualBudget(10000);
        budget.setAidEligibility(StudentProfile.AidEligibility.YES);
        budget.setIncomeBracket(StudentProfile.IncomeBracket.UNDER_30K);
        r = afford.evaluate(budget, tulsa);
        check("Tulsa within $10k budget using average net price", r.status == FitStatus.MATCH);
        budget.setAidEligibility(StudentProfile.AidEligibility.NOT_SURE);
        r = afford.evaluate(budget, tulsa);
        check("Tulsa above budget before aid when aid is unsure", r.status == FitStatus.NEEDS_VERIFICATION
            && r.label.equals("Above budget before aid"));

        // ---------- Profile 5: everything "not sure" ----------
        StudentProfile unsure = new StudentProfile();
        List<CollegeRecommendation> ranked = new RecommendationEngine().recommend(unsure, new ArrayList<>(byName.values()));
        check("All not sure: still returns colleges", !ranked.isEmpty());
        check("All not sure: preferences unknown, never mismatch", ranked.get(0).social.status == FitStatus.UNKNOWN);
        check("All not sure: 7 next steps", new NextStepGenerator().profileSteps(unsure).size() == 7);

        // ---------- Engine: ranking and discovery for Amina ----------
        ranked = new RecommendationEngine().recommend(amina, new ArrayList<>(byName.values()));
        check("Amina: first result awards nursing", ranked.get(0).programPriority == 0);
        List<CollegeRecommendation> elsewhere = new RecommendationEngine().worthALookElsewhere(amina, ranked, 4);
        check("Amina: discovery list has no New York colleges", elsewhere.stream().noneMatch(x -> x.college.state.equals("NY")));
        // Best fit puts easier admission ahead of highly selective colleges when everything else ties
        StudentProfile econ = new StudentProfile();
        Program economics = null;
        for (Program p : programs) if ("4506".equals(p.cipCode)) economics = p;
        if (economics != null) econ.setProgram(economics);
        econ.setLocationScope(StudentProfile.LocationScope.ANY_STATE);
        List<CollegeRecommendation> eco = new RecommendationEngine().recommend(econ, new ArrayList<>(byName.values()));
        // Best fit is neutral: it does not use admission rate or price
        CollegeRecommendation first = eco.get(0), second = eco.get(1);
        boolean sameTier = first.programPriority == second.programPriority && first.mismatches() == second.mismatches()
            && first.social.matched == second.social.matched;
        check("Best fit: ties are broken by name, not by selectivity",
            !sameTier || first.college.name.compareToIgnoreCase(second.college.name) <= 0);
        eco.sort(RecommendationEngine.comparator(RecommendationEngine.SortOrder.EASIER_ADMISSION, econ));
        boolean ordered = true;
        for (int i = 1; i < eco.size(); i++)
            if (eco.get(i).programPriority == eco.get(i - 1).programPriority
                && RecommendationEngine.admissionAccess(eco.get(i).college) > RecommendationEngine.admissionAccess(eco.get(i - 1).college))
                ordered = false;
        check("Easier admission: open admission and higher rates come first", ordered);
        boolean harvardLate = true;
        for (int i = 0; i < eco.size(); i++)
            if (eco.get(i).college.name.equals("Harvard University")) harvardLate = i > 0;
        check("Easier admission: Harvard is not first", harvardLate);
        eco.sort(RecommendationEngine.comparator(RecommendationEngine.SortOrder.NAME, econ));
        check("Name order is alphabetical", eco.get(0).college.name.compareToIgnoreCase(eco.get(1).college.name) <= 0);

        // Shortlist export: one row per college, commas safely quoted
        Shortlist list = new Shortlist();
        list.add(bronx); list.add(lehman); list.add(bronx);
        check("Shortlist ignores duplicates", list.size() == 2);
        Path tmp = Files.createTempFile("collegefit", ".csv");
        list.exportCsv(tmp, amina, new RecommendationEngine());
        List<String> lines = Files.readAllLines(tmp);
        check("Export has header, 2 colleges and a note", lines.size() == 4);
        check("Export keeps commas inside quotes", lines.get(1).contains("\"") && CollegeDataLoader.readCsvRecord(
            new java.io.BufferedReader(new java.io.StringReader(lines.get(1)))).size() == 10);
        Files.delete(tmp);

        System.out.println("\nAmina's top results:");
        for (int i = 0; i < Math.min(5, ranked.size()); i++)
            System.out.println("  " + ranked.get(i).college + " | " + ranked.get(i).program.label);
        System.out.println("Worth a look outside NY:");
        for (CollegeRecommendation x : elsewhere) System.out.println("  " + x.college);

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
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
