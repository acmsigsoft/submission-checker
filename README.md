# PDF Checker

Simple tool to check paper submissions for conformance
to formatting and double blind policies.

The tool can be used by program chairs to check submissions,
as well as by authors to check papers they intend to
submit themselves.

The tool builds on Apache PDFBox to extract pages and text 
from a pdf, and then applies simple regex matching to find
suspicious patterns.

**WARNING:** _The tool is based on heuristics. Therefore it will **miss violations**, 
and it will likely raise **false alarms**._ 
Use the tool at your own risk.
The conference call for papers is leading. Ultimately it is the
decision of the program chairs to desk reject papers violating the
submission guidelines.

## Usage

The tool runs in java.
It assumes all pdf you want to check are in a folder,
which you offer to the command line.

- Clone git repository
- Run `mvn clean package`
- `java -jar target/pdfchecker-{version}-jar-with-dependencies.jar` <folder-with-pdfs>

## Features

The tool can check:

- Number of pages
- Page where references start
- Occurrence of figures, tables, appendix etc after page 10
- IEEE style (defined as not ACM)
- Non-anonymous emails mentioned on page 1

Features on the backlog:

- Check acknowledgements for double blind violations
- Check previous work mentioned for double blind violations

In the long run the PdfChecker class may be split into multiple
smaller classes, with e.g. separate checkers for IEEE and ACM.

## Contributors

The PdfChecker tool was first developed for [ICSE 2021][icse2021] by 
Arie van Deursen (TU Delft) and Tao Xie (Peking University).
It thankfully uses ideas from:

- Jane Cleland-Huang (Notre Dame University)
- Robert Feldt (Chalmers)
- Darko Marinov (UIUC)

The ICSE 2019 submissions were used as initial
test bed to detect violations and compare them
with actual desk rejects from 2019 as manually
identified by ICSE 2019 program chairs Tevfik Bultan
and Jon Whittle.

[icse2021]: https://conf.researchr.org/home/icse-2021

## Contributing

You're welcome to contribute if you see additional meaningful checks.
Issues with ideas or pull requests with working code (and tests) always welcome!

The tool is licensed under the Apache License, Version 2.0,
 http://www.apache.org/licenses/LICENSE-2.0