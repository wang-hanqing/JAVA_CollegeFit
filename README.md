# CollegeFit

College planning made clear for newcomer students and families.
APAN 5175 term project, Hanqing Wang (hw3146).

## Data

Source: U.S. Department of Education, College Scorecard
(Most Recent Institution-Level and Field-of-Study data, released June 10, 2026).

Raw files are not included because of their size. To rebuild the processed data:

1. Download the data from https://collegescorecard.ed.gov/data/
2. Put `Most-Recent-Cohorts-Institution.csv` and
   `Most-Recent-Cohorts-Field-of-Study.csv` in `data/raw/`
3. Run the notebooks in order: `01`, `02`, `03`

## Project structure

```
data/processed/   Cleaned data used by the Java app
notebooks/        Data exploration and export
```