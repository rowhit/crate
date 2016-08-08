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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Parameters {

    public static final Parameters EMPTY = new Parameters(Collections.emptyList());
    public static final List<Parameters> EMPTY_BULK = Collections.emptyList();
    private final List<?> params;

    public static List<Parameters> toBulkParams(Object[][] bulkArgs) {
        List<Parameters> bulkParams = new ArrayList<>(bulkArgs.length);
        for (Object[] bulkArg : bulkArgs) {
            bulkParams.add(new Parameters(Arrays.asList(bulkArg)));
        }
        return bulkParams;
    }

    public static List<Parameters> toBulkParams(List<List<Object>> bulkArgs) {
        List<Parameters> bulkParams = new ArrayList<>(bulkArgs.size());
        for (List<Object> bulkArg : bulkArgs) {
            bulkParams.add(new Parameters(bulkArg));
        }
        return bulkParams;
    }

    public Parameters(Object... args) {
        this(Arrays.asList(args));
    }

    public Parameters(List<?> params) {
        this.params = params;
    }

    public int size() {
        return params.size();
    }

    public Object get(int index) {
        return params.get(index);
    }

    public Object[] toArray() {
        return params.toArray(new Object[0]);
    }
}
