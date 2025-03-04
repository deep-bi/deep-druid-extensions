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

import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import org.apache.druid.query.aggregation.BufferAggregator;

public class NoopValueMatchBufferAggregator implements BufferAggregator {

    @Override
    public void init(ByteBuffer byteBuffer, int i) {
        // No-op
    }

    @Override
    public void aggregate(ByteBuffer byteBuffer, int i) {
        // No-op
    }

    @Nullable
    @Override
    public Object get(ByteBuffer byteBuffer, int i) {
        return null;
    }

    @Override
    public float getFloat(ByteBuffer byteBuffer, int i) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public long getLong(ByteBuffer byteBuffer, int i) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void close() {
        // No-op
    }
}
