import java.util.*;

/** Turns every "not sure" answer and every status that needs checking into a checklist item. */
public class NextStepGenerator {

    /** Steps that come from her own answers; the same for every college. */
    public List<String> profileSteps(StudentProfile s) {
        List<String> steps = new ArrayList<>();
        if (s.getPathway() == StudentProfile.Pathway.NOT_SURE) steps.add(Messages.get("step.pathway"));
        if (!s.hasPrograms()) steps.add(Messages.get("step.program"));
        if (s.getSatStatus() != StudentProfile.SatStatus.PROVIDED) steps.add(Messages.get("step.sat"));
        if (s.getAidEligibility() == StudentProfile.AidEligibility.NOT_SURE
                || s.getAidEligibility() == StudentProfile.AidEligibility.PREFER_NOT_TO_SAY) steps.add(Messages.get("step.aid"));
        if (s.getBudgetStatus() != StudentProfile.ChoiceStatus.KNOWN) steps.add(Messages.get("step.budget"));
        if (!s.getIncomeBracket().isKnown()) steps.add(Messages.get("step.income"));
        steps.add(Messages.get("step.support"));
        return steps;
    }

    /** Steps for one college only; her general steps are shown once on the results page. */
    public List<String> collegeSteps(StudentProfile s, CollegeRecommendation r) {
        Set<String> steps = new LinkedHashSet<>(); // keeps order and drops duplicates
        for (EvaluationResult e : new EvaluationResult[]{r.program, r.academic, r.financial, r.social})
            for (String line : e.nextStep.split("\n")) if (!line.isBlank()) steps.add(line);
        steps.add(Messages.get("step.support"));
        return new ArrayList<>(steps);
    }
}
