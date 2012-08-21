/*
 * Copyright 2012 Midokura Pte. Ltd.
 */

package com.midonet.cluster;

import java.util.Arrays;
import java.util.UUID;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

import com.midokura.midolman.Setup;
import com.midokura.midolman.config.MidolmanConfig;
import com.midokura.midolman.config.ZookeeperConfig;
import com.midokura.midolman.guice.cluster.ClusterClientModule;
import com.midokura.midolman.guice.config.MockConfigProviderModule;
import com.midokura.midolman.guice.config.TypedConfigModule;
import com.midokura.midolman.guice.reactor.ReactorModule;
import com.midokura.midolman.guice.zookeeper.MockZookeeperConnectionModule;
import com.midokura.midolman.state.Directory;
import com.midokura.midolman.state.MacPortMap;
import com.midokura.midolman.state.ReplicatedMap;
import com.midokura.midolman.state.StateAccessException;
import com.midokura.midolman.state.ZkStateSerializationException;
import com.midokura.midolman.state.zkManagers.BridgeZkManager;
import com.midokura.midonet.cluster.Client;
import com.midokura.midonet.cluster.client.BridgeBuilder;
import com.midokura.midonet.cluster.client.Builder;
import com.midokura.midonet.cluster.client.DeviceBuilder;
import com.midokura.midonet.cluster.client.MacLearningTable;
import com.midokura.midonet.cluster.client.SourceNatResource;
import com.midokura.packets.MAC;
import com.midokura.util.functors.Callback1;
import com.midokura.util.functors.Callback3;

public class LocalClientImplTest {
    
    private Logger log = LoggerFactory.getLogger(LocalClientImplTest.class);

    @Inject
    Client client;
    Injector injector = null;
    String zkRoot = "/test/v3/midolman";


    HierarchicalConfiguration fillConfig(HierarchicalConfiguration config) {
        config.addNodes(ZookeeperConfig.GROUP_NAME,
                        Arrays.asList(new HierarchicalConfiguration.Node
                                      ("midolman_root_key", zkRoot)));
        return config;

    }

    BridgeZkManager getBridgeZkManager() {
        return injector.getInstance(BridgeZkManager.class);
    }

    Directory zkDir() {
        return injector.getInstance(Directory.class);
    }

    @Before
    public void initialize() {
        HierarchicalConfiguration config = fillConfig(
            new HierarchicalConfiguration());
        injector = Guice.createInjector(
            new MockConfigProviderModule(config),
            new MockZookeeperConnectionModule(),
            new TypedConfigModule<MidolmanConfig>(MidolmanConfig.class),

            new ReactorModule(),
            new ClusterClientModule()
        );
        injector.injectMembers(this);

    }


    @Test
    public void getBridgeTest()
        throws StateAccessException, InterruptedException, KeeperException {

        initializeZKStructure();
        Setup.createZkDirectoryStructure(zkDir(), zkRoot);
        UUID bridgeId = getBridgeZkManager().create(
            new BridgeZkManager.BridgeConfig("test", null, null));
        TestBridgeBuilder bridgeBuilder = new TestBridgeBuilder();
        client.getBridge(bridgeId, bridgeBuilder);
        Thread.sleep(2000);
        assertThat("Build is called", bridgeBuilder.getBuildCallsCount(),
                   equalTo(1));
        // let's cause a bridge update
        getBridgeZkManager().update(bridgeId, new BridgeZkManager.BridgeConfig("test1",
            UUID.randomUUID(), UUID.randomUUID()));
        Thread.sleep(2000);
        assertThat("Bridge update was notified",
                   bridgeBuilder.getBuildCallsCount(), equalTo(2));

    }
    
    @Test
    public void MacPortMapTest() throws InterruptedException, KeeperException, ZkStateSerializationException, StateAccessException {
        initializeZKStructure();
        Setup.createZkDirectoryStructure(zkDir(), zkRoot);
        UUID bridgeId = getBridgeZkManager().create(
            new BridgeZkManager.BridgeConfig("test", UUID.randomUUID(), UUID.randomUUID()));
        TestBridgeBuilder bridgeBuilder = new TestBridgeBuilder();
        client.getBridge(bridgeId, bridgeBuilder);
        Thread.sleep(2000);
        assertThat("Build is called", bridgeBuilder.getBuildCallsCount(), equalTo(1));
        
        // and a new packet. 
        MAC mac = MAC.random(); 
        UUID portUUID = UUID.randomUUID(); 
        
        ///////////
        // This sends two notifications. 
        bridgeBuilder.simulateNewPacket(mac,portUUID); 
        ///////////
        
        Thread.sleep(2000);
        // make sure the  notifications sent what we expected.
        assertEquals(bridgeBuilder.getNotifiedMAC(), mac); 
        assertNull(bridgeBuilder.getNotifiedUUID()[0]); 
        assertEquals(portUUID, bridgeBuilder.getNotifiedUUID()[1]);
        
        // make sure the packet is there. 
        assertEquals(portUUID, bridgeBuilder.getPort(mac)); 
        
        // remove the port. 
        bridgeBuilder.removePort(mac, portUUID); 
        // make sure the  notifications sent what we expected.
        Thread.sleep(2000);
        assertEquals(bridgeBuilder.getNotifiedMAC(), mac); 
        assertEquals(portUUID, bridgeBuilder.getNotifiedUUID()[0]); 
        assertNull(bridgeBuilder.getNotifiedUUID()[1]);
        
        // make sure that the mac <-> port association has been removed. 
        assertNull(bridgeBuilder.getPort(mac)); 
        
        assertThat("Bridge update was notified", bridgeBuilder.getBuildCallsCount(), equalTo(1));
    }

    void initializeZKStructure() throws InterruptedException, KeeperException {

        String[] nodes = zkRoot.split("/");
        String path = "/";
        for (String node : nodes) {
            if (!node.isEmpty()) {
                zkDir().add(path + node, null, CreateMode.PERSISTENT);
                path += node;
                path += "/";
            }
        }
    }

    // hint could modify this class so we can get the map from it. 
    class TestBridgeBuilder implements BridgeBuilder {
        int buildCallsCount = 0;
        MacLearningTable mlTable; 
        MAC[] notifiedMAC = new MAC[1]; 
        UUID[] notifiedUUID = new UUID[2]; 
        
        public void simulateNewPacket(MAC mac, UUID portId) {
            mlTable.add(mac, portId); 
        }

        public void removePort(MAC mac, UUID portId) {
            mlTable.remove(mac, portId); 
        }
        
        public UUID getPort(MAC mac) {
            final UUID result[] = new UUID[1]; 
            
            mlTable.get(mac, new Callback1<UUID>() {
                @Override
                public void call(UUID v) {
                    result[0] = v; 
                }
            });
            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            } 
            return result[0]; 
        }

        public MAC getNotifiedMAC() {
            return notifiedMAC[0]; 
        }
        
        public UUID[] getNotifiedUUID() {
            return notifiedUUID; 
        }
       
        public int getBuildCallsCount() {
            return buildCallsCount;
        }

        @Override
        public void setTunnelKey(long key) {
        }

        @Override
        public void setMacLearningTable(MacLearningTable table) {
            mlTable = table; 
        }

        @Override
        public void setSourceNatResource(SourceNatResource resource) {
        }

        @Override
        public DeviceBuilder setID(UUID id) {
            return this;
        }

        @Override
        public DeviceBuilder setInFilter(UUID filterID) {
            return this;
        }

        @Override
        public DeviceBuilder setOutFilter(UUID filterID) {
            return this;
        }

        @Override
        public Builder start() {
            return this;
        }

        @Override
        public void build() {
            buildCallsCount++;
            // add the callback 
            mlTable.notify(new Callback3<MAC,UUID,UUID>() {
                @Override
                public void call(MAC mac, UUID oldPortID, UUID newPortID) {
                    notifiedMAC[0] = mac; 
                    notifiedUUID[0] = oldPortID; 
                    notifiedUUID[1] = newPortID; 
                }
            }); 
        }

    }
}