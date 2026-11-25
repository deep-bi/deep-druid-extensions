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

import bi.deep.util.IPRangeUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import inet.ipaddr.IPAddress;
import java.util.List;
import java.util.Set;
import org.apache.druid.query.cache.CacheKeyBuilder;
import org.apache.druid.query.dimension.DimensionSpec;
import org.apache.druid.query.extraction.ExtractionFn;
import org.apache.druid.segment.DimensionSelector;
import org.apache.druid.segment.column.ColumnType;

@JsonTypeName("ip-range-filtered-spec")
public class IPRangeFilteredDimensionSpec implements DimensionSpec {
    public static final byte CACHE_TYPE_ID_IP_RANGE_DIM = 0x5;
    private final String name;
    private final DimensionSpec delegate;
    // one instance per spec: Druid calls getExtractionFn() per selector and capability check, and the function
    // holds the snapshot of the IPs and their memoised digest
    private final IPRangeFilteredExtractionFn extractionFn;

    @JsonCreator
    public IPRangeFilteredDimensionSpec(
            @JsonProperty("name") String name,
            @JsonProperty("delegate") DimensionSpec delegate,
            @JsonProperty("values") List<IPAddress> ips) {
        if (ips == null || ips.isEmpty()) {
            throw new IllegalArgumentException("values are not defined");
        }
        this.name = Preconditions.checkNotNull(name, "name must be defined");
        this.delegate = Preconditions.checkNotNull(delegate, "delegate");
        this.extractionFn = new IPRangeFilteredExtractionFn(ips);
    }

    public static DimensionSelector makeDimensionSelector(Set<String> values, DimensionSelector valueSelector) {
        return new IPRangeFilteredDimensionSelector(
                valueSelector, new IPRangeFilteredExtractionFn(IPRangeUtil.mapStringsToIps(values)));
    }

    @JsonProperty("delegate")
    public DimensionSpec getDelegate() {
        return delegate;
    }

    @JsonProperty("values")
    public List<IPAddress> getIps() {
        return extractionFn.getIps();
    }

    @Override
    public String getDimension() {
        return delegate.getDimension();
    }

    @Override
    @JsonProperty("name")
    public String getOutputName() {
        return name;
    }

    @Override
    public ColumnType getOutputType() {
        return ColumnType.STRING;
    }

    @Override
    public ExtractionFn getExtractionFn() {
        return extractionFn;
    }

    @Override
    public DimensionSelector decorate(DimensionSelector selector) {
        return selector;
    }

    @Override
    public boolean mustDecorate() {
        return false;
    }

    @Override
    public boolean preservesOrdering() {
        return extractionFn.preservesOrdering();
    }

    @Override
    public DimensionSpec withDimension(String newDimension) {
        return new IPRangeFilteredDimensionSpec(name, delegate.withDimension(newDimension), getIps());
    }

    @Override
    public byte[] getCacheKey() {
        return new CacheKeyBuilder(CACHE_TYPE_ID_IP_RANGE_DIM)
                .appendString(name)
                .appendCacheable(delegate)
                .appendByteArray(extractionFn.getIpsDigest())
                .build();
    }
}
