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

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.Aggregator;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.util.ListStatistics;
import org.openjdk.jmh.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Record the used heap memory of a benchmark iteration by forcing a full garbage collection.
 * Experimental, not recommended for usage. Use {@link HeapProfiler} instead
 *
 * @author Jens Wilke
 */
public class ForcedGcMemoryProfiler implements InternalProfiler {

    private static boolean runOnlyAfterLastIteration = true;
    @SuppressWarnings("unused")
    private static Object keepReference;
    private static long gcTimeMillis = -1;
    private static long usedHeapViaHistogram = -1;
    private static volatile boolean enabled = false;
    private static UsageTuple usageAfterIteration;
    private static UsageTuple usageAfterSettled;

    /**
     * The benchmark needs to hand over the reference so the memory is kept after
     * the shutdown of the benchmark and can be measured.
     */
    public static void keepReference(Object _rootReferenceToKeep) {
        if (enabled) {
            keepReference = _rootReferenceToKeep;
        }
    }

    public static UsageTuple getUsage() {
        MemoryUsage _heapUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage _nonHeapUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        long _usedHeapMemory = _heapUsage.getUsed();
        long _usedNonHeap = _nonHeapUsage.getUsed();
        System.err.println("[getMemoryMXBean] usedHeap=" + _usedHeapMemory + ", usedNonHeap=" + _usedNonHeap + ", totalUsed=" + (_usedHeapMemory + _usedNonHeap));
        System.err.println("[Runtime totalMemory-freeMemory] used memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        return new UsageTuple(_heapUsage, _nonHeapUsage);
    }

    /**
     * Called from the benchmark when the objects are still referenced to record the
     * used memory. Enforces a full garbage collection and records memory usage.
     * Waits and triggers GC again, as long as the memory is still reducing. Some workloads
     * needs some time until they drain queues and finish all the work.
     */
    public static void recordUsedMemory() {
        long t0 = System.currentTimeMillis();
        long usedMemorySettled;
        if (runSystemGC()) {
            usageAfterIteration = getUsage();
            long m2 = usageAfterIteration.getTotalUsed();
            do {
                try {
                    Thread.sleep(567);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                runSystemGC();
                usedMemorySettled = m2;
                usageAfterSettled = getUsage();
                m2 = usageAfterSettled.getTotalUsed();
            } while (m2 < usedMemorySettled);
            gcTimeMillis = System.currentTimeMillis() - t0;
        }
        usedHeapViaHistogram = printHeapHistogram(System.out, 30);
    }

    public static boolean runSystemGC() {
        List<GarbageCollectorMXBean> enabledBeans = new ArrayList<>();

        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = bean.getCollectionCount();
            if (count != -1) {
                enabledBeans.add(bean);
            }
        }

        long beforeGcCount = countGc(enabledBeans);

        System.runFinalization();
        System.gc();
        System.runFinalization();
        System.gc();

        final int MAX_WAIT_MSECS = 20 * 1000;
        final int STABLE_TIME_MSECS = 500;

        if (enabledBeans.isEmpty()) {
            System.err.println("WARNING: MXBeans can not report GC info.");
            return false;
        }

        boolean gcHappened = false;

        long start = System.nanoTime();
        long gcHappenedTime = 0;
        while (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < MAX_WAIT_MSECS) {
            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long afterGcCount = countGc(enabledBeans);

            if (!gcHappened) {
                if (afterGcCount - beforeGcCount >= 2) {
                    gcHappened = true;
                }
            }
            if (gcHappened) {
                if (afterGcCount == beforeGcCount) {
                    if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - gcHappenedTime) > STABLE_TIME_MSECS) {
                        return true;
                    }
                } else {
                    gcHappenedTime = System.nanoTime();
                    beforeGcCount = afterGcCount;
                }
            }
        }
        if (gcHappened) {
            System.err.println("WARNING: System.gc() was invoked but unable to wait while GC stopped, is GC too asynchronous?");
        } else {
            System.err.println("WARNING: System.gc() was invoked but couldn't detect a GC occurring, is System.gc() disabled?");
        }
        return false;
    }

    private static long countGc(final List<GarbageCollectorMXBean> _enabledBeans) {
        long cnt = 0;
        for (GarbageCollectorMXBean bean : _enabledBeans) {
            cnt += bean.getCollectionCount();
        }
        return cnt;
    }

    public static String getJmapExcutable() {
        String javaHome = System.getProperty("java.home");
        String jreDir = File.separator + "jre";
        if (javaHome.endsWith(jreDir)) {
            javaHome = javaHome.substring(0, javaHome.length() - jreDir.length());
        }
        return (javaHome +
                File.separator +
                "bin" +
                File.separator +
                "jmap" +
                (Utils.isWindows() ? ".exe" : ""));
    }

    public static long printHeapHistogram(PrintStream out, int _maxLines) {
        long _totalBytes = 0;
        boolean _partial = false;
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{
                    getJmapExcutable(),
                    "-histo",
                    Long.toString(Utils.getPid())});
            InputStream in = proc.getInputStream();
            LineNumberReader r = new LineNumberReader(new InputStreamReader(in));
            String s;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(buffer);
            while ((s = r.readLine()) != null) {
                if ( s.startsWith("Total")) {
                    ps.println(s);
                    String[] sa = s.split("\\s+");
                    _totalBytes = Long.parseLong(sa[2]);
                } else if (r.getLineNumber() <= _maxLines) {
                    ps.println(s);
                } else {
                    if (!_partial) {
                        ps.println("[ ... truncated ... ]");
                    }
                    _partial = true;
                }
            }
            r.close();
            in.close();
            ps.close();
            byte[] _histoOuptut = buffer.toByteArray();
            buffer = new ByteArrayOutputStream();
            ps = new PrintStream(buffer);
            ps.println("[Heap Histogram Live Objects] used=" + _totalBytes);
            ps.write(_histoOuptut);
            ps.println();
            ps.close();
            out.write(buffer.toByteArray());
        } catch (Exception ex) {
            System.err.println("ForcedGcMemoryProfiler: error attaching / reading histogram");
            ex.printStackTrace();
        }
        return _totalBytes;
    }

    int iterationNumber = 0;

    @Override
    public Collection<? extends Result> afterIteration(final BenchmarkParams benchmarkParams, final IterationParams iterationParams, final IterationResult result) {
        if (runOnlyAfterLastIteration) {
            if (iterationParams.getType() != IterationType.MEASUREMENT
                    || iterationParams.getCount() != ++iterationNumber) {
                return Collections.emptyList();
            }
        }
        recordUsedMemory();
        List<Result> l = new ArrayList<>();
        l.addAll(Arrays.asList(
                new OptionalScalarResult("+forced-gc-mem.gcTimeMillis", (double) gcTimeMillis, "ms", AggregationPolicy.AVG),
                new OptionalScalarResult("+forced-gc-mem.usedHeap", (double) usedHeapViaHistogram, "bytes", AggregationPolicy.AVG)
        ));
        if (usageAfterIteration != null) {
            // old metrics, t.b. removed
            l.addAll(Arrays.asList(
                    new OptionalScalarResult("+forced-gc-mem.used.settled", (double) usageAfterSettled.getTotalUsed(), "bytes", AggregationPolicy.AVG),
                    new OptionalScalarResult("+forced-gc-mem.used.after", (double) usageAfterIteration.getTotalUsed(), "bytes", AggregationPolicy.AVG),
                    new OptionalScalarResult("+forced-gc-mem.total", (double) usageAfterSettled.getTotalCommitted(), "bytes", AggregationPolicy.AVG)
            ));
            l.addAll(Arrays.asList(
                    new OptionalScalarResult("+forced-gc-mem.totalUsed", (double) usageAfterSettled.getTotalUsed(), "bytes", AggregationPolicy.AVG),
                    new OptionalScalarResult("+forced-gc-mem.totalUsed.after", (double) usageAfterIteration.getTotalUsed(), "bytes", AggregationPolicy.AVG),
                    new OptionalScalarResult("+forced-gc-mem.totalCommitted", (double) usageAfterSettled.getTotalCommitted(), "bytes", AggregationPolicy.AVG),
                    new OptionalScalarResult("+forced-gc-mem.totalCommitted.after", (double) usageAfterIteration.getTotalCommitted(), "bytes", AggregationPolicy.AVG),
                    new OptionalScalarResult("+forced-gc-mem.heapUsed", (double) usageAfterSettled.heap.getUsed(), "bytes", AggregationPolicy.AVG),
                    new OptionalScalarResult("+forced-gc-mem.heapUsed.after", (double) usageAfterIteration.heap.getUsed(), "bytes", AggregationPolicy.AVG)
            ));
        }
        LinuxVmProfiler.addLinuxVmStats("+forced-gc-mem.linuxVm", l);
        keepReference = null;
        return l;
    }

    @Override
    public void beforeIteration(final BenchmarkParams benchmarkParams, final IterationParams iterationParams) {
        usageAfterIteration = usageAfterSettled = null;
        enabled = true;
    }

    @Override
    public String getDescription() {
        return "Adds used memory to the result, if recorded via recordUsedMemory()";
    }

    static class UsageTuple {
        MemoryUsage heap;
        MemoryUsage nonHeap;

        public UsageTuple(final MemoryUsage _heapUsage, final MemoryUsage _nonHeapUsage) {
            heap = _heapUsage; nonHeap = _nonHeapUsage;
        }

        public long getTotalUsed() {
            return heap.getUsed() + nonHeap.getUsed();
        }

        public long getTotalCommitted() {
            return heap.getCommitted() + nonHeap.getCommitted();
        }

    }

}