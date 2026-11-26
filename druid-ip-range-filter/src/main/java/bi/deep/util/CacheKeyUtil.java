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
package bi.deep.util;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

public final class CacheKeyUtil {
    private CacheKeyUtil() {}

    /**
     * Fixed-size digest of a string collection for use in cache keys. Filters can carry millions of values,
     * so embedding them verbatim would make every per-segment cache key as large as the query itself.
     * Values are sorted so the digest does not depend on set iteration order, and each one is length-prefixed
     * so that {"a,b"} and {"a", "b"} hash differently. Mirrors how Druid's InDimFilter builds its cache key.
     */
    public static byte[] hashStringsIgnoringOrder(Collection<String> values) {
        return hash(
                values.size(),
                values.stream()
                        .sorted(Comparator.nullsFirst(Comparator.naturalOrder()))
                        .iterator());
    }

    /**
     * Like {@link #hashStringsIgnoringOrder} but keeps iteration order, for callers whose output depends on it.
     */
    public static byte[] hashStrings(Collection<String> values) {
        return hash(values.size(), values.iterator());
    }

    private static byte[] hash(int size, Iterator<String> values) {
        final Hasher hasher = Hashing.sha256().newHasher();
        hasher.putInt(size);

        while (values.hasNext()) {
            final String value = values.next();
            if (value == null) {
                hasher.putInt(-1);
            } else {
                hasher.putInt(value.length());
                hasher.putString(value, StandardCharsets.UTF_8);
            }
        }

        return hasher.hash().asBytes();
    }
}
