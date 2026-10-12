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
package bi.deep.filtering.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bi.deep.entity.IPSetContents;
import bi.deep.util.IPRangeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.format.IPAddressRange;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.druid.error.DruidException;
import org.apache.druid.java.util.common.IAE;
import org.junit.jupiter.api.Test;

class IPRangeUtilTest {

    @Test
    void testExtractIPRanges() {
        String input = "192.168.1.1-192.168.1.100,10.0.0.0/24";
        IPSetContents contents = IPRangeUtil.extractIPSetContents(input);
        assertEquals(2, contents.getRanges().size());
    }

    @Test
    void testGetMatchingIP_EmptyResult() {
        String input = "192.168.1.1";
        List<IPAddress> ips = IPRangeUtil.mapStringsToIps(Sets.newHashSet("10.0.0.1"));
        assertNull(IPRangeUtil.getMatchingIPs(input, ips));
    }

    @Test
    void testGetMatchingIP_SingleMatch() {
        String input = "192.168.1.50,192.168.1.49";
        List<IPAddress> ips = IPRangeUtil.mapStringsToIps(Sets.newHashSet("192.168.1.50"));
        assertEquals("192.168.1.50", IPRangeUtil.getMatchingIPs(input, ips));
    }

    @Test
    void testGetMatchingIP_MultipleMatches() throws JsonProcessingException {
        String input = "192.168.1.50,10.0.0.0/24";
        List<IPAddress> ips = IPRangeUtil.mapStringsToIps(Sets.newHashSet("192.168.1.50", "10.0.0.1"));
        String expectedJson = new ObjectMapper().writeValueAsString(Arrays.asList("192.168.1.50", "10.0.0.1"));
        assertEquals(expectedJson, IPRangeUtil.getMatchingIPs(input, ips));
    }

    @Test
    void testGetMatchingIPRanges_EmptyResult() {
        String input = "192.168.1.1-192.168.1.100";
        List<IPAddress> ips = IPRangeUtil.mapStringsToIps(Sets.newHashSet("10.0.0.1"));
        assertNull(IPRangeUtil.getMatchingIPs(input, ips));
    }

    @Test
    void testGetMatchingIPRanges_SingleMatch() {
        String input = "192.168.1.1-192.168.1.100";
        List<IPAddress> ips = IPRangeUtil.mapStringsToIps(Sets.newHashSet("192.168.1.50"));
        assertEquals("192.168.1.50", IPRangeUtil.getMatchingIPs(input, ips));
    }

    @Test
    void testGetMatchingIPRanges_MultipleMatches() throws JsonProcessingException {
        String input = "192.168.1.1-192.168.1.100,10.0.0.0/24";
        List<IPAddress> ips = IPRangeUtil.mapStringsToIps(Sets.newHashSet("192.168.1.50", "10.0.0.1"));
        String expectedJson = new ObjectMapper().writeValueAsString(Arrays.asList("192.168.1.50", "10.0.0.1"));
        assertEquals(expectedJson, IPRangeUtil.getMatchingIPs(input, ips));
    }

    @Test
    void testMapStringsToIps() {
        Set<String> ips = Sets.newHashSet("192.168.1.1", "10.0.0.1");
        List<IPAddress> result = IPRangeUtil.mapStringsToIps(ips);
        assertEquals(2, result.size());
        assertEquals(new IPAddressString("192.168.1.1").getAddress(), result.get(0));
    }

    @Test
    void testContainsAnyIP() {
        String input = "192.168.1.1-192.168.1.100";
        IPSetContents ranges = IPRangeUtil.extractIPSetContents(input);
        List<IPAddress> ips = IPRangeUtil.mapStringsToIps(Sets.newHashSet("192.168.1.50"));
        assertTrue(ranges.containsAnyIP(ips, false));
    }

    @Test
    void testContainsAnyIP_NoMatch() {
        String input = "192.168.1.1-192.168.1.100";
        IPSetContents ranges = IPRangeUtil.extractIPSetContents(input);
        List<IPAddress> ips = IPRangeUtil.mapStringsToIps(Sets.newHashSet("10.0.0.1"));
        assertFalse(ranges.containsAnyIP(ips, false));
    }

    @Test
    void testContainsSingleIp_Match() {
        String input = "192.168.1.1";
        IPSetContents ranges = IPRangeUtil.extractIPSetContents(input);
        List<IPAddress> ips = IPRangeUtil.mapStringsToIps(Sets.newHashSet("192.168.1.1"));
        assertTrue(ranges.containsAnyIP(ips, false));
    }

    @Test
    void testContainsSingleIp_NoMatch() {
        String input = "192.168.1.1";
        IPSetContents ranges = IPRangeUtil.extractIPSetContents(input);
        List<IPAddress> ips = IPRangeUtil.mapStringsToIps(Sets.newHashSet("192.168.1.2"));
        assertFalse(ranges.containsAnyIP(ips, false));
    }

    @Test
    void testContainsMixedWithRange_Match() {
        String input = "192.168.1.1, 192.168.1.3-192.168.1.100";
        IPSetContents ranges = IPRangeUtil.extractIPSetContents(input);
        List<IPAddress> ips = IPRangeUtil.mapStringsToIps(Sets.newHashSet("192.168.1.1"));
        assertTrue(ranges.containsAnyIP(ips, false));
    }

    @Test
    void testContainsMixedWithRange_NoMatch() {
        String input = "192.168.1.1, 192.168.1.3-192.168.1.100";
        IPSetContents ranges = IPRangeUtil.extractIPSetContents(input);
        List<IPAddress> ips = IPRangeUtil.mapStringsToIps(Sets.newHashSet("192.168.1.2"));
        assertFalse(ranges.containsAnyIP(ips, false));
    }

    @Test
    void testCidrNormalization() {
        IPAddressRange first = IPRangeUtil.fromString("10.0.0.12/24"); // normalized to 10.0.0.0/24
        IPAddressRange second = IPRangeUtil.fromString("10.0.0.0/24");
        assertEquals(first, second);
    }

    @Test
    void testArrowIpv4Basic() {
        IPAddressRange range = IPRangeUtil.fromString("10.10.10.10 -> 10.10.10.20");

        IPAddress lower = range.getLower();
        IPAddress upper = range.getUpper();

        assertEquals("10.10.10.10", lower.toString());
        assertEquals("10.10.10.20", upper.toString());
    }

    @Test
    void testArrowIpv4WithSpaces() {
        IPAddressRange range = IPRangeUtil.fromString("  10.0.0.1  ->  10.0.0.10  ");
        assertEquals("10.0.0.1", range.getLower().toString());
        assertEquals("10.0.0.10", range.getUpper().toString());
    }

    @Test
    void testArrowIpv6() {
        IPAddressRange range = IPRangeUtil.fromString("2001:db8::1 -> 2001:db8::ff");
        assertEquals("2001:db8::1", range.getLower().toString());
        assertEquals("2001:db8::ff", range.getUpper().toString());
    }

    @Test
    void testArrowSameIp() {
        IPAddressRange range = IPRangeUtil.fromString("192.168.1.5->192.168.1.5");
        assertEquals("192.168.1.5", range.getLower().toString());
        assertEquals("192.168.1.5", range.getUpper().toString());
    }

    @Test
    void testArrowInvalidLowerIp() {
        Exception ex = assertThrows(DruidException.class, () -> IPRangeUtil.fromString("10.0.0.x->10.0.0.10"));
        assertTrue(ex.getMessage().contains("Invalid lower IP"));
    }

    @Test
    void testArrowInvalidUpperIp() {
        Exception ex = assertThrows(DruidException.class, () -> IPRangeUtil.fromString("10.0.0.1->10.0.0.x"));
        assertTrue(ex.getMessage().contains("Invalid upper IP"));
    }

    @Test
    void testArrowMissingLeftSide() {
        Exception ex = assertThrows(DruidException.class, () -> IPRangeUtil.fromString("->10.0.0.10"));
        assertTrue(ex.getMessage().contains("Malformed"));
    }

    @Test
    void testArrowMissingRightSide() {
        Exception ex = assertThrows(DruidException.class, () -> IPRangeUtil.fromString("10.0.0.1->"));
        assertTrue(ex.getMessage().contains("Malformed"));
    }

    @Test
    void testArrowIpv4Ipv6Mismatch() {
        Exception ex = assertThrows(IAE.class, () -> IPRangeUtil.fromString("10.0.0.1 -> 2001:db8::1"));
        assertTrue(ex.getMessage().contains("IPv4/IPv6 mismatch"));
    }

    @Test
    void testMultipleArrowsNotAllowed() {
        Exception ex = assertThrows(DruidException.class, () -> IPRangeUtil.fromString("10.0.0.1->10.0.0.2->10.0.0.3"));
        assertTrue(ex.getMessage().contains("Multiple"));
    }

    @Test
    void testToStringForEmptyValues() {
        assertEquals("null", IPRangeUtil.toString((IPAddressRange) null));

        assertEquals("[]", IPRangeUtil.toString((Collection<IPAddressRange>) null));
        assertEquals("[]", IPRangeUtil.toString(List.of()));
    }
}
