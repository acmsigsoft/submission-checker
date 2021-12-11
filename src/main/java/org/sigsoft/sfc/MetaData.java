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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;

public class MetaData {

    private final HashMap<String, PaperMetaData> papers = new HashMap<>();

    public PaperMetaData getPaper(String id) {
        return papers.get(id);
    }

    public void loadHotCRPauthors(String fileName) throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (stream == null) {
                throw new IOException(String.format("Cannot find csv file %s on class path", fileName));
            }
            try (var reader = new InputStreamReader(stream);
                 var records = hotCrpCSVFormat().parse(reader)
            ) {
                for (CSVRecord record : records) {
                    processHotCRPRecord(record);
                }
            }
        }
    }

    private CSVFormat hotCrpCSVFormat() {
        return CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();
    }

    public void loadHotCRPauthors(Reader reader) throws IOException {
        try (var records = hotCrpCSVFormat().parse(reader)) {
            for (CSVRecord record : records) {
                processHotCRPRecord(record);
            }
        }
    }

    private PaperMetaData processHotCRPRecord(CSVRecord record) {
        var author = hotCrpAuthor(record);
        var paper = hotCrpPaper(record);
        paper.addAuthor(author);
        return paper;
    }

    private Author hotCrpAuthor(CSVRecord record) {
        var author = new Author();
        author.setEmail(record.get("email"));
        author.setFirstName(record.get("first"));
        author.setLastName(record.get("last"));
        return author;
    }

    private PaperMetaData hotCrpPaper(CSVRecord record) {
        String id = record.get("paper");
        PaperMetaData paper = papers.get(id);
        if (paper == null) {
            paper = new PaperMetaData(id, record.get("title"));
            papers.put(id, paper);
        }
        return paper;
    }

    public PaperMetaData forFile(File paper) {
        String name = paper.getName();
        int before = name.lastIndexOf("-paper");
        int suffix = name.lastIndexOf(".pdf");
        String id = name.substring(before + "-paper".length(), suffix);
        return getPaper(id);
    }
}
