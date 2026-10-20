# CollegeFit Java changes

## Update 8: v0.3 student journey (new Swing interface)

Five steps: Get started, Your direction, See options, Explore details, Next steps.

- Start page with four starting points: a subject, a place, two-year or four-year, or "I'm not sure yet".
- Direction page asks only about study goals and campus; scores and costs are folded away as optional
  and can also be added later on any college page.
- Guided "not sure" page: two ways to start college (counts and median published costs from the data),
  then eight common fields with the number of colleges in the student's state.
- Starting options: two colleges from each type (two-year, four-year public, four-year private nonprofit),
  ordered by recent degrees in the chosen fields; "Show more" per group; the order rule is shown and can be changed
  (easier admission, lowest reference cost, name). Chosen states are the search scope.
- Every "why explore" sentence is built from data (degrees awarded, admission, net price, matched preferences).
- Details page: overview tiles (degrees, admission, published cost, completion rate, undergraduates),
  Program and academic, Financial and Social boxes, why and what to verify, four tabs, official links.
- My colleges: compare up to 3 saved colleges, a combined to-do list and questions to ask; export and Start over.
- Completion rate added (C150_4 for four-year, C150_L4 for two-year). Update notebooks/03_export.ipynb:
  add "C150_4" and "C150_L4" to the keep list, then re-run it. Updated CSVs are included.
- All screens are custom-painted Swing (no external libraries).

Removed classes: StudentFormPanel, ResultsPanel, CollegeCardPanel, CollegeDetailsPanel, MyListDialog,
CompareDialog, ChipList. New: StartPanel, DirectionPanel, UnsurePanel, PathwayExplainer, OptionsPanel,
OptionCard, DetailsPanel, MyCollegesPanel, ExtraInfoPanel, StateNames. Tests: 54 scenario checks.

## Update 7: design review (v0.2)
## Update 6: neutral default ranking
## Update 5: lens names Academic, Financial, Social
## Update 4: Save, My list, Export to CSV
## Update 3: data packed inside CollegeFit.jar
## Update 2: programs from the data with search
## Update 1: home state, aid eligibility, messages_en.properties, three lenses, ScenarioTests
