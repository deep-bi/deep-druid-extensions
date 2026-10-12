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

import bi.deep.util.IPRangeUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import inet.ipaddr.format.IPAddressRange;
import java.io.IOException;

public class IPRangeArraySerializer extends JsonSerializer<IPRangeArray> {
    @Override
    public void serialize(IPRangeArray value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartArray();

        for (IPAddressRange ip : value.getAddressRanges()) {
            gen.writeString(IPRangeUtil.toString(ip));
        }
        gen.writeEndArray();
    }
}
