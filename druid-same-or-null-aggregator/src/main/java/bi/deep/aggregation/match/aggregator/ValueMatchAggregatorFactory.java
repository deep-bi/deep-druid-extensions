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
package bi.deep.aggregation.match.aggregator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.druid.java.util.common.IAE;
import org.apache.druid.query.aggregation.AggregateCombiner;
import org.apache.druid.query.aggregation.Aggregator;
import org.apache.druid.query.aggregation.AggregatorFactory;
import org.apache.druid.query.aggregation.BufferAggregator;
import org.apache.druid.query.aggregation.ObjectAggregateCombiner;
import org.apache.druid.query.cache.CacheKeyBuilder;
import org.apache.druid.segment.ColumnSelectorFactory;
import org.apache.druid.segment.ColumnValueSelector;
import org.apache.druid.segment.NilColumnValueSelector;
import org.apache.druid.segment.column.ColumnType;

@JsonTypeName("sameOrNull")
public class ValueMatchAggregatorFactory extends AggregatorFactory {
    private static final byte CACHE_ID = 0x70;

    private final String name;
    private final String fieldName;
    private final int maxSize;
    private final ColumnType outputType;

    @JsonCreator
    public ValueMatchAggregatorFactory(
            @JsonProperty("name") @Nullable final String name,
            @JsonProperty("fieldName") final String fieldName,
            @JsonProperty("maxSize") @Nullable final Integer maxSize,
            @JsonProperty("outputType") @Nullable ColumnType outputType) {

        if (StringUtils.isBlank(fieldName)) {
            throw new IAE("Must have a valid, non-null aggregator fieldName");
        }

        this.fieldName = fieldName;
        this.name = Optional.ofNullable(name).orElse(fieldName);
        this.maxSize = Optional.ofNullable(maxSize).orElse(1024);
        this.outputType = Optional.ofNullable(outputType).orElse(ColumnType.UNKNOWN_COMPLEX);
    }

    @Override
    public Aggregator factorize(final ColumnSelectorFactory metricFactory) {
        final ColumnValueSelector<?> selector = metricFactory.makeColumnValueSelector(getFieldName());

        return selector instanceof NilColumnValueSelector
                ? new NoopValueMatchAggregator()
                : new ValueMatchBuildAggregator(selector);
    }

    @Override
    public BufferAggregator factorizeBuffered(ColumnSelectorFactory metricFactory) {
        final ColumnValueSelector<?> selector = metricFactory.makeColumnValueSelector(getFieldName());

        return selector instanceof NilColumnValueSelector
                ? new NoopValueMatchBufferAggregator()
                : new ValueMatchBufferBuildAggregator(selector);
    }

    @Override
    public Comparator<Object> getComparator() {
        return (o1, o2) -> {
            if (o1 == o2) {
                return 0;
            }

            if (o1 instanceof Comparable && o2 instanceof Comparable) {
                @SuppressWarnings("unchecked")
                final Comparable<Object> comparable = (Comparable<Object>) o1;

                return (comparable).compareTo(o2);
            }

            return Objects.toString(o1).compareTo(Objects.toString(o2));
        };
    }

    @Nullable
    @Override
    public Object combine(@Nullable Object lhs, @Nullable Object rhs) {
        if (lhs == null) return rhs;
        if (rhs == null) return lhs;

        return Objects.equals(lhs, rhs) ? lhs : null;
    }

    @Override
    public AggregatorFactory getCombiningFactory() {
        return new ValueMatchAggregatorFactory(getName(), getName(), getMaxIntermediateSize(), getResultType());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public AggregateCombiner makeAggregateCombiner() {
        return new ObjectAggregateCombiner() {
            @Nullable
            private Object currentValue = null;

            private boolean isDifferent = false;

            @Override
            public void reset(ColumnValueSelector selector) {
                currentValue = selector.getObject();
                isDifferent = false;
            }

            @Override
            public void fold(ColumnValueSelector selector) {
                if (isDifferent) {
                    return;
                }

                final Object value = selector.getObject();

                if (currentValue == null) {
                    currentValue = value;
                } else if (!Objects.equals(currentValue, value)) {
                    isDifferent = true;
                }
            }

            @Nullable
            @Override
            public Object getObject() {
                return isDifferent ? null : currentValue;
            }

            @Override
            public Class<?> classOfObject() {
                return Object.class;
            }
        };
    }

    @Override
    public Object deserialize(Object object) {
        return object;
    }

    @Nullable
    @Override
    public Object finalizeComputation(@Nullable Object object) {
        return object;
    }

    @Override
    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public String getFieldName() {
        return fieldName;
    }

    @JsonProperty
    public int getMaxSize() {
        return maxSize;
    }

    @JsonProperty
    public ColumnType getOutputType() {
        return outputType;
    }

    @Override
    public List<String> requiredFields() {
        return Collections.singletonList(fieldName);
    }

    @Override
    public AggregatorFactory withName(String newName) {
        return new ValueMatchAggregatorFactory(newName, getFieldName(), getMaxIntermediateSize(), getResultType());
    }

    @Override
    public ColumnType getIntermediateType() {
        return getResultType();
    }

    @Override
    public ColumnType getResultType() {
        return getOutputType();
    }

    @Override
    public int getMaxIntermediateSize() {
        return getMaxSize();
    }

    @Override
    public byte[] getCacheKey() {
        return new CacheKeyBuilder(getCacheId())
                .appendString("sameOfNull")
                .appendString(name)
                .appendString(fieldName)
                .appendInt(maxSize)
                .appendString(outputType.toString())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ValueMatchAggregatorFactory that = (ValueMatchAggregatorFactory) o;

        return Objects.equals(name, that.name)
                && Objects.equals(fieldName, that.fieldName)
                && Objects.equals(maxSize, that.maxSize)
                && Objects.equals(outputType, that.outputType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, fieldName, maxSize, outputType);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{"
                + "name=" + name
                + ", fieldName=" + fieldName
                + ", maxSize=" + maxSize
                + ", resultType=" + outputType
                + "}";
    }

    protected byte getCacheId() {
        return CACHE_ID;
    }
}
