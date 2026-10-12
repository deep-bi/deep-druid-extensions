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
import javax.annotation.Nullable;
import org.apache.druid.collections.bitmap.BitmapFactory;
import org.apache.druid.collections.bitmap.MutableBitmap;
import org.apache.druid.query.dimension.DimensionSpec;
import org.apache.druid.query.monomorphicprocessing.RuntimeShapeInspector;
import org.apache.druid.segment.ColumnValueSelector;
import org.apache.druid.segment.DimensionDictionarySelector;
import org.apache.druid.segment.DimensionIndexer;
import org.apache.druid.segment.DimensionSelector;
import org.apache.druid.segment.EncodedKeyComponent;
import org.apache.druid.segment.ObjectColumnSelector;
import org.apache.druid.segment.column.ColumnCapabilities;
import org.apache.druid.segment.column.ColumnCapabilitiesImpl;
import org.apache.druid.segment.data.CloseableIndexed;
import org.apache.druid.segment.incremental.IncrementalIndex;
import org.apache.druid.segment.incremental.IncrementalIndexRowHolder;

public class IPRangeDimensionIndexer implements DimensionIndexer<IPRange, IPRange, IPRange> {
    @Override
    public EncodedKeyComponent<IPRange> processRowValsToUnsortedEncodedKeyComponent(
            @Nullable Object dimValues, boolean reportParseExceptions) {
        IPRange range = IPRange.from(dimValues);
        return new EncodedKeyComponent<>(range, range.getLengthOfEncodedKeyComponent());
    }

    @Override
    public void setSparseIndexed() {}

    @Override
    public IPRange getUnsortedEncodedValueFromSorted(IPRange val) {
        return val;
    }

    @Override
    public CloseableIndexed<IPRange> getSortedIndexedValues() {
        throw new UnsupportedOperationException("IpRange columns do not support value dictionaries.");
    }

    @Override
    public IPRange getMinValue() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public IPRange getMaxValue() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int getCardinality() {
        return DimensionDictionarySelector.CARDINALITY_UNKNOWN;
    }

    @Override
    public DimensionSelector makeDimensionSelector(
            DimensionSpec spec, IncrementalIndexRowHolder currEntry, IncrementalIndex.DimensionDesc desc) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public ColumnValueSelector<?> makeColumnValueSelector(
            IncrementalIndexRowHolder currEntry, IncrementalIndex.DimensionDesc desc) {
        final int dimIndex = desc.getIndex();
        return new ObjectColumnSelector<IPRange>() {
            @Override
            public void inspectRuntimeShape(RuntimeShapeInspector inspector) {}

            @Nullable
            @Override
            public IPRange getObject() {
                return (IPRange) currEntry.get().getDims()[dimIndex];
            }

            @Override
            public Class<IPRange> classOfObject() {
                return IPRange.class;
            }
        };
    }

    @Override
    public ColumnCapabilities getColumnCapabilities() {
        return new ColumnCapabilitiesImpl().setType(IPRangeDimensionModule.TYPE);
    }

    @Override
    public int compareUnsortedEncodedKeyComponents(@Nullable IPRange lhs, @Nullable IPRange rhs) {
        return IPRange.COMPARATOR.compare(lhs, rhs);
    }

    @Override
    public boolean checkUnsortedEncodedKeyComponentsEqual(@Nullable IPRange lhs, @Nullable IPRange rhs) {
        return IPRange.COMPARATOR.compare(lhs, rhs) == 0;
    }

    @Override
    public int getUnsortedEncodedKeyComponentHashCode(@Nullable IPRange key) {
        return key == null ? 0 : key.hashCode();
    }

    @Override
    public Object convertUnsortedEncodedKeyComponentToActualList(IPRange key) {
        return key;
    }

    @Override
    public ColumnValueSelector convertUnsortedValuesToSorted(ColumnValueSelector selectorWithUnsortedValues) {
        return selectorWithUnsortedValues;
    }

    @Override
    public void fillBitmapsFromUnsortedEncodedKeyComponent(
            IPRange key, int rowNum, MutableBitmap[] bitmapIndexes, BitmapFactory factory) {
        throw new UnsupportedOperationException("Not supported");
    }
}
