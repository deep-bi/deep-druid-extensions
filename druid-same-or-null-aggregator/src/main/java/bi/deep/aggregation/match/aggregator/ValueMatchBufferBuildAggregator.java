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

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.annotation.Nullable;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.druid.query.aggregation.BufferAggregator;
import org.apache.druid.segment.ColumnValueSelector;
import org.apache.druid.segment.nested.StructuredData;

public class ValueMatchBufferBuildAggregator implements BufferAggregator {
    private static final int NIL = -1;
    private static final byte TRUE = 1;
    private static final byte FALSE = 0;

    private static final int FLAG_OFFSET = 0; // 1 byte for isDifferent flag
    private static final int LENGTH_OFFSET = 1; // 4 bytes for object byte length
    private static final int VALUE_OFFSET = 5; // Start of object bytes

    private final ColumnValueSelector<?> selector;

    public ValueMatchBufferBuildAggregator(final ColumnValueSelector<?> selector) {
        this.selector = selector;
    }

    @Override
    public void init(ByteBuffer buffer, int position) {
        buffer.put(position + FLAG_OFFSET, FALSE); // isDifferent = false
        buffer.putInt(position + LENGTH_OFFSET, NIL); // No object stored
    }

    @Override
    public void aggregate(ByteBuffer buf, int position) {
        byte isDifferent = buf.get(position + FLAG_OFFSET);

        if (isDifferent == TRUE) {
            return; // Already different, exit early
        }

        Object obj = selector.getObject();

        if (obj == null) {
            return; // Ignore nulls objects
        }

        if (obj instanceof StructuredData) {
            obj = ((StructuredData) obj).getValue();
        }

        if (!(obj instanceof Serializable)) {
            return; // Ignore non-serializable objects
        }

        byte[] objBytes = SerializationUtils.serialize((Serializable) obj); // Serialize object
        int storedLength = buf.getInt(position + LENGTH_OFFSET);

        if (storedLength == NIL) {
            // First value, store it
            buf.putInt(position + LENGTH_OFFSET, objBytes.length);
            buf.position(position + VALUE_OFFSET);
            buf.put(objBytes);
        } else {
            // Compare stored bytes with new value
            byte[] storedBytes = new byte[storedLength];
            buf.position(position + VALUE_OFFSET);
            buf.get(storedBytes);

            if (!Arrays.equals(storedBytes, objBytes)) {
                buf.put(position + FLAG_OFFSET, TRUE); // Different value found, set flag
            }
        }
    }

    @Override
    public void relocate(int oldPosition, int newPosition, ByteBuffer oldBuffer, ByteBuffer newBuffer) {
        // Copy isDifferent flag (1 byte)
        byte isDifferent = oldBuffer.get(oldPosition + FLAG_OFFSET);
        newBuffer.put(newPosition + FLAG_OFFSET, isDifferent);

        // Copy length (4 bytes)
        int length = oldBuffer.getInt(oldPosition + LENGTH_OFFSET);
        newBuffer.putInt(newPosition + LENGTH_OFFSET, length);

        if (length != NIL) {
            // Copy actual serialized object bytes
            byte[] objectBytes = new byte[length];
            oldBuffer.position(oldPosition + VALUE_OFFSET);
            oldBuffer.get(objectBytes);

            newBuffer.position(newPosition + VALUE_OFFSET);
            newBuffer.put(objectBytes);
        }
    }

    @Nullable
    @Override
    public Object get(ByteBuffer buf, int position) {
        if (buf.get(position + FLAG_OFFSET) == TRUE) {
            return null; // Different values found, return null
        }

        int length = buf.getInt(position + LENGTH_OFFSET);
        if (length == NIL) {
            return null; // No object stored
        }

        byte[] bytes = new byte[length];
        buf.position(position + VALUE_OFFSET);
        buf.get(bytes);

        return SerializationUtils.deserialize(bytes);
    }

    @Override
    public float getFloat(final ByteBuffer buffer, final int position) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public long getLong(final ByteBuffer buffer, final int position) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void close() {
        // NO-OP
    }
}
