/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package bi.deep.filtering.ip.range.impl;

import bi.deep.filtering.common.IPRange;
import bi.deep.filtering.common.PredicateFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.druid.error.InvalidInput;
import org.apache.druid.query.filter.ColumnIndexSelector;
import org.apache.druid.query.filter.Filter;
import org.apache.druid.query.filter.ValueMatcher;
import org.apache.druid.segment.ColumnSelectorFactory;
import org.apache.druid.segment.index.BitmapColumnIndex;

public class MultiRangeIPFilterImpl implements Filter {
    private final String column;
    private final Set<IPRange> ipV4Ranges;
    private final Set<IPRange> ipV6Ranges;
    private final boolean ignoreVersionMismatch;

    public MultiRangeIPFilterImpl(String column, Set<IPRange> ranges, boolean ignoreVersionMismatch) {

        if (column == null) {
            throw InvalidInput.exception("Column cannot be null");
        }

        if (CollectionUtils.isEmpty(ranges)) {
            throw InvalidInput.exception("ranges cannot be null or empty");
        }

        this.column = column;
        this.ipV4Ranges = ranges.stream().filter(IPRange::isIPv4).collect(Collectors.toSet());
        this.ipV6Ranges = ranges.stream().filter(IPRange::isIPv6).collect(Collectors.toSet());
        this.ignoreVersionMismatch = ignoreVersionMismatch;
    }

    @Nullable
    @Override
    public BitmapColumnIndex getBitmapColumnIndex(ColumnIndexSelector columnIndexSelector) {
        return null;
    }

    @Override
    public ValueMatcher makeMatcher(ColumnSelectorFactory factory) {
        return new PredicateFactory(factory.makeColumnValueSelector(column), this::contains);
    }

    @VisibleForTesting
    public boolean contains(@NotNull final String addressStr) {
        final IPAddress ipAddress = new IPAddressString(addressStr).getAddress();

        if (ipAddress == null) {
            return ignoreVersionMismatch;
        }

        // Check if we have same version ranges defined
        if (ipAddress.isIPv4() && !ipV4Ranges.isEmpty()) {
            return ipV4Ranges.stream().anyMatch(range -> range.contains(ipAddress));
        } else if (ipAddress.isIPv6() && !ipV6Ranges.isEmpty()) {
            return ipV6Ranges.stream().anyMatch(range -> range.contains(ipAddress));
        }

        return ignoreVersionMismatch;
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
                && Objects.equals(ipV4Ranges, that.ipV4Ranges)
                && Objects.equals(ipV6Ranges, that.ipV6Ranges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, ipV4Ranges, ipV6Ranges, ignoreVersionMismatch);
    }
}
