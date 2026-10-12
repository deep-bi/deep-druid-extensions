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
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.druid.segment.ColumnValueSelector;
import org.apache.druid.segment.DimensionMergerV9;
import org.apache.druid.segment.GenericColumnSerializer;
import org.apache.druid.segment.IndexableAdapter;
import org.apache.druid.segment.column.ColumnDescriptor;
import org.apache.druid.segment.column.ValueType;
import org.apache.druid.segment.serde.ComplexColumnPartSerde;
import org.apache.druid.segment.serde.ComplexColumnSerializer;
import org.apache.druid.segment.writeout.SegmentWriteOutMedium;

public class IPRangeDimensionMergerV9 implements DimensionMergerV9 {
    private final GenericColumnSerializer serializer;

    public IPRangeDimensionMergerV9(String dimensionName, SegmentWriteOutMedium segmentWriteOutMedium) {
        serializer =
                ComplexColumnSerializer.create(segmentWriteOutMedium, dimensionName, IPRangeObjectStrategy.INSTANCE);

        try {
            serializer.open();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public ColumnDescriptor makeColumnDescriptor() {
        return new ColumnDescriptor.Builder()
                .setValueType(ValueType.COMPLEX)
                .setHasMultipleValues(false)
                .addSerde(ComplexColumnPartSerde.serializerBuilder()
                        .withTypeName(IPRangeDimensionModule.TYPE_NAME)
                        .withDelegate(serializer)
                        .build())
                .build();
    }

    @Override
    public void writeMergedValueDictionary(List<IndexableAdapter> adapters) throws IOException {}

    @Override
    public ColumnValueSelector convertSortedSegmentRowValuesToMergedRowValues(
            int segmentIndex, ColumnValueSelector source) {
        return source;
    }

    @Override
    public void processMergedRow(ColumnValueSelector selector) throws IOException {
        serializer.serialize(selector);
    }

    @Override
    public void writeIndexes(@Nullable List<IntBuffer> segmentRowNumConversions) throws IOException {}

    @Override
    public boolean hasOnlyNulls() {
        return false;
    }
}
