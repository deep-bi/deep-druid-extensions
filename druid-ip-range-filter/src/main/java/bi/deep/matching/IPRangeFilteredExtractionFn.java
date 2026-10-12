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

import bi.deep.util.IPRangeUtil;
import com.fasterxml.jackson.annotation.JsonTypeName;
import inet.ipaddr.IPAddress;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.druid.query.extraction.DimExtractionFn;

@JsonTypeName("ip-range-extraction-fn")
public class IPRangeFilteredExtractionFn extends DimExtractionFn {

    public static final byte CACHE_TYPE_ID_IP_RANGE_FN = 0xF;

    private final List<IPAddress> ips;

    public IPRangeFilteredExtractionFn(List<IPAddress> ips) {
        this.ips = ips;
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
        return new byte[] {CACHE_TYPE_ID_IP_RANGE_FN};
    }
}
