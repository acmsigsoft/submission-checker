# SIGSOFT Submission Checker

The SIGSOFT Submission Checker (SSC) is a simple tool to check paper submissions to 
conferences for conformance to formatting (in ACM and IEEE styles) 
and author identity hiding policies.

The tool can be used by program chairs to check all submissions in bulk,
as well as by individual authors to check papers they intend to
submit themselves.

The tool builds on Apache PDFBox to extract pages and text 
from a pdf, and then applies simple regular expression matching to find
suspicious patterns.

**WARNING:** _The Submission Checker is based on heuristics. 
Therefore it will **miss violations**, and it will likely raise **false alarms**._ 
Use the tool at your own risk.
The conference call for papers is leading when deciding whether a paper
meets the submission guidelines. This decision is made by the 
program chairs (possibly using the tool's output to base this decision on). 

## Usage

The tool runs in java 11 and is built using Apache [maven](https://maven.apache.org/).
It takes as arguments individual pdf files or folders containing pdf files
which it will check.

To run the tool:

- Clone git repository
- Run `mvn clean package`
- `java -jar target/submission-checker-{version}-jar-with-dependencies.jar` <folder-with-pdfs>
- The `--help` option can be used to display usage information.

## Features

The tool can warn about:

- Number of pages over the limit
- Page where references start over the limit
- Occurrence of figures, tables, appendix etc after on pages that should contain references only.
- Conformance to ACM or IEEE (defined as not ACM) style
- Non-anonymous emails mentioned on page 1
- Suspicious wording like "our previous paper [3]"
- Pdf meta-data that might reveal authors.

While the tool can handle papers in both IEEE and ACM style,
the support for ACM style papers is still work in progress,
and the accuracy may be a little lower.

In the long run the PdfChecker class may be split into multiple
smaller classes, with e.g. separate checkers for IEEE and ACM.

## Contributors

The Submission Checker was first developed for [ICSE 2021][icse2021] by 
Arie van Deursen (TU Delft) and Tao Xie (Peking University).
It thankfully uses ideas from:

- Jane Cleland-Huang (Notre Dame University)
- Robert Feldt (Chalmers)
- Darko Marinov (UIUC)

The ICSE 2019 submissions (IEEE format) were used as initial
test bed to detect violations and compare them
with actual desk rejects from 2019 as manually
identified by ICSE 2019 program chairs Tevfik Bultan
and Jon Whittle.

[icse2021]: https://conf.researchr.org/home/icse-2021

## Contributing

You're welcome to contribute if you see additional meaningful checks.
Issues with ideas or pull requests with working code (and tests) always welcome!
Before spending lots of effort on a pull request make sure to open an issue first,
so that we can discuss ideas upfront.

If you have an issue for a specific paper, and you don't feel like sharing that
version of the paper online yet (in an issue report), instead contact your program chairs,
or send an email to [Arie van Deursen](https://avandeursen.com/about/).

The tool is licensed under the Apache License, Version 2.0,
 http://www.apache.org/licenses/LICENSE-2.0