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
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conduct series of tests on PDF paper to make sure it meets
 * double blind and formatting standards.
 * Using ICSE 2021 and hence IEEE style as starting point,
 * but extensible for other conferences / styles.
 *
 * Relies on Apache PDFBox, which has excellent facilities to extra text
 * from pdf documents (also used in Apache Tika).
 *
 */

public class PdfChecker {

    @NotNull PdfDocument document;
    //PDDocument pdf;
    private String fileName;
    int pageLimit = 10;
    int referenceLimit = 2;

    private Log log = LogFactory.getLog(PdfChecker.class);

    public int getTotalLimit() {
        return pageLimit + referenceLimit;
    }

    public int pageCount() {
        return document.pageCount();
    }

    public void setDocument(PdfDocument document) {
        this.document = document;
    }

    /**
     * Find the page of the references section.
     * @return Page number for references section or 0 if not found.
     */
    public int referencesPage() {
        // Search from back for page containing references section.
        for (int pagenr = document.pageCount(); pagenr > 0; pagenr--) {
            if (isReferencesPage(pagenr) != null) {
                return pagenr;
            }
        }
        // not found.
        return 0;
    }

    /**
     * Verify whether given page contains references.
     * @param pagenr Page that may contain references section.
     * @return Null if page does not contain References. Text on same page before references if it does.
      */
    private String isReferencesPage(int pagenr) {
        String pageText = document.textAtPage(pagenr);
        Pattern references = Pattern.compile(
                "^\\s*((REFERENCES)|(R E F E R E N C E S)|(PUBLICATIONS))\\s*$",
                Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher findRefs = references.matcher(pageText);
        if (findRefs.find()) {
            // We're on the references page.
            // Return text _preceding_ the References.
            // (should be empty if references start on page 11).
            return pageText.substring(0, findRefs.start());
        }
        // Not on the references page.
        return null;
    }

    /**
     * Establish whether paper is created with ACM style.
     * @return True if paper was created with acmart
     */
    boolean isACM() {
        String producer = document.metaDataCreator();
        if (producer != null && match("acmart", producer) != null) {
            return true;
        } else {
            // no luck with meta data. Check content of first page.
            return revealingEmail("permissions@acm.org") != null;
        }
    }

    /**
     * Establish whether paper is created with IEEE style.
     * Unfortunately no positive test exists, so we use heuristics.
     * @return True if paper likely IEEE
     */
    boolean isIEEE() {
        if (isACM()) {
            return false;
        }
        // we're not sure, but let's assume it's IEEE style.
        // can we think of more checks to do here?
        return true;
    }

    public String figuresAfterLimit() {
        if (document.pageCount() <= pageLimit) {
            return null;
        }
        for (int pagenr = document.pageCount(); pagenr > pageLimit; pagenr--) {
            String figText = pageContainsFigure(pagenr);
            if (figText != null) {
                return figText;
            }
        }
        String beforeReferences = isReferencesPage(pageLimit + 1);
        final int leewayForPageNr = 8;
        if (beforeReferences == null || beforeReferences.length() <= leewayForPageNr) {
            return null;
        } else {
            int abusiveCharCount = beforeReferences.length();
            String content = beforeReferences.substring(0, leewayForPageNr);
            return String.format("%d-chars-before-REFERENCES: %s", abusiveCharCount, content);
        }
    }

    public String pageContainsFigure(int pagenr) {
        String figRegEx = "^\\s*((((Fig\\.)|(Figure))\\s*\\d+)|(TABLE\\s*[IVX]+)|(APPENDIX\\s*\\w*)|ACKNOWLEDGE?MENTS)$";
        return findOnPage(pagenr, figRegEx);
    }

    public String findAuthorIdentity() {
        return document.metaDataAuthor();
    }

    public String revealingEmail(String email) {
        String emailPattern = email.replaceAll("\\.", "\\\\.");
        return findOnPage(1, emailPattern);
    }

    public String findEmails() {
        // The email pattern is not exact, as authors can be sloppy in
        // writing their email in the paper.
        String name = "\\w+[\\w\\.\\-]*";
        String curlyNames = String.format("(\\{)?%s(,\\s*%s)*(\\})?", name, name);
        String domain = "\\w+(\\.[a-zA-Z]\\w*)+";
        String pat = curlyNames + "\\s*@\\s*" + domain;
        String found = findOnPage(1, pat);
        if (found != null) {
            if (blindedEmail(found)) {
                log.info(String.format("File %s: Blinded email OK: %s", this.getFileName(), found));
                return null;
            }
        }
        return found;
    }

    private boolean blindedEmail(String email) {
        String safeNames = "anonymous|anon|doe|blinded|nn|nobody|none|email|anonymized|firstname|lastname|xyz|xxx";
        String safeDomains = "email|example|domain|address|blind";
        String regex = String.format(".*(%s|%s).*", safeNames, safeDomains);
        return match(regex, email) != null;
    }

    /**
     * Check if the given page matches a given pattern
     * @param pagenr Page containing the relevant pattern
     * @param regexp Regular expression to search for
     * @return Matching text, if found, null otherwise.
     */
    private String findOnPage(int pagenr, @NotNull String regexp) {
        assert pagenr > 0;
        assert pagenr <= document.pageCount();
        return match(regexp, document.textAtPage(pagenr));
    }

    private String match(@NotNull String regexp, @NotNull String text) {
        Pattern pat = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher match = pat.matcher(text);
        if (match.find()) {
            return match.group(0);
        } else {
            return null;
        }
    }

    /**
     * Identify idenity revealing references to previous work.
     * This test seems moderately useful at best, so use with caution.
     * @return String representing reference to previous work, or null if none could be found.
     */
    public String previousWork() {
        String our = "our|my";
        String previous = "previous|earlier|prior";
        String work = "work|study|approach|papers?|publications?|result";
        String citation = "\\[[\\d\\,]+\\]";
        String regex = "(" + String.join(")\\s+(", our, previous, work, citation) + ")";
        return match(regex, document.fullText());
    }

    public String getFileName() {
        return document.getFileName();
    }
}
