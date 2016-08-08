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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import io.crate.analyze.EvaluatingNormalizer;
import io.crate.analyze.symbol.Literal;
import io.crate.analyze.symbol.Symbol;
import io.crate.analyze.where.EqualityExtractor;
import io.crate.executor.transport.TransportActionProvider;
import io.crate.metadata.*;
import io.crate.metadata.sys.SysNodesTableInfo;
import io.crate.operation.collect.CollectInputSymbolVisitor;
import io.crate.operation.collect.CrateCollector;
import io.crate.operation.collect.JobCollectContext;
import io.crate.operation.collect.RowsCollector;
import io.crate.operation.collect.collectors.NodeStatsCollector;
import io.crate.operation.projectors.RowReceiver;
import io.crate.operation.reference.sys.RowContextReferenceResolver;
import io.crate.planner.node.dql.CollectPhase;
import io.crate.planner.node.dql.RoutedCollectPhase;
import io.crate.types.DataTypes;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Singleton;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Singleton
public class NodeStatsCollectSource implements CollectSource {

    private final TransportActionProvider transportActionProvider;
    private final ClusterService clusterService;
    private final CollectInputSymbolVisitor<RowCollectExpression<?, ?>> inputSymbolVisitor;
    private final EqualityExtractor eqExtractor;

    @Inject
    public NodeStatsCollectSource(TransportActionProvider transportActionProvider,
                                  ClusterService clusterService,
                                  Functions functions) {
        this.transportActionProvider = transportActionProvider;
        this.clusterService = clusterService;
        this.inputSymbolVisitor = new CollectInputSymbolVisitor<>(functions, RowContextReferenceResolver.INSTANCE);
        EvaluatingNormalizer normalizer = new EvaluatingNormalizer(functions, RowGranularity.NODE, RowContextReferenceResolver.INSTANCE);
        this.eqExtractor = new EqualityExtractor(normalizer);
    }

    @Override
    public Collection<CrateCollector> getCollectors(CollectPhase phase, RowReceiver downstream, JobCollectContext jobCollectContext) {
        RoutedCollectPhase collectPhase = (RoutedCollectPhase) phase;
        if (collectPhase.whereClause().noMatch()) {
            return ImmutableList.<CrateCollector>of(RowsCollector.empty(downstream));
        }
        final List<List<Symbol>> exactMatches = this.eqExtractor.extractParentMatches(
                ImmutableList.<ColumnIdent>builder()
                    .add(SysNodesTableInfo.Columns.ID)
                    .build(),
                collectPhase.whereClause().query(),
                new StmtCtx()
        );
        Iterator<DiscoveryNode> nodes = clusterService.state().getNodes().iterator();
        if (exactMatches != null) {
            nodes = Iterators.filter(
                    clusterService.state().getNodes().iterator(),
                    new Predicate<DiscoveryNode>() {
                        @Override
                        public boolean apply(@Nullable DiscoveryNode input) {
                            for (List<Symbol> exactMatch : exactMatches) {
                                for (Symbol match : exactMatch) {
                                    if (match instanceof Literal
                                            && match.valueType().equals(DataTypes.STRING)) {
                                        if(((BytesRef)((Literal) match).value()).utf8ToString().equals(input.id())) {
                                            return true;
                                        }
                                    }
                                }
                            }
                            return false;
                        }
                    });
        }
        List<DiscoveryNode> newNodes = Lists.newArrayList(nodes);
        System.out.println(">>>>>>> new nodes: "+newNodes);
        return ImmutableList.<CrateCollector>of(new NodeStatsCollector(
                transportActionProvider.transportStatTablesActionProvider(),
                downstream,
                collectPhase,
                newNodes,
                inputSymbolVisitor
            )
        );
    }
}
