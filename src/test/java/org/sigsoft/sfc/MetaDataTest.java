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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

public class MetaDataTest {

    MetaData md = new MetaData();

    @BeforeEach
    public void setUp() throws IOException {
        md.loadHotCRPauthors("icse2017-authors.csv");
    }

    @Test
    public void testRegularReadHotCRPLoad() throws IOException {
        PaperMetaData paper13 = md.getPaper("13");
        assertThat(paper13).isNotNull();
        assertThat(paper13.getAuthors()).hasSize(3);
        assertThat(paper13.getAuthors().get(0).getName()).isEqualTo("Alexandre Perez");
    }

    @Test
    public  void testLatexNames() {
        PaperMetaData paper666 = md.getPaper("666");
        assertThat(paper666).isNotNull();
        assertThat(paper666.getAuthors()).hasSize(1);
        assertThat(paper666.getAuthors().get(0).getName()).isEqualTo("Erich K{\\\"a}stner");
    }
}
