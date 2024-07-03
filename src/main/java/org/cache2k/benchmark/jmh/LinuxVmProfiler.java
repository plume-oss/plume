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
import org.openjdk.jmh.results.ScalarResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Add Linux VM metrics to the result. Parces {@code /proc/self/status} and adds all
 * metrics starting with {@code Vm}. In particular interesting is {@code VmRSS} and {@code VmHWM}
 * to analyze the memory usage.
 *
 * @author Jens Wilke
 * @see <a href="https://cruftex.net/2017/03/28/The-6-Memory-Metrics-You-Should-Track-in-Your-Java-Benchmarks.html">Blog Article</a>
 */
public class LinuxVmProfiler implements InternalProfiler {

    private static final String PREFIX = "+linux.proc.status";

    /**
     * Parse the linux {@code /proc/self/status} and add everything prefixed with "Vm" as metric to
     * the profiling result.
     */
    public static void addLinuxVmStats(String prefix, List<Result> l) {
        File f = new File("/proc/self/status");
        if (!f.exists()) return;
        try {
            LineNumberReader r = new LineNumberReader(new InputStreamReader(new FileInputStream(f.getAbsolutePath())));
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.startsWith("Vm")) {
                    continue;
                }
                String[] sa = line.split("\\s+");
                if (sa.length != 3) {
                    throw new IOException("Format error: 3 elements expected");
                }
                if (!sa[2].equals("kB")) {
                    throw new IOException("Format error: unit kB expected, was: " + sa[2]);
                }
                String name = sa[0].substring(0, sa[0].length() - 1);
                l.add(
                        new OptionalScalarResult(prefix + "." + name, (double) Long.parseLong(sa[1]), "kB", AggregationPolicy.AVG)
                );
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams,
                                                       IterationParams iterationParams, IterationResult result) {
        List<Result> l = new ArrayList<>();
        addLinuxVmStats(PREFIX, l);
        return l;
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {

    }

    @Override
    public String getDescription() {
        return "Adds Linux VM metrics to the result";
    }

}