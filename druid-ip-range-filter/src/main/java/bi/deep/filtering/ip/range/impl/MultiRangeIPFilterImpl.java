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
package bi.deep.filtering.ip.range.impl;

import bi.deep.filtering.common.IPAddressPredicate;
import bi.deep.filtering.common.IPAddressPredicateFactory;
import bi.deep.range.IPRange;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import inet.ipaddr.IPAddress;
import inet.ipaddr.ipv4.IPv4Address;
import inet.ipaddr.ipv4.IPv4AddressTrie;
import inet.ipaddr.ipv6.IPv6Address;
import inet.ipaddr.ipv6.IPv6AddressTrie;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.druid.error.InvalidInput;
import org.apache.druid.query.dimension.DefaultDimensionSpec;
import org.apache.druid.query.filter.ColumnIndexSelector;
import org.apache.druid.query.filter.Filter;
import org.apache.druid.query.filter.ValueMatcher;
import org.apache.druid.segment.ColumnSelectorFactory;
import org.apache.druid.segment.index.BitmapColumnIndex;

public class MultiRangeIPFilterImpl implements Filter {
    private final String column;

    private final IPv4AddressTrie v4Trie = new IPv4AddressTrie();
    private final IPv6AddressTrie v6Trie = new IPv6AddressTrie();
    private final boolean ignoreVersionMismatch;

    public MultiRangeIPFilterImpl(String column, Set<IPRange> ranges, boolean ignoreVersionMismatch) {

        if (column == null) {
            throw InvalidInput.exception("Column cannot be null");
        }

        if (CollectionUtils.isEmpty(ranges)) {
            throw InvalidInput.exception("ranges cannot be null or empty");
        }

        this.column = column;
        ranges.forEach(this::collectRange);
        this.ignoreVersionMismatch = ignoreVersionMismatch;
    }

    @Nullable
    @Override
    public BitmapColumnIndex getBitmapColumnIndex(ColumnIndexSelector columnIndexSelector) {
        return null;
    }

    @Override
    public ValueMatcher makeMatcher(ColumnSelectorFactory factory) {
        return factory.makeDimensionSelector(new DefaultDimensionSpec(column, column))
                .makeValueMatcher(new IPAddressPredicateFactory(IPAddressPredicate.of(this::contains)));
    }

    @VisibleForTesting
    public boolean contains(@NotNull final IPAddress ipAddress) {
        // Check if we have same version ranges defined
        if (ipAddress.isIPv4()) {
            return v4Trie.isEmpty() ? ignoreVersionMismatch : v4Trie.elementContains((IPv4Address) ipAddress);
        } else if (ipAddress.isIPv6()) {
            return v6Trie.isEmpty() ? ignoreVersionMismatch : v6Trie.elementContains((IPv6Address) ipAddress);
        } else {
            return ignoreVersionMismatch;
        }
    }

    private void collectRange(IPRange range) {
        for (IPAddress block : range.getAddressRange().spanWithPrefixBlocks()) {
            if (block.isIPv4()) {
                v4Trie.add((IPv4Address) block);
            } else {
                v6Trie.add((IPv6Address) block);
            }
        }
    }

    @Override
    public Set<String> getRequiredColumns() {
        return ImmutableSet.of(column);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MultiRangeIPFilterImpl)) {
            return false;
        }

        final MultiRangeIPFilterImpl that = (MultiRangeIPFilterImpl) o;

        return ignoreVersionMismatch == that.ignoreVersionMismatch
                && Objects.equals(column, that.column)
                && Objects.equals(v4Trie, that.v4Trie)
                && Objects.equals(v6Trie, that.v6Trie);
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, v4Trie, v6Trie, ignoreVersionMismatch);
    }
}
