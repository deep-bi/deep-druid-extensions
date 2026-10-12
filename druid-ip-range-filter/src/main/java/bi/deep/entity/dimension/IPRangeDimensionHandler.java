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

import java.io.File;
import java.util.Comparator;
import org.apache.druid.data.input.impl.DimensionSchema;
import org.apache.druid.java.util.common.io.Closer;
import org.apache.druid.segment.ColumnValueSelector;
import org.apache.druid.segment.DimensionHandler;
import org.apache.druid.segment.DimensionIndexer;
import org.apache.druid.segment.DimensionMergerV9;
import org.apache.druid.segment.IndexSpec;
import org.apache.druid.segment.ProgressIndicator;
import org.apache.druid.segment.column.ColumnCapabilities;
import org.apache.druid.segment.selector.settable.SettableColumnValueSelector;
import org.apache.druid.segment.selector.settable.SettableObjectColumnValueSelector;
import org.apache.druid.segment.writeout.SegmentWriteOutMedium;

public class IPRangeDimensionHandler implements DimensionHandler<IPRange, IPRange, IPRange> {

    private final String dimensionName;

    public IPRangeDimensionHandler(String dimensionName) {
        this.dimensionName = dimensionName;
    }

    @Override
    public String getDimensionName() {
        return dimensionName;
    }

    @Override
    public DimensionSchema getDimensionSchema(ColumnCapabilities capabilities) {
        return new IPRangeDimensionSchema(getDimensionName());
    }

    @Override
    public DimensionIndexer<IPRange, IPRange, IPRange> makeIndexer() {
        return new IPRangeDimensionIndexer();
    }

    @Override
    public DimensionMergerV9 makeMerger(
            String outputName,
            IndexSpec indexSpec,
            SegmentWriteOutMedium segmentWriteOutMedium,
            ColumnCapabilities capabilities,
            ProgressIndicator progress,
            File segmentBaseDir,
            Closer closer) {
        return new IPRangeDimensionMergerV9(outputName, segmentWriteOutMedium);
    }

    @Override
    public int getLengthOfEncodedKeyComponent(IPRange dimVals) {
        if (dimVals == null) {
            return 0;
        }

        return dimVals.getLengthOfEncodedKeyComponent();
    }

    @Override
    public Comparator<ColumnValueSelector> getEncodedValueSelectorComparator() {
        return (s1, s2) -> IPRange.COMPARATOR.compare((IPRange) s1.getObject(), (IPRange) s2.getObject());
    }

    @Override
    public SettableColumnValueSelector makeNewSettableEncodedValueSelector() {
        return new SettableObjectColumnValueSelector();
    }
}
