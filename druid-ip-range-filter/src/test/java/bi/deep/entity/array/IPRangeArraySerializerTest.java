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
package bi.deep.entity.array;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bi.deep.entity.dimension.IPRangeArray;
import bi.deep.entity.dimension.IPRangeArraySerializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

class IPRangeArraySerializerTest {

    @Test
    void testSerialization() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();

        module.addSerializer(IPRangeArray.class, new IPRangeArraySerializer());
        mapper.registerModule(module);

        IPRangeArray array = IPRangeArray.from(ImmutableList.of(
                "e3e7:682:c209:4cac:629f:6fbf:d82c:7cd",
                "10.0.0.12/24", // 10.0.0.12/24 is effectively equal to 10.0.0.0/24, so we expect to have 1 range
                // 10.0.0.0 -> 10.0.0.255 in the output
                "10.0.0.0/24",
                "48.146.23.142",
                "2404:6800:4003:c03::/64",
                "172.16.0.5-172.16.0.20",
                "2001:0db8:85a3::8a2e:0370:1000-2001:0db8:85a3::8a2e:0370:9000"));
        String json = mapper.writeValueAsString(array);
        assertNotNull(json);

        assertEquals(
                "[\"10.0.0.0-10.0.0.255\",\"172.16.0.5-172.16.0.20\",\"2001:db8:85a3::8a2e:370:1000-2001:db8:85a3::8a2e:370:9000\",\"2404:6800:4003:c03::-2404:6800:4003:c03:ffff:ffff:ffff:ffff\",\"48.146.23.142\",\"e3e7:682:c209:4cac:629f:6fbf:d82c:7cd\"]",
                json);
    }
}
