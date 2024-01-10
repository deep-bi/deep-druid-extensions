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

import java.util.List;
import java.util.Set;
import org.apache.druid.query.aggregation.Aggregator;
import org.apache.druid.segment.DimensionSelector;
import org.slf4j.LoggerFactory;

public class ExactDistinctCountAggregator implements Aggregator {
    private final List<DimensionSelector> selectors;
    private final HashcodeRegistry hashcodeRegistry;
    private final Integer maxNumberOfValues;
    private final boolean failOnLimitExceeded;
    private boolean achievedLimit;

    public ExactDistinctCountAggregator(
            List<DimensionSelector> selectors,
            Set<Integer> set,
            Integer maxNumberOfValues,
            boolean failOnLimitExceeded) {
        this.selectors = selectors;
        this.maxNumberOfValues = maxNumberOfValues;
        this.failOnLimitExceeded = failOnLimitExceeded;
        this.hashcodeRegistry = new HashcodeRegistry(set);
    }

    @Override
    public void aggregate() {
        if (achievedLimit) {
            return;
        }

        if (hashcodeRegistry.size() >= maxNumberOfValues) {
            if (hashcodeRegistry.contains(selectors)) {
                return;
            }
            if (failOnLimitExceeded) {
                throw new RuntimeException("Reached max number of values: " + maxNumberOfValues);
            } else {
                achievedLimit = true;
                LoggerFactory.getLogger(this.getClass()).warn("Reached max number of values, result is limited");
                return;
            }
        }

        hashcodeRegistry.add(selectors);
    }

    @Override
    public Object get() {
        return hashcodeRegistry.getRegistry();
    }

    @Override
    public void close() {}

    @Override
    public float getFloat() {
        throw new UnsupportedOperationException("ExactDistinctCountAggregator does not support getFloat()");
    }

    @Override
    public long getLong() {
        throw new UnsupportedOperationException("ExactDistinctCountAggregator does not support getLong()");
    }

    @Override
    public double getDouble() {
        throw new UnsupportedOperationException("ExactDistinctCountAggregator does not support getDouble()");
    }
}
