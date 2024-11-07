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

import bi.deep.filtering.common.PredicateFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.druid.error.InvalidInput;
import org.apache.druid.query.filter.ColumnIndexSelector;
import org.apache.druid.query.filter.Filter;
import org.apache.druid.query.filter.ValueMatcher;
import org.apache.druid.segment.ColumnSelectorFactory;
import org.apache.druid.segment.index.BitmapColumnIndex;

public class FixedSetIPFilterImpl implements Filter {
    private final String column;
    private final SortedSet<IPAddress> addressSet;
    private final boolean ignoreInvalidAddress;

    public FixedSetIPFilterImpl(String column, SortedSet<IPAddress> addressSet, boolean ignoreInvalidAddress) {
        if (column == null) {
            throw InvalidInput.exception("column cannot be null");
        }

        if (CollectionUtils.isEmpty(addressSet)) {
            throw InvalidInput.exception("addressSet cannot be null or empty");
        }

        this.column = column;
        this.ignoreInvalidAddress = ignoreInvalidAddress;
        this.addressSet = addressSet;
    }

    @Nullable
    @Override
    public BitmapColumnIndex getBitmapColumnIndex(ColumnIndexSelector columnIndexSelector) {
        return null;
    }

    @Override
    public ValueMatcher makeMatcher(ColumnSelectorFactory factory) {
        return new PredicateFactory(factory.makeColumnValueSelector(column), this::contain);
    }

    @VisibleForTesting
    public boolean contain(@NotNull final String addressStr) {
        final IPAddress ipAddress = new IPAddressString(addressStr).getAddress();
        return ipAddress == null ? ignoreInvalidAddress : addressSet.contains(ipAddress);
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
        if (!(o instanceof FixedSetIPFilterImpl)) {
            return false;
        }

        final FixedSetIPFilterImpl that = (FixedSetIPFilterImpl) o;

        return Objects.equals(column, that.column) && Objects.equals(addressSet, that.addressSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, addressSet);
    }
}
