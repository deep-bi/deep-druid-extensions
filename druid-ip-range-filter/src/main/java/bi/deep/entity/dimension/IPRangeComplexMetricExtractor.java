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

import javax.annotation.Nullable;
import org.apache.druid.data.input.InputRow;
import org.apache.druid.segment.serde.ComplexMetricExtractor;

public class IPRangeComplexMetricExtractor implements ComplexMetricExtractor {
    @Override
    public Class extractedClass() {
        return IPRange.class;
    }

    @Nullable
    @Override
    public IPRange extractValue(InputRow inputRow, String fieldName) {
        final Object input = inputRow.getRaw(fieldName);
        return IPRange.from(input);
    }
}
