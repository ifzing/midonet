/*
 * Copyright (c) 2012 Midokura Pte. Ltd
 */

package com.midokura.midolman.monitoring;

import com.google.inject.*;
import com.midokura.midolman.host.services.HostService;
import com.midokura.midolman.monitoring.metrics.VMMetricsCollection;
import com.midokura.midolman.monitoring.store.CassandraStore;
import com.midokura.midolman.monitoring.store.Store;
import org.apache.cassandra.config.ConfigurationException;
import org.apache.thrift.transport.TTransportException;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

public class ReporterVmMetricsTest extends AbstractModule {

    String metricThreadCount = "ThreadCount";
    int replicationFactor = 1;
    int ttlInSecs = 1000;
    long startTime, endTime;

    @Test
    public void reporterTest() throws InterruptedException, UnknownHostException {

        Injector injector = Guice.createInjector(new ReporterVmMetricsTest());
        VMMetricsCollection vmMetrics = injector.getInstance(VMMetricsCollection.class);
        vmMetrics.registerMetrics();
        Store store = new CassandraStore("localhost:9171",
                "Mido Cluster",
                "MM_Monitoring",
                "TestColumnFamily",
                replicationFactor, ttlInSecs);
        startTime = System.currentTimeMillis();
        MidoReporter reporter = new MidoReporter(store, "MidonetMonitoring");
        reporter.start(1000, TimeUnit.MILLISECONDS);
        Thread.sleep(10000);
        List<String> metrics = store.getMetricsForType(VMMetricsCollection.class.getSimpleName());
        assertThat("We saved all the metrics in VMMetricsCollection", metrics.size(), is(VMMetricsCollection.getMetricsCount()));

        String hostName = InetAddress.getLocalHost().getHostName();

        List<String> types = store.getMetricsTypeForTarget(hostName);
        assertThat("We saved the type VMMetricsCollection for this target", types.size(), is(1));
        assertThat("We save the right type", types.get(0), is(VMMetricsCollection.class.getSimpleName()));
        endTime = System.currentTimeMillis();

        Map<String, Long> res = store.getTSPoints(VMMetricsCollection.class.getSimpleName(), hostName, metricThreadCount, startTime, endTime);
        assertThat("We collected some TS point", res.size(), greaterThan(0));

    }

    @Before
    public void startUp() throws IOException, TTransportException, ConfigurationException, InterruptedException {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
    }

    @After
    public void cleanUp() {
        EmbeddedCassandraServerHelper.stopEmbeddedCassandra();
    }

    @Override
    protected void configure() {
        NodeAgentHostIdProvider provider = new NodeAgentHostIdProvider(new HostService());
        bind(HostIdProvider.class).toInstance(provider);
    }

}