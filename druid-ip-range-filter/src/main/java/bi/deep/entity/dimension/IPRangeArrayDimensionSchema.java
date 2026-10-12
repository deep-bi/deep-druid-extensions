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

import bi.deep.guice.IPRangeDimensionModule;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.druid.data.input.impl.DimensionSchema;
import org.apache.druid.segment.column.ColumnType;

@JsonTypeName(IPRangeDimensionModule.ARRAY_TYPE_NAME)
public class IPRangeArrayDimensionSchema extends DimensionSchema {

    @JsonCreator
    public IPRangeArrayDimensionSchema(@JsonProperty("name") String name) {
        super(name, MultiValueHandling.SORTED_ARRAY, true);
    }

    @Override
    public String getTypeName() {
        return IPRangeDimensionModule.ARRAY_TYPE_NAME;
    }

    @Override
    public ColumnType getColumnType() {
        return IPRangeDimensionModule.ARRAY_TYPE;
    }
}
