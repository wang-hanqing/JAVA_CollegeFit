# CollegeFit Java changes

## Update 9: user-flow review fixes (v0.3.1)

Bugs
- SAT and budget boxes now appear on their own lines when "I have scores" or "I know my budget" is chosen
  (they were squeezed out of view on Mac). Error messages name the missing answer.
- Details page: facts are stacked (label, then value), and box statuses sit under the title, so nothing overlaps.
- Opaque page backgrounds and full repaints remove the black areas seen when switching pages on Mac.

Navigation and data notes
- Home button (and clickable logo) on every page; keeps answers and saved colleges. Start over still clears them.
- Help includes "About the data" with the year of each data source; years are no longer repeated on cards.

Grouping and wording
- Groups: Community colleges (public two-year), Four-year public colleges, Private colleges
  (nonprofit; the 78 private two-year colleges moved here).
- Program counts in the program list follow the chosen states; with no state chosen they are national.
- Cards: "Agriculture: 25 graduates last year (associate)", "Admission rate 84%",
  "Cost before aid $12,429/yr. Average paid after grants $5,258/yr*". The green reason box was removed from cards.
- Details: plain names for each cost, shorter program facts, no empty test-policy or SAT rows,
  "Open admission" explained in words.
- "How your answers are used" rewritten; the order menu shows a one-line explanation of each order.
- Export button says "Export all saved colleges".

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
