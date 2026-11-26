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
package bi.deep.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bi.deep.filtering.ip.range.FixedSetIPFilter;
import bi.deep.filtering.ip.range.IPNativeRangeMatchingFilter;
import bi.deep.filtering.ip.range.MultiRangeIPFilter;
import bi.deep.filtering.ip.range.RangeMatchingIPFilter;
import bi.deep.matching.IPNativeRangeArrayFilteredDimensionSpec;
import bi.deep.matching.IPNativeRangeArrayFilteredVirtualColumn;
import bi.deep.matching.IPRangeFilteredDimensionSpec;
import bi.deep.matching.IPRangeFilteredExtractionFn;
import bi.deep.matching.IPRangeFilteredVirtualColumn;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.druid.java.util.common.Cacheable;
import org.apache.druid.query.dimension.DefaultDimensionSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CacheKeySnapshotTest {
    private static <T extends Cacheable> Arguments setBacked(
            String type, Function<Set<String>, T> factory, Function<T, Collection<String>> values) {
        return Arguments.of(type, factory, values);
    }

    static Iterable<Arguments> setBacked() {
        return Arrays.asList(
                setBacked("ip_multi_range", s -> new MultiRangeIPFilter("d", s, false), MultiRangeIPFilter::getRanges),
                setBacked("ip_fixed_set", s -> new FixedSetIPFilter("d", s), FixedSetIPFilter::getRanges),
                setBacked(
                        "ip_range_match", s -> new RangeMatchingIPFilter("d", s, false), RangeMatchingIPFilter::getIps),
                setBacked(
                        "ip_native_match",
                        s -> new IPNativeRangeMatchingFilter("d", s),
                        IPNativeRangeMatchingFilter::getIps),
                setBacked(
                        "ip-range-filtered",
                        s -> new IPRangeFilteredVirtualColumn("vc", DefaultDimensionSpec.of("ip"), s),
                        IPRangeFilteredVirtualColumn::getIps),
                setBacked(
                        "ip-native-filtered",
                        s -> new IPNativeRangeArrayFilteredVirtualColumn("vc", DefaultDimensionSpec.of("ip"), s),
                        IPNativeRangeArrayFilteredVirtualColumn::getIps),
                setBacked(
                        "ip-native-filtered-spec",
                        s -> new IPNativeRangeArrayFilteredDimensionSpec("out", DefaultDimensionSpec.of("ip"), s),
                        IPNativeRangeArrayFilteredDimensionSpec::getIps));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("setBacked")
    <T extends Cacheable> void testMutatingInputDoesNotDesyncValuesAndCacheKey(
            String type, Function<Set<String>, T> factory, Function<T, Collection<String>> values) {
        final Set<String> input = new HashSet<>(Arrays.asList("10.0.0.0/24"));
        final T c = factory.apply(input);
        final byte[] before = c.getCacheKey();

        input.add("192.168.0.0/16");

        // the values used for matching must still be the ones the memoised key describes
        assertEquals(1, values.apply(c).size());
        assertArrayEquals(before, factory.apply(new HashSet<>(values.apply(c))).getCacheKey());
    }

    @Test
    void testGettersReturnUnmodifiableSnapshots() {
        final Set<String> input = new HashSet<>(Arrays.asList("10.0.0.0/24"));
        final List<Supplier<Collection<?>>> getters = Arrays.asList(
                () -> new MultiRangeIPFilter("d", input, false).getRanges(),
                () -> new FixedSetIPFilter("d", input).getRanges(),
                () -> new RangeMatchingIPFilter("d", input, false).getIps(),
                () -> new IPNativeRangeMatchingFilter("d", input).getIps(),
                () -> new IPRangeFilteredVirtualColumn("vc", DefaultDimensionSpec.of("ip"), input).getIps(),
                () -> new IPNativeRangeArrayFilteredVirtualColumn("vc", DefaultDimensionSpec.of("ip"), input).getIps(),
                () -> new IPNativeRangeArrayFilteredDimensionSpec("o", DefaultDimensionSpec.of("ip"), input).getIps(),
                () -> new IPRangeFilteredDimensionSpec("o", DefaultDimensionSpec.of("ip"), ips("10.0.0.1")).getIps());

        for (Supplier<Collection<?>> getter : getters) {
            assertThrows(UnsupportedOperationException.class, () -> getter.get().clear());
        }
    }

    @Test
    void testRangeFilteredSpecSnapshotsList() {
        final List<IPAddress> input = new ArrayList<>(ips("10.0.0.1"));
        final IPRangeFilteredDimensionSpec spec =
                new IPRangeFilteredDimensionSpec("o", DefaultDimensionSpec.of("ip"), input);
        final byte[] before = spec.getCacheKey();

        input.add(new IPAddressString("10.0.0.2").getAddress());

        assertArrayEquals(before, spec.getCacheKey());
        assertEquals(1, spec.getIps().size());
    }

    @Test
    void testExtractionFnWithNullListHasCacheKeyAndMatchesNothing() {
        final IPRangeFilteredExtractionFn fn = new IPRangeFilteredExtractionFn(null);

        assertNull(fn.apply("10.0.0.0/24"));
        assertArrayEquals(new IPRangeFilteredExtractionFn(new ArrayList<>()).getCacheKey(), fn.getCacheKey());
    }

    @Test
    void testMatchFiltersUseDistinctCacheIds() {
        final Set<String> ips = new HashSet<>(Arrays.asList("10.0.0.1"));

        assertNotEquals(
                new RangeMatchingIPFilter("d", ips, false).getCacheKey()[0],
                new IPNativeRangeMatchingFilter("d", ips).getCacheKey()[0]);
    }

    private static List<IPAddress> ips(String... values) {
        final List<IPAddress> out = new ArrayList<>();
        for (String v : values) {
            out.add(new IPAddressString(v).getAddress());
        }
        return out;
    }

    @Test
    void testExtractionFnSnapshotsList() {
        final List<IPAddress> input = new ArrayList<>(ips("10.0.0.1"));
        final IPRangeFilteredExtractionFn fn = new IPRangeFilteredExtractionFn(input);
        final byte[] before = fn.getCacheKey();

        input.add(new IPAddressString("10.0.0.2").getAddress());

        // matching must still use the list the memoised key describes
        assertEquals("10.0.0.1", fn.apply("10.0.0.0/24"));
        assertArrayEquals(before, fn.getCacheKey());
    }
}
