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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.util.stream.LongStream;
import org.apache.druid.error.DruidException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IPRangeTest {

    @Test
    void testIP4Creation() {
        final IPRange ipV4Range = new IPRange("0.0.0.0/255.255.255.255");

        assertNotNull(ipV4Range);
        assertEquals("0.0.0.0", ipV4Range.getLower().toString());
        assertEquals("255.255.255.255", ipV4Range.getUpper().toString());
        assertTrue(ipV4Range.isIPv4());
        assertFalse(ipV4Range.isIPv6());
    }

    @Test
    void testIP6Creation() {
        final IPRange ipV6Range =
                new IPRange("6f:ad2f:938:5f8f:7f94:ddd0:e1a5:4f/58db:b320:7914:41f9:12ca:5ccc:9c05:cd1e");

        assertNotNull(ipV6Range);
        assertEquals("6f:ad2f:938:5f8f:7f94:ddd0:e1a5:4f", ipV6Range.getLower().toString());
        assertEquals(
                "58db:b320:7914:41f9:12ca:5ccc:9c05:cd1e", ipV6Range.getUpper().toString());
        assertTrue(ipV6Range.isIPv6());
        assertFalse(ipV6Range.isIPv4());
    }

    @ParameterizedTest
    @ValueSource(strings = {"39.181.2.192", "6f:ad2f:938:5f8f:7f94:ddd0:e1a5:4f"})
    void rangeContainsTest(String addressStr) {
        final long count = 10;
        final IPAddress refAddress = new IPAddressString(addressStr).getAddress();
        final IPAddress lower = refAddress.increment(count);
        final IPAddress upper = lower.increment(count);
        final IPRange ipV4Range = new IPRange(String.format("%s/%s", lower, upper));

        assertNotNull(ipV4Range);
        assertTrue(ipV4Range.contains(lower));
        assertTrue(ipV4Range.contains(upper));

        // Within Range
        assertTrue(LongStream.range(0, count).mapToObj(lower::increment).allMatch(ipV4Range::contains));
        // Lower than Range
        assertTrue(LongStream.range(0, count).mapToObj(refAddress::increment).noneMatch(ipV4Range::contains));
        // More than Range
        assertTrue(LongStream.range(1, count).mapToObj(upper::increment).noneMatch(ipV4Range::contains));
    }

    @Test
    void testInvalidRangeWithNull() {
        DruidException exception = assertThrows(DruidException.class, () -> new IPRange(null));
        assertEquals("Range cannot be null or empty", exception.getMessage());
    }

    @Test
    void testInvalidRangeWithEmpty() {
        DruidException exception = assertThrows(DruidException.class, () -> new IPRange(""));
        assertEquals("Range cannot be null or empty", exception.getMessage());
    }

    @Test
    void testInvalidRangeWithInvalidString() {
        DruidException exception = assertThrows(DruidException.class, () -> new IPRange("invalidLowerIP/0.0.0.0"));
        assertEquals("Invalid lower IP address", exception.getMessage());
    }

    @Test
    void testInvalidRangeWithInvalidUpperString() {
        DruidException exception = assertThrows(DruidException.class, () -> new IPRange("0.0.0.0/invalidUpperIP"));
        assertEquals("Invalid upper IP address", exception.getMessage());
    }

    @Test
    void testInvalidRangeWithInvalidSeparator() {
        DruidException exception = assertThrows(DruidException.class, () -> new IPRange("0.0.0.0-127.0.1.0"));
        assertEquals("Range should include lower and upper bounds in format `lower/upper`", exception.getMessage());
    }
}
