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

package org.sigsoft;

import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BatchChecker {

    static {
        // PdfBox can generate a lot of noise.
        java.util.logging.Logger
                .getLogger("org.apache.pdfbox").setLevel(java.util.logging.Level.SEVERE);
        java.util.logging.Logger
                .getLogger("org.apache.fontbox.ttf").setLevel(java.util.logging.Level.SEVERE);
    }

    private final Log log = LogFactory.getLog(BatchChecker.class);

    // Style can be "ACM" or "IEEE";
    public String style = "IEEE";

    public String paperIssues(PdfChecker pc) {
        List<String> issues = new ArrayList<>();
        if (pc.pageCount() > pc.getTotalLimit()) {
            issues.add(String.format("oversize:%d", pc.pageCount()));
        }
        if (pc.referencesPage() > pc.pageLimit + 1) {
            issues.add(String.format("reference-page-after-limit:%d",pc.referencesPage()));
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

        String result;
        if (issues.isEmpty()) {
            result = "no-issues   ";
        } else {
            result = String.format("issues-found {%s}", String.join(", ", issues));
        }
        result = result + String.format(" ``%s''", pc.getTitle());

        return String.format("%-24s %s", pc.getFileName(), result);
    }

    public void processPaper(File paper) throws IOException {
        try (PdfDocument doc = new PdfDocument()) {
            log.trace("Processing " + paper.getName());
            doc.loadFile(paper);
            PdfChecker pc = new PdfChecker();
            pc.setDocument(doc);
            System.out.println(paperIssues(pc));
            // for debugging this is sometimes useful:
            // System.out.println(doc.textAtPage(11));
        }
    }

    public void processPapers(File paperDir) throws IOException {
        File[] files = paperDir.listFiles((d, name) -> name.endsWith(".pdf"));
        Arrays.sort(files);
        for (final File paper : files) {
            processPaper(paper);
        }
    }

    public void processOptions(String ...argv) throws ParseException, IOException {
        Options options = new Options();
        options.addOption("s", "style", true, "ACM or IEEE style, default IEEE");
        options.addOption("h", "help", false, "Display help information");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, argv);

        this.style = cmd.getOptionValue("s", "IEEE");
        if (cmd.hasOption("h")) {
            usage(null);
            return;
        }
        processFileArgs(cmd.getArgs());
    }

    public void processFileArgs(String[] argv) throws IOException {
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
                usage(String.format("Argument not a folder: %s", arg));
            }
        }
    }

    public void usage(String error) {
        if (error != null) {
            System.err.println(String.format("ERROR: %s", error));
        }
        String msg = String.format("Usage: %s [options] [folder-with-pdfs...] [pdf-file ...]\n", BatchChecker.class.getName());
        msg += "  Options:\n";
        msg += "  --style <style>    Set style in ACM or IEEE, default IEEE\n";
        System.err.println(msg);
    }

    public static void main(String[] argv) throws IOException, ParseException {
        BatchChecker bc = new BatchChecker();
        bc.processOptions(argv);
     }
}
