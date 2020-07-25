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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class BatchChecker {

    private Log log = LogFactory.getLog(BatchChecker.class);

    public boolean paperOK(PdfChecker pc) {
        return pc.pageCount() <= pc.getTotalLimit() &&
                pc.pageCount() > 1
                && pc.referencesPage() <= pc.pageLimit +1
                && pc.isIEEE()
                && pc.figuresAfterLimit() == null
                && pc.findEmails() == null
                && pc.findAuthorIdentity() == null;
    }

    public String reportPaper(PdfChecker pc) {
        return String.format("%s, %d, %d, %b, %s, %s, %s",
                pc.getFileName(),
                pc.pageCount(),
                pc.referencesPage(),
                pc.isIEEE(),
                pc.figuresAfterLimit(),
                pc.findEmails(),
                pc.findAuthorIdentity());
    }

    public void processPaper(File paper) throws IOException {
        try (PdfDocument doc = new PdfDocument()) {
            log.trace("Processing " + paper.getName());
            doc.loadFile(paper);
            PdfChecker pc = new PdfChecker();
            pc.setDocument(doc);
            if (!paperOK(pc)) {
                System.out.println(reportPaper(pc));
            }
            // System.out.println(pc.text(1));
        }
    }

    public void processPapers(File paperDir) throws IOException {
        File[] files = paperDir.listFiles((d, name) -> name.endsWith(".pdf"));
        Arrays.sort(files);
        for (final File paper : files) {
            processPaper(paper);
        }
    }

    public static void main(String[] argv) throws IOException {
        BatchChecker bc = new BatchChecker();
        if (argv.length == 0) {
            String defaultFolder = "icse2019";
            usage("No arguments provided, switching to default folder: " + defaultFolder);
            bc.processPapers(new File(defaultFolder));
        }
        for (String arg: argv) {
            File farg = new File(arg);
            if (farg.isDirectory()) {
                bc.processPapers(farg);
            } else if (arg.endsWith(".pdf")) {
                bc.processPaper(farg);
            } else {
                usage(String.format("Argument not a folder: %s", arg));
            }
        }
     }

     private static void usage(String error) {
        String msg = String.format("Usage: %s [folder-with-pdfs...] | [pdf-file ...]", BatchChecker.class.getName());
        System.err.println(msg);
        System.err.println(error);
     }
}
