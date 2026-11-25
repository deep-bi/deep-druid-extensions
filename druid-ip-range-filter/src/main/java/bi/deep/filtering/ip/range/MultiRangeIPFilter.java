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

import bi.deep.filtering.ip.range.impl.MultiRangeIPFilterImpl;
import bi.deep.range.IPRange;
import bi.deep.util.CacheKeyUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.RangeSet;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.druid.error.InvalidInput;
import org.apache.druid.query.cache.CacheKeyBuilder;
import org.apache.druid.query.filter.AbstractOptimizableDimFilter;
import org.apache.druid.query.filter.DimFilter;
import org.apache.druid.query.filter.DimFilterUtils;
import org.apache.druid.query.filter.Filter;

@JsonTypeName("ip_multi_range")
public class MultiRangeIPFilter extends AbstractOptimizableDimFilter implements DimFilter {
    private static final byte CACHE_ID = 0x50;

    private final String dimension;
    private final Set<String> ranges;
    // sorting and hashing a large set is costly and getCacheKey can run many times per query
    private final Supplier<byte[]> rangesDigest;
    private final boolean ignoreVersionMismatch;

    @JsonCreator
    public MultiRangeIPFilter(
            @JsonProperty("dimension") String dimension,
            @JsonProperty("ranges") Set<String> ranges,
            @JsonProperty("ignoreVersionMismatch") @Nullable Boolean ignoreVersionMismatch) {
        this.dimension = Preconditions.checkNotNull(dimension, "dimension");
        // snapshot so the memoised digest cannot drift from the values used for matching
        this.ranges = Collections.unmodifiableSet(new LinkedHashSet<>(Preconditions.checkNotNull(ranges, "ranges")));

        if (CollectionUtils.isEmpty(this.ranges)) {
            throw InvalidInput.exception("ranges cannot be null or empty");
        }

        this.rangesDigest = Suppliers.memoize(() -> CacheKeyUtil.hashStringsIgnoringOrder(this.ranges));
        this.ignoreVersionMismatch = ignoreVersionMismatch != null && ignoreVersionMismatch;
    }

    @JsonProperty("dimension")
    public String getDimension() {
        return dimension;
    }

    @JsonProperty("ranges")
    public Set<String> getRanges() {
        return ranges;
    }

    @JsonProperty("ignoreVersionMismatch")
    public boolean isIgnoreVersionMismatch() {
        return ignoreVersionMismatch;
    }

    @Override
    public Filter toFilter() {
        final Set<IPRange> collect = ranges.stream().map(IPRange::new).collect(Collectors.toSet());
        return new MultiRangeIPFilterImpl(dimension, collect, ignoreVersionMismatch);
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
                .appendByteArray(rangesDigest.get())
                .appendByte(DimFilterUtils.STRING_SEPARATOR)
                .appendBoolean(ignoreVersionMismatch)
                .build();
    }
}
