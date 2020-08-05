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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Access content of Pdf documents.
 *
 * Serves as simple wrapper around Apache PDFBox.
 * Helps in testing as it can be easily mocked.
 */
public class PdfDocument implements AutoCloseable {

    @NotNull
    private PDDocument pdf;

    @NotNull
    private String fileName;

    public void loadFile(File paperFile) throws IOException {
        this.fileName = paperFile.getName();
        this.pdf = PDDocument.load(paperFile);
    }

    public void loadResource(String fileName) throws IOException {
        this.fileName = fileName;
        InputStream input = getClass().getClassLoader().getResourceAsStream(fileName);
        pdf = PDDocument.load(input);
    }

    @NotNull
    public String getFileName() {
        return fileName;
    }

    public void close() throws IOException {
        pdf.close();
    }

    public int pageCount() {
        return pdf.getNumberOfPages();
    }

    public String fullText() {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(pdf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the text of a specific page.
     *
     * @param pagenr Number of page to extract (ranges from 1 to nr of pages).
     * @return The text of the page
     * @throws RuntimeException in case pdf was messed up.
     */
    public String textAtPage(int pagenr) {
        assert pagenr > 0;
        assert pagenr <= pageCount();

        try {
            // See https://stackoverflow.com/a/15689797/165292
            PDFTextStripper reader = new PDFTextStripper();
            reader.setStartPage(pagenr);
            reader.setEndPage(pagenr);
            return reader.getText(pdf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Check whether authors set their name in the pdf's meta-data.
     * Note: Set to "Anonymous Author(s)" when using ACM style properly.
     * @return Null if no name was given, actual name otherwise.
     */
    public String metaDataAuthor() {
        PDDocumentInformation pdi = pdf.getDocumentInformation();
        String metaAuthor = pdi.getAuthor();
        if (metaAuthor != null) {
            String stripped = metaAuthor.strip();
            return stripped.equals("")? null: stripped;
        } else {
            return null;
        }
    }

    /**
     * Check whether creating tool was set in pdf's meta-data.
     * Done, for example when ACM style is used properly, which then sets it to
     * "LaTeX with acmart ..."
     * @return Null if no tool was mentioned, name of tool otherwise.
     */
    public String metaDataCreator() {
        PDDocumentInformation pdi = pdf.getDocumentInformation();
        String creator = pdi.getCreator();
        if (creator == null || creator.equals("")) {
            return null;
        } else {
            return creator;
        }
    }

    public String metaDataTitle() {
        PDDocumentInformation pdi = pdf.getDocumentInformation();
        String title = pdi.getTitle();
        if (title == null) {
            return null;
        }
        title = title.strip();
        if (title.equals("")) {
            return null;
        }
        return title;
    }
}
