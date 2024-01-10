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

import com.google.common.collect.Sets;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.druid.query.aggregation.BufferAggregator;
import org.apache.druid.query.monomorphicprocessing.RuntimeShapeInspector;
import org.apache.druid.segment.DimensionSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExactDistinctCountBufferAggregator implements BufferAggregator {
    private static final Logger LOG = LoggerFactory.getLogger(ExactDistinctCountBufferAggregator.class);
    private final List<DimensionSelector> selectors;
    private final Integer maxNumberOfValues;
    private final boolean failOnLimitExceeded;
    private boolean achievedLimit;

    public ExactDistinctCountBufferAggregator(
            List<DimensionSelector> selectors, Integer maxNumberOfValues, boolean failOnLimitExceeded) {
        LOG.debug("buf constructor");
        this.selectors = selectors;
        this.maxNumberOfValues = maxNumberOfValues;
        this.failOnLimitExceeded = failOnLimitExceeded;
    }

    @Override
    public void init(ByteBuffer byteBuffer, int i) {
        LOG.debug("buf init position " + i);
        LOG.debug(selectors.getClass().getSimpleName());
        HashSet<Integer> mutableSet = Sets.newHashSet();
        byte[] byteValue = SerializationUtils.serialize(mutableSet);
        byteBuffer.position(i);
        byteBuffer.putInt(byteValue.length);
        byteBuffer.put(byteValue);
    }

    @Override
    public void aggregate(@Nonnull ByteBuffer byteBuffer, int position) {
        if (achievedLimit) {
            return;
        }

        HashcodeRegistry hashcodeRegistry = new HashcodeRegistry(getMutableSet(byteBuffer, position));

        if (hashcodeRegistry.size() >= maxNumberOfValues) {
            if (hashcodeRegistry.contains(selectors)) {
                return;
            }
            if (failOnLimitExceeded) {
                throw new RuntimeException("Reached max number of values: " + maxNumberOfValues);
            } else {
                achievedLimit = true;
                LOG.warn("Reached max number of values, result is limited");
                return;
            }
        }

        hashcodeRegistry.add(selectors);

        byte[] byteValue = SerializationUtils.serialize(hashcodeRegistry.getRegistry());
        byteBuffer.position(position);
        byteBuffer.putInt(byteValue.length);
        byteBuffer.put(byteValue);
    }

    private HashSet<Integer> getMutableSet(final ByteBuffer buffer, final int position) {
        buffer.position(position);
        final int size = buffer.getInt();
        final byte[] bytes = new byte[size];
        buffer.get(bytes);
        return SerializationUtils.deserialize(bytes);
    }

    @Nullable
    @Override
    public Object get(@Nonnull ByteBuffer byteBuffer, int i) {
        HashSet<Integer> mutableSet = getMutableSet(byteBuffer, i);
        LOG.debug("Returning " + mutableSet.toString() + "with size " + mutableSet.size());
        return mutableSet;
    }

    @Override
    public float getFloat(@Nonnull ByteBuffer byteBuffer, int i) {
        throw new UnsupportedOperationException("ExactDistinctCountBufferAggregator does not support getFloat()");
    }

    @Override
    public long getLong(@Nonnull ByteBuffer byteBuffer, int i) {
        throw new UnsupportedOperationException("ExactDistinctCountBufferAggregator does not support getLong()");
    }

    @Override
    public void close() {}

    @Override
    public void inspectRuntimeShape(RuntimeShapeInspector inspector) {
        inspector.visit("selector", selectors);
    }
}
