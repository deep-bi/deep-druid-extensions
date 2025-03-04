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
package bi.deep;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.nio.ByteBuffer;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.ValidationException;
import org.apache.druid.java.util.common.StringUtils;
import org.apache.druid.query.aggregation.*;
import org.apache.druid.query.dimension.DefaultDimensionSpec;
import org.apache.druid.segment.ColumnSelectorFactory;
import org.apache.druid.segment.DimensionSelector;
import org.apache.druid.segment.column.ColumnType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExactDistinctCountAggregatorFactory extends AggregatorFactory {
    private final String name;
    private final List<String> fieldNames;
    private final Integer maxNumberOfValues;
    private final Boolean failOnLimitExceeded;
    private static final Logger LOG = LoggerFactory.getLogger(ExactDistinctCountAggregatorFactory.class);

    @JsonCreator
    public ExactDistinctCountAggregatorFactory(
            @JsonProperty("name") String name,
            @JsonProperty("fieldNames") List<String> fieldNames,
            @JsonProperty("maxNumberOfValues") Integer maxNumberOfValues,
            @JsonProperty("failOnLimitExceeded") Boolean failOnLimitExceeded) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(fieldNames);
        Preconditions.checkArgument(!fieldNames.isEmpty());

        this.name = name;
        this.fieldNames = fieldNames;

        if (maxNumberOfValues != null && maxNumberOfValues <= 0) {
            throw new ValidationException("Invalid maxNumberOfValues -> '" + maxNumberOfValues + '\'');
        }

        this.failOnLimitExceeded = failOnLimitExceeded != null && failOnLimitExceeded;

        this.maxNumberOfValues = maxNumberOfValues != null ? maxNumberOfValues : 10000;
    }

    @Override
    @Nonnull
    public Aggregator factorize(@Nonnull ColumnSelectorFactory columnFactory) {
        List<DimensionSelector> selectors = new ArrayList<>();

        for (String fieldName : fieldNames) {
            DimensionSelector selector = makeDimensionSelector(columnFactory, fieldName);

            if (selector instanceof DimensionSelector.NullDimensionSelectorHolder) {
                throw new ValidationException("There is no column: " + fieldName);
            }

            selectors.add(selector);
        }

        return new ExactDistinctCountAggregator(selectors, Sets.newHashSet(), maxNumberOfValues, failOnLimitExceeded);
    }

    @Override
    @Nonnull
    public BufferAggregator factorizeBuffered(@Nonnull ColumnSelectorFactory columnFactory) {
        throw new UnsupportedOperationException("Not supported for groupBy and topN queries");
    }

    @Override
    @Nonnull
    public AggregatorFactory withName(@Nonnull String newName) {
        return new ExactDistinctCountAggregatorFactory(
                newName, getFieldNames(), maxNumberOfValues, failOnLimitExceeded);
    }

    private DimensionSelector makeDimensionSelector(final ColumnSelectorFactory columnFactory, String fieldName) {
        return columnFactory.makeDimensionSelector(DefaultDimensionSpec.of(fieldName));
    }

    @Override
    @Nullable
    public Comparator<?> getComparator() {
        return null;
    }

    @Override
    public Object combine(Object lhs, Object rhs) {
        Set<Object> combinedSet = Sets.newHashSet();
        if (lhs != null) {
            LOG.debug(lhs.toString());
            combinedSet.addAll((Collection<?>) lhs);
        }

        if (rhs != null) {
            LOG.debug(rhs.toString());
            combinedSet.addAll((Collection<?>) rhs);
        }
        return combinedSet;
    }

    @Override
    @Nonnull
    public AggregatorFactory getCombiningFactory() {
        return new ExactDistinctCountAggregatorFactory(name, fieldNames, maxNumberOfValues, failOnLimitExceeded);
    }

    @Override
    @Nonnull
    public AggregatorFactory getMergingFactory(AggregatorFactory other) throws AggregatorFactoryNotMergeableException {
        if (other.getName().equals(this.getName()) && this.getClass() == other.getClass()) {
            return getCombiningFactory();
        } else {
            throw new AggregatorFactoryNotMergeableException(this, other);
        }
    }

    @Override
    @Nonnull
    public List<AggregatorFactory> getRequiredColumns() {
        return ImmutableList.of(
                new ExactDistinctCountAggregatorFactory(name, fieldNames, maxNumberOfValues, failOnLimitExceeded));
    }

    @Override
    @Nonnull
    public Object deserialize(@Nonnull Object object) {
        return object;
    }

    @Nullable
    @Override
    public Object finalizeComputation(@Nullable Object object) {
        if (object instanceof Collection) {
            return ((Collection<?>) object).size();
        } else {
            return object;
        }
    }

    @JsonProperty
    public List<String> getFieldNames() {
        return ImmutableList.copyOf(fieldNames);
    }

    @JsonProperty
    public Integer getMaxNumberOfValues() {
        return maxNumberOfValues;
    }

    @JsonProperty
    public Boolean getFailOnLimitExceeded() {
        return failOnLimitExceeded;
    }

    @Override
    @Nonnull
    @JsonProperty
    public String getName() {
        return name;
    }

    @Override
    @Nonnull
    public List<String> requiredFields() {
        return this.getFieldNames();
    }

    @Override
    public byte[] getCacheKey() {
        byte[] fieldNameBytes = StringUtils.toUtf8(fieldNames.toString());
        byte[] bitMapFactoryCacheKey = StringUtils.toUtf8(this.getClass().getSimpleName());
        byte[] maxValuesBytes = StringUtils.toUtf8(maxNumberOfValues.toString());
        byte[] failOnLimitExceededBytes = StringUtils.toUtf8(failOnLimitExceeded.toString());
        return ByteBuffer.allocate(4
                        + fieldNameBytes.length
                        + bitMapFactoryCacheKey.length
                        + maxValuesBytes.length
                        + failOnLimitExceededBytes.length)
                .put(AggregatorUtil.DISTINCT_COUNT_CACHE_KEY)
                .put(fieldNameBytes)
                .put(AggregatorUtil.STRING_SEPARATOR)
                .put(bitMapFactoryCacheKey)
                .put(AggregatorUtil.STRING_SEPARATOR)
                .put(maxValuesBytes)
                .put(AggregatorUtil.STRING_SEPARATOR)
                .put(failOnLimitExceededBytes)
                .array();
    }

    @Override
    @Nonnull
    public ColumnType getIntermediateType() {
        return ColumnType.LONG;
    }

    @Override
    @Nonnull
    public ColumnType getResultType() {
        return ColumnType.LONG;
    }

    @Override
    public int getMaxIntermediateSize() {
        return (int) Math.ceil(maxNumberOfValues * 16 + (maxNumberOfValues / 0.75) * 8);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ExactDistinctCountAggregatorFactory that = (ExactDistinctCountAggregatorFactory) o;

        if (!fieldNames.equals(that.fieldNames)) {
            return false;
        }
        if (!maxNumberOfValues.equals(that.maxNumberOfValues)) {
            return false;
        }
        if (failOnLimitExceeded.booleanValue() != that.failOnLimitExceeded.booleanValue()) {
            return false;
        }
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldNames, maxNumberOfValues, failOnLimitExceeded);
    }

    @Override
    public String toString() {
        return "ExactDistinctCountAggregatorFactory{" + "name='"
                + name + '\'' + ", fieldNames='"
                + fieldNames + '\'' + ", maxNumberOfValues="
                + maxNumberOfValues + ", failOnLimitExceeded="
                + failOnLimitExceeded.toString() + '}';
    }
}
