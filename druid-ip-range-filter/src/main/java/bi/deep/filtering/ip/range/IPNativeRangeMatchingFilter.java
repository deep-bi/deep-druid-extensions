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

import bi.deep.filtering.ip.range.impl.IPRangeMatchingFilterImpl;
import bi.deep.util.IPRangeUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.RangeSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.druid.query.cache.CacheKeyBuilder;
import org.apache.druid.query.filter.AbstractOptimizableDimFilter;
import org.apache.druid.query.filter.DimFilter;
import org.apache.druid.query.filter.DimFilterUtils;
import org.apache.druid.query.filter.Filter;

@JsonTypeName("ip_native_match")
public class IPNativeRangeMatchingFilter extends AbstractOptimizableDimFilter implements DimFilter {
    private static final byte CACHE_ID = 0x53;
    private final String dimension;
    private final Set<String> ips;

    @JsonCreator
    public IPNativeRangeMatchingFilter(
            @JsonProperty("dimension") String dimension, @JsonProperty("values") Set<String> ips) {
        this.dimension = Preconditions.checkNotNull(dimension, "dimension");
        if (CollectionUtils.isEmpty(ips)) {
            throw new IllegalArgumentException("values are not defined");
        }

        this.ips = ips;
    }

    @JsonProperty("dimension")
    public String getDimension() {
        return dimension;
    }

    @JsonProperty("values")
    public Set<String> getIps() {
        return ips;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IPNativeRangeMatchingFilter)) {
            return false;
        }

        final IPNativeRangeMatchingFilter that = (IPNativeRangeMatchingFilter) o;

        return Objects.equals(dimension, that.dimension) && Objects.equals(ips, that.ips);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, ips);
    }

    @Override
    public Filter toFilter() {
        return new IPRangeMatchingFilterImpl(dimension, IPRangeUtil.mapStringsToIps(ips));
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
                .appendString(ips.toString())
                .build();
    }
}
