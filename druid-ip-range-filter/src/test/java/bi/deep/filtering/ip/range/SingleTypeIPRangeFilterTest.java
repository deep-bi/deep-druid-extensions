/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package bi.deep.filtering.ip.range;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bi.deep.filtering.common.IPBoundedRange;
import bi.deep.filtering.ip.range.impl.SingleTypeIPRangeFilterImpl;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.util.Objects;
import java.util.stream.LongStream;
import org.apache.druid.query.filter.Filter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SingleTypeIPRangeFilterTest {
    private final IPAddress ipV4Address = new IPAddressString("39.181.2.192").getAddress();
    private final IPAddress ipV6Address = new IPAddressString("6f:ad2f:938:5f8f:7f94:ddd0:e1a5:4f").getAddress();

    @Test
    void testMatchIpv6InIpv4Range() {
        final long count = 10;
        final SingleTypeIPRangeFilter dimFilter = new SingleTypeIPRangeFilter(
                "dimension",
                new IPBoundedRange(
                        ipV4Address.toString(), ipV4Address.increment(count).toString(), false, false),
                true);

        final Filter filter = dimFilter.toFilter();
        Assertions.assertInstanceOf(SingleTypeIPRangeFilterImpl.class, filter);

        final SingleTypeIPRangeFilterImpl filterImp = (SingleTypeIPRangeFilterImpl) filter;

        assertTrue(LongStream.range(0, count)
                .mapToObj(ipV4Address::increment)
                .map(Objects::toString)
                .allMatch(filterImp::contains));
        assertTrue(LongStream.range(0, count)
                .mapToObj(ipV6Address::increment)
                .map(Objects::toString)
                .allMatch(filterImp::contains));
        assertTrue(filterImp.contains(ipV6Address.increment(count + 1).toString()));
        assertFalse(filterImp.contains(ipV4Address.increment(count + 1).toString()));
    }

    @Test
    void testMismatchIpv6InIpv4Range() {
        final long count = 10;
        final SingleTypeIPRangeFilter dimFilter = new SingleTypeIPRangeFilter(
                "dimension",
                new IPBoundedRange(
                        ipV4Address.toString(), ipV4Address.increment(count).toString(), false, false),
                false);
        final Filter filter = dimFilter.toFilter();
        Assertions.assertInstanceOf(SingleTypeIPRangeFilterImpl.class, filter);

        final SingleTypeIPRangeFilterImpl filterImp = (SingleTypeIPRangeFilterImpl) filter;

        assertTrue(LongStream.range(0, count)
                .mapToObj(ipV4Address::increment)
                .map(Objects::toString)
                .allMatch(filterImp::contains));
        assertFalse(LongStream.range(0, count)
                .mapToObj(ipV6Address::increment)
                .map(Objects::toString)
                .allMatch(filterImp::contains));
        assertFalse(filterImp.contains(ipV4Address.increment(count + 1).toString()));
    }

    @Test
    void testMatchIpv4InIpv6Range() {
        final long count = 10;
        final SingleTypeIPRangeFilter dimFilter = new SingleTypeIPRangeFilter(
                "dimension",
                new IPBoundedRange(
                        ipV6Address.toString(), ipV6Address.increment(count).toString(), false, false),
                true);
        final Filter filter = dimFilter.toFilter();
        Assertions.assertInstanceOf(SingleTypeIPRangeFilterImpl.class, filter);

        final SingleTypeIPRangeFilterImpl filterImp = (SingleTypeIPRangeFilterImpl) filter;

        assertTrue(LongStream.range(0, count)
                .mapToObj(ipV6Address::increment)
                .map(Objects::toString)
                .allMatch(filterImp::contains));
        assertTrue(LongStream.range(0, count)
                .mapToObj(ipV4Address::increment)
                .map(Objects::toString)
                .allMatch(filterImp::contains));
        assertTrue(filterImp.contains(ipV4Address.increment(count + 1).toString()));
        assertFalse(filterImp.contains(ipV6Address.increment(count + 1).toString()));
    }

    @Test
    void testMismatchIpv4InIpv6Range() {
        final long count = 10;
        final SingleTypeIPRangeFilter dimFilter = new SingleTypeIPRangeFilter(
                "dimension",
                new IPBoundedRange(
                        ipV6Address.toString(), ipV6Address.increment(count).toString(), false, false),
                false);
        final Filter filter = dimFilter.toFilter();
        Assertions.assertInstanceOf(SingleTypeIPRangeFilterImpl.class, filter);

        final SingleTypeIPRangeFilterImpl filterImp = (SingleTypeIPRangeFilterImpl) filter;

        assertTrue(LongStream.range(0, count)
                .mapToObj(ipV6Address::increment)
                .map(Objects::toString)
                .allMatch(filterImp::contains));
        assertFalse(LongStream.range(0, count)
                .mapToObj(ipV4Address::increment)
                .map(Objects::toString)
                .allMatch(filterImp::contains));
        assertFalse(filterImp.contains(ipV6Address.increment(count + 1).toString()));
    }
}
