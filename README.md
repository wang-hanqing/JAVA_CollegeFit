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

- Five-step journey: start from what you know, get a small set of starting options grouped by college type,
  understand each college through Academic, Financial and Social facts, then compare up to 3 and act.
- Results are never ranked by an overall score. Default order inside each group: recent degrees awarded in
  the chosen fields. Students can also order by easier admission, lowest reference cost, or name.
- Every explanation comes from the data. Unsupported claims (program quality, campus life, aid generosity) are not shown.
- All screen text is in `src/messages_en.properties`, so other languages can be added later.
