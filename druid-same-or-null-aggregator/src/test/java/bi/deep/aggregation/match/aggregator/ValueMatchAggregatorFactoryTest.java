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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.apache.druid.java.util.common.granularity.Granularities;
import org.apache.druid.query.Druids;
import org.apache.druid.query.aggregation.AggregateCombiner;
import org.apache.druid.query.aggregation.Aggregator;
import org.apache.druid.query.aggregation.CountAggregatorFactory;
import org.apache.druid.query.aggregation.TestDoubleColumnSelectorImpl;
import org.apache.druid.query.aggregation.TestObjectColumnSelector;
import org.apache.druid.query.timeseries.TimeseriesQuery;
import org.apache.druid.query.timeseries.TimeseriesQueryQueryToolChest;
import org.apache.druid.segment.column.ColumnType;
import org.apache.druid.segment.column.RowSignature;
import org.junit.jupiter.api.Test;

class ValueMatchAggregatorFactoryTest {

    @Test
    void testEquals() {
        EqualsVerifier.forClass(ValueMatchAggregatorFactory.class)
                .withNonnullFields("name", "fieldName", "maxSize", "outputType")
                .usingGetClass()
                .verify();
    }

    @Test
    void testGuessAggregatorHeapFootprint() {
        ValueMatchAggregatorFactory factory =
                new ValueMatchAggregatorFactory("myFactory", "myField", 512, ColumnType.LONG);

        assertEquals(512, factory.guessAggregatorHeapFootprint(1));
        assertEquals(512, factory.guessAggregatorHeapFootprint(100));
        assertEquals(512, factory.guessAggregatorHeapFootprint(1000));
        assertEquals(512, factory.guessAggregatorHeapFootprint(1_000_000_000_000L));
    }

    @Test
    void testAggregatorDefault() {
        ValueMatchAggregatorFactory factory = new ValueMatchAggregatorFactory(null, "myField", null, null);

        assertEquals("myField", factory.getName());
        assertEquals(ColumnType.UNKNOWN_COMPLEX, factory.getOutputType());
        assertEquals(1024, factory.guessAggregatorHeapFootprint(1_000_000_000_000L));
    }

    @Test
    void testMaxIntermediateSize() {
        ValueMatchAggregatorFactory factory =
                new ValueMatchAggregatorFactory("myFactory", "myField", 512, ColumnType.LONG);
        assertEquals(512, factory.getMaxIntermediateSize());
    }

    @Test
    void testResultArraySignature() {
        final TimeseriesQuery query = Druids.newTimeseriesQueryBuilder()
                .dataSource("dummy")
                .intervals("2000/3000")
                .granularity(Granularities.HOUR)
                .aggregators(
                        new CountAggregatorFactory("count"),
                        new ValueMatchAggregatorFactory("myFactory", "myField", 512, ColumnType.STRING))
                .build();

        assertEquals(
                RowSignature.builder()
                        .addTimeColumn()
                        .add("count", ColumnType.LONG)
                        .add("myFactory", ColumnType.STRING)
                        .build(),
                new TimeseriesQueryQueryToolChest().resultArraySignature(query));
    }

    @Test
    void testNullReservoir() {
        final ValueMatchAggregatorFactory factory =
                new ValueMatchAggregatorFactory("myFactory", "myField", 512, ColumnType.FLOAT);
        final double[] values = new double[] {1, 2, 3, 4, 5, 6};
        final TestDoubleColumnSelectorImpl selector = new TestDoubleColumnSelectorImpl(values);

        try (final Aggregator agg1 = new ValueMatchBuildAggregator(selector)) {
            assertNull(factory.combine(null, agg1.get()));
            assertNull(factory.combine(agg1.get(), null));

            AggregateCombiner<?> ac = factory.makeAggregateCombiner();
            ac.fold(new TestObjectColumnSelector<>(new Object[] {10}));
            assertNotNull(ac.getObject());
        }
    }
}
