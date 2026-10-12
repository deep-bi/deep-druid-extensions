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

import bi.deep.filtering.common.MatchPredicateFactory;
import com.google.common.collect.ImmutableSet;
import inet.ipaddr.IPAddress;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.druid.query.filter.ColumnIndexSelector;
import org.apache.druid.query.filter.Filter;
import org.apache.druid.query.filter.ValueMatcher;
import org.apache.druid.segment.ColumnSelectorFactory;
import org.apache.druid.segment.filter.Filters;
import org.apache.druid.segment.index.BitmapColumnIndex;

public class IPRangeMatchingFilterImpl implements Filter {
    private final String column;
    private final List<IPAddress> ips;

    public IPRangeMatchingFilterImpl(String column, List<IPAddress> ips) {
        if (column == null) {
            throw new IllegalArgumentException("Column cannot be null");
        }
        this.column = column;
        this.ips = ips;
    }

    @Nullable
    @Override
    public BitmapColumnIndex getBitmapColumnIndex(ColumnIndexSelector selector) {
        return null;
    }

    @Override
    public ValueMatcher makeMatcher(ColumnSelectorFactory factory) {
        return Filters.makeValueMatcher(factory, column, new MatchPredicateFactory(ips));
    }

    @Override
    public Set<String> getRequiredColumns() {
        return ImmutableSet.of(column);
    }
}
