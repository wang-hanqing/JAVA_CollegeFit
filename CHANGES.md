# CollegeFit Java changes

## Update 15: browser preview with CheerpJ (v0.4.5)

- New folder web_browser/: index.html runs CollegeFit.jar in the browser with CheerpJ (Java 11), plus setup notes.
- The jar is now built with --release 11 (works on the desktop as before).
- --web mode: export goes to the browser's virtual file system and a "Download my list" button saves it;
  the export is deleted at start and on Start over; college links open a copy box.

## Update 14: "Private college" wording (v0.4.4)

- Screen text says "Private college" everywhere instead of "Private nonprofit". Help states that for-profit colleges are not included.

## Update 13: earnings for whole fields (v0.4.3)

- Program earnings now also appear when a whole field is chosen (for example "Engineering (all)"):
  the details page lists the 3 largest programs in that field with reported earnings, by name and degree level.
  Before, earnings were shown only for a single program. Earnings stay on the details page only.
- Earnings are skipped when many programs are chosen, so the details stay short. Tests: 70.

## Update 12: easier browsing on Explore (v0.4.2)

- "Show all N" opens one college group with every college, three per row, and "Back to all college types".
  (Before, "Show more" added 4 colleges at a time and made the three columns very long.)
- A "?" next to the order line explains, step by step, how colleges are ordered for the current priorities.
- Empty groups explain why: for example, no community colleges when a bachelor's degree is chosen,
  with a link to include associate degrees.

## Update 11: program relevance is not academic quality (v0.4.1)

- Three fields added from the data already downloaded (no new source):
  transfer-out rate (TRANS_4 / TRANS_L4), students per faculty member (STUFACR),
  and program-level median earnings 1 year after completing (EARN_MDN_1YR, field-of-study file).
  They are shown on the details page only; they never change the order of colleges. Compare has no new rows.
- Details: "Academics and programs" in four parts: Program information, Admissions, Student outcomes
  (completion and transfer-out rates), Academic resources (students per faculty, and what to explore on the college website).
- "My area of study" priority: colleges offering more of the chosen programs first, then degree levels that fit the pathway.
  Historical degree counts only break remaining ties and are labeled as size, not quality.
- Wording: "degrees awarded (historical)" instead of "graduates last year"; the Compare "more graduates" marker was removed.
- Update notebooks/03_export.ipynb: add "TRANS_4", "TRANS_L4", "STUFACR" to the colleges keep list and
  "EARN_MDN_1YR" to the programs usecols, then re-run. Updated CSVs are included. Tests: 69.

## Update 10: guided decision-making (v0.4)

Six steps: Get started, Your direction, Priorities, Explore, Compare and decide, Next steps.
College details open from Explore or Compare and return there.

- Priorities (new page): up to 3 of College costs and financial aid, Graduation outcomes,
  Admission accessibility, My area of study, Campus environment; "?" explains the data behind each; can be skipped.
- Priority-based ordering inside each college group: #1, then #2, then #3, then name. No weights, no overall score.
  Close values count as ties ($1,000 cost steps, 5-point completion steps, 10-point admission steps).
  Missing or not-comparable values go last: out-of-state public costs, and private two-year completion rates.
- Explore cards show only the program and the top two priorities, with Keep and Not for me.
  Not for me asks for an optional reason, moves the college to a folded list, and can be undone or edited.
  When the same reason is used twice, a suggestion appears; nothing changes until the student accepts.
- Compare and decide: rows follow the priorities; "Different degree path" and a banner when associate and bachelor's
  colleges are compared; markers only when differences are large and measured the same way
  ("Not directly comparable" otherwise); the student records her own reasons; a trade-off summary in priority order.
- Next steps: colleges with her reasons, to-do list, questions; export includes her reasons.
- New classes: PrioritiesPanel, Comparison, ComparePanel, NextStepsPanel. Removed: MyCollegesPanel. Tests: 64.

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
