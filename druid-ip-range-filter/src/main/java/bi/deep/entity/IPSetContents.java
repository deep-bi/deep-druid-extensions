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
package bi.deep.entity;

import bi.deep.range.IPBoundedRange;
import inet.ipaddr.IPAddress;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.collections4.CollectionUtils;

public class IPSetContents {

    @Nullable
    private final List<IPAddress> ipAddresses;

    @Nullable
    private final List<IPBoundedRange> ranges;

    public IPSetContents(@Nullable List<IPAddress> ipAddresses, @Nullable List<IPBoundedRange> ranges) {
        this.ipAddresses = ipAddresses;
        this.ranges = ranges;
    }

    @Nullable
    public List<IPBoundedRange> getRanges() {
        return ranges;
    }

    public boolean contains(IPAddress address, boolean ignoreVersionMismatch) {
        return (CollectionUtils.isNotEmpty(ipAddresses) && ipAddresses.contains(address))
                || (CollectionUtils.isNotEmpty(ranges)
                        && ranges.stream().anyMatch(r -> r.contains(address, ignoreVersionMismatch)));
    }

    public boolean containsAnyIP(List<IPAddress> candidates, boolean ignoreVersionMismatch) {
        for (IPAddress ip : candidates) {
            if (ipAddresses != null && !ipAddresses.isEmpty() && ipAddresses.contains(ip)) {
                return true;
            }
        }
        return ranges != null && ranges.stream().anyMatch(r -> r.containsAnyIP(candidates, ignoreVersionMismatch));
    }

    public boolean isEmpty() {
        return CollectionUtils.isEmpty(ranges) && CollectionUtils.isEmpty(ipAddresses);
    }
}
