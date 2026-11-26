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

import bi.deep.filtering.ip.range.impl.FixedSetIPFilterImpl;
import bi.deep.util.CacheKeyUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.RangeSet;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.druid.error.InvalidInput;
import org.apache.druid.query.cache.CacheKeyBuilder;
import org.apache.druid.query.filter.AbstractOptimizableDimFilter;
import org.apache.druid.query.filter.DimFilter;
import org.apache.druid.query.filter.DimFilterUtils;
import org.apache.druid.query.filter.Filter;

/**
 * @deprecated This class is deprecated in favour of `ip_range_match` filter.
 * Please use {@link bi.deep.filtering.ip.range.RangeMatchingIPFilter} instead.
 */
@Deprecated
@JsonTypeName("ip_fixed_range")
public class FixedSetIPFilter extends AbstractOptimizableDimFilter implements DimFilter {
    private static final byte CACHE_ID = 0x52;

    private final String dimension;
    private final Set<String> ranges;
    // sorting and hashing a large set is costly and getCacheKey can run many times per query
    private final Supplier<byte[]> rangesDigest;

    @JsonCreator
    public FixedSetIPFilter(@JsonProperty("dimension") String dimension, @JsonProperty("ranges") Set<String> ranges) {
        this.dimension = Preconditions.checkNotNull(dimension, "dimension");
        // snapshot so the memoised digest cannot drift from the values used for matching
        this.ranges = Collections.unmodifiableSet(new LinkedHashSet<>(Preconditions.checkNotNull(ranges, "ranges")));

        if (CollectionUtils.isEmpty(this.ranges)) {
            throw InvalidInput.exception("range cannot be null or empty");
        }

        this.rangesDigest = Suppliers.memoize(() -> CacheKeyUtil.hashStringsIgnoringOrder(this.ranges));
    }

    @Override
    public Filter toFilter() {
        final SortedSet<IPAddress> addresses = getRanges().stream()
                .map(this::map)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        return new FixedSetIPFilterImpl(dimension, addresses);
    }

    @JsonProperty("dimension")
    public String getDimension() {
        return dimension;
    }

    @JsonProperty("ranges")
    public Set<String> getRanges() {
        return ranges;
    }

    public IPAddress map(final String addressStr) {
        return new IPAddressString(addressStr).getAddress();
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
                .build();
    }
}
