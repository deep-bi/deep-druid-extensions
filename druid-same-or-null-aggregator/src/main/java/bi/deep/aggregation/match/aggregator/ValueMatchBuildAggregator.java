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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.apache.druid.query.aggregation.Aggregator;
import org.apache.druid.segment.ColumnValueSelector;

public class ValueMatchBuildAggregator implements Aggregator {
    private final ColumnValueSelector<?> selector;
    private final AtomicReference<Object> currentValue;
    private final AtomicBoolean isDifferent;

    public ValueMatchBuildAggregator(final ColumnValueSelector<?> selector) {
        this.selector = selector;
        this.currentValue = new AtomicReference<>();
        this.isDifferent = new AtomicBoolean(false);
    }

    @Override
    public void aggregate() {
        if (isDifferent.get()) {
            return;
        }

        final Object obj = selector.getObject();

        if (obj == null) {
            return;
        }

        currentValue.updateAndGet(value -> {
            if (value == null) {
                return obj; // initialize
            }

            if (!isDifferent.get() && !Objects.equals(value, obj)) {
                isDifferent.set(true);
            }

            return value; // Retain value
        });
    }

    @Nullable
    @Override
    public Object get() {
        return isDifferent.get() ? null : currentValue.get();
    }

    @Override
    public float getFloat() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public long getLong() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public double getDouble() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void close() {
        // No-op
    }
}
