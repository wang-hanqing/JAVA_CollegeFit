import java.util.ArrayList;
import java.util.List;

/**
 * Financial lens. Lists each price separately (published cost, in-state and out-of-state tuition,
 * historical net price) and compares a budget only to one clearly labeled reference cost.
 * Aid eligibility is context only: it never tells us her actual aid.
 */
public class FinancialEvaluator implements LensEvaluator {
    static final double SLIGHTLY_OVER = 1.10; // up to 10% above budget

    @Override
    public EvaluationResult evaluate(StudentProfile s, College c) {
        List<Fact> facts = new ArrayList<>();
        boolean outOfStatePublic = c.isPublic() && s.hasHomeState() && !s.getHomeState().equals(c.state);

        // 1. Published prices, each with its own label
        facts.add(new Fact(Messages.get("fin.cost"), money(c.costOfAttendance),
            Messages.get(c.isPublic() ? "fin.cost.public.note" : "fin.cost.note"), null));
        if (c.isPublic()) {
            facts.add(new Fact(Messages.get("fin.tuitionIn"), money(c.tuitionIn), Messages.get("fin.tuitionIn.note"), null));
            facts.add(new Fact(Messages.get("fin.tuitionOut"), money(c.tuitionOut), Messages.get("fin.tuitionOut.note"), null));
            if (outOfStatePublic)
                facts.add(new Fact(Messages.get("fin.oosEstimate"), money(c.outOfStateCostEstimate()), Messages.get("fin.oosEstimate.note"), null));
        } else {
            facts.add(new Fact(Messages.get("fin.tuition"), money(c.tuitionIn), Messages.get("fin.tuition.note"), null));
        }

        // 2. Historical net price: her income range if known, otherwise all aid recipients
        Integer net = null;
        if (outOfStatePublic) {
            facts.add(new Fact(Messages.get("fin.net"), Messages.get("fin.net.oos"), Messages.get("fin.net.oos.note"), null));
        } else if (s.getIncomeBracket().isKnown() && c.netPrice(s.getIncomeBracket()) != null) {
            net = c.netPrice(s.getIncomeBracket());
            facts.add(new Fact(Messages.get("fin.net.bracket", s.getIncomeBracket()), money(net), netNote(c, net), null));
        } else if (c.netPriceAll != null) {
            facts.add(new Fact(Messages.get("fin.net.all"), money(c.netPriceAll), netNote(c, c.netPriceAll), null));
        } else {
            facts.add(new Fact(Messages.get("fin.net"), Messages.get("notReported"), "", null));
        }

        // 3. What to verify next
        String next;
        if (c.netPriceCalculatorUrl == null || c.netPriceCalculatorUrl.isBlank()) next = Messages.get("fin.next.noNpc");
        else if (c.isPublic() && !outOfStatePublic) next = Messages.get("fin.next.residency");
        else next = Messages.get("fin.next");

        Integer published = outOfStatePublic ? c.outOfStateCostEstimate() : c.costOfAttendance;
        if (published == null && net == null && c.netPriceAll == null)
            return new EvaluationResult(FitStatus.UNKNOWN, Messages.get("fin.label.insufficient"), facts, "", next);

        String reason = net != null && net < 10000 ? Messages.get("fin.reason.net", money(net)) : "";

        // 4. Budget comparison, only when the budget is known
        if (s.getBudgetStatus() != StudentProfile.ChoiceStatus.KNOWN)
            return new EvaluationResult(FitStatus.NEEDS_VERIFICATION, Messages.get("fin.label.verify"), facts, reason, next);

        int budget = s.getAnnualBudget();
        boolean useNet = s.getAidEligibility() == StudentProfile.AidEligibility.YES && net != null;
        if (!useNet && published == null)
            return new EvaluationResult(FitStatus.NEEDS_VERIFICATION, Messages.get("fin.label.verify"), facts, reason, next);
        int reference = useNet ? net : published; // negative averages stay as reported
        String basis = Messages.get(useNet ? "fin.basis.net" : "fin.basis.published");
        facts.add(new Fact(Messages.get("fin.budget"), money(budget),
            Messages.get("fin.budget.note", basis, money(reference)), reference <= budget));

        if (reference <= budget)
            // INFO, not MATCH: a historical average or published price below her budget does not confirm affordability.
            return new EvaluationResult(FitStatus.INFO, Messages.get("fin.label.within"), facts,
                Messages.get("fin.reason.within"), next);
        boolean aidMightHelp = !useNet && s.getAidEligibility() != StudentProfile.AidEligibility.NO;
        if (aidMightHelp)
            return new EvaluationResult(FitStatus.NEEDS_VERIFICATION, Messages.get("fin.label.beforeAid"), facts, reason, next);
        if (reference <= budget * SLIGHTLY_OVER)
            return new EvaluationResult(FitStatus.NEEDS_VERIFICATION, Messages.get("fin.label.slightly"), facts, reason, next);
        return new EvaluationResult(FitStatus.MISMATCH, Messages.get("fin.label.above"), facts, "", next);
    }

    private static String netNote(College c, int net) {
        String note = Messages.get(c.isPublic() ? "fin.net.public.note" : "fin.net.note");
        if (net < 0) note += " " + Messages.get("fin.net.negative.note");
        return note;
    }

    private static String money(Integer n) { return Messages.money(n); }
}
