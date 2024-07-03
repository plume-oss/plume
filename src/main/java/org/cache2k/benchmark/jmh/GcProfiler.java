package org.cache2k.benchmark.jmh;

/*
 * #%L
 * Benchmarks: JMH suite.
 * %%
 * Copyright (C) 2013 - 2021 headissue GmbH, Munich
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/*
 * Copied from JMH GCProfiler. Original copyright:
 *
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ScalarResult;
import org.openjdk.jmh.util.HashMultiset;
import org.openjdk.jmh.util.Multiset;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GcProfiler implements InternalProfiler {

    private long beforeTime;
    private long beforeGCCount;
    private long beforeGCTime;
    private HotspotAllocationSnapshot beforeAllocated;
    private final NotificationListener listener;
    private volatile Multiset<String> churn;
    private List<Long> usedAfterGc = Collections.synchronizedList(new ArrayList<>());
    private List<Long> committedAfterGc = Collections.synchronizedList(new ArrayList<>());

    public GcProfiler() throws ProfilerException {
        churn = new HashMultiset<String>();

        NotificationListener listener;
        try {
            final Class<?> infoKlass = Class.forName("com.sun.management.GarbageCollectionNotificationInfo");
            final Field notifNameField = infoKlass.getField("GARBAGE_COLLECTION_NOTIFICATION");
            final Method infoMethod = infoKlass.getMethod("from", CompositeData.class);
            final Method getGcInfo = infoKlass.getMethod("getGcInfo");
            final Method getMemoryUsageBeforeGc = getGcInfo.getReturnType().getMethod("getMemoryUsageBeforeGc");
            final Method getMemoryUsageAfterGc = getGcInfo.getReturnType().getMethod("getMemoryUsageAfterGc");

            listener = new NotificationListener() {
                @Override
                public void handleNotification(Notification n, Object o) {
                    try {
                        if (n.getType().equals(notifNameField.get(null))) {
                            StringBuilder debugLine = new StringBuilder();
                            Object info = infoMethod.invoke(null, n.getUserData());
                            Object gcInfo = getGcInfo.invoke(info);
                            Map<String, MemoryUsage> mapBefore = (Map<String, MemoryUsage>) getMemoryUsageBeforeGc.invoke(gcInfo);
                            Map<String, MemoryUsage> mapAfter = (Map<String, MemoryUsage>) getMemoryUsageAfterGc.invoke(gcInfo);
                            long committed = 0;
                            long used = 0;
                            for (Map.Entry<String, MemoryUsage> entry : mapAfter.entrySet()) {
                                String name = entry.getKey();
                                MemoryUsage after = entry.getValue();
                                committed += after.getCommitted();
                                used += after.getUsed();
                                debugLine.append(name).append("=").append(after.getUsed()).append(", ");
                                MemoryUsage before = mapBefore.get(name);
                                long c = before.getUsed() - after.getUsed();
                                if (c > 0) {
                                    churn.add(name, c);
                                }
                            }
                            usedAfterGc.add(used);
                            committedAfterGc.add(committed);
                            System.out.println("[GC Notification Listener] " + debugLine + "Total used=" + used + ", Total committed=" + committed);
                        }
                    } catch (IllegalAccessException e) {
                    } catch (InvocationTargetException e) {
                    }
                }
            };
        } catch (ClassNotFoundException e) {
            throw new ProfilerException(e);
        } catch (NoSuchFieldException e) {
            throw new ProfilerException(e);
        } catch (NoSuchMethodException e) {
            throw new ProfilerException(e);
        }

        this.listener = listener;
    }

    @Override
    public String getDescription() {
        return "GC profiling via standard MBeans";
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        installHooks();

        long gcTime = 0;
        long gcCount = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += bean.getCollectionCount();
            gcTime += bean.getCollectionTime();
        }
        this.beforeGCCount = gcCount;
        this.beforeGCTime = gcTime;
        this.beforeAllocated = HotspotAllocationSnapshot.create();
        this.beforeTime = System.nanoTime();
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult iResult) {

        try {
            Thread.sleep(567);
        } catch (InterruptedException ignore) {

        }

        uninstallHooks();
        long afterTime = System.nanoTime();

        HotspotAllocationSnapshot newSnapshot = HotspotAllocationSnapshot.create();

        long gcTime = 0;
        long gcCount = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += bean.getCollectionCount();
            gcTime += bean.getCollectionTime();
        }

        List<Result<?>> results = new ArrayList<>();

        if (beforeAllocated == HotspotAllocationSnapshot.EMPTY) {
            results.add(new ScalarResult(Defaults.PREFIX + "gc.alloc.rate",
                    Double.NaN,
                    "MB/sec", AggregationPolicy.AVG));
        } else {
            long allocated = newSnapshot.subtract(beforeAllocated);
            results.add(new ScalarResult(Defaults.PREFIX + "gc.alloc.rate",
                    (afterTime != beforeTime) ?
                            1.0 * allocated / 1024 / 1024 * TimeUnit.SECONDS.toNanos(1) / (afterTime - beforeTime) :
                            Double.NaN,
                    "MB/sec", AggregationPolicy.AVG));
            if (allocated != 0) {
                long allOps = iResult.getMetadata().getAllOps();
                results.add(new ScalarResult(Defaults.PREFIX + "gc.alloc.rate.norm",
                        (allOps != 0) ?
                                1.0 * allocated / allOps :
                                Double.NaN,
                        "B/op", AggregationPolicy.AVG));
            }
        }

        results.add(new ScalarResult(
                Defaults.PREFIX + "gc.count",
                gcCount - beforeGCCount,
                "counts",
                AggregationPolicy.AVG));

        if (gcCount != beforeGCCount || gcTime != beforeGCTime) {
            results.add(new ScalarResult(
                    Defaults.PREFIX + "gc.time",
                    gcTime - beforeGCTime,
                    "ms",
                    AggregationPolicy.AVG));
        }

        for (String space : churn.keys()) {
            double churnRate = (afterTime != beforeTime) ?
                    1.0 * churn.count(space) * TimeUnit.SECONDS.toNanos(1) / (afterTime - beforeTime) / 1024 / 1024 :
                    Double.NaN;

            double churnNorm = 1.0 * churn.count(space) / iResult.getMetadata().getAllOps();

            String spaceName = space.replaceAll(" ", "_");

            results.add(new ScalarResult(
                    Defaults.PREFIX + "gc.churn." + spaceName + "",
                    churnRate,
                    "MB/sec",
                    AggregationPolicy.AVG));

            results.add(new ScalarResult(Defaults.PREFIX + "gc.churn." + spaceName + ".norm",
                    churnNorm,
                    "B/op",
                    AggregationPolicy.AVG));
        }

        if (!usedAfterGc.isEmpty()) {
            Collections.sort(usedAfterGc);
            long _maximumUsedAfterGc = usedAfterGc.get(usedAfterGc.size() - 1);
            results.add(new ScalarResult(Defaults.PREFIX + "gc.maximumUsedAfterGc",
                    _maximumUsedAfterGc,
                    "bytes",
                    AggregationPolicy.AVG));
            System.out.println("maximumUsedAfterGc=" + _maximumUsedAfterGc);
        }
        if (!committedAfterGc.isEmpty()) {
            Collections.sort(committedAfterGc);
            long _committedUsedAfterGc = committedAfterGc.get(committedAfterGc.size() - 1);
            results.add(new ScalarResult(Defaults.PREFIX + "gc.maximumCommittedAfterGc",
                    _committedUsedAfterGc,
                    "bytes",
                    AggregationPolicy.AVG));
            System.out.println("maximumCommittedAfterGc=" + _committedUsedAfterGc);
        }

        return results;
    }

    private boolean hooksInstalled;

    public synchronized void installHooks() {
        if (hooksInstalled) return;
        hooksInstalled = true;
        churn = new HashMultiset<String>();
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            ((NotificationEmitter) bean).addNotificationListener(listener, null, null);
        }
    }

    public synchronized void uninstallHooks() {
        if (!hooksInstalled) return;
        hooksInstalled = false;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            try {
                ((NotificationEmitter) bean).removeNotificationListener(listener);
            } catch (ListenerNotFoundException e) {
            }
        }
    }

    static class HotspotAllocationSnapshot {
        public final static HotspotAllocationSnapshot EMPTY = new HotspotAllocationSnapshot(new long[0], new long[0]);

        private static volatile Method GET_THREAD_ALLOCATED_BYTES;
        private static volatile boolean allocationNotAvailable;

        private final long[] threadIds;
        private final long[] allocatedBytes;

        private HotspotAllocationSnapshot(long[] threadIds, long[] allocatedBytes) {
            this.threadIds = threadIds;
            this.allocatedBytes = allocatedBytes;
        }

        /**
         * Takes a snapshot of thread allocation counters.
         * The method might allocate, however it is assumed that allocations made by "current thread" will
         * be excluded from the result while performing {@link HotspotAllocationSnapshot#subtract(HotspotAllocationSnapshot)}
         *
         * @return snapshot of thread allocation counters
         */
        public static HotspotAllocationSnapshot create() {
            Method getBytes = getAllocatedBytesGetter();
            if (getBytes == null) {
                return HotspotAllocationSnapshot.EMPTY;
            }
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            try {
                long[] threadIds = threadMXBean.getAllThreadIds();
                long[] allocatedBytes = (long[]) getBytes.invoke(threadMXBean, (Object) threadIds);
                return new HotspotAllocationSnapshot(threadIds, allocatedBytes);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
            return HotspotAllocationSnapshot.EMPTY;
        }

        /**
         * Estimates allocated bytes based on two snapshots.
         * The problem is threads can come and go while performing the benchmark,
         * thus we would miss allocations made in a thread that was created and died between the snapshots.
         * <p/>
         * <p>Current thread is intentionally excluded since it believed to execute jmh infrastructure code only.
         *
         * @return estimated number of allocated bytes between profiler calls
         */
        public long subtract(HotspotAllocationSnapshot other) {
            HashMap<Long, Integer> prevIndex = new HashMap<Long, Integer>();
            for (int i = 0; i < other.threadIds.length; i++) {
                long id = other.threadIds[i];
                prevIndex.put(id, i);
            }
            long currentThreadId = Thread.currentThread().getId();
            long allocated = 0;
            for (int i = 0; i < threadIds.length; i++) {
                long id = threadIds[i];
                if (id == currentThreadId) {
                    continue;
                }
                allocated += allocatedBytes[i];
                Integer prev = prevIndex.get(id);
                if (prev != null) {
                    allocated -= other.allocatedBytes[prev];
                }
            }
            return allocated;
        }

        private static Method getAllocatedBytesGetter() {
            Method getBytes = GET_THREAD_ALLOCATED_BYTES;
            if (getBytes != null || allocationNotAvailable) {
                return getBytes;
            }
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            try {
                getBytes = threadMXBean.getClass().getMethod("getThreadAllocatedBytes", long[].class);
                getBytes.setAccessible(true);
            } catch (Throwable e) { // To avoid jmh failure in case of incompatible JDK and/or inaccessible method
                getBytes = null;
                allocationNotAvailable = true;
                System.out.println("Allocation profiling is not available: " + e.getMessage());
            }
            GET_THREAD_ALLOCATED_BYTES = getBytes;
            return getBytes;
        }
    }

    static class Defaults {
        static final String PREFIX = "+c2k.";
    }

}