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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
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
    int pageLimit = 10;
    int referenceLimit = 2;

    private PaperMetaData metaData = null;

    final private Log log = LogFactory.getLog(PdfChecker.class);

    public int getTotalLimit() {
        return pageLimit + referenceLimit;
    }

    public int pageCount() {
        return document.pageCount();
    }

    public void setDocument(@NotNull PdfDocument document) {
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
            // We're not interested in line nrs, so strip those first.
            String before = pageText.substring(0, findRefs.start());
            return stripHeader(before);
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
        }
        // no luck with meta data. Check content of first page.
        String permissions = revealingEmail("permissions@acm.org");
        if (permissions != null) {
            return true;
        }
        // no luck with official copyright template. Try format.
        String format = revealingEmail("ACM Reference format:");
        if (format != null) {
            return true;
        }
        // no luck: Are there (many) line numbers?
        String page1 = document.textAtPage(1);
        int lineCount = countLineNumbers(page1.split("\n"));
        return lineCount > 30;
    }

    String stripHeader(@NotNull String acmPage) {
        String[] lines = acmPage.split("\\n");
        int leftColumn = countLineNumbers(lines);
        final int minimumPageLength = 30;
        if (leftColumn > minimumPageLength && leftColumn < lines.length) {
            // if you'd like to know the header, it is in lines[leftColumn];
            // in rare cases the header can take three (!) lines. Let's check.
            if (leftColumn + 1 < lines.length && !Pattern.matches("\\d+", lines[leftColumn + 1])) {
                leftColumn++;
                if (leftColumn + 1 < lines.length && !Pattern.matches("\\d+", lines[leftColumn + 1])) {
                    leftColumn++;
                }
            }
            String[] remainingLines = Arrays.copyOfRange(lines, leftColumn + 1, lines.length);
            int rightColumn = countLineNumbers(remainingLines);
            if (rightColumn > minimumPageLength) {
                return String.join("\n", Arrays.copyOfRange(remainingLines, rightColumn, remainingLines.length));
            } else {
                // left column numbered, but right one not ...
                return String.join("\n", remainingLines);
            }
        } else {
            //return acmPage;
            return stripNonNumberedHeader(acmPage);
        }
    }

    String stripNonNumberedHeader(@NotNull String acmPage) {
        if (pageCount() < 3) {
            // cannot determine whether headers are used.
            return acmPage;
        }
        String title = getTitle();
        // in ACM format odd pages can contain title header.
        String page3 = document.textAtPage(3);
        String line1 = page3.substring(0, page3.indexOf("\n"));
        if (title != null && line1.startsWith(title)) {
            // assume header is one line, and drop it from the page.
            return acmPage.substring(acmPage.indexOf("\n") + 1);
        } else {
            // nothing to strip
            return acmPage;
        }
    }

    /**
     * Strip initial line numbers from text, if present (as is the case for ACM formating).
     * @param acmText Text potentially containing line numbers.
     * @return Text with starting line numbers removed.
     */
    String stripLineNumbers(@NotNull String acmText) {
        String[] lines = acmText.split("\\n");
        int count = countLineNumbers(lines);
        assert count >= 0;
        assert count <= lines.length;
        return String.join("\n", Arrays.copyOfRange(lines, count, lines.length)) + "\n";
    }

    /**
     * Count how many of the starting lines of this string are pure line numbers,
     * i.e., a consecutive series of numbers on new lines.
     * @param lines Text potentially starting with line numbers
     * @return Count of the number of starting lines that are just numbers.
     */
    int countLineNumbers(String ...lines) {
        int i = 0;
        int counter = -1;
        while (i < lines.length) {
            try {
                int nr = Integer.parseInt(lines[i]);
                if (counter != -1) {
                    if (counter + 1 != nr ) {
                         // Numbers not consecutive -- done.
                        break;
                    }
                }
                counter = nr;
                i++;
            } catch(NumberFormatException nfe) {
                // first line of real text. Done.
                break;
            }
        }
        return i;
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
        if (copyrightIEEE(getFirstLine(document.textAtPage(1)))) {
            // an old MS Word Template, but ok for now.
            return true;
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
            String content = beforeReferences.substring(0, leewayForPageNr).replaceAll("\\n", "\\\\n");
            return String.format("%d-chars-before-REFERENCES: %s", abusiveCharCount, content);
        }
    }

    public String pageContainsFigure(int pagenr) {
        String figRegEx = "^\\s*((((Fig\\.)|(Figure))\\s*\\d+)|(TABLE\\s*[IVX]+)|(APPENDIX)|ACKNOWLEDGE?MENTS)";
        return findOnPage(pagenr, figRegEx);
    }

    public String findAuthorIdentity() {
        String meta = document.metaDataAuthor();
        if (meta != null) {
            return blindedIdentity(meta)? null : meta;
        }
        return null;
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
            if (blindedIdentity(found)) {
                return null;
            }
        }
        return found;
    }

    private boolean blindedIdentity(@NotNull String id) {
        String safeNames = "anonymous|anon|doe|blinded|nn|nobody|none|email|anonymized|firstname|lastname|xyz|xxx|author";
        String safeDomains = "email|example|domain|address|blind|review";
        String regex = String.format(".*(%s|%s|permissions@acm.org).*", safeNames, safeDomains);
        return match(regex, id) != null;
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
        return match(pat, text);
    }

    private String match(@NotNull Pattern pattern, @NotNull String text) {
        Matcher match = pattern.matcher(text);
        if (match.find()) {
            return match.group(0).strip();
        } else {
            return null;
        }
    }

    /**
     * Identify identity-revealing references to previous work.
     * This test seems moderately useful at best, so use with caution.
     * @return String representing reference to previous work, or null if none could be found.
     */
    public String previousWork() {
        // This is an expensive search, so we compile the pattern statically.
        // TODO: it would be good to actually look up the citation in the text, and include in
        // the report, as sometimes it is properly anonymized.
        return match(previousWorkPattern(), document.fullText());
    }

    private static Pattern _previousWorkPattern = null;
    private static Pattern previousWorkPattern() {
        if (_previousWorkPattern == null) {
            String our = "our|my";
            String previous = "previous|earlier|prior";
            String work = "work|study|studies|approach|papers?|publications?|result|findings?";
            String citation = "\\[[\\d\\,]+\\]";
            String regex = "(" + String.join(")\\s+(", our, previous, work, citation) + ")";
            _previousWorkPattern = Pattern.compile(regex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        }
        return _previousWorkPattern;
    }

    public String getFileName() {
        return document.getFileName();
    }

    /**
     * Obtain the title of the document, which we assume to be the first line on page 1.
     * (Note: Meta data rarely set, so discarded).
     * @return Title of the document.
     */
    public String getTitle() {
        String page1 = stripLineNumbers(document.textAtPage(1));
        String line1 = getFirstLine(page1).strip();
        if (copyrightIEEE(line1)) {
            line1 = getFirstLine(page1.substring(line1.length() + 2));
        }
        if (line1.strip().equals("")) {
            return document.metaDataTitle();
        }
        return line1;
    }

    @NotNull private String getFirstLine(@NotNull String text) {
        int newLine = text.indexOf("\n");
        if (newLine == -1) {
            return "";
        } else {
            return text.substring(0, newLine);
        }
    }

    private boolean copyrightIEEE(@NotNull String line) {
        return line.matches(".*20XX IEEE.*");
    }

    public void setMetaData(PaperMetaData metaData) {
        this.metaData = metaData;
    }

    public String revealingMetaData() {
        if (metaData == null) {
            return null;
        }
        String page1 = document.textAtPage(1);
        String result = null;
        for (Author author: metaData.getAuthors()) {
            String name = Pattern.quote(author.getName());
            String email = Pattern.quote(author.getEmail());
            String regex = String.format("(%s)|(%s)", name, email);
            String found = match(regex, page1);
            if (found != null) {
                if (result == null) {
                    result = found;
                } else {
                    result += "," + found;
                }
             }
        }
        if (result == "") {
            result = null;
        }
        return result;
    }
}
