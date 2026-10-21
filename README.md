# CollegeFit

College planning made clear for newcomer students and families.
APAN 5175 term project, Hanqing Wang (hw3146).

CollegeFit helps students explore U.S. colleges through three lenses: Academic, Financial and Social.
"Not sure" is always a valid answer, and every unknown becomes a next step.
It does not predict admission, promise aid, or ask about immigration status.

## Run the app

Requires Java 17 or newer. From this folder:

```bash
javac --release 17 -d out src/*.java
cp src/*.properties out/
java -cp out CollegeFitSwingApp            # all 2,989 colleges and 383 programs
java -cp out CollegeFitSwingApp --sample   # 30-college test sample
java -cp out ScenarioTests                 # scenario tests on the sample
```

A ready-to-run `CollegeFit.jar` with the data packed inside is attached to the GitHub Release:

```bash
java -jar CollegeFit.jar
```

## Project structure

```
src/              Java source (Swing app, evaluators, tests) and messages_en.properties
data/processed/   Cleaned data used by the Java app
UI_UX/            Screenshots of the five-step journey
notebooks/        Data exploration and export (Python)
```

## Data

Source: U.S. Department of Education, College Scorecard
(Most Recent Institution-Level and Field-of-Study data, released June 10, 2026).

Raw files are not included because of their size. To rebuild the processed data:

1. Download the data from https://collegescorecard.ed.gov/data/
2. Put `Most-Recent-Cohorts-Institution.csv` and `Most-Recent-Cohorts-Field-of-Study.csv` in `data/raw/`
3. Run the notebooks in order: `01`, `02`, `03`

## Design notes

- Six-step journey: start from what you know, choose up to three priorities, explore a small set of colleges grouped by type,
  keep or set aside colleges with reasons, compare kept colleges and their trade-offs, then take next steps.
- No overall score. Inside each group, colleges follow the student's priorities in order; close values count as ties,
  and missing or not-comparable values never get an advantage.
- Comparisons mark differences only when they are large and measured the same way. Associate and bachelor's
  programs are shown as different paths.
- Every explanation comes from the data. All screen text is in `src/messages_en.properties`.

## Data limitations and future enrichment

CollegeFit describes colleges; it does not measure academic quality.

- Program records (degrees awarded) show that a college has a related program and how large it was. They do not show
  teaching quality, research opportunities or reputation.
- Program earnings are medians for graduates who received federal aid and were working; many small programs are not reported.
- Completion and transfer rates describe past first-time, full-time students, not a student's own outcome.
- Program accreditation (for example, for nursing) is not in the data and must be checked with the college.

Possible future enrichment: retention and other College Scorecard outcome fields, IPEDS institutional indicators,
and information from college and program websites. Rankings, if ever used, would stay outside the core logic.
