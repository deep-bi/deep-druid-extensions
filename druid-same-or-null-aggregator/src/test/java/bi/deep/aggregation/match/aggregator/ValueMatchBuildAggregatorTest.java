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
package bi.deep.aggregation.match.aggregator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.stream.IntStream;
import org.apache.druid.segment.ColumnValueSelector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValueMatchBuildAggregatorTest extends TestDataProvider {
    @Mock
    private ColumnValueSelector<Object> valueSelector;

    @ParameterizedTest
    @MethodSource("provideMixedTypeValues")
    void testAggregateWithSingleValue(Object value) {
        try (ValueMatchBuildAggregator aggregator = new ValueMatchBuildAggregator(valueSelector)) {
            when(valueSelector.getObject()).thenReturn(value);
            aggregator.aggregate();

            Object result = aggregator.get();
            assertNotNull(result, "Result should not be null after aggregation.");
            assertEquals(value, result, "The aggregated value should match the selector's value.");
        }
    }

    @ParameterizedTest
    @MethodSource("provideMixedTypeValues")
    void testAggregateWithSameValue(Object value) {
        try (ValueMatchBuildAggregator aggregator = new ValueMatchBuildAggregator(valueSelector)) {
            IntStream.range(1, 10).forEach(index -> {
                when(valueSelector.getObject()).thenReturn(value);
                aggregator.aggregate();
            });

            Object result = aggregator.get();
            assertNotNull(result, "Result should not be null after aggregation.");
            assertEquals(value, result, "The aggregated value should match the selector's value.");
        }
    }

    @ParameterizedTest
    @MethodSource("provideMixedTypeDifferentValues")
    void testAggregateWithDifferentValue(Object one, Object two) {
        try (ValueMatchBuildAggregator aggregator = new ValueMatchBuildAggregator(valueSelector)) {
            when(valueSelector.getObject()).thenReturn(one);
            aggregator.aggregate();

            when(valueSelector.getObject()).thenReturn(two);
            aggregator.aggregate();

            Object result = aggregator.get();
            Assertions.assertNull(result, "Result should be null after aggregation.");
        }
    }
}
