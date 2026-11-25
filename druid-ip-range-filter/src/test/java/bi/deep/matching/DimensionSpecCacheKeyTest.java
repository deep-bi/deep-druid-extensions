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
package bi.deep.matching;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import bi.deep.util.IPRangeUtil;
import com.google.common.collect.ImmutableSet;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.druid.query.dimension.DefaultDimensionSpec;
import org.apache.druid.query.dimension.DimensionSpec;
import org.junit.jupiter.api.Test;

class DimensionSpecCacheKeyTest {
    private static final DimensionSpec DELEGATE = DefaultDimensionSpec.of("ip");

    private static List<IPAddress> ips(String... values) {
        return Arrays.stream(values)
                .map(v -> new IPAddressString(v).getAddress())
                .collect(Collectors.toList());
    }

    @Test
    void testExtractionFnCacheKeyDiffersForDifferentIps() {
        assertFalse(Arrays.equals(
                new IPRangeFilteredExtractionFn(ips("10.0.0.1")).getCacheKey(),
                new IPRangeFilteredExtractionFn(ips("10.0.0.2")).getCacheKey()));
    }

    @Test
    void testExtractionFnCacheKeyKeepsOrderBecauseOutputDoes() {
        final IPRangeFilteredExtractionFn ab = new IPRangeFilteredExtractionFn(ips("10.0.0.1", "10.0.0.2"));
        final IPRangeFilteredExtractionFn ba = new IPRangeFilteredExtractionFn(ips("10.0.0.2", "10.0.0.1"));
        final String row = "10.0.0.0/24";

        // same IPs, different order: the output differs, so the keys must too
        assertFalse(ab.apply(row).equals(ba.apply(row)));
        assertFalse(Arrays.equals(ab.getCacheKey(), ba.getCacheKey()));
    }

    @Test
    void testRangeFilteredSpecCacheKeyDiffersForDifferentIps() {
        final IPRangeFilteredDimensionSpec a = new IPRangeFilteredDimensionSpec("out", DELEGATE, ips("10.0.0.1"));
        final IPRangeFilteredDimensionSpec b = new IPRangeFilteredDimensionSpec("out", DELEGATE, ips("10.0.0.2"));

        assertFalse(Arrays.equals(a.getCacheKey(), b.getCacheKey()));
    }

    @Test
    void testRangeFilteredSpecCacheKeyDiffersByNameDelegateAndOrder() {
        final List<IPAddress> ab = ips("10.0.0.1", "10.0.0.2");
        final IPRangeFilteredDimensionSpec base = new IPRangeFilteredDimensionSpec("out", DELEGATE, ab);

        assertFalse(Arrays.equals(
                base.getCacheKey(), new IPRangeFilteredDimensionSpec("other", DELEGATE, ab).getCacheKey()));
        assertFalse(Arrays.equals(
                base.getCacheKey(),
                new IPRangeFilteredDimensionSpec("out", DefaultDimensionSpec.of("other_ip"), ab).getCacheKey()));
        assertFalse(Arrays.equals(
                base.getCacheKey(),
                new IPRangeFilteredDimensionSpec("out", DELEGATE, ips("10.0.0.2", "10.0.0.1")).getCacheKey()));
    }

    @Test
    void testNativeSpecCacheKeyDiffersForDifferentValues() {
        final IPNativeRangeArrayFilteredDimensionSpec a =
                new IPNativeRangeArrayFilteredDimensionSpec("out", DELEGATE, ImmutableSet.of("10.0.0.1"));
        final IPNativeRangeArrayFilteredDimensionSpec b =
                new IPNativeRangeArrayFilteredDimensionSpec("out", DELEGATE, ImmutableSet.of("10.0.0.2"));

        assertFalse(Arrays.equals(a.getCacheKey(), b.getCacheKey()));
    }

    @Test
    void testNativeSpecCacheKeyIgnoresValueOrder() {
        final IPNativeRangeArrayFilteredDimensionSpec a = new IPNativeRangeArrayFilteredDimensionSpec(
                "out", DELEGATE, new LinkedHashSet<>(Arrays.asList("10.0.0.1", "::1")));
        final IPNativeRangeArrayFilteredDimensionSpec b = new IPNativeRangeArrayFilteredDimensionSpec(
                "out", DELEGATE, new LinkedHashSet<>(Arrays.asList("::1", "10.0.0.1")));

        assertArrayEquals(a.getCacheKey(), b.getCacheKey());
    }

    @Test
    void testCacheKeySizeDoesNotGrowWithValues() {
        final Set<String> many = IntStream.range(0, 100_000)
                .mapToObj(i -> String.format("10.%d.%d.%d", (i >> 16) & 0xff, (i >> 8) & 0xff, i & 0xff))
                .collect(Collectors.toSet());
        final List<IPAddress> manyIps = IPRangeUtil.mapStringsToIps(many);

        assertEquals(
                new IPRangeFilteredExtractionFn(ips("10.0.0.1")).getCacheKey().length,
                new IPRangeFilteredExtractionFn(manyIps).getCacheKey().length);
        assertEquals(
                new IPRangeFilteredDimensionSpec("out", DELEGATE, ips("10.0.0.1")).getCacheKey().length,
                new IPRangeFilteredDimensionSpec("out", DELEGATE, manyIps).getCacheKey().length);
        assertEquals(
                new IPNativeRangeArrayFilteredDimensionSpec("out", DELEGATE, ImmutableSet.of("10.0.0.1"))
                        .getCacheKey()
                        .length,
                new IPNativeRangeArrayFilteredDimensionSpec("out", DELEGATE, many).getCacheKey().length);
    }

    @Test
    void testNullElementsDoNotBreakCacheKey() {
        final List<IPAddress> withNull =
                new ArrayList<>(Arrays.asList(null, ips("10.0.0.1").get(0)));
        final IPRangeFilteredExtractionFn fn = new IPRangeFilteredExtractionFn(withNull);

        // apply() skips the null; getCacheKey() must not throw on it
        assertEquals("10.0.0.1", fn.apply("10.0.0.0/24"));
        assertEquals(fn.getCacheKey().length, new IPRangeFilteredExtractionFn(ips("10.0.0.1")).getCacheKey().length);
        new IPRangeFilteredDimensionSpec("out", DELEGATE, withNull).getCacheKey();
    }

    @Test
    void testRangeFilteredSpecReusesOneExtractionFn() {
        final IPRangeFilteredDimensionSpec spec =
                new IPRangeFilteredDimensionSpec("out", DELEGATE, ips("10.0.0.1", "10.0.0.2"));

        assertSame(spec.getExtractionFn(), spec.getExtractionFn());
        assertSame(spec.getIps(), ((IPRangeFilteredExtractionFn) spec.getExtractionFn()).getIps());
    }
}
