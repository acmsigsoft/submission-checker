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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PdfCheckerTest {

    PdfChecker pc;
    PdfDocument doc;

    @BeforeEach
    public void setUp() throws IOException {
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
                "Title page",
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
            "Appendix I",
            "Appendix",
            "Acknowledgments",
            "Acknowledgements"
    })
    void testTextOnReferencePage(String text) {
        createReferencesOnePager(String.format("TEXT BEFORE\n%s\nTEXT AFTER", text));
        assertEquals(text, pc.figuresAfterLimit());
    }

    @Test
    void testTextBeforeReferences() {
        createReferencesOnePager("123456789\nREFERENCES\n[1] Test Infected");
        String expected = "10-chars-before-REFERENCES: 12345678";
        assertEquals(expected, pc.figuresAfterLimit());
    }

    @Test
    void testOnlyReferences() {
        createReferencesOnePager("REFERENCES\n[1] Test Infected");
        assertEquals(null, pc.figuresAfterLimit());
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
        assertEquals(null, pc.findEmails());
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

    private void createReferencesOnePager(String content) {
        String text = content + "\n[1] Test Infected\n";
        when(doc.textAtPage(eq(1))).thenReturn(text);
        pc.pageLimit = 0;
        pc.referenceLimit = 1;
    }

    private void createDocument(String[] pages) {
        for (int i = 0; i < pages.length; i++) {
            when(doc.textAtPage(eq(i + 1))).thenReturn(pages[i] + "\n");
        }
        when(doc.pageCount()).thenReturn(pages.length);
    }
}
