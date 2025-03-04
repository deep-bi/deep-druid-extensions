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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;
import org.apache.commons.lang.SerializationUtils;
import org.apache.druid.segment.ColumnValueSelector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValueMatchBufferBuildAggregatorTest extends TestDataProvider {
    @Mock
    private ColumnValueSelector<Object> valueSelector;

    @InjectMocks
    private ValueMatchBufferBuildAggregator aggregator;

    private ByteBuffer buffer;
    private final int position = 0; // Buffer start position

    @BeforeEach
    void setUp() {
        aggregator = new ValueMatchBufferBuildAggregator(valueSelector);
        buffer = ByteBuffer.allocate(1024);
        aggregator.init(buffer, position);
    }

    @Test
    void testInitialStateIsNull() {
        assertNull(aggregator.get(buffer, position), "Initial state should be null");
    }

    @Test
    void testHandlesNullValuesGracefully() {
        when(valueSelector.getObject()).thenReturn("hello");

        aggregator.aggregate(buffer, position);
        when(valueSelector.getObject()).thenReturn(null);
        aggregator.aggregate(buffer, position);

        assertEquals("hello", aggregator.get(buffer, position), "Null values should be ignored");
    }

    @Test
    void testSerializationAndDeserialization() {
        when(valueSelector.getObject()).thenReturn("testValue");

        aggregator.aggregate(buffer, position);
        Object storedValue = aggregator.get(buffer, position);

        assertEquals("testValue", storedValue, "Before serialization");

        // Serialize the stored object
        byte[] serialized = SerializationUtils.serialize((String) storedValue);
        Object deserialized = SerializationUtils.deserialize(serialized);

        assertEquals("testValue", deserialized, "After deserialization");
    }

    @ParameterizedTest
    @MethodSource("provideMixedTypeValues")
    void testAggregateWithSingleValue(Object value) {
        when(valueSelector.getObject()).thenReturn(value);
        aggregator.aggregate(buffer, position);

        Object result = aggregator.get(buffer, position);
        assertNotNull(result, "Result should not be null after aggregation.");
        assertEquals(value, result, "The aggregated value should match the selector's value.");
    }

    @ParameterizedTest
    @MethodSource("provideMixedTypeValues")
    void testAggregateWithSameValue(Object value) {
        IntStream.range(1, 10).forEach(index -> {
            when(valueSelector.getObject()).thenReturn(value);
            aggregator.aggregate(buffer, position);
        });

        Object result = aggregator.get(buffer, position);
        assertNotNull(result, "Result should not be null after aggregation.");
        assertEquals(value, result, "The aggregated value should match the selector's value.");
    }

    @ParameterizedTest
    @MethodSource("provideMixedTypeDifferentValues")
    void testAggregateWithDifferentValue(Object one, Object two) {
        when(valueSelector.getObject()).thenReturn(one);
        aggregator.aggregate(buffer, position);

        when(valueSelector.getObject()).thenReturn(two);
        aggregator.aggregate(buffer, position);

        Object result = aggregator.get(buffer, position);
        Assertions.assertNull(result, "Result should be null after aggregation.");
    }
}
