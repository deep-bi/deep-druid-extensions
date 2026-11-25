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

import bi.deep.util.CacheKeyUtil;
import bi.deep.util.IPRangeUtil;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import inet.ipaddr.IPAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.druid.query.cache.CacheKeyBuilder;
import org.apache.druid.query.extraction.DimExtractionFn;

@JsonTypeName("ip-range-extraction-fn")
public class IPRangeFilteredExtractionFn extends DimExtractionFn {

    public static final byte CACHE_TYPE_ID_IP_RANGE_FN = 0xF;

    private final List<IPAddress> ips;
    private final Supplier<byte[]> ipsDigest;

    public IPRangeFilteredExtractionFn(@Nullable List<IPAddress> ips) {
        // null means "no match" in apply(); the snapshot keeps the memoised digest in step with matching
        this.ips = ips == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(ips));
        this.ipsDigest = Suppliers.memoize(() -> digest(this.ips));
    }

    List<IPAddress> getIps() {
        return ips;
    }

    byte[] getIpsDigest() {
        return ipsDigest.get();
    }

    // order-preserving because apply() lists matches in the order of ips
    private static byte[] digest(List<IPAddress> ips) {
        return CacheKeyUtil.hashStrings(
                ips.stream().map(ip -> ip == null ? null : ip.toString()).collect(Collectors.toList()));
    }

    @Nullable
    @Override
    public String apply(@Nullable String value) {
        return value == null ? null : IPRangeUtil.getMatchingIPs(value, ips);
    }

    @Override
    public boolean preservesOrdering() {
        return false;
    }

    @Override
    public ExtractionType getExtractionType() {
        return ExtractionType.MANY_TO_ONE;
    }

    @Override
    public byte[] getCacheKey() {
        return new CacheKeyBuilder(CACHE_TYPE_ID_IP_RANGE_FN)
                .appendByteArray(ipsDigest.get())
                .build();
    }
}
