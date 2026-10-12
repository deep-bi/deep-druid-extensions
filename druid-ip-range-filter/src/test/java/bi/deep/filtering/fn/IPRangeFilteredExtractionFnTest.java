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
package bi.deep.filtering.fn;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bi.deep.matching.IPRangeFilteredExtractionFn;
import bi.deep.util.IPRangeUtil;
import com.google.common.collect.Sets;
import inet.ipaddr.IPAddress;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IPRangeFilteredExtractionFnTest {

    private IPRangeFilteredExtractionFn extractionFn;

    static Stream<Arguments> provideTestCases() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("10.161.12.13-10.161.12.15", "10.161.12.13"),
                Arguments.of("10.161.12.13-10.161.12.15, weird string", "10.161.12.13"),
                Arguments.of("weird string", null),
                Arguments.of("172.16.0.1", null));
    }

    @BeforeEach
    void setUp() {
        List<IPAddress> testIps = IPRangeUtil.mapStringsToIps(Sets.newHashSet("10.162.59.19", "10.161.12.13"));
        extractionFn = new IPRangeFilteredExtractionFn(testIps);
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void testApply(String input, String expected) {
        assertEquals(expected, extractionFn.apply(input), "Unexpected extraction result");
    }
}
