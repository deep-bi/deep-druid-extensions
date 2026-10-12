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

import bi.deep.entity.IPSetContents;
import bi.deep.filtering.ip.range.impl.RangeMatchingIPFilterImpl;
import bi.deep.range.IPBoundedRange;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.util.Collections;
import org.apache.druid.query.filter.Filter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RangeMatchingIPFilterTest {
    private final IPAddress ipV4Address = new IPAddressString("39.181.2.192").getAddress();
    private final IPAddress ipV6Address = new IPAddressString("6f:ad2f:938:5f8f:7f94:ddd0:e1a5:4f").getAddress();

    private void testFilterMatching(
            IPAddress ipAddress,
            boolean ignoreVersionMismatch,
            boolean expectV4Match,
            boolean expectV6Match,
            boolean expectV4IncMatch,
            boolean expectV6IncMatch) {
        final long diff = 10;
        final RangeMatchingIPFilter dimFilter = new RangeMatchingIPFilter(
                "dimension", Collections.singleton(ipAddress.toString()), ignoreVersionMismatch);
        final Filter filter = dimFilter.toFilter();
        Assertions.assertInstanceOf(RangeMatchingIPFilterImpl.class, filter);
        RangeMatchingIPFilterImpl filterImpl = (RangeMatchingIPFilterImpl) filter;

        IPBoundedRange rangeV4 = new IPBoundedRange(
                ipV4Address.toString(), ipV4Address.increment(diff).toString(), false, false);
        assertEquals(expectV4Match, filterImpl.anyMatch(new IPSetContents(null, Collections.singletonList(rangeV4))));

        IPBoundedRange rangeV6 = new IPBoundedRange(
                ipV6Address.toString(), ipV6Address.increment(diff).toString(), false, false);
        assertEquals(expectV6Match, filterImpl.anyMatch(new IPSetContents(null, Collections.singletonList(rangeV6))));

        IPBoundedRange rangeV4Inc = new IPBoundedRange(
                ipV4Address.increment(diff).toString(),
                ipV4Address.increment(diff * 2).toString(),
                false,
                false);
        assertEquals(
                expectV4IncMatch, filterImpl.anyMatch(new IPSetContents(null, Collections.singletonList(rangeV4Inc))));

        IPBoundedRange rangeV6Inc = new IPBoundedRange(
                ipV6Address.increment(diff).toString(),
                ipV6Address.increment(diff * 2).toString(),
                false,
                false);
        assertEquals(
                expectV6IncMatch, filterImpl.anyMatch(new IPSetContents(null, Collections.singletonList(rangeV6Inc))));
    }

    private void doIpListTest(
            IPAddress targetIp, IPAddress testIp, boolean ignoreVersionMismatch, boolean expectMatch) {
        RangeMatchingIPFilter dimFilter = new RangeMatchingIPFilter(
                "dimension", Collections.singleton(targetIp.toString()), ignoreVersionMismatch);
        RangeMatchingIPFilterImpl impl = (RangeMatchingIPFilterImpl) dimFilter.toFilter();
        IPSetContents contents = new IPSetContents(Collections.singletonList(testIp), Collections.emptyList());

        if (expectMatch) {
            assertTrue(
                    impl.anyMatch(contents),
                    () -> String.format(
                            "Expected match for %s vs %s (ignoreMismatch=%b)",
                            testIp, targetIp, ignoreVersionMismatch));
        } else {
            assertFalse(
                    impl.anyMatch(contents),
                    () -> String.format(
                            "Expected no match for %s vs %s (ignoreMismatch=%b)",
                            testIp, targetIp, ignoreVersionMismatch));
        }
    }

    @Test
    public void testIpListMatchingScenarios() {
        doIpListTest(ipV4Address, ipV4Address, true, true);
        doIpListTest(ipV4Address, ipV4Address.increment(1), true, false);

        doIpListTest(ipV4Address, ipV4Address, false, true);
        doIpListTest(ipV4Address, ipV6Address, false, false);

        doIpListTest(ipV6Address, ipV6Address, true, true);
        doIpListTest(ipV6Address, ipV6Address.increment(1), true, false);

        doIpListTest(ipV6Address, ipV6Address, false, true);
        doIpListTest(ipV6Address, ipV4Address, false, false);
    }

    @Test
    public void testV4FilterMatchingIgnoreVersionMismatch() {
        testFilterMatching(ipV4Address, true, true, true, false, true);
    }

    @Test
    public void testV4FilterMatchingVersionMismatch() {
        testFilterMatching(ipV4Address, false, true, false, false, false);
    }

    @Test
    public void testV6FilterMatchingIgnoreVersionMismatch() {
        testFilterMatching(ipV6Address, true, true, true, true, false);
    }

    @Test
    public void testV6FilterMatchingVersionMismatch() {
        testFilterMatching(ipV6Address, false, false, true, false, false);
    }
}
