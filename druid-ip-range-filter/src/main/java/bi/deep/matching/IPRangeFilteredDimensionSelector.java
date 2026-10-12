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
package bi.deep.matching;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.druid.java.util.common.logger.Logger;
import org.apache.druid.query.filter.DruidObjectPredicate;
import org.apache.druid.query.filter.DruidPredicateFactory;
import org.apache.druid.query.filter.ValueMatcher;
import org.apache.druid.query.monomorphicprocessing.RuntimeShapeInspector;
import org.apache.druid.segment.AbstractDimensionSelector;
import org.apache.druid.segment.DimensionSelector;
import org.apache.druid.segment.IdLookup;
import org.apache.druid.segment.data.IndexedInts;
import org.apache.druid.segment.data.ZeroIndexedInts;

public class IPRangeFilteredDimensionSelector extends AbstractDimensionSelector {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger log = new Logger(IPRangeFilteredDimensionSelector.class);

    protected final DimensionSelector baseSelector;
    private final IPRangeFilteredExtractionFn extractionFn;

    public IPRangeFilteredDimensionSelector(DimensionSelector baseSelector, IPRangeFilteredExtractionFn extractionFn) {
        this.baseSelector = baseSelector;
        this.extractionFn = extractionFn;
    }

    @Override
    @Nonnull
    public IndexedInts getRow() {
        return ZeroIndexedInts.instance();
    }

    @Override
    @Nonnull
    public ValueMatcher makeValueMatcher(@Nullable String value) {
        return new ValueMatcher() {
            @Override
            public boolean matches(boolean includeUnknown) {
                String extractionResult = getObject();

                if (extractionResult == null) {
                    return includeUnknown || value == null;
                }

                if (extractionResult.startsWith("[") && extractionResult.endsWith("]")) {
                    try {
                        List<String> array = OBJECT_MAPPER.readValue(extractionResult, new TypeReference<>() {});
                        return array.stream().anyMatch(x -> (includeUnknown && x == null) || Objects.equals(x, value));
                    } catch (IOException e) {
                        log.warn(e, "Failed to parse JSON array, returning false: %s", extractionResult);
                        return false;
                    }
                }

                return Objects.equals(extractionResult, value);
            }

            @Override
            public void inspectRuntimeShape(RuntimeShapeInspector inspector) {
                inspector.visit("selector", baseSelector);
            }
        };
    }

    @Override
    @Nonnull
    public ValueMatcher makeValueMatcher(@Nonnull DruidPredicateFactory predicateFactory) {
        return new ValueMatcher() {
            @Override
            public boolean matches(boolean includeUnknown) {
                final DruidObjectPredicate<String> predicate = predicateFactory.makeStringPredicate();
                String extractionResult = getObject();
                if (extractionResult == null) {
                    return predicate.apply(null).matches(includeUnknown);
                }
                if (extractionResult.startsWith("[") && extractionResult.endsWith("]")) {
                    try {
                        List<String> elements = OBJECT_MAPPER.readValue(extractionResult, new TypeReference<>() {});
                        return elements.stream()
                                .anyMatch(x -> predicate.apply(x).matches(includeUnknown));
                    } catch (IOException e) {
                        log.warn(e, "Failed to parse JSON array, returning false: %s", extractionResult);
                        return false;
                    }
                }

                return predicate.apply(extractionResult).matches(includeUnknown);
            }

            @Override
            public void inspectRuntimeShape(RuntimeShapeInspector inspector) {
                inspector.visit("selector", baseSelector);
                inspector.visit("predicate", predicateFactory);
            }
        };
    }

    @Override
    public void inspectRuntimeShape(RuntimeShapeInspector inspector) {
        baseSelector.inspectRuntimeShape(inspector);
    }

    @Nullable
    @Override
    public String getObject() {
        return extractionFn.apply(Objects.toString(baseSelector.getObject(), ""));
    }

    @Override
    @Nonnull
    public Class<?> classOfObject() {
        return Object.class;
    }

    @Override
    public int getValueCardinality() {
        return CARDINALITY_UNKNOWN;
    }

    @Nullable
    @Override
    public String lookupName(int id) {
        return getObject();
    }

    @Override
    public boolean nameLookupPossibleInAdvance() {
        return false;
    }

    @Nullable
    @Override
    public IdLookup idLookup() {
        return null;
    }
}
