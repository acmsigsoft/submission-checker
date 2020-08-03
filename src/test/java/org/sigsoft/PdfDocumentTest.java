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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class PdfDocumentTest {

    PdfDocument pdf;

    @BeforeEach
    public void setUp() throws IOException {
        pdf = new PdfDocument();
        pdf.loadResource("icse2017-ddu.pdf");
    }

    @AfterEach
    public void tearDown() throws IOException {
        pdf.close();
    }

    @Test
    public void testFullText() {
        String fullText = pdf.fullText();

        String titleStart = "A Test-suite Diagnosability Metric";
        assertTrue(fullText.startsWith(titleStart));

        String paperEnd = "November 16 - 22, 2014, 2014, pp. 654–665.\n";
        assertTrue(fullText.endsWith(paperEnd));
    }

    @Test
    public void testAtPage()  {
        String page2 = pdf.textAtPage(2);
        String firstLine = "the test-suite contains a test case";
        assertEquals(firstLine, page2.substring(0, firstLine.length()));
    }

    @Test
    public void testMetaAuthor() {
        assertNull(pdf.metaDataAuthor());
    }

    @Test
    public void testCreator() {
        assertEquals("TeX", pdf.metaDataCreator());
    }

    @Test
    public void testMetaATitle() {
        assertNull(pdf.metaDataTitle());
    }


    @Test
    public void testPageCount() {
        assertEquals(11, pdf.pageCount());
    }
}