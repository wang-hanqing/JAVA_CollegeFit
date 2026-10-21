# CollegeFit in the browser (CheerpJ preview)

This folder runs the same `CollegeFit.jar` in a web browser with CheerpJ.
No app server, no database: the Java program runs inside each visitor's browser tab.

## 1. Build the jar (Java 11 bytecode)

CheerpJ fully supports Java 8 and 11, so the jar is compiled with `--release 11`. From the CollegeFit root:

```bash
javac --release 11 -d out src/*.java
cp src/*.properties out/
jar cfe web_browser/CollegeFit.jar CollegeFitSwingApp -C out . data/processed/colleges.csv data/processed/programs.csv
```

## 2. Test on your computer

CheerpJ needs a web server that supports HTTP Range requests. `python -m http.server` does NOT work.
With Node.js installed (https://nodejs.org), run from the CollegeFit root:

```bash
npx http-server -p 8080
```

Open http://localhost:8080/web_browser/ in Chrome. Do not open index.html by double-clicking it.

## 3. Share with GitHub Pages

1. `.gitignore` ignores `*.jar`. Add this line so the browser jar is uploaded: `!web_browser/CollegeFit.jar`
2. Push, then on GitHub: Settings > Pages > Deploy from a branch > `main` / root > Save
3. After a minute the link is: https://wang-hanqing.github.io/JAVA_CollegeFit/web_browser/

## What is different in the browser

- Started with `--web`.
- Export writes the CSV inside the browser; the "Download my list" button at the top saves it to the device.
  The file is deleted when the app starts and on Start over.
- College links open a box to copy the link (a desktop browser cannot be opened from inside the page).
- Fonts may look slightly different, and the first load is slower.

## Notes before wider use

- CheerpJ loads its runtime from Leaning Technologies' servers, and the hosting site may keep access logs.
  The app itself sends no answers anywhere.
- Check the CheerpJ license terms. Personal projects and evaluation are covered by its community license;
  an IRC deployment may need a different license.
