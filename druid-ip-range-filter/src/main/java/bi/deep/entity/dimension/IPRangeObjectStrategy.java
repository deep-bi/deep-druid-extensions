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
package bi.deep.entity.dimension;

import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import org.apache.druid.java.util.common.IAE;
import org.apache.druid.segment.data.ObjectStrategy;

public class IPRangeObjectStrategy implements ObjectStrategy<IPRange> {
    public static final ObjectStrategy<IPRange> INSTANCE = new IPRangeObjectStrategy();
    private static final byte[] EMPTY_BYTES = new byte[] {};

    @Override
    public int compare(final IPRange s1, final IPRange s2) {
        return IPRange.COMPARATOR.compare(s1, s2);
    }

    @Override
    public IPRange fromByteBuffer(final ByteBuffer buffer, final int numBytes) {
        if (numBytes == 0) {
            return IPRange.EMPTY;
        }

        final byte[] data = new byte[numBytes];
        buffer.get(data);

        try {
            return IPRange.from(data);
        } catch (Exception e) {
            throw new IAE("Unable to read from byte buffer", e);
        }
    }

    @Override
    public Class<IPRange> getClazz() {
        return IPRange.class;
    }

    @Override
    public byte[] toBytes(@Nullable IPRange range) {
        if (range == null || range == IPRange.EMPTY) {
            return EMPTY_BYTES;
        }

        return range.toBytes();
    }
}
