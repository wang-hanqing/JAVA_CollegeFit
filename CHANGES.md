# CollegeFit Java changes

## Update 7: design review and student journey (v0.2)

Trust and accuracy
- Academic lens never returns MATCH. Open admission and admission rates are neutral context (new status INFO).
  SAT is compared section by section: below, within, above or mixed range; "not assessed" without scores.
- Financial lens never returns MATCH. A reference cost under the budget is "Reference cost within budget" (INFO).
  Published cost, in-state and out-of-state tuition and net price are listed separately. Negative net prices
  are shown as reported (e.g. -$1,264) with a note. If income is unknown, the overall net price (NPT4) is shown.
  If no net price calculator is listed, the next step points to the financial aid office.
- Program evidence has three states: awarded, zero awards in both years, not confirmed.
- Social preferences shape the ranking but no longer hide colleges. A checkbox lets the student filter.
- Colleges with no award records at either level are no longer removed by the pathway filter.

Flexible exploration
- Programs: any number, plus whole fields such as "All Engineering programs" (419 choices).
- Preferred states, campus settings and sizes are multi-select (OR within each item).
- "Focus" menu re-ranks results for one program interest without editing the form.

Understand, compare, act
- Redesigned details page: "Why explore it" and "What to verify" boxes, then itemized Program, Academic,
  Financial and Social sections, and official links.
- Compare 2 or 3 saved colleges side by side from My list.
- Start over clears answers, results and the saved list.
- Export asks for confirmation and explains what the file contains.

New classes: Fact, ChipList, CipFamilies, CompareDialog. Tests: 48 scenario checks.

## Update 6: neutral default ranking
## Update 5: lens names Academic, Financial, Social
## Update 4: Save, My list, Export to CSV
## Update 3: data packed inside CollegeFit.jar
## Update 2: programs from the data with search; Sort by
## Update 1: home state, aid eligibility, messages_en.properties, three lenses, ScenarioTests
