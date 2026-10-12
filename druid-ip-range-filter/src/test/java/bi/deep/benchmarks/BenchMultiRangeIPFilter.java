/*
 * Copyright Deep BI, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bi.deep.benchmarks;

import bi.deep.filtering.ip.range.impl.MultiRangeIPFilterImpl;
import bi.deep.range.IPRange;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.format.IPAddressRange;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class BenchMultiRangeIPFilter {

    private static final int WARMUP_MS = 1000;
    private static final long ROWS = 1_000_000L;
    private static final int[] SIZES = {100, 1_000, 10_000, 100_000, 1_000_000};

    public static void main(String[] args) {
        System.out.println("=== BenchMultiRangeIPFilter (sizes=" + Arrays.toString(SIZES) + ", rows=" + ROWS + ") ===");
        for (int size : SIZES) {
            runScenario(size);
        }
        System.out.println("Done.");
    }

    private static void runScenario(int rangeCount) {
        System.out.println("\n--- Ranges: " + rangeCount + " ---");

        Set<IPRange> ranges = genRanges(rangeCount);

        long t0 = System.nanoTime();
        MultiRangeIPFilterImpl filter = new MultiRangeIPFilterImpl("col", ranges, false);
        long buildNs = System.nanoTime() - t0;
        System.out.printf(Locale.ROOT, "Build: %.2f ms%n", buildNs / 1_000_000.0);

        IPAddress[] rows = genRows(ranges, (int) ROWS);

        long wuEnd = System.nanoTime() + WARMUP_MS * 1_000_000L;
        int i = 0;
        while (System.nanoTime() < wuEnd) {
            filter.contains(rows[i]);
            i++;
            if (i == rows.length) {
                i = 0;
            }
        }

        long start = System.nanoTime();
        for (IPAddress ip : rows) {
            filter.contains(ip);
        }
        long elapsed = System.nanoTime() - start;

        double secs = elapsed / 1_000_000_000.0;
        double rowsPerSec = ROWS / secs;
        System.out.printf(Locale.ROOT, "contains(): %.3f s  (%.2f rows/s)%n", secs, rowsPerSec);
    }

    private static Set<IPRange> genRanges(int n) {
        LinkedHashSet<IPRange> out = new LinkedHashSet<>(n * 2);
        for (int i = 0; i < n; i++) {
            int idx = i >>> 1;
            if ((i & 1) == 0) {
                int b = idx & 0xFF;
                int c = (idx >>> 8) & 0xFF;
                out.add(new IPRange("10." + b + "." + c + ".0/24"));
            } else {
                String h1 = Integer.toHexString(idx & 0xFFFF);
                String h2 = Integer.toHexString((idx >>> 16) & 0xFFFF);
                out.add(new IPRange("2001:db8:" + h1 + ":" + h2 + "::/64"));
            }
        }
        return out;
    }

    private static IPAddress[] genRows(Set<IPRange> ranges, int count) {
        ArrayList<IPAddress> hits = new ArrayList<>();
        for (IPRange r : ranges) {
            IPAddressRange sr = r.getAddressRange();
            hits.add(sr.getLower());
        }

        ArrayList<IPAddress> misses = new ArrayList<>(count);
        for (int i = 0; i < count / 2; i++) {
            int b = 18 + ((i >> 8) & 1);
            int c = (i >> 4) & 0xFF;
            int d = i & 0x0F;
            IPAddress ip = new IPAddressString("198." + b + "." + c + "." + d).getAddress();
            if (ip != null) {
                misses.add(ip);
            }
        }
        for (int i = 0; i < count / 2; i++) {
            String s = "2001:db9:" + Integer.toHexString(i & 0xFFFF) + ":" + Integer.toHexString((i >> 4) & 0xFFFF)
                    + "::1";
            IPAddress ip = new IPAddressString(s).getAddress();
            if (ip != null) {
                misses.add(ip);
            }
        }

        ArrayList<IPAddress> rows = new ArrayList<>(count);
        int hi = 0;
        int mi = 0;
        while (rows.size() < count) {
            if ((rows.size() & 1) == 0) {
                rows.add(hits.get(hi % hits.size()));
                hi++;
            } else {
                rows.add(misses.get(mi % misses.size()));
                mi++;
            }
        }
        return rows.toArray(new IPAddress[0]);
    }
}
