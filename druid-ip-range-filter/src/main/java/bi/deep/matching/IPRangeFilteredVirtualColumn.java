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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.druid.query.cache.CacheKeyBuilder;
import org.apache.druid.query.dimension.DefaultDimensionSpec;
import org.apache.druid.query.dimension.DimensionSpec;
import org.apache.druid.segment.ColumnInspector;
import org.apache.druid.segment.ColumnSelectorFactory;
import org.apache.druid.segment.ColumnValueSelector;
import org.apache.druid.segment.DimensionSelector;
import org.apache.druid.segment.VirtualColumn;
import org.apache.druid.segment.column.ColumnCapabilities;
import org.apache.druid.segment.column.ColumnCapabilitiesImpl;
import org.apache.druid.segment.column.ColumnType;

@JsonTypeName("ip-range-filtered")
public class IPRangeFilteredVirtualColumn implements VirtualColumn {

    public static final byte CACHE_TYPE_ID_IP_RANGE_VC = 0x04;
    private final String name;
    private final DimensionSpec delegate;
    private final Set<String> ips;

    @JsonCreator
    public IPRangeFilteredVirtualColumn(
            @JsonProperty("name") String name,
            @JsonProperty("delegate") DimensionSpec delegate,
            @JsonProperty("values") Set<String> ips) {
        if (ips == null || ips.isEmpty()) {
            throw new IllegalArgumentException("values are not defined");
        }
        this.name = Preconditions.checkNotNull(name, "name must be defined");
        this.delegate = Preconditions.checkNotNull(delegate, "delegate");
        this.ips = ips;
    }

    @JsonProperty("name")
    @Override
    public String getOutputName() {
        return name;
    }

    @JsonProperty("values")
    public Set<String> getIps() {
        return ips;
    }

    @JsonProperty("delegate")
    public DimensionSpec getDelegate() {
        return delegate;
    }

    @Override
    public DimensionSelector makeDimensionSelector(DimensionSpec dimensionSpec, ColumnSelectorFactory factory) {
        return IPRangeFilteredDimensionSpec.makeDimensionSelector(ips, factory.makeDimensionSelector(delegate));
    }

    @Override
    public ColumnValueSelector<?> makeColumnValueSelector(String columnName, ColumnSelectorFactory factory) {
        return makeDimensionSelector(DefaultDimensionSpec.of(columnName), factory);
    }

    @Override
    public ColumnCapabilities capabilities(String columnName) {
        return new ColumnCapabilitiesImpl().setType(ColumnType.STRING).setHasMultipleValues(true);
    }

    @Override
    public ColumnCapabilities capabilities(ColumnInspector inspector, String columnName) {
        return inspector.getColumnCapabilities(delegate.getDimension());
    }

    @Override
    public List<String> requiredColumns() {
        return Collections.singletonList(delegate.getDimension());
    }

    @Override
    public boolean usesDotNotation() {
        return false;
    }

    @Override
    public byte[] getCacheKey() {
        CacheKeyBuilder builder = new CacheKeyBuilder(CACHE_TYPE_ID_IP_RANGE_VC)
                .appendString(name)
                .appendCacheable(delegate)
                .appendStringsIgnoringOrder(ips);
        return builder.build();
    }
}
