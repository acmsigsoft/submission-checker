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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PdfCheckerTest {

    PdfChecker pc;
    PdfDocument doc;

    @BeforeEach
    public void setUp() {
        pc = new PdfChecker();

        // we'll mock the document so that we can easily test various content cases.
        doc = mock(PdfDocument.class);
        // most unit tests can work with a single page document.
        when(doc.pageCount()).thenReturn(1);

        pc.setDocument(doc);
    }

    @AfterEach
    public void tearDown() throws IOException {
        doc.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "References",
            "    References",
            "Publications",
            "R E F E R E N C E S",
            "REFERENCES"
    })
    void testReference(String references) {
        String fullPage = String.format("TEXT-BEFORE\n%s\nTEXT-AFTER", references);
        when(doc.textAtPage(eq(1))).thenReturn(fullPage);
        assertEquals(1, pc.referencesPage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Reference\n",
            "My References\n",
            "References of this paper\n"
    })
    void testNoReference(String noreferences) {
        String fullPage = String.format("TEXT-BEFORE\n%sTEXT-AFTER", noreferences);
        when(doc.textAtPage(eq(1))).thenReturn(fullPage);
        assertEquals(0, pc.referencesPage());
    }

    @Test
    void multiPageReferences() {
        String[] pages = {
                "Title page\nAbstract",
                "Introduction",
                "References",
                "[1] Test Infected"
        };
        createDocument(pages);
        assertEquals(3, pc.referencesPage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Table I",
            "Figure 3",
            "Figure 3: With a caption",
            "Fig. 4: Also with a caption",
            "Appendix I",
            "Appendix",
            "Acknowledgments",
            "Acknowledgements"
    })
    void testTextOnReferencePage(String text) {
        createReferencesOnePager(String.format("TEXT BEFORE\n%s\nTEXT AFTER", text));
        String resultingText = pc.figuresAfterLimit();
        assertThat(text).startsWith(resultingText);
    }

    @Test
    void testLinesBeforeReferences() {
        createReferencesOnePager("1\n2\n3\n4\nREFERENCES\n[1] Test Infected");
        assertNull(pc.figuresAfterLimit());
    }

    @Test
    void testPlainTextBeforeReferences() {
        createReferencesOnePager("ABCDEFGHIJ\nREFERENCES\n[1] Test Infected");
        String expected = "11-chars-before-REFERENCES: ABCDEFGH";
        assertEquals(expected, pc.figuresAfterLimit());
    }

    @Test
    void testOnlyReferences() {
        createReferencesOnePager("REFERENCES\n[1] Test Infected");
        assertNull(pc.figuresAfterLimit());
    }

    private void createReferencesOnePager(String content) {
        String text = content + "\n[1] Test Infected\n";
        when(doc.textAtPage(eq(1))).thenReturn(text);
        pc.pageLimit = 0;
        pc.referenceLimit = 1;
    }

    private void createDocument(String... pages) {
        for (int i = 0; i < pages.length; i++) {
            when(doc.textAtPage(eq(i + 1))).thenReturn(pages[i] + "\n");
        }
        when(doc.pageCount()).thenReturn(pages.length);
        when(doc.fullText()).thenReturn(String.join("\n", pages));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "bill.gates@microsoft.com",
            "{bill, melinda}@gates.foundation",
            "sloppy.sloppy @ with.spaces"
    })
    void testRevealingEmail(String email) {
        String fullPage = String.format("TEXT-BEFORE\n%s\nTEXT-AFTER", email);
        when(doc.textAtPage(eq(1))).thenReturn(fullPage);
        assertEquals(email, pc.findEmails());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "anonymous@fnerk.com",
            "blinded@for.review",
            "john.doe@example.org"
    })
    void testBlindedEmail(String email) {
        String fullPage = String.format("TEXT-BEFORE\n%s\nTEXT-AFTER", email);
        when(doc.textAtPage(eq(1))).thenReturn(fullPage);
        assertNull(pc.findEmails());
    }

    @Test
    void testACMproducer() {
        when(doc.metaDataCreator()).thenReturn("LaTeX with acmart 2019/08/24 v1.64 Typesetting articles for the Association for Computing Machinery and hyperref 2017/03/14 v6.85a Hypertext links for LaTeX");
        assertTrue(pc.isACM());
        assertFalse(pc.isIEEE());
    }

    @Test
    void testACMpermissions() {
        when(doc.textAtPage(eq(1))).thenReturn("Bla bla permissions@acm.org in footnote");
        assertTrue(pc.isACM());
        assertFalse(pc.isIEEE());
    }

    @Test
    void testMetaAuthor() {
        when(doc.metaDataAuthor()).thenReturn("Donald Knuth");
        assertEquals("Donald Knuth", pc.findAuthorIdentity());
    }

    @Test
    void testCountingLineNumbers() {
        String[] text = {"10", "11", "12", "13", "HELLO WORLD"};
        assertEquals(4, pc.countLineNumbers(text));
    }

    @Test
    void testCountingNonConsecutiveLineNumbers() {
        String[] text = {"10", "11", "14", "15", "HELLO WORLD"};
        assertEquals(2, pc.countLineNumbers(text));
    }

    @Test
    void testCountingNoLineNumbers() {
        String[] text = {"HELLO WORLD"};
        assertEquals(0, pc.countLineNumbers(text));
    }

    @Test
    void testStrippingNonSequentialLineNumbers() {
        String text = "10\n11\n14\n15\nHELLO WORLD";
        assertEquals("14\n15\nHELLO WORLD\n", pc.stripLineNumbers(text));
    }

    @Test
    void testStrippingSequentialLineNumbers() {
        String text = "10\n11\n12\n13\nHELLO WORLD";
        assertEquals("HELLO WORLD\n", pc.stripLineNumbers(text));
    }

    @Test
    void testStripHeaderNoLines() {
        String[] pages = {
                "Title\nAuthors\nAbstract",
                "Conference Header\nIntroduction",
                "Title Header\nRelated Work"
        };
        createDocument(pages);
        assertEquals("Introduction", pc.stripHeader(pages[1]));
    }

    @Test
    void testMetaTitle() {
        when(doc.metaDataTitle()).thenReturn("Test Infected");
        createDocument("Test Infected: A long title\nAbstract\n");
        assertEquals("Test Infected: A long title", pc.getTitle());
    }

    @Test
    void testFirstLineTitle() {
        createDocument("Test Infected\nAbstract\nBla bla");
        assertEquals("Test Infected", pc.getTitle());
    }

    @Test
    void testNumberedFirstLineTitle() {
        createDocument("1\n2\n3\nTest Infected\nAbstract\nBla bla");
        assertEquals("Test Infected", pc.getTitle());
    }

    @Test
    void testIEEEtitle() {
        createDocument("XXXX 20XX IEEE \nTest Infected\nAbstract");
        assertEquals("Test Infected", pc.getTitle());
    }

    @Test
    void testEmptyTitle() {
        createDocument("");
        assertNull(pc.getTitle());
    }

    @Test
    void testEmptyFirstPage() {
        // a somewhat bizarre special case needed for some pdf documents.
        createDocument("", "Page 2\nAbstract");
        when(doc.metaDataTitle()).thenReturn("Test Infected");
        assertEquals("Test Infected", pc.getTitle());
    }

    @Test
    void testTitleOnly() {
        createDocument("Just a Title\n");
        assertEquals("Just a Title", pc.getTitle());
    }

    @Test
    void testWhiteSpace() {
        createDocument("   Test Infected   \nFnerk\n");
        assertEquals("Test Infected", pc.getTitle());
    }

    @Test
    void testPreviousWork() {
        createDocument("In our previous work [2]", "References\n[2] K. Beck. Test Infected");
        assertEquals("our previous work [2]", pc.previousWork());
    }

    @Test
    void testMetaData() {
        createDocument("Test Infected. Kent Beck and Erich Gamma");
        var paper = createPaper("Test Infected", "Kent", "Beck", "kent@beck.com");
        pc.setMetaData(paper);
        assertThat(pc.revealingMetaData()).isEqualTo("Kent Beck");
    }

    @Test
    void testLatexMetaData() {
        createDocument("Emil und die Detektive. Erich Kästner");
        var paper = createPaper("Test Infected", "Erich", "K{\\\"a}stner", "kent@beck.com");
        pc.setMetaData(paper);
        assertThat(pc.revealingMetaData()).as("Latex in %s not resolved").isNull();
    }

    @ParameterizedTest
    @MethodSource("inconsistentTitleProvider")
    void testInconsistentTitles(String metaTitle, String pdfTitle, String textTitle, String cause) {
        createDocument(String.format("%s\nAnonymous Author", textTitle));
        when(doc.metaDataTitle()).thenReturn(pdfTitle);
        var paper = createPaper(metaTitle, "Unblinded", "Author", "unblinded@university.com");
        pc.setMetaData(paper);
        assertThat(pc.titlesConsistent()).contains(cause);
    }

    static Stream<Arguments> inconsistentTitleProvider() {
        return Stream.of(
                arguments("inconsistent", "title", "provider", "provider"),
                arguments("inconsistent", "inconsistent", "provider", "provider"),
                arguments("inconsistent", "inconsistent", "provider", "inconsistent"),
                arguments(null, "title", "provider", "provider")
        );
    }

    static Stream<Arguments> consistentTitleProvider() {
        return Stream.of(
                arguments("t1", "t1", "t1"),
                arguments("almost t1", "almost t2", "almost t3"),
                arguments("Hello: World!", "Hello World", "Hello World?"),
                arguments("UPPER", "upper", "UPPER"),
                arguments("t1:sub1", "t1", "t1"),
                arguments(null, "t2", "t2"),
                arguments(null, null, "t3"),
                arguments(null, null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("consistentTitleProvider")
    void testConsistentTitles(String metaTitle, String pdfTitle, String textTitle) {
        createDocument(String.format("%s\nAnonymous Author", textTitle));
        when(doc.metaDataTitle()).thenReturn(pdfTitle);
        var paper = createPaper(metaTitle, "Unblinded", "Author", "unblinded@university.com");
        pc.setMetaData(paper);
        assertThat(pc.titlesConsistent()).isNull();
    }



    private PaperMetaData createPaper(String title, String first, String last, String email) {
        var paper = new PaperMetaData("1234", title);
        var author = new Author();
        author.setFirstName(first);
        author.setLastName(last);
        author.setEmail(email);
        paper.addAuthor(author);
        return paper;
    }
}
