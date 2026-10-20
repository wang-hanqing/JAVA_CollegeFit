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

- Results are ranked without an overall score. Default "Best fit": program evidence, fewest mismatches,
  most of her programs covered, most social preferences matched, preferred states, then name. Students can also sort by easier admission,
  lowest reference cost, or name.
- All screen text is in `src/messages_en.properties`, so other languages can be added later.
- Values are historical, school-level statistics, not personal predictions.
