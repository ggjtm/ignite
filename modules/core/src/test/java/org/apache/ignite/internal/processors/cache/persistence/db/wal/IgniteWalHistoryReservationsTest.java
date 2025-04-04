/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache.persistence.db.wal;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.WALMode;
import org.apache.ignite.failure.StopNodeFailureHandler;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.TestRecordingCommunicationSpi;
import org.apache.ignite.internal.processors.cache.distributed.dht.preloader.GridDhtPartitionsSingleMessage;
import org.apache.ignite.internal.processors.cache.distributed.dht.topology.GridDhtLocalPartition;
import org.apache.ignite.internal.processors.cache.persistence.wal.FileWriteAheadLogManager;
import org.apache.ignite.internal.util.lang.GridAbsPredicate;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.WithSystemProperty;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import static org.apache.ignite.IgniteSystemProperties.IGNITE_DEFAULT_DISK_PAGE_COMPRESSION;
import static org.apache.ignite.IgniteSystemProperties.IGNITE_PDS_WAL_REBALANCE_THRESHOLD;

/**
 *
 */
public class IgniteWalHistoryReservationsTest extends GridCommonAbstractTest {
    /** */
    private volatile boolean client;

    /** */
    private WALMode walMode;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        cfg.setCommunicationSpi(new TestRecordingCommunicationSpi());

        cfg.setClientMode(client);

        cfg.setConsistentId("NODE$" + gridName.charAt(gridName.length() - 1));

        cfg.setFailureHandler(new StopNodeFailureHandler());

        DataStorageConfiguration memCfg = new DataStorageConfiguration()
            .setDefaultDataRegionConfiguration(
                new DataRegionConfiguration()
                    .setMaxSize(200L * 1024 * 1024)
                    .setPersistenceEnabled(true))
            .setWalMode(walMode)
            .setWalSegmentSize(512 * 1024);

        cfg.setDataStorageConfiguration(memCfg);

        CacheConfiguration ccfg1 = new CacheConfiguration();

        ccfg1.setName("cache1");
        ccfg1.setBackups(1);
        ccfg1.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        ccfg1.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);
        ccfg1.setAffinity(new RendezvousAffinityFunction(false, 32));

        cfg.setCacheConfiguration(ccfg1);

        cfg.setFailureDetectionTimeout(20_000);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        stopAllGrids();

        cleanPersistenceDir();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        walMode = WALMode.LOG_ONLY;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        System.clearProperty(IGNITE_PDS_WAL_REBALANCE_THRESHOLD);

        client = false;

        stopAllGrids();

        cleanPersistenceDir();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testReservedOnExchange() throws Exception {
        System.setProperty(IGNITE_PDS_WAL_REBALANCE_THRESHOLD, "0");

        final int entryCnt = 10_000;
        final int initGridCnt = 4;

        final IgniteEx ig0 = (IgniteEx)startGrids(initGridCnt + 1);

        ig0.cluster().state(ClusterState.ACTIVE);

        stopGrid(initGridCnt);

        Assert.assertEquals(5, ig0.context().state().clusterState().baselineTopology().consistentIds().size());

        long start = U.currentTimeMillis();

        log.warning("Start loading");

        try (IgniteDataStreamer<Object, Object> st = ig0.dataStreamer("cache1")) {
            for (int k = 0; k < entryCnt; k++) {
                st.addData(k, k);

                printProgress(k);
            }
        }

        log.warning("Finish loading time:" + (U.currentTimeMillis() - start));

        forceCheckpoint();

        start = U.currentTimeMillis();

        log.warning("Start loading");

        try (IgniteDataStreamer<Object, Object> st = ig0.dataStreamer("cache1")) {
            st.allowOverwrite(true);

            for (int k = 0; k < entryCnt; k++) {
                st.addData(k, k * 2);

                printProgress(k);
            }
        }

        log.warning("Finish loading time:" + (U.currentTimeMillis() - start));

        forceCheckpoint();

        start = U.currentTimeMillis();

        log.warning("Start loading");

        try (IgniteDataStreamer<Object, Object> st = ig0.dataStreamer("cache1")) {
            st.allowOverwrite(true);

            for (int k = 0; k < entryCnt; k++) {
                st.addData(k, k);

                printProgress(k);
            }
        }

        log.warning("Finish loading time:" + (U.currentTimeMillis() - start));

        forceCheckpoint();

        TestRecordingCommunicationSpi spi = new TestRecordingCommunicationSpi();

        spi.blockMessages((node, msg) -> {
            if (msg instanceof GridDhtPartitionsSingleMessage) {
                GridDhtPartitionsSingleMessage sm = (GridDhtPartitionsSingleMessage)msg;

                return sm.exchangeId() != null;
            }

            return false;
        });

        GridTestUtils.runAsync(new Runnable() {
            @Override public void run() {
                try {
                    IgniteConfiguration cfg = getConfiguration(getTestIgniteInstanceName(initGridCnt));

                    cfg.setCommunicationSpi(spi);

                    startGrid(optimize(cfg));
                }
                catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        });

        spi.waitForBlocked();

        boolean reserved = GridTestUtils.waitForCondition(new GridAbsPredicate() {
            @Override public boolean apply() {
                for (int g = 0; g < initGridCnt; g++) {
                    IgniteEx ig = grid(g);

                    if (isReserveListEmpty(ig))
                        return false;
                }

                return true;
            }
        }, 10_000);

        assert reserved;

        spi.stopBlock();

        boolean released = GridTestUtils.waitForCondition(new GridAbsPredicate() {
            @Override public boolean apply() {
                for (int g = 0; g < initGridCnt; g++) {
                    IgniteEx ig = grid(g);

                    if (!isReserveListEmpty(ig))
                        return false;
                }

                return true;
            }
        }, 10_000);

        assert released;
    }

    /**
     * @return {@code true} if reserve list is empty.
     */
    private boolean isReserveListEmpty(IgniteEx ig) {
        FileWriteAheadLogManager wal = (FileWriteAheadLogManager)ig.context().cache().context().wal();

        Object segmentAware = GridTestUtils.getFieldValue(wal, "segmentAware");

        synchronized (segmentAware) {
            Map reserved = GridTestUtils.getFieldValue(GridTestUtils.getFieldValue(segmentAware, "reservationStorage"), "reserved");

            if (reserved.isEmpty())
                return true;
        }
        return false;
    }

    /**
     *
     */
    private void printProgress(int k) {
        if (k % 1000 == 0)
            log.warning("Keys -> " + k);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testRemovesArePreloadedIfHistoryIsAvailable() throws Exception {
        int entryCnt = 10_000;

        IgniteEx ig0 = (IgniteEx)startGrids(2);

        ig0.cluster().state(ClusterState.ACTIVE);

        IgniteCache<Integer, Integer> cache = ig0.cache("cache1");

        for (int k = 0; k < entryCnt; k++)
            cache.put(k, k);

        stopGrid(1);

        for (int k = 0; k < entryCnt; k += 2)
            cache.remove(k);

        IgniteEx ig1 = startGrid(1);

        awaitPartitionMapExchange();

        IgniteCache<Integer, Integer> cache1 = ig1.cache("cache1");

        assertEquals(entryCnt / 2, cache.size());
        assertEquals(entryCnt / 2, cache1.size());

        for (Integer k = 0; k < entryCnt; k++) {
            if (k % 2 == 0) {
                assertTrue("k=" + k, !cache.containsKey(k));
                assertTrue("k=" + k, !cache1.containsKey(k));
            }
            else {
                assertEquals("k=" + k, k, cache.get(k));
                assertEquals("k=" + k, k, cache1.get(k));
            }
        }

        for (int p = 0; p < ig1.affinity("cache1").partitions(); p++) {
            GridDhtLocalPartition p0 = ig0.context().cache().cache("cache1").context().topology().localPartition(p);
            GridDhtLocalPartition p1 = ig1.context().cache().cache("cache1").context().topology().localPartition(p);

            assertTrue(p0.updateCounter() > 0);
            assertEquals(p0.updateCounter(), p1.updateCounter());
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testNodeIsClearedIfHistoryIsUnavailable() throws Exception {
        int entryCnt = 10_000;

        IgniteEx ig0 = (IgniteEx)startGrids(2);

        ig0.cluster().state(ClusterState.ACTIVE);

        IgniteCache<Integer, Integer> cache = ig0.cache("cache1");

        for (int k = 0; k < entryCnt; k++)
            cache.put(k, k);

        forceCheckpoint();

        stopGrid(1);

        for (int k = 0; k < entryCnt; k += 2)
            cache.remove(k);

        forceCheckpoint();

        for (Integer k = 0; k < entryCnt; k++) {
            if (k % 2 == 0)
                assertTrue("k=" + k, !cache.containsKey(k));
            else
                assertEquals("k=" + k, k, cache.get(k));
        }

        IgniteEx ig1 = startGrid(1);

        awaitPartitionMapExchange();

        IgniteCache<Integer, Integer> cache1 = ig1.cache("cache1");

        assertEquals(entryCnt / 2, cache.size());
        assertEquals(entryCnt / 2, cache1.size());

        for (Integer k = 0; k < entryCnt; k++) {
            if (k % 2 == 0) {
                assertTrue("k=" + k, !cache.containsKey(k));
                assertTrue("k=" + k, !cache1.containsKey(k));
            }
            else {
                assertEquals("k=" + k, k, cache.get(k));
                assertEquals("k=" + k, k, cache1.get(k));
            }
        }

        for (int p = 0; p < ig1.affinity("cache1").partitions(); p++) {
            GridDhtLocalPartition p0 = ig0.context().cache().cache("cache1").context().topology().localPartition(p);
            GridDhtLocalPartition p1 = ig1.context().cache().cache("cache1").context().topology().localPartition(p);

            assertTrue(p0.updateCounter() > 0);
            assertEquals(p0.updateCounter(), p1.updateCounter());
        }
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testWalHistoryPartiallyRemoved() throws Exception {
        Assume.assumeTrue(
            "https://issues.apache.org/jira/browse/IGNITE-16891",
            IgniteSystemProperties.getString(IGNITE_DEFAULT_DISK_PAGE_COMPRESSION) == null
        );

        int entryCnt = 9_500;

        IgniteEx ig0 = startGrids(2);

        ig0.cluster().state(ClusterState.ACTIVE);

        IgniteCache<Integer, Integer> cache = ig0.cache("cache1");

        for (int k = 0; k < entryCnt; k++)
            cache.put(k, k);

        GridTestUtils.runAsync(new Callable<Object>() {
            @Override public Object call() throws Exception {
                forceCheckpoint();

                return null;
            }
        });

        File walArchPath = ig0.context().pdsFolderResolver().fileTree().walArchive();

        stopAllGrids();

        U.delete(walArchPath);

        startGrid(0);

        Ignite ig1 = startGrid(1);

        ig1.cluster().state(ClusterState.ACTIVE);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testNodeLeftDuringExchange() throws Exception {
        System.setProperty(IGNITE_PDS_WAL_REBALANCE_THRESHOLD, "0");

        final int entryCnt = 10_000;
        final int initGridCnt = 4;

        final Ignite ig0 = startGrids(initGridCnt);

        ig0.cluster().state(ClusterState.ACTIVE);

        IgniteCache<Object, Object> cache = ig0.cache("cache1");

        for (int k = 0; k < entryCnt; k++)
            cache.put(k, k);

        forceCheckpoint();

        TestRecordingCommunicationSpi spi = new TestRecordingCommunicationSpi();

        spi.blockMessages((node, msg) -> {
            if (msg instanceof GridDhtPartitionsSingleMessage) {
                GridDhtPartitionsSingleMessage sm = (GridDhtPartitionsSingleMessage)msg;

                return sm.exchangeId() != null;
            }

            return false;
        });

        GridTestUtils.runAsync(new Runnable() {
            @Override public void run() {
                try {
                    IgniteConfiguration cfg = getConfiguration(getTestIgniteInstanceName(initGridCnt));

                    cfg.setCommunicationSpi(spi);

                    startGrid(optimize(cfg));
                }
                catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        });

        spi.waitForBlocked();

        boolean reserved = GridTestUtils.waitForCondition(new GridAbsPredicate() {
            @Override public boolean apply() {
                for (int g = 0; g < initGridCnt; g++) {
                    IgniteEx ig = grid(g);

                    if (isReserveListEmpty(ig))
                        return false;
                }

                return true;
            }
        }, 10_000);

        assert reserved;

        spi.stopBlock();

        stopGrid(getTestIgniteInstanceName(initGridCnt - 1), true, false);

        boolean released = GridTestUtils.waitForCondition(new GridAbsPredicate() {
            @Override public boolean apply() {
                for (int g = 0; g < initGridCnt - 1; g++) {
                    IgniteEx ig = grid(g);

                    if (!isReserveListEmpty(ig))
                        return false;
                }

                return true;
            }
        }, 10_000);

        assert released;

        awaitPartitionMapExchange();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    @WithSystemProperty(key = IGNITE_PDS_WAL_REBALANCE_THRESHOLD, value = "0")
    public void testCheckpointsNotReserveWithWalModeNone() throws Exception {
        walMode = WALMode.NONE;

        IgniteEx grid = startGrids(2);

        grid.cluster().state(ClusterState.ACTIVE);

        IgniteCache<Object, Object> cache = grid.createCache(new CacheConfiguration<>("cache").setBackups(1));

        for (int i = 0; i < 1000; i++)
            cache.put(i, i);

        stopGrid(1);

        for (int i = 1000; i < 2000; i++)
            cache.put(i, i);

        startGrid(1);
    }
}
