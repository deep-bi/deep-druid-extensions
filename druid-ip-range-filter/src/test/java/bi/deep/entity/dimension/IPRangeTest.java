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
package bi.deep.entity.dimension;

import static inet.ipaddr.Address.ADDRESS_LOW_VALUE_COMPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableSortedSet;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.LongStream;
import org.apache.druid.error.DruidException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class IPRangeTest {

    @Test
    void testIP4Creation() {
        final IPRange ipV4Range = IPRange.from("0.0.0.0/255.255.255.255");

        assertNotNull(ipV4Range);
        assertEquals("0.0.0.0", ipV4Range.getAddressRange().getLower().toString());
        assertEquals("255.255.255.255", ipV4Range.getAddressRange().getUpper().toString());

        assertFalse(ipV4Range.getAddressRange().getLower().isIPv6());
        assertTrue(ipV4Range.getAddressRange().getLower().isIPv4());
        assertEquals("0.0.0.0-255.255.255.255", ipV4Range.toString());
    }

    @Test
    void testIP6Creation() {
        final IPRange ipV6Range =
                IPRange.from("6f:ad2f:938:5f8f:7f94:ddd0:e1a5:4f/58db:b320:7914:41f9:12ca:5ccc:9c05:cd1e");

        assertNotNull(ipV6Range);
        assertEquals(
                "6f:ad2f:938:5f8f:7f94:ddd0:e1a5:4f",
                ipV6Range.getAddressRange().getLower().toString());
        assertEquals(
                "58db:b320:7914:41f9:12ca:5ccc:9c05:cd1e",
                ipV6Range.getAddressRange().getUpper().toString());
        assertTrue(ipV6Range.getAddressRange().getLower().isIPv6());
        assertFalse(ipV6Range.getAddressRange().getLower().isIPv4());
        assertEquals(
                "6f:ad2f:938:5f8f:7f94:ddd0:e1a5:4f-58db:b320:7914:41f9:12ca:5ccc:9c05:cd1e", ipV6Range.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"39.181.2.192", "6f:ad2f:938:5f8f:7f94:ddd0:e1a5:4f"})
    void rangeContainsTest(String addressStr) {
        final long count = 10;
        final IPAddress refAddress = new IPAddressString(addressStr).getAddress();
        final IPAddress lower = refAddress.increment(count);
        final IPAddress upper = lower.increment(count);
        final IPRange ipV4Range = IPRange.fromString(String.format("%s-%s", lower, upper));

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

    @ParameterizedTest
    @ValueSource(strings = {"39.181.2.192", "6f:ad2f:938:5f8f:7f94:ddd0:e1a5:4f"})
    void rangeContainsWithList(String addressStr) {
        final long count = 10;
        final IPAddress refAddress = new IPAddressString(addressStr).getAddress();
        final IPAddress lower = refAddress.increment(count);
        final IPAddress upper = lower.increment(count);
        final IPRange ipV4Range = IPRange.fromString(String.format("%s-%s", lower, upper));

        assertNotNull(ipV4Range);
        assertTrue(ipV4Range.contains(lower));
        assertTrue(ipV4Range.contains(upper));

        // Within Range
        LongStream.range(0, count)
                .mapToObj(lower::increment)
                .map(ImmutableSortedSet::of)
                .allMatch(ipV4Range::contains);
        // Lower than Range
        assertTrue(LongStream.range(0, count)
                .mapToObj(refAddress::increment)
                .map(ImmutableSortedSet::of)
                .noneMatch(ipV4Range::contains));
        // More than Range
        assertTrue(LongStream.range(1, count)
                .mapToObj(upper::increment)
                .map(ImmutableSortedSet::of)
                .noneMatch(ipV4Range::contains));

        SortedSet<IPAddress> searchSet = new TreeSet<>(ADDRESS_LOW_VALUE_COMPARATOR);
        searchSet.add(refAddress.increment(count - 1));
        searchSet.add(upper.increment(1));

        assertFalse(ipV4Range.contains(searchSet));
    }

    @Test
    void testInvalidRangeWithNull() {
        DruidException exception = assertThrows(DruidException.class, () -> IPRange.fromString(null));
        assertEquals("Range cannot be null or empty", exception.getMessage());
    }

    @Test
    void testInvalidRangeWithEmpty() {
        DruidException exception = assertThrows(DruidException.class, () -> IPRange.fromString(""));
        assertEquals("Range cannot be null or empty", exception.getMessage());
    }

    @Test
    void testInvalidRangeWithInvalidString() {
        DruidException exception =
                assertThrows(DruidException.class, () -> IPRange.fromString("invalidLowerIP/0.0.0.0"));
        assertEquals("Invalid lower IP 'invalidLowerIP'.", exception.getMessage());
    }

    @Test
    void testInvalidRangeWithInvalidUpperString() {
        DruidException exception =
                assertThrows(DruidException.class, () -> IPRange.fromString("0.0.0.0/invalidUpperIP"));
        assertEquals("Invalid upper IP 'invalidUpperIP'.", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
        "192.168.1.5->192.168.1.5,192.168.1.5-192.168.1.5",
        "192.168.1.5,192.168.1.5",
        "10.0.0.0/24,10.0.0.0-10.0.0.255",
        "6f:ad2f:938:5f8f:7f94:ddd0:e1a5:4f,6f:ad2f:938:5f8f:7f94:ddd0:e1a5:4f",
        "6f:ad2f:938:5f8f::/64,6f:ad2f:938:5f8f::-6f:ad2f:938:5f8f:ffff:ffff:ffff:ffff"
    })
    void testToString(String input, String output) {
        IPRange range = IPRange.fromString(input);
        assertEquals(output, range.toString());
    }
}
