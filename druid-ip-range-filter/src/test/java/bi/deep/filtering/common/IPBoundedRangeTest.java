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
package bi.deep.filtering.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class IPBoundedRangeTest {
    private final IPAddress ipV4Address = new IPAddressString("39.181.2.192").getAddress();
    private final IPAddress ipV6Address = new IPAddressString("6f:ad2f:938:5f8f:7f94:ddd0:e1a5:4f").getAddress();

    @Test
    void testMatchIpv6InIpv4Range() {
        final long count = 10;
        final IPBoundedRange boundedRange = new IPBoundedRange(
                ipV4Address.toString(), ipV4Address.increment(count).toString(), false, false);

        assertTrue(LongStream.range(0, count)
                .mapToObj(ipV4Address::increment)
                .allMatch(ip -> boundedRange.contains(ip, false)));
        assertTrue(LongStream.range(0, count)
                .mapToObj(ipV6Address::increment)
                .allMatch(ip -> boundedRange.contains(ip, true)));
        assertTrue(boundedRange.contains(ipV6Address.increment(count + 1), true));
        assertFalse(boundedRange.contains(ipV4Address.increment(count + 1), false));
    }

    @Test
    void testOpenLowerBoundRange() {
        final long count = 10;
        final IPBoundedRange boundedRange = new IPBoundedRange(
                ipV4Address.toString(), ipV4Address.increment(count).toString(), true, false);
        assertFalse(boundedRange.contains(boundedRange.getLowerIPAddress(), true));
        assertTrue(boundedRange.contains(boundedRange.getLowerIPAddress().increment(1), false));
    }

    @Test
    void testOpenUpperBoundRange() {
        final long count = 10;
        final IPBoundedRange boundedRange = new IPBoundedRange(
                ipV4Address.toString(), ipV4Address.increment(count).toString(), false, true);

        assertTrue(boundedRange.contains(boundedRange.getLowerIPAddress().increment(count - 1), false));
        assertFalse(boundedRange.contains(boundedRange.getUpperIPAddress(), false));
    }

    @Test
    void testMismatchIpv6InIpv4Range() {
        final long count = 10;
        final IPBoundedRange boundedRange = new IPBoundedRange(
                ipV4Address.toString(), ipV4Address.increment(count).toString(), false, false);

        assertTrue(LongStream.range(0, count)
                .mapToObj(ipV4Address::increment)
                .allMatch(ip -> boundedRange.contains(ip, false)));
        assertFalse(LongStream.range(0, count)
                .mapToObj(ipV6Address::increment)
                .allMatch(ip -> boundedRange.contains(ip, false)));
        assertFalse(boundedRange.contains(ipV4Address.increment(count + 1), false));
    }

    @Test
    void testMatchIpv4InIpv6Range() {
        final long count = 10;
        final IPBoundedRange boundedRange = new IPBoundedRange(
                ipV6Address.toString(), ipV6Address.increment(count).toString(), false, false);
        assertTrue(LongStream.range(0, count)
                .mapToObj(ipV6Address::increment)
                .allMatch(ip -> boundedRange.contains(ip, false)));
        assertTrue(LongStream.range(0, count)
                .mapToObj(ipV4Address::increment)
                .allMatch(ip -> boundedRange.contains(ip, true)));
        assertTrue(boundedRange.contains(ipV4Address.increment(count + 1), true));
        assertFalse(boundedRange.contains(ipV6Address.increment(count + 1), false));
    }

    @Test
    void testMismatchIpv4InIpv6Range() {
        final long count = 10;
        final IPBoundedRange boundedRange = new IPBoundedRange(
                ipV6Address.toString(), ipV6Address.increment(count).toString(), false, false);

        assertTrue(LongStream.range(0, count)
                .mapToObj(ipV6Address::increment)
                .allMatch(ip -> boundedRange.contains(ip, false)));
        assertFalse(LongStream.range(0, count)
                .mapToObj(ipV4Address::increment)
                .allMatch(ip -> boundedRange.contains(ip, false)));
        assertFalse(boundedRange.contains(ipV6Address.increment(count + 1), false));
    }
}
