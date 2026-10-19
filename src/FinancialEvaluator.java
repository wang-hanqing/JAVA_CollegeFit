/**
 * Financial lens (appendix A2). Shows costs and labeled reference prices.
 * Aid eligibility is context only: it never tells us her actual aid.
 */
public class FinancialEvaluator implements LensEvaluator {
    static final double SLIGHTLY_OVER = 1.10; // up to 10% above budget

    @Override
    public EvaluationResult evaluate(StudentProfile s, College c) {
        // 1. Which published cost applies? Publics depend on where she lives.
        Integer published;
        String costNote;
        boolean residencyStep = false;
        boolean outOfStatePublic = false;
        if (!c.isPublic()) {
            published = c.costOfAttendance;
            costNote = Messages.get("afford.cost.private", Messages.money(published));
        } else if (s.hasHomeState() && !s.getHomeState().equals(c.state)) {
            published = c.outOfStateCostEstimate();
            outOfStatePublic = true;
            costNote = Messages.get("afford.cost.outOfState", Messages.money(published));
        } else {
            published = c.costOfAttendance;
            residencyStep = true; // in-state rates depend on residency rules, not only on her address
            costNote = Messages.get("afford.cost.inState", Messages.money(published));
        }

        // 2. Is a historical net price available and relevant? Public net prices use in-state rates.
        Integer net = null;
        String netNote = "";
        if (s.getIncomeBracket().isKnown() && !outOfStatePublic) {
            net = c.netPrice(s.getIncomeBracket());
            if (net != null) netNote = " " + Messages.get("afford.net", s.getIncomeBracket(), Messages.money(net));
        }
        String explanation = costNote + netNote + " " + Messages.get("afford.caveat");
        String next = residencyStep ? Messages.get("afford.next.residency") : Messages.get("afford.next");
        if (outOfStatePublic) explanation += " " + Messages.get("afford.outOfState.note");

        if (published == null && net == null) {
            return new EvaluationResult(FitStatus.UNKNOWN, Messages.get("afford.insufficient.label"),
                Messages.get("afford.insufficient.explain"), Messages.get("afford.next"));
        }

        // 3. No budget: show the numbers without judging them.
        if (s.getBudgetStatus() != StudentProfile.ChoiceStatus.KNOWN) {
            return new EvaluationResult(FitStatus.NEEDS_VERIFICATION, Messages.get("afford.verify.label"),
                explanation, next);
        }

        // 4. Budget known: compare to one clearly labeled reference cost.
        int budget = s.getAnnualBudget();
        boolean useNet = s.getAidEligibility() == StudentProfile.AidEligibility.YES && net != null;
        int reference = useNet ? Math.max(net, 0) : (published == null ? 0 : published);
        if (!useNet && published == null) {
            return new EvaluationResult(FitStatus.NEEDS_VERIFICATION, Messages.get("afford.verify.label"), explanation, next);
        }
        String basis = useNet ? Messages.get("afford.basis.net") : Messages.get("afford.basis.published");
        String gap = " " + Messages.get("afford.gap", Messages.money(budget), basis, Messages.money(reference));

        if (reference <= budget) {
            return new EvaluationResult(FitStatus.MATCH, Messages.get("afford.within.label"), explanation + gap, next);
        }
        // Above budget on the published cost while aid is still possible: verify, don't reject.
        boolean aidMightHelp = !useNet && s.getAidEligibility() != StudentProfile.AidEligibility.NO;
        if (aidMightHelp) {
            return new EvaluationResult(FitStatus.NEEDS_VERIFICATION, Messages.get("afford.beforeAid.label"),
                explanation + gap, next);
        }
        if (reference <= budget * SLIGHTLY_OVER) {
            return new EvaluationResult(FitStatus.NEEDS_VERIFICATION, Messages.get("afford.slightly.label"),
                explanation + gap, next);
        }
        return new EvaluationResult(FitStatus.MISMATCH, Messages.get("afford.above.label"), explanation + gap, next);
    }
}
