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
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Record the total size in bytes of active objects on the heap after the last
 * benchmark iteration and print a heap histogram. The measure is determined by
 * calling {@code jmap} with the PID of the benchmark process. Since calling jmap
 * will result in a garbage collection in the VM running the benchmark, this is never
 * done in between iterations.
 *
 * @author Jens Wilke
 */
public class HeapProfiler implements InternalProfiler {

    private static String getJmapExcutable() {
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

    private static long printHeapHistogram(PrintStream out, int maxLines) {
        long totalBytes = 0;
        boolean partial = false;
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{
                    getJmapExcutable(),
                    "-histo:live",
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
                    totalBytes = Long.parseLong(sa[2]);
                } else if (r.getLineNumber() <= maxLines) {
                    ps.println(s);
                } else {
                    if (!partial) {
                        ps.println("[ ... truncated ... ]");
                    }
                    partial = true;
                }
            }
            r.close();
            in.close();
            ps.close();
            byte[] histoOtput = buffer.toByteArray();
            buffer = new ByteArrayOutputStream();
            ps = new PrintStream(buffer);
            ps.println("[jmap heap histogram, truncated at " + maxLines + " lines]");
            ps.write(histoOtput);
            ps.println();
            ps.close();
            out.write(buffer.toByteArray());
        } catch (Exception ex) {
            System.err.println("ForcedGcMemoryProfiler: error attaching / reading histogram");
            ex.printStackTrace();
        }
        return totalBytes;
    }

    private static Object reference;

    /**
     * Prevent object from being garbage collected at the last iteration.
     */
    public static void keepReference(Object target) {
        reference = target;
    }

    int iterationNumber = 0;

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams,
                                                       IterationParams iterationParams,
                                                       IterationResult result) {
        if (iterationParams.getType() != IterationType.MEASUREMENT
                || iterationParams.getCount() != ++iterationNumber) {
            return Collections.emptyList();
        }
        long bytes = printHeapHistogram(System.out, 30);
        List<Result> l = Arrays.asList(
                new OptionalScalarResult("+liveObjects", (double) bytes, "bytes", AggregationPolicy.AVG)
        );
        return l;
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) { }

    @Override
    public String getDescription() {
        return "Adds used bytes of the active heap objects measured at the end of the last iteration.";
    }

}