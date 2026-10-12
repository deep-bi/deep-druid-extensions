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
package bi.deep.range;

import bi.deep.util.IPRangeUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import inet.ipaddr.IPAddress;
import inet.ipaddr.format.IPAddressRange;
import java.util.Objects;

public class IPRange {
    private final IPAddressRange addressRange;
    private final IPAddress.IPVersion ipVersion;

    @JsonCreator
    public IPRange(String range) {
        this.addressRange = IPRangeUtil.fromString(range);
        this.ipVersion = addressRange.getLower().getIPVersion();
    }

    public boolean contains(final IPAddress address) {
        return addressRange.contains(address);
    }

    public boolean isIPv4() {
        return this.ipVersion == IPAddress.IPVersion.IPV4;
    }

    public boolean isIPv6() {
        return this.ipVersion == IPAddress.IPVersion.IPV6;
    }

    public IPAddress getLower() {
        return addressRange.getLower();
    }

    public IPAddress getUpper() {
        return addressRange.getUpper();
    }

    public IPAddressRange getAddressRange() {
        return addressRange;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IPRange)) {
            return false;
        }

        final IPRange that = (IPRange) o;
        return Objects.equals(addressRange, that.addressRange) && ipVersion == that.ipVersion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(addressRange, ipVersion);
    }
}
