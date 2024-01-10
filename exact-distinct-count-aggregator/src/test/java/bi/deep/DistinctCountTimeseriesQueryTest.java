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
package bi.deep;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.druid.data.input.MapBasedInputRow;
import org.apache.druid.java.util.common.DateTimes;
import org.apache.druid.java.util.common.granularity.Granularities;
import org.apache.druid.query.Druids;
import org.apache.druid.query.QueryRunnerTestHelper;
import org.apache.druid.query.Result;
import org.apache.druid.query.aggregation.CountAggregatorFactory;
import org.apache.druid.query.timeseries.DefaultTimeseriesQueryMetrics;
import org.apache.druid.query.timeseries.TimeseriesQuery;
import org.apache.druid.query.timeseries.TimeseriesQueryEngine;
import org.apache.druid.query.timeseries.TimeseriesResultValue;
import org.apache.druid.segment.TestHelper;
import org.apache.druid.segment.incremental.*;
import org.apache.druid.testing.InitializedNullHandlingTest;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DistinctCountTimeseriesQueryTest extends InitializedNullHandlingTest {
    private static final String VISITOR_ID = "visitor_id";
    private static final String CLIENT_TYPE = "client_type";
    private static final DateTime DATE_TIME = DateTimes.of("2016-03-04T00:00:00.000Z");

    private TimeseriesQueryEngine engine;
    private IncrementalIndex index;

    @Before
    public void setup() throws IndexSizeExceededException {
        engine = new TimeseriesQueryEngine();
        index = new OnheapIncrementalIndex.Builder()
                .setIndexSchema(new IncrementalIndexSchema.Builder()
                        .withQueryGranularity(Granularities.SECOND)
                        .withMetrics(new CountAggregatorFactory("cnt"))
                        .build())
                .setMaxRowCount(1000)
                .build();
        long timestamp = DATE_TIME.getMillis();
        index.add(new MapBasedInputRow(
                timestamp,
                Lists.newArrayList(VISITOR_ID, CLIENT_TYPE),
                ImmutableMap.of(VISITOR_ID, "0", CLIENT_TYPE, "iphone")));
        index.add(new MapBasedInputRow(
                timestamp,
                Lists.newArrayList(VISITOR_ID, CLIENT_TYPE),
                ImmutableMap.of(VISITOR_ID, "1", CLIENT_TYPE, "iphone")));
        index.add(new MapBasedInputRow(
                timestamp,
                Lists.newArrayList(VISITOR_ID, CLIENT_TYPE),
                ImmutableMap.of(VISITOR_ID, "2", CLIENT_TYPE, "android")));
    }

    @Test
    public void testFailQuery() {
        TimeseriesQuery queryToFail = Druids.newTimeseriesQueryBuilder()
                .dataSource(QueryRunnerTestHelper.DATA_SOURCE)
                .granularity(QueryRunnerTestHelper.ALL_GRAN)
                .intervals(QueryRunnerTestHelper.FULL_ON_INTERVAL_SPEC)
                .aggregators(Lists.newArrayList(
                        QueryRunnerTestHelper.ROWS_COUNT,
                        new ExactDistinctCountAggregatorFactory("UV", ImmutableList.of(VISITOR_ID), 2, true)))
                .build();

        Assert.assertThrows(RuntimeException.class, () -> engine.process(
                        queryToFail, new IncrementalIndexStorageAdapter(index), new DefaultTimeseriesQueryMetrics())
                .toList());
    }

    @Test
    public void testPartialQuery() {
        TimeseriesQuery partialQuery = Druids.newTimeseriesQueryBuilder()
                .dataSource(QueryRunnerTestHelper.DATA_SOURCE)
                .granularity(QueryRunnerTestHelper.ALL_GRAN)
                .intervals(QueryRunnerTestHelper.FULL_ON_INTERVAL_SPEC)
                .aggregators(Lists.newArrayList(
                        QueryRunnerTestHelper.ROWS_COUNT,
                        new ExactDistinctCountAggregatorFactory("UV", ImmutableList.of(VISITOR_ID), 2, false)))
                .build();

        final Iterable<Result<TimeseriesResultValue>> results = engine.process(
                        partialQuery, new IncrementalIndexStorageAdapter(index), new DefaultTimeseriesQueryMetrics())
                .toList();
        Set<Integer> set = ImmutableSet.of(
                ImmutableList.of("0").hashCode(), ImmutableList.of("1").hashCode());

        List<Result<TimeseriesResultValue>> expectedResults = Collections.singletonList(
                new Result<>(DATE_TIME, new TimeseriesResultValue(ImmutableMap.of("UV", set, "rows", 3L))));
        TestHelper.assertExpectedResults(expectedResults, results);
    }

    @Test
    public void testFullQuery() {
        TimeseriesQuery fullQuery = Druids.newTimeseriesQueryBuilder()
                .dataSource(QueryRunnerTestHelper.DATA_SOURCE)
                .granularity(QueryRunnerTestHelper.ALL_GRAN)
                .intervals(QueryRunnerTestHelper.FULL_ON_INTERVAL_SPEC)
                .aggregators(Lists.newArrayList(
                        QueryRunnerTestHelper.ROWS_COUNT,
                        new ExactDistinctCountAggregatorFactory("UV", ImmutableList.of(VISITOR_ID), 3, true)))
                .build();

        final Iterable<Result<TimeseriesResultValue>> fullResults = engine.process(
                        fullQuery, new IncrementalIndexStorageAdapter(index), new DefaultTimeseriesQueryMetrics())
                .toList();

        Set<Integer> set = ImmutableSet.of(
                ImmutableList.of("0").hashCode(),
                ImmutableList.of("1").hashCode(),
                ImmutableList.of("2").hashCode());

        List<Result<TimeseriesResultValue>> fullExpectedResults = Collections.singletonList(
                new Result<>(DATE_TIME, new TimeseriesResultValue(ImmutableMap.of("UV", set, "rows", 3L))));
        TestHelper.assertExpectedResults(fullExpectedResults, fullResults);
    }

    @Test
    public void testMultiDimensionQuery() {
        TimeseriesQuery multiDimensionQuery = Druids.newTimeseriesQueryBuilder()
                .dataSource(QueryRunnerTestHelper.DATA_SOURCE)
                .granularity(QueryRunnerTestHelper.ALL_GRAN)
                .intervals(QueryRunnerTestHelper.FULL_ON_INTERVAL_SPEC)
                .aggregators(Lists.newArrayList(
                        QueryRunnerTestHelper.ROWS_COUNT,
                        new ExactDistinctCountAggregatorFactory(
                                "UV", ImmutableList.of(VISITOR_ID, CLIENT_TYPE), 4, true)))
                .build();

        final Iterable<Result<TimeseriesResultValue>> multiDimensionResults = engine.process(
                        multiDimensionQuery,
                        new IncrementalIndexStorageAdapter(index),
                        new DefaultTimeseriesQueryMetrics())
                .toList();

        Set<Integer> set = ImmutableSet.of(
                ImmutableList.of("0", "iphone").hashCode(),
                ImmutableList.of("1", "iphone").hashCode(),
                ImmutableList.of("2", "android").hashCode());

        List<Result<TimeseriesResultValue>> multiDimensionExpectedResults = Collections.singletonList(
                new Result<>(DATE_TIME, new TimeseriesResultValue(ImmutableMap.of("UV", set, "rows", 3L))));

        TestHelper.assertExpectedResults(multiDimensionResults, multiDimensionExpectedResults);
    }

    @Test
    public void testMultiDimensionWithDuplicateRows() throws IndexSizeExceededException {
        index.add(new MapBasedInputRow(
                DATE_TIME.getMillis(),
                Lists.newArrayList(VISITOR_ID, CLIENT_TYPE),
                ImmutableMap.of(VISITOR_ID, "1", CLIENT_TYPE, "iphone")));
        index.add(new MapBasedInputRow(
                DATE_TIME.getMillis(),
                Lists.newArrayList(VISITOR_ID, CLIENT_TYPE),
                ImmutableMap.of(VISITOR_ID, "3", CLIENT_TYPE, "blackberry")));
        index.add(new MapBasedInputRow(
                DATE_TIME.getMillis(),
                Lists.newArrayList(VISITOR_ID, CLIENT_TYPE),
                ImmutableMap.of(VISITOR_ID, "3", CLIENT_TYPE, "blackberry")));
        TimeseriesQuery multiDimensionQuery = Druids.newTimeseriesQueryBuilder()
                .dataSource(QueryRunnerTestHelper.DATA_SOURCE)
                .granularity(QueryRunnerTestHelper.ALL_GRAN)
                .intervals(QueryRunnerTestHelper.FULL_ON_INTERVAL_SPEC)
                .aggregators(Lists.newArrayList(
                        QueryRunnerTestHelper.ROWS_COUNT,
                        new ExactDistinctCountAggregatorFactory(
                                "UV", ImmutableList.of(VISITOR_ID, CLIENT_TYPE), 4, true)))
                .build();

        final Iterable<Result<TimeseriesResultValue>> multiDimensionResults = engine.process(
                        multiDimensionQuery,
                        new IncrementalIndexStorageAdapter(index),
                        new DefaultTimeseriesQueryMetrics())
                .toList();

        Set<Integer> set = ImmutableSet.of(
                ImmutableList.of("0", "iphone").hashCode(),
                ImmutableList.of("1", "iphone").hashCode(),
                ImmutableList.of("2", "android").hashCode(),
                ImmutableList.of("3", "blackberry").hashCode());

        List<Result<TimeseriesResultValue>> multiDimensionExpectedResults = Collections.singletonList(
                new Result<>(DATE_TIME, new TimeseriesResultValue(ImmutableMap.of("UV", set, "rows", 4L))));

        TestHelper.assertExpectedResults(multiDimensionResults, multiDimensionExpectedResults);
    }
}
