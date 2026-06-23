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
package bi.deep;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import org.apache.druid.data.input.MapBasedInputRow;
import org.apache.druid.jackson.DefaultObjectMapper;
import org.apache.druid.segment.transform.Transform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CurrentTimestampTransformTest {
    @Test
    public void testCurrentTimestampReturnsLongMillis() {
        final CurrentTimestampTransform transform = new CurrentTimestampTransform("test");
        final MapBasedInputRow row = new MapBasedInputRow(0L, Collections.emptyList(), Map.of("dim", "value"));

        final long before = System.currentTimeMillis();
        final Object transformedValue = transform.getRowFunction().eval(row);
        final long after = System.currentTimeMillis();

        Assertions.assertInstanceOf(Number.class, transformedValue);
        final long millis = ((Number) transformedValue).longValue();
        Assertions.assertTrue(millis >= before);
        Assertions.assertTrue(millis <= after);
    }

    @Test
    public void testCurrentTimestampRemainsDynamicAcrossEvaluations() throws Exception {
        final CurrentTimestampTransform transform = new CurrentTimestampTransform("test");
        final MapBasedInputRow row = new MapBasedInputRow(0L, Collections.emptyList(), Map.of("dim", "value"));

        final long first = ((Number) transform.getRowFunction().eval(row)).longValue();
        Thread.sleep(5L);
        final long second = ((Number) transform.getRowFunction().eval(row)).longValue();

        Assertions.assertTrue(second >= first + 1L);
    }

    @Test
    public void testDeserializesAsTransformSubtype() throws Exception {
        final ObjectMapper mapper = new DefaultObjectMapper();
        mapper.registerModules(new IngestionTimeModule().getJacksonModules());

        final Transform transform =
                mapper.readValue("{\"type\":\"currentTimestamp\",\"name\":\"test\"}", Transform.class);

        Assertions.assertEquals(new CurrentTimestampTransform("test"), transform);
        Assertions.assertTrue(transform.getRequiredColumns().isEmpty());
    }
}
