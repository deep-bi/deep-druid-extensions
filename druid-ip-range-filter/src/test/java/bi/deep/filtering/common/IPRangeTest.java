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
package bi.deep.filtering.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bi.deep.range.IPRange;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.apache.druid.error.DruidException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("parseCases")
    void parses(ParseCase c) {
        if (c.ex == null) {
            assertDoesNotThrow(() -> new IPRange(c.in));
        } else {
            Throwable ex = assertThrows(c.ex, () -> new IPRange(c.in));
            assertEquals(c.msg, ex.getMessage());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("containsCases")
    void containsChecks(ContainsCase c) {
        IPRange r = new IPRange(c.range);
        IPAddress a = new IPAddressString(c.addr).getAddress();
        assertEquals(c.expected, r.contains(a));
    }

    static Stream<ParseCase> parseCases() {
        return Stream.of(
                ParseCase.err(null, DruidException.class, "Range cannot be null or empty"),
                ParseCase.err("", DruidException.class, "Range cannot be null or empty"),
                ParseCase.err("invalidLowerIP/0.0.0.0", DruidException.class, "Invalid lower IP 'invalidLowerIP'."),
                ParseCase.err("0.0.0.0/invalidUpperIP", DruidException.class, "Invalid upper IP 'invalidUpperIP'."),
                ParseCase.err(
                        "999.999.999.999",
                        DruidException.class,
                        "Malformed input '999.999.999.999'. Expected IP, CIDR, or range."),
                ParseCase.err("10.0.0.1/", DruidException.class, "Malformed '10.0.0.1/'. Empty side around separator."),
                ParseCase.err("/24", DruidException.class, "Malformed '/24'. Empty side around separator."),
                ParseCase.err(
                        "2001:db8::/129", DruidException.class, "Malformed CIDR '2001:db8::/129'. Expected ip/prefix."),
                ParseCase.err("10.0.0.0/33", DruidException.class, "Malformed CIDR '10.0.0.0/33'. Expected ip/prefix."),
                ParseCase.err(
                        "10.0.0.1/::1", IllegalArgumentException.class, "IPv4/IPv6 mismatch: '10.0.0.1' vs '::1'."),
                ParseCase.ok("192.0.2.5"),
                ParseCase.ok("2001:db8::1"),
                ParseCase.ok("10.0.0.0/24"),
                ParseCase.ok("2001:db8::/48"),
                ParseCase.ok("10.0.0.1/10.0.0.20"),
                ParseCase.ok("0.0.0.0/0"),
                ParseCase.ok("255.255.255.255/32"),
                ParseCase.ok("::/0"),
                ParseCase.ok("2001:db8::1/128"),
                ParseCase.ok("  10.1.1.1  "),
                ParseCase.ok("2001:db8::1/2001:db8::ff"),
                ParseCase.ok("10.0.0.1/10.0.0.1"),
                ParseCase.ok("0.0.0.0-127.0.1.0"));
    }

    static Stream<ContainsCase> containsCases() {
        return Stream.of(
                // single
                ContainsCase.of("192.0.2.5", "192.0.2.5", true),
                ContainsCase.of("192.0.2.5", "192.0.2.6", false),

                // CIDR
                ContainsCase.of("10.0.0.0/24", "10.0.0.128", true),
                ContainsCase.of("10.0.0.0/24", "10.0.1.1", false),

                // explicit range (boundaries + middle)
                ContainsCase.of("10.0.0.5/10.0.0.10", "10.0.0.5", true),
                ContainsCase.of("10.0.0.5/10.0.0.10", "10.0.0.10", true),
                ContainsCase.of("10.0.0.5/10.0.0.10", "10.0.0.8", true),
                ContainsCase.of("10.0.0.5/10.0.0.10", "10.0.0.4", false),
                ContainsCase.of("10.0.0.5/10.0.0.10", "10.0.0.11", false),
                ContainsCase.of("10.0.0.10/10.0.0.5", "10.0.0.7", true), // reversed order

                // IPv6 range
                ContainsCase.of("2001:db8::1/2001:db8::ff", "2001:db8::80", true),
                ContainsCase.of("2001:db8::1/2001:db8::ff", "2001:db8:1::1", false));
    }

    private static final class ParseCase {
        final String in;
        final Class<? extends Throwable> ex;
        final String msg;

        private ParseCase(String in, Class<? extends Throwable> ex, String msg) {
            this.in = in;
            this.ex = ex;
            this.msg = msg;
        }

        static ParseCase ok(String in) {
            return new ParseCase(in, null, null);
        }

        static ParseCase err(String in, Class<? extends Throwable> ex, String msg) {
            return new ParseCase(in, ex, msg);
        }

        @Override
        public String toString() {
            return in == null ? "null" : in;
        }
    }

    private static final class ContainsCase {
        final String range, addr;
        final boolean expected;

        private ContainsCase(String range, String addr, boolean expected) {
            this.range = range;
            this.addr = addr;
            this.expected = expected;
        }

        static ContainsCase of(String range, String addr, boolean expected) {
            return new ContainsCase(range, addr, expected);
        }

        @Override
        public String toString() {
            return range + " :: " + addr + " => " + expected;
        }
    }
}
