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
package bi.deep.filtering.ip.range;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bi.deep.filtering.ip.range.impl.MultiRangeIPFilterImpl;
import com.google.common.collect.ImmutableSet;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.util.Arrays;
import java.util.stream.LongStream;
import org.apache.druid.query.filter.Filter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MultiRangeIPFilterTest {
    private final IPAddress ipV4Address = new IPAddressString("39.181.2.192").getAddress();
    private final IPAddress ipV6Address = new IPAddressString("6f:ad2f:938:5f8f:7f94:ddd0:e1a5:4f").getAddress();

    @Test
    void testMatchWithinBothIpv4AndIpV6() {
        final long count = 10;
        final MultiRangeIPFilter dimFilter = new MultiRangeIPFilter(
                "dimension",
                ImmutableSet.of(
                        String.format("%s/%s", ipV4Address, ipV4Address.increment(count)),
                        String.format("%s/%s", ipV6Address, ipV6Address.increment(count))),
                true);
        final Filter filter = dimFilter.toFilter();
        Assertions.assertInstanceOf(MultiRangeIPFilterImpl.class, filter);

        final MultiRangeIPFilterImpl filterImp = (MultiRangeIPFilterImpl) filter;

        assertTrue(LongStream.range(0, count).mapToObj(ipV4Address::increment).allMatch(filterImp::contains));
        assertTrue(LongStream.range(0, count).mapToObj(ipV6Address::increment).allMatch(filterImp::contains));
        assertFalse(filterImp.contains(ipV4Address.increment(count + 1)));
        assertFalse(filterImp.contains(ipV6Address.increment(count + 1)));
    }

    @Test
    void testMatchIpv6InIpv4Range() {
        final long count = 10;
        final MultiRangeIPFilter dimFilter = new MultiRangeIPFilter(
                "dimension", ImmutableSet.of(String.format("%s/%s", ipV4Address, ipV4Address.increment(count))), true);
        final Filter filter = dimFilter.toFilter();
        Assertions.assertInstanceOf(MultiRangeIPFilterImpl.class, filter);

        final MultiRangeIPFilterImpl filterImp = (MultiRangeIPFilterImpl) filter;

        assertTrue(LongStream.range(0, count).mapToObj(ipV4Address::increment).allMatch(filterImp::contains));
        assertTrue(LongStream.range(0, count).mapToObj(ipV6Address::increment).allMatch(filterImp::contains));
        assertTrue(filterImp.contains(ipV6Address.increment(count + 1)));
        assertFalse(filterImp.contains(ipV4Address.increment(count + 1)));
    }

    @Test
    void testMismatchIpv6InIpv4Range() {
        final long count = 10;
        final MultiRangeIPFilter dimFilter = new MultiRangeIPFilter(
                "dimension", ImmutableSet.of(String.format("%s/%s", ipV4Address, ipV4Address.increment(count))), false);
        final Filter filter = dimFilter.toFilter();
        Assertions.assertInstanceOf(MultiRangeIPFilterImpl.class, filter);

        final MultiRangeIPFilterImpl filterImp = (MultiRangeIPFilterImpl) filter;

        assertTrue(LongStream.range(0, count).mapToObj(ipV4Address::increment).allMatch(filterImp::contains));
        assertFalse(LongStream.range(0, count).mapToObj(ipV6Address::increment).allMatch(filterImp::contains));
        assertFalse(filterImp.contains(ipV4Address.increment(count + 1)));
    }

    @Test
    void testMatchIpv4InIpv6Range() {
        final long count = 10;
        final MultiRangeIPFilter dimFilter = new MultiRangeIPFilter(
                "dimension", ImmutableSet.of(String.format("%s/%s", ipV6Address, ipV6Address.increment(count))), true);
        final Filter filter = dimFilter.toFilter();
        Assertions.assertInstanceOf(MultiRangeIPFilterImpl.class, filter);

        final MultiRangeIPFilterImpl filterImp = (MultiRangeIPFilterImpl) filter;

        assertTrue(LongStream.range(0, count).mapToObj(ipV6Address::increment).allMatch(filterImp::contains));
        assertTrue(LongStream.range(0, count).mapToObj(ipV4Address::increment).allMatch(filterImp::contains));
        assertTrue(filterImp.contains(ipV4Address.increment(count + 1)));
        assertFalse(filterImp.contains(ipV6Address.increment(count + 1)));
    }

    @Test
    void testMismatchIpv4InIpv6Range() {
        final long count = 10;
        final MultiRangeIPFilter dimFilter = new MultiRangeIPFilter(
                "dimension", ImmutableSet.of(String.format("%s/%s", ipV6Address, ipV6Address.increment(count))), false);
        final Filter filter = dimFilter.toFilter();
        Assertions.assertInstanceOf(MultiRangeIPFilterImpl.class, filter);

        final MultiRangeIPFilterImpl filterImp = (MultiRangeIPFilterImpl) filter;

        assertTrue(LongStream.range(0, count).mapToObj(ipV6Address::increment).allMatch(filterImp::contains));
        assertFalse(LongStream.range(0, count).mapToObj(ipV4Address::increment).allMatch(filterImp::contains));
        assertFalse(filterImp.contains(ipV6Address.increment(count + 1)));
    }

    @Test
    void testCidrEquality() {
        final MultiRangeIPFilter fHost = new MultiRangeIPFilter("dimension", ImmutableSet.of("10.0.0.12/24"), false);
        final MultiRangeIPFilter fNet = new MultiRangeIPFilter("dimension", ImmutableSet.of("10.0.0.0/24"), false);

        final MultiRangeIPFilterImpl a = (MultiRangeIPFilterImpl) fHost.toFilter();
        final MultiRangeIPFilterImpl b = (MultiRangeIPFilterImpl) fNet.toFilter();

        for (String s : Arrays.asList(
                "10.0.0.0", "10.0.0.12", "10.0.0.200", "10.0.0.255", "10.0.1.0", "11.0.0.1", "9.255.255.255")) {
            IPAddress ip = new IPAddressString(s).getAddress();
            assertEquals(a.contains(ip), b.contains(ip), "Mismatch on " + s);
        }
    }
}
