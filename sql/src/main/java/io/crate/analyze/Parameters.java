/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.analyze;

import io.crate.core.collections.Row;
import io.crate.core.collections.RowN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Parameters {

    private static final Object[] EMPTY_ROW = new Object[0];
    public static final Row EMPTY = new Row() {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public Object get(int index) {
            return null;
        }

        @Override
        public Object[] materialize() {
            return EMPTY_ROW;
        }
    };
    public static final List<Row> EMPTY_BULK = Collections.emptyList();

    public static List<Row> toBulkParams(Object[][] bulkArgs) {
        List<Row> bulkParams = new ArrayList<>(bulkArgs.length);
        for (Object[] bulkArg : bulkArgs) {
            bulkParams.add(new RowN(bulkArg));
        }
        return bulkParams;
    }

    public static List<Row> toBulkParams(List<List<Object>> bulkArgs) {
        List<Row> bulkParams = new ArrayList<>(bulkArgs.size());
        for (List<Object> bulkArg : bulkArgs) {
            bulkParams.add(new RowN(bulkArg.toArray(new Object[0])));
        }
        return bulkParams;
    }
}
