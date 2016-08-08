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

package io.crate.operation.collect.sources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.crate.action.sql.SQLResponse;
import io.crate.analyze.WhereClause;
import io.crate.analyze.symbol.Reference;
import io.crate.analyze.symbol.Symbol;
import io.crate.integrationtests.SQLTransportIntegrationTest;
import io.crate.metadata.*;
import io.crate.planner.distribution.DistributionInfo;
import io.crate.planner.node.dql.RoutedCollectPhase;
import io.crate.planner.projection.Projection;
import io.crate.types.DataTypes;
import org.elasticsearch.test.ESIntegTestCase;
import org.junit.Test;

import java.util.*;

@ESIntegTestCase.ClusterScope(minNumDataNodes = 2)
public class NodeStatsCollectSourceTest extends SQLTransportIntegrationTest{

    @Test
    public void testFilterNodes() throws Exception {
        final List<String> nodes = Arrays.asList(internalCluster().getNodeNames());
        final String node = randomFrom(nodes);

        // TODO: check what's going wrong when only selecting id
        SQLResponse res = execute("select id, name, hostname from sys.nodes where name = ?", new Object[]{node});
        String id = (String)res.rows()[0][0];

        System.out.println(">>>>>>> intersting part:");
        execute("select id from sys.nodes where id = ?", new Object[]{id});

        NodeStatsCollectSource nodeStatsCollectSource = internalCluster().getInstance(NodeStatsCollectSource.class);
        Reference shardId = new Reference(new ReferenceInfo(
                new ReferenceIdent(new TableIdent("sys", "shards"), "id"), RowGranularity.SHARD, DataTypes.INTEGER));
        RoutedCollectPhase collectPhase;
        collectPhase = new RoutedCollectPhase(
                UUID.randomUUID(),
                1,
                "collect",
                new Routing(ImmutableMap.<String, Map<String, List<Integer>>>of()),
                RowGranularity.SHARD,
                Collections.<Symbol>singletonList(shardId),
                ImmutableList.<Projection>of(),
                WhereClause.MATCH_ALL,
                DistributionInfo.DEFAULT_BROADCAST
        );
    }
}
