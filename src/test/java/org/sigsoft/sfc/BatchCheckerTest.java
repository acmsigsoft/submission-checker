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

import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

public class BatchCheckerTest {

    @Test
    public void testMain() throws IOException, ParseException, URISyntaxException {
        String paper = resource("icse2017-paper13.pdf");
        String meta = resource("icse2017-authors.csv");
        String[] args = {"--style", "IEEE", "--meta", meta, paper};
        BatchChecker.main(args);
    }

    @Test
    public void testJustFile() throws IOException, ParseException, URISyntaxException {
        String paper = resource("icse2017-paper13.pdf");
        BatchChecker.main(new String[]{paper});
    }

    private String resource(String name) throws URISyntaxException {
        return Paths.get(ClassLoader.getSystemResource(name).toURI()).toString();
    }
}
