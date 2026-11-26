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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import bi.deep.entity.dimension.IPRange;
import bi.deep.util.IPRangeUtil;
import com.google.common.collect.ImmutableSet;
import inet.ipaddr.IPAddressString;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.druid.query.dimension.DefaultDimensionSpec;
import org.apache.druid.query.dimension.DimensionSpec;
import org.apache.druid.query.monomorphicprocessing.RuntimeShapeInspector;
import org.apache.druid.segment.ObjectColumnSelector;
import org.apache.druid.segment.VirtualColumn;
import org.apache.druid.segment.virtual.VirtualColumnCacheHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FilteredVirtualColumnCacheKeyTest {
    @FunctionalInterface
    interface Factory {
        VirtualColumn create(String name, DimensionSpec delegate, Set<String> ips);
    }

    private static final DimensionSpec DELEGATE = DefaultDimensionSpec.of("ip");

    static Iterable<Arguments> factories() {
        return Arrays.asList(
                Arguments.of("ip-range-filtered", (Factory) IPRangeFilteredVirtualColumn::new),
                Arguments.of("ip-native-filtered", (Factory) IPNativeRangeArrayFilteredVirtualColumn::new));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("factories")
    void testCacheKeyDiffersForDifferentValues(String type, Factory factory) {
        final VirtualColumn a = factory.create("vc", DELEGATE, ImmutableSet.of("10.0.0.1", "192.168.0.1"));
        final VirtualColumn b = factory.create("vc", DELEGATE, ImmutableSet.of("10.0.0.2", "192.168.0.2"));

        assertFalse(Arrays.equals(a.getCacheKey(), b.getCacheKey()));
    }

    @Test
    void testNativeCacheKeyKeepsValueOrderBecauseOutputDoes() {
        final IPNativeRangeArrayFilteredVirtualColumn first = new IPNativeRangeArrayFilteredVirtualColumn(
                "vc", DELEGATE, new LinkedHashSet<>(Arrays.asList("10.0.0.1", "10.0.0.1/32")));
        final IPNativeRangeArrayFilteredVirtualColumn second = new IPNativeRangeArrayFilteredVirtualColumn(
                "vc", DELEGATE, new LinkedHashSet<>(Arrays.asList("10.0.0.1/32", "10.0.0.1")));
        final IPRange row = new IPRange(new IPAddressString("10.0.0.0/24").getAddress());

        // the selector keeps whichever equal address comes first, so the rendered match follows input order
        assertNotEquals(
                String.valueOf(nativeSelector(first, row).getObject()),
                String.valueOf(nativeSelector(second, row).getObject()));
        assertFalse(Arrays.equals(first.getCacheKey(), second.getCacheKey()));
    }

    private static IPRangeArrayFilteredDimensionSelector nativeSelector(
            IPNativeRangeArrayFilteredVirtualColumn vc, Object row) {
        return new IPRangeArrayFilteredDimensionSelector(
                new ObjectColumnSelector<Object>() {
                    @Override
                    public Object getObject() {
                        return row;
                    }

                    @Override
                    public Class<Object> classOfObject() {
                        return Object.class;
                    }

                    @Override
                    public void inspectRuntimeShape(RuntimeShapeInspector inspector) {}
                },
                IPRangeUtil.mapStringsToIps(vc.getIps()));
    }

    @Test
    void testStringCacheKeyKeepsValueOrderBecauseOutputDoes() {
        final Set<String> ab = new LinkedHashSet<>(Arrays.asList("10.0.0.1", "10.0.0.2"));
        final Set<String> ba = new LinkedHashSet<>(Arrays.asList("10.0.0.2", "10.0.0.1"));
        final IPRangeFilteredVirtualColumn first = new IPRangeFilteredVirtualColumn("vc", DELEGATE, ab);
        final IPRangeFilteredVirtualColumn second = new IPRangeFilteredVirtualColumn("vc", DELEGATE, ba);

        // same IPs, reversed: the selector's JSON output is reversed too, so the keys must differ
        final IPRangeFilteredExtractionFn fnAb =
                new IPRangeFilteredExtractionFn(IPRangeUtil.mapStringsToIps(first.getIps()));
        final IPRangeFilteredExtractionFn fnBa =
                new IPRangeFilteredExtractionFn(IPRangeUtil.mapStringsToIps(second.getIps()));
        assertNotEquals(fnAb.apply("10.0.0.0/24"), fnBa.apply("10.0.0.0/24"));
        assertFalse(Arrays.equals(first.getCacheKey(), second.getCacheKey()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("factories")
    void testCacheKeyDiffersByNameAndDelegate(String type, Factory factory) {
        final Set<String> ips = ImmutableSet.of("10.0.0.1");
        final VirtualColumn base = factory.create("vc", DELEGATE, ips);
        final VirtualColumn otherName = factory.create("other", DELEGATE, ips);
        final VirtualColumn otherDelegate = factory.create("vc", DefaultDimensionSpec.of("other_ip"), ips);

        assertFalse(Arrays.equals(base.getCacheKey(), otherName.getCacheKey()));
        assertFalse(Arrays.equals(base.getCacheKey(), otherDelegate.getCacheKey()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("factories")
    void testCacheKeySizeDoesNotGrowWithValues(String type, Factory factory) {
        final Set<String> many = IntStream.range(0, 100_000)
                .mapToObj(i -> String.format("10.%d.%d.%d/32", (i >> 16) & 0xff, (i >> 8) & 0xff, i & 0xff))
                .collect(Collectors.toSet());
        final VirtualColumn small = factory.create("vc", DELEGATE, ImmutableSet.of("10.0.0.1"));
        final VirtualColumn large = factory.create("vc", DELEGATE, many);

        assertEquals(small.getCacheKey().length, large.getCacheKey().length);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("factories")
    void testCacheKeyUsesDruidUserDefinedPrefix(String type, Factory factory) {
        // ids 0x04 and 0x05 belong to Druid's built-in virtual columns; extensions must use the user-defined id
        final byte[] key =
                factory.create("vc", DELEGATE, ImmutableSet.of("10.0.0.1")).getCacheKey();

        assertEquals(VirtualColumnCacheHelper.CACHE_TYPE_ID_USER_DEFINED, key[0]);
    }

    @Test
    void testCacheKeyDiffersBetweenVirtualColumnTypes() {
        final Set<String> ips = ImmutableSet.of("10.0.0.1");

        assertFalse(Arrays.equals(
                new IPRangeFilteredVirtualColumn("vc", DELEGATE, ips).getCacheKey(),
                new IPNativeRangeArrayFilteredVirtualColumn("vc", DELEGATE, ips).getCacheKey()));
    }
}
