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
import static org.junit.jupiter.api.Assertions.assertFalse;

import bi.deep.filtering.ip.range.MultiRangeIPFilter;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CacheKeyUtilTest {
    @Test
    void testDigestIsFixedSize() {
        assertEquals(32, CacheKeyUtil.hashStringsIgnoringOrder(ImmutableSet.of("10.0.0.1")).length);
        assertEquals(32, CacheKeyUtil.hashStringsIgnoringOrder(hostCidrs(100_000)).length);
    }

    @Test
    void testFilterCacheKeySizeDoesNotGrowWithRanges() {
        final MultiRangeIPFilter small = new MultiRangeIPFilter("dimension", ImmutableSet.of("10.0.0.0/24"), false);
        final MultiRangeIPFilter large = new MultiRangeIPFilter("dimension", hostCidrs(100_000), false);

        assertEquals(small.getCacheKey().length, large.getCacheKey().length);
    }

    @Test
    void testNullValuesAreDistinguished() {
        final Set<String> withNull = new HashSet<>(Arrays.asList("10.0.0.1", null));
        final Set<String> withEmpty = new HashSet<>(Arrays.asList("10.0.0.1", ""));

        assertFalse(Arrays.equals(
                CacheKeyUtil.hashStringsIgnoringOrder(withNull), CacheKeyUtil.hashStringsIgnoringOrder(withEmpty)));
        assertArrayEquals(
                CacheKeyUtil.hashStringsIgnoringOrder(withNull),
                CacheKeyUtil.hashStringsIgnoringOrder(new HashSet<>(Arrays.asList(null, "10.0.0.1"))));
    }

    private static Set<String> hostCidrs(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> String.format("10.%d.%d.%d/32", (i >> 16) & 0xff, (i >> 8) & 0xff, i & 0xff))
                .collect(Collectors.toSet());
    }
}
