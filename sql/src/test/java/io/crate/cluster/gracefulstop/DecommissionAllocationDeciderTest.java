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

package io.crate.cluster.gracefulstop;

import io.crate.metadata.settings.CrateSettings;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.routing.RestoreSource;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.UnassignedInfo;
import org.elasticsearch.cluster.routing.allocation.RoutingAllocation;
import org.elasticsearch.cluster.routing.allocation.decider.Decision;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.settings.NodeSettingsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DecommissionAllocationDeciderTest {

    private RoutingAllocation routingAllocation;
    private ShardRouting primaryShard;
    private ShardRouting replicaShard;
    private RoutingNode n1;
    private RoutingNode n2;

    @Before
    public void setUp() throws Exception {
        routingAllocation = mock(RoutingAllocation.class);
        when(routingAllocation.decision(any(Decision.class), anyString(), anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[0];
            }
        });

        primaryShard = ShardRouting.newUnassigned(
            "t", 0, mock(RestoreSource.class), true, mock(UnassignedInfo.class));
        replicaShard = ShardRouting.newUnassigned(
            "t", 0, mock(RestoreSource.class), false, mock(UnassignedInfo.class));
        n1 = new RoutingNode("n1", mock(DiscoveryNode.class));
        n2 = new RoutingNode("n2", mock(DiscoveryNode.class));
    }

    @Test
    public void testShouldNotBeAbleToAllocatePrimaryOntoDecommissionedNode() throws Exception {
        Settings settings = Settings.builder()
            .put(DecommissioningService.DECOMMISSION_PREFIX + "n1", true).build();

        DecommissionAllocationDecider allocationDecider =
            new DecommissionAllocationDecider(settings, mock(NodeSettingsService.class));

        Decision decision = allocationDecider.canAllocate(primaryShard, n1, routingAllocation);
        assertThat(decision.type(), is(Decision.Type.NO));

        decision = allocationDecider.canAllocate(primaryShard, n2, routingAllocation);
        assertThat(decision.type(), is(Decision.Type.YES));
    }

    @Test
    public void testCanAlwaysAllocateIfDataAvailabilityIsNone() throws Exception {
        Settings settings = Settings.builder()
            .put(CrateSettings.GRACEFUL_STOP_MIN_AVAILABILITY.settingName(), "none")
            .put(DecommissioningService.DECOMMISSION_PREFIX + "n1", true).build();
        DecommissionAllocationDecider allocationDecider =
            new DecommissionAllocationDecider(settings, mock(NodeSettingsService.class));

        Decision decision = allocationDecider.canAllocate(primaryShard, n1, routingAllocation);
        assertThat(decision.type(), is(Decision.Type.YES));

        decision = allocationDecider.canAllocate(primaryShard, n2, routingAllocation);
        assertThat(decision.type(), is(Decision.Type.YES));
    }

    @Test
    public void testReplicasCanRemainButCannotAllocateOnDecommissionedNodeWithPrimariesDataAvailability() throws Exception {
        Settings settings = Settings.builder()
            .put(CrateSettings.GRACEFUL_STOP_MIN_AVAILABILITY.settingName(), "primaries")
            .put(DecommissioningService.DECOMMISSION_PREFIX + "n1", true).build();

        DecommissionAllocationDecider allocationDecider =
            new DecommissionAllocationDecider(settings, mock(NodeSettingsService.class));

        Decision decision = allocationDecider.canAllocate(replicaShard, n1, routingAllocation);
        assertThat(decision.type(), is(Decision.Type.NO));

        decision = allocationDecider.canRemain(replicaShard, n1, routingAllocation);
        assertThat(decision.type(), is(Decision.Type.YES));
    }

    @Test
    public void testCannotAllocatePrimaryOrReplicaIfDataAvailabilityIsFull() throws Exception {
        Settings settings = Settings.builder()
            .put(CrateSettings.GRACEFUL_STOP_MIN_AVAILABILITY.settingName(), "full")
            .put(DecommissioningService.DECOMMISSION_PREFIX + "n1", true).build();

        DecommissionAllocationDecider allocationDecider =
            new DecommissionAllocationDecider(settings, mock(NodeSettingsService.class));

        Decision decision = allocationDecider.canAllocate(replicaShard, n1, routingAllocation);
        assertThat(decision.type(), is(Decision.Type.NO));
        decision = allocationDecider.canRemain(replicaShard, n1, routingAllocation);
        assertThat(decision.type(), is(Decision.Type.NO));

        decision = allocationDecider.canAllocate(primaryShard, n1, routingAllocation);
        assertThat(decision.type(), is(Decision.Type.NO));
        decision = allocationDecider.canRemain(primaryShard, n1, routingAllocation);
        assertThat(decision.type(), is(Decision.Type.NO));
    }
}
