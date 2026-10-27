# CollegeFit

College planning made clear for newcomer students and families.
APAN 5175 term project, Hanqing Wang (hw3146).

CollegeFit helps students explore U.S. colleges through three lenses: Academic, Financial and Social.
"Not sure" is always a valid answer, and every unknown becomes a next step.
It does not predict admission, promise aid, or ask about immigration status.

## Run the app

Requires Java 11 or newer. From this folder:

```bash
javac --release 11 -d out src/*.java
cp src/*.properties out/
java -cp out CollegeFitSwingApp            
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

Sources:
- U.S. Department of Education, College Scorecard (Most Recent Institution-Level and Field-of-Study data, released June 10, 2026).
- U.S. Department of Labor, O*NET 31.0 database (career interest types and Job Zones), CC BY 4.0.
- NCES and BLS, CIP 2020 to SOC 2018 crosswalk (which programs prepare for which careers).

Raw files are not included because of their size. To rebuild the processed data:

1. Download the data from https://collegescorecard.ed.gov/data/
2. Put `Most-Recent-Cohorts-Institution.csv` and `Most-Recent-Cohorts-Field-of-Study.csv` in `data/raw/`
3. Run the notebooks in order: `01`, `02`, `03`
4. For the "not sure" path: download the O*NET database (Excel) from https://www.onetcenter.org/database.html and unzip it to
   `data/raw/db_31_0_excel/`; download `CIP2020_SOC2018_Crosswalk.xlsx` from https://nces.ed.gov/ipeds/cipcode/resources.aspx?y=56
   to `data/raw/`; then run `04_interests.ipynb`

## Design notes

- Six-step journey: start from what you know, choose up to three priorities, explore a small set of colleges grouped by type,
  keep or set aside colleges with reasons, compare kept colleges and their trade-offs, then take next steps.
- No overall score. Inside each group, colleges follow the student's priorities in order; close values count as ties,
  and missing or not-comparable values never get an advantage.
- Comparisons mark differences only when they are large and measured the same way. Associate and bachelor's
  programs are shown as different paths.
- Students who are not sure start from what they enjoy doing: six O*NET interest types lead to linked careers and fields.
  This is not a test and gives no result type; it only suggests fields to explore.
- Every explanation comes from the data. All screen text is in `src/messages_en.properties`.

## Data limitations and future enrichment

CollegeFit describes colleges; it does not measure academic quality.

- Program records (degrees awarded) show that a college has a related program and how large it was. They do not show
  teaching quality, research opportunities or reputation.
- Program earnings are medians for graduates who received federal aid and were working; many small programs are not reported.
- Completion and transfer rates describe past first-time, full-time students, not a student's own outcome.
- Program accreditation (for example, for nursing) is not in the data and must be checked with the college.
- The CIP-SOC crosswalk is descriptive, not based on where graduates actually work: a link means a program prepares for a career.
- Program content, curriculum, program-specific admission requirements and application deadlines are not in the data.
  The app points students to what to check with each college instead of stating it.
- Belonging, community and student support are not covered, because no reliable comparable data is available.

Possible future enrichment: NCES program definitions ("what you would learn"), a school-specific verification
checklist built from data the app already has (test policy, net price calculator, license reminders), and,
later, sourced application requirements with a "last checked" date. Rankings, if ever used, would stay outside the core logic.

## Design feedback and roadmap

A first design review (October 2026) confirmed three strengths: flexible ways to start exploring,
a clear side-by-side comparison, and a useful shortlist export. It found three gaps:

| Priority | Gap | Status |
| --- | --- | --- |
| P0 | Early-stage exploration: the app served students with a direction better than students unsure of their interests | Completed in v0.5 ("What do you enjoy doing?") |
| P1 | Program information: little on program content, curriculum and program-specific requirements | Exploring reliable sources, starting with NCES program definitions |
| P1 | Application readiness: no school-specific guidance on materials and deadlines | Planned: a school-specific verification checklist in Next steps |

Next steps:

- [x] Improve the "Not sure" pathway with interest-to-major exploration (v0.5)
- [ ] Explore reliable data sources for program information and application requirements
- [ ] Add a school-specific verification checklist to Next steps

Deprioritized: belonging and community factors. They matter, but the current data cannot assess them reliably;
we will revisit them based on student feedback.

Product focus: strengthen early-stage exploration while keeping the existing search, comparison and export experience.
