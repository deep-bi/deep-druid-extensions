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
package bi.deep.filtering.ip.range;

import bi.deep.filtering.ip.range.impl.SingleTypeIPRangeFilterImpl;
import bi.deep.range.IPBoundedRange;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.RangeSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.druid.query.cache.CacheKeyBuilder;
import org.apache.druid.query.filter.AbstractOptimizableDimFilter;
import org.apache.druid.query.filter.DimFilter;
import org.apache.druid.query.filter.DimFilterUtils;
import org.apache.druid.query.filter.Filter;

/**
 * @deprecated This class is deprecated in favour of `ip_multi_range` filter.
 * Please use {@link bi.deep.filtering.ip.range.MultiRangeIPFilter} instead.
 */
@Deprecated
@JsonTypeName("ip_single_range")
public class SingleTypeIPRangeFilter extends AbstractOptimizableDimFilter implements DimFilter {
    private static final byte CACHE_ID = 0x51;
    private final String dimension;
    private final IPBoundedRange range;
    private final boolean ignoreVersionMismatch;

    @JsonCreator
    public SingleTypeIPRangeFilter(
            @JsonProperty("dimension") String dimension,
            @JsonProperty("range") IPBoundedRange range,
            @JsonProperty("ignoreVersionMismatch") @Nullable Boolean ignoreVersionMismatch) {
        this.dimension = Preconditions.checkNotNull(dimension, "dimension");
        this.range = Preconditions.checkNotNull(range, "range");
        this.ignoreVersionMismatch = ignoreVersionMismatch != null && ignoreVersionMismatch;
    }

    @JsonProperty("dimension")
    public String getDimension() {
        return dimension;
    }

    @JsonProperty("range")
    public IPBoundedRange getRange() {
        return range;
    }

    @JsonProperty("ignoreVersionMismatch")
    public boolean isIgnoreVersionMismatch() {
        return ignoreVersionMismatch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SingleTypeIPRangeFilter)) {
            return false;
        }

        final SingleTypeIPRangeFilter that = (SingleTypeIPRangeFilter) o;

        return ignoreVersionMismatch == that.ignoreVersionMismatch
                && Objects.equals(dimension, that.dimension)
                && Objects.equals(range, that.range);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, range, ignoreVersionMismatch);
    }

    @Override
    public Filter toFilter() {
        return new SingleTypeIPRangeFilterImpl(dimension, range, ignoreVersionMismatch);
    }

    @Nullable
    @Override
    public RangeSet<String> getDimensionRangeSet(String dimension) {
        return null;
    }

    @Override
    public Set<String> getRequiredColumns() {
        return ImmutableSet.of(dimension);
    }

    @Override
    public byte[] getCacheKey() {
        return new CacheKeyBuilder(CACHE_ID)
                .appendString(dimension)
                .appendByte(DimFilterUtils.STRING_SEPARATOR)
                .appendString(range.getLower())
                .appendByte(DimFilterUtils.STRING_SEPARATOR)
                .appendString(range.getUpper())
                .appendByte(DimFilterUtils.STRING_SEPARATOR)
                .appendBoolean(ignoreVersionMismatch)
                .appendByte(DimFilterUtils.STRING_SEPARATOR)
                .appendBoolean(range.isLowerOpen())
                .appendByte(DimFilterUtils.STRING_SEPARATOR)
                .appendBoolean(range.isUpperOpen())
                .build();
    }
}
