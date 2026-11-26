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

import bi.deep.guice.IPRangeDimensionModule;
import bi.deep.util.CacheKeyUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.druid.query.cache.CacheKeyBuilder;
import org.apache.druid.query.dimension.DimensionSpec;
import org.apache.druid.query.extraction.ExtractionFn;
import org.apache.druid.segment.DimensionSelector;
import org.apache.druid.segment.column.ColumnType;

@JsonTypeName("ip-native-filtered-spec")
public class IPNativeRangeArrayFilteredDimensionSpec implements DimensionSpec {
    public static final byte CACHE_TYPE_ID_IP_RANGE_DIM = 0x6;
    private final String name;
    private final DimensionSpec delegate;
    private final Set<String> ips;
    // sorting and hashing a large set is costly and getCacheKey can run many times per query
    private final Supplier<byte[]> ipsDigest;

    @JsonCreator
    public IPNativeRangeArrayFilteredDimensionSpec(
            @JsonProperty("name") String name,
            @JsonProperty("delegate") DimensionSpec delegate,
            @JsonProperty("values") Set<String> ips) {
        if (ips == null || ips.isEmpty()) {
            throw new IllegalArgumentException("values are not defined");
        }
        this.name = Preconditions.checkNotNull(name, "name must be defined");
        this.delegate = Preconditions.checkNotNull(delegate, "delegate");
        // snapshot so the memoised digest cannot drift from the values used for matching
        this.ips = Collections.unmodifiableSet(new LinkedHashSet<>(ips));
        this.ipsDigest = Suppliers.memoize(() -> CacheKeyUtil.hashStringsIgnoringOrder(this.ips));
    }

    @JsonProperty("delegate")
    public DimensionSpec getDelegate() {
        return delegate;
    }

    @JsonProperty("values")
    public Set<String> getIps() {
        return ips;
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
        return IPRangeDimensionModule.ARRAY_TYPE;
    }

    @Override
    public ExtractionFn getExtractionFn() {
        return null;
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
        return true;
    }

    @Override
    public DimensionSpec withDimension(String newDimension) {
        return new IPNativeRangeArrayFilteredDimensionSpec(name, getDelegate().withDimension(newDimension), ips);
    }

    @Override
    public byte[] getCacheKey() {
        return new CacheKeyBuilder(CACHE_TYPE_ID_IP_RANGE_DIM)
                .appendString(name)
                .appendCacheable(delegate)
                .appendByteArray(ipsDigest.get())
                .build();
    }
}
