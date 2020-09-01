/*
 * Copyright 2020, Arie van Deursen, TU Delft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.sigsoft.sfc;

import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BatchChecker {

    private final Log log = LogFactory.getLog(BatchChecker.class);

    // Style can be "ACM" or "IEEE";
    public String style = "IEEE";

    // Which pages, if any, to display on stdout
    public String showText = null;

    // Separate meta-data for all files processed, if available.
    public MetaData meta = null;

    public String paperIssues(PdfChecker pc) {
        List<String> issues = new ArrayList<>();
        if (pc.pageCount() > pc.getTotalLimit()) {
            issues.add(String.format("oversize:%d", pc.pageCount()));
        } else {
            final int minimum = Math.max(2, pc.pageLimit / 2);
            if (pc.pageCount() <= minimum) {
                issues.add(String.format("paper-very-short:%d", pc.pageCount()));
            }
        }
        if (pc.referencesPage() > pc.pageLimit + 1) {
            issues.add(String.format("reference-page-after-limit:%d", pc.referencesPage()));
        }
        if (style.equals("ACM") && !pc.isACM() || style.equals("IEEE") && !pc.isIEEE()) {
            issues.add(String.format("wrong-template:must-be-%s", style));
        }
        String textAfterLimit = pc.figuresAfterLimit();
        if (textAfterLimit != null) {
            issues.add(String.format("non-references-after-p10:``%s''", textAfterLimit));
        }
        String emails = pc.findEmails();
        if (emails != null) {
            issues.add(String.format("author-revealing-email:``%s''", emails));
        }
        String authors = pc.findAuthorIdentity();
        if (authors != null) {
            issues.add(String.format("possibly-author-revealing-meta-data:``%s''", authors));
        }
        String previousWork = pc.previousWork();
        if (previousWork != null) {
            issues.add(String.format("previous-work-mentioned:``%s''", previousWork));
        }
        String revealingMeta = pc.revealingMetaData();
        if (revealingMeta != null) {
            issues.add(String.format("possibly-identity-revealing-data:``%s''", revealingMeta));
        }
        // pc.titlesConsistent gives too many false alarms. Omitted for now.

        String result;
        if (issues.isEmpty()) {
            result = "no-issues   ";
        } else {
            result = String.format("issues-found {%s}", String.join(", ", issues));
        }
        result = result + String.format(" ``%s''", pc.getTitle());

        return String.format("%-24s %s", pc.getFileName(), result);
    }

    public void processPaper(File paper) {
        try (PdfDocument doc = new PdfDocument()) {
            log.trace("Processing " + paper.getName());

            doc.loadFile(paper);
            PdfChecker pc = new PdfChecker();
            pc.setDocument(doc);
            pc.setMetaData(getPaperMetaData(paper));

            displayPages(doc, this.showText);

            String analysis = paperIssues(pc);
            System.out.println(analysis);
        } catch(Exception e) {
            // show error, but permit progressing to next paper.
            System.err.printf("Error processing %s. %s%n", paper.getName(), e);
            e.printStackTrace();
        }
    }

    private PaperMetaData getPaperMetaData(File paper) {
        if (meta == null) {
            return null;
        } else {
            return meta.forFile(paper);
        }
    }


    public void displayPages(PdfDocument doc, String pages) {
        if (pages == null) {
            return;
        }
        if ("all".equals(pages)) {
            System.out.println(doc.fullText());
            return;
        }
        try {
            int pagenr = Integer.parseInt(pages);
            if (pagenr < 1 || pagenr > doc.pageCount()) {
                usage(String.format("Page nrs ``%d'' out of range for file %s", pagenr, doc.getFileName()));
            } else {
                System.out.printf("START-PAGE %d (of %d) %s: ---\n%sEND-PAGE\n%n", pagenr, doc.pageCount(), doc.getFileName(), doc.textAtPage(pagenr));
            }
        } catch (NumberFormatException nfe) {
            usage(String.format("Wrong <pagenr> ``%s'' given to option showtext.", pages));
        }
    }

    public void processPapers(File paperDir) {
        File[] files = paperDir.listFiles((d, name) -> name.endsWith(".pdf"));
        Arrays.sort(files);
        for (final File paper : files) {
            processPaper(paper);
        }
    }

    public void processOptions(String ...argv) throws IOException {
        Options options = new Options();
        options.addOption("s", "style", true, "ACM or IEEE style, default IEEE");
        options.addOption("h", "help", false, "Display help information");
        options.addOption("t", "showtext", true, "Display text of given page on stdout");
        options.addOption("m", "meta", true, "CSV file with author meta-data.");
        CommandLineParser parser = new DefaultParser();

        CommandLine cmd;
        try {
            cmd = parser.parse(options, argv);
        } catch(ParseException pe) {
            usage(pe.getMessage());
            return;
        }

        this.style = cmd.getOptionValue("s", "IEEE");
        this.showText = cmd.getOptionValue("t", null);
        if (cmd.hasOption("h")) {
            usage(null);
            return;
        }
        if (cmd.hasOption("m")) {
            meta = new MetaData();
            meta.loadHotCRPauthors(new FileReader(cmd.getOptionValue("m")));
        }
        processFileArgs(cmd.getArgs());
    }

    public void processFileArgs(String[] argv) {
        if (argv.length == 0) {
            usage("No arguments provided, not processing any files.");
        }
        for (String arg: argv) {
            File farg = new File(arg);
            if (farg.isDirectory()) {
                processPapers(farg);
            } else if (arg.endsWith(".pdf")) {
                processPaper(farg);
            } else {
                usage(String.format("Argument not a file or folder: %s", arg));
            }
        }
    }

    public void usage(String error) {
        if (error != null) {
            System.err.printf("ERROR: %s%n", error);
        }
        String tool = BatchChecker.class.getName();
        String msg = String.format("Usage: %s [options] [folder-with-pdfs...] [pdf-file ...]\n", tool);
        msg += "  Options:\n";
        msg += "  -s|--style <style>    Set style in ACM or IEEE, default IEEE\n";
        msg += "  -t|--showtext <pages> Show plain text of pages on stdout. <pages> can be nr or 'all'\n";
        msg += "                        Combine with '... | grep ...' to fetch custom patterns\n";
        msg += "  -m|--meta <csv-file>  .csv file with author meta data. One row per author. Valid columns:\n";
        msg += "                        paper,title,first,last,affiliation,email\n";
        msg += "  -h|--help             Show this information";
        System.err.println(msg);
    }

    public static void main(String[] argv) throws IOException {
        BatchChecker bc = new BatchChecker();
        bc.processOptions(argv);
    }
}
