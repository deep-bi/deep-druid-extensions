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
package bi.deep.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bi.deep.entity.dimension.IPRange;
import bi.deep.entity.dimension.IPRangeObjectStrategy;
import inet.ipaddr.format.IPAddressRange;
import java.nio.ByteBuffer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IPRangeObjectStrategyTest {

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void testSerialization(String ipRange) {
        IPRange range = IPRange.from(ipRange);
        byte[] bytes = IPRangeObjectStrategy.INSTANCE.toBytes(range);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        IPRange deserialized = IPRangeObjectStrategy.INSTANCE.fromByteBuffer(wrap, bytes.length);
        assertNotNull(deserialized);

        IPAddressRange addressRange = range.getAddressRange();
        addressRange = addressRange.getLower().spanWithRange(addressRange.getUpper());
        assertEquals(addressRange, deserialized.getAddressRange());
    }

    @Test
    void testEmptyRangeSerialization() {
        IPRange range = IPRange.EMPTY;
        byte[] bytes = IPRangeObjectStrategy.INSTANCE.toBytes(range);
        assertNotNull(bytes);
        assertEquals(0, bytes.length);

        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        IPRange deserialized = IPRangeObjectStrategy.INSTANCE.fromByteBuffer(wrap, bytes.length);
        assertNotNull(deserialized);
        assertEquals(range.getAddressRange(), deserialized.getAddressRange());
    }

    static Stream<Arguments> provideTestCases() {
        return Stream.of(
                Arguments.of("e3e7:682:c209:4cac:629f:6fbf:d82c:7cd"),
                Arguments.of("48.146.23.142"),
                Arguments.of("2404:6800:4003:c03::/64"),
                Arguments.of("10.0.0.0/24"),
                Arguments.of("172.16.0.5-172.16.0.20"),
                Arguments.of("2001:0db8:85a3::8a2e:0370:1000-2001:0db8:85a3::8a2e:0370:9000"));
    }
}
