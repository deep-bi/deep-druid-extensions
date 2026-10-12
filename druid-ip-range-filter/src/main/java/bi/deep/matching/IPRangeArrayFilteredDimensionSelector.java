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
package bi.deep.matching;

import static inet.ipaddr.Address.ADDRESS_LOW_VALUE_COMPARATOR;

import bi.deep.entity.dimension.IPRange;
import bi.deep.entity.dimension.IPRangeArray;
import inet.ipaddr.IPAddress;
import inet.ipaddr.format.IPAddressRange;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.druid.java.util.common.logger.Logger;
import org.apache.druid.query.filter.DruidPredicateFactory;
import org.apache.druid.query.filter.ValueMatcher;
import org.apache.druid.query.monomorphicprocessing.RuntimeShapeInspector;
import org.apache.druid.segment.AbstractDimensionSelector;
import org.apache.druid.segment.ColumnValueSelector;
import org.apache.druid.segment.IdLookup;
import org.apache.druid.segment.data.IndexedInts;
import org.apache.druid.segment.data.ZeroIndexedInts;

public class IPRangeArrayFilteredDimensionSelector extends AbstractDimensionSelector {
    private static final Logger log = new Logger(IPRangeFilteredDimensionSelector.class);
    protected final ColumnValueSelector columnSelector;
    private final SortedSet<IPAddress> rangesToMatch = new TreeSet<>(ADDRESS_LOW_VALUE_COMPARATOR);

    public IPRangeArrayFilteredDimensionSelector(ColumnValueSelector columnSelector, List<IPAddress> rangesToMatch) {
        this.columnSelector = columnSelector;
        this.rangesToMatch.addAll(rangesToMatch);
    }

    @Override
    @Nonnull
    public IndexedInts getRow() {
        return ZeroIndexedInts.instance();
    }

    @Override
    @Nonnull
    public ValueMatcher makeValueMatcher(@Nullable String value) {
        throw new UnsupportedOperationException(value);
    }

    @Override
    @Nonnull
    public ValueMatcher makeValueMatcher(@Nonnull DruidPredicateFactory predicateFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void inspectRuntimeShape(RuntimeShapeInspector inspector) {
        columnSelector.inspectRuntimeShape(inspector);
    }

    @Nullable
    @Override
    public IPRangeArray getObject() {
        Object value = columnSelector.getObject();

        if (value == null) {
            return null;
        }

        if (value instanceof IPRangeArray) {
            IPRangeArray rangeArray = (IPRangeArray) value;
            List<IPAddressRange> addressRanges =
                    rangesToMatch.stream().filter(rangeArray::contains).collect(Collectors.toList());
            return addressRanges.isEmpty() ? null : new IPRangeArray(addressRanges);
        }

        if (value instanceof IPRange) {
            IPRange rangeArray = (IPRange) value;
            List<IPAddressRange> addressRanges =
                    rangesToMatch.stream().filter(rangeArray::contains).collect(Collectors.toList());
            return addressRanges.isEmpty() ? null : new IPRangeArray(addressRanges);
        }

        log.warn("Expected either IPRange or IPRangeArray but found: {}", value.getClass());
        return null;
    }

    @Override
    @Nonnull
    public Class<?> classOfObject() {
        return IPRangeArray.class;
    }

    @Override
    public int getValueCardinality() {
        return CARDINALITY_UNKNOWN;
    }

    @Nullable
    @Override
    public String lookupName(int id) {
        return null;
    }

    @Override
    public boolean nameLookupPossibleInAdvance() {
        return false;
    }

    @Nullable
    @Override
    public IdLookup idLookup() {
        return null;
    }
}
