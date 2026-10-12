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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressSeqRange;
import inet.ipaddr.IPAddressString;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.apache.druid.java.util.common.IAE;

public class IPBoundedRange {
    private final boolean lowerOpen;
    private final boolean upperOpen;

    private final IPAddress lowerIPAddress;
    private final IPAddress upperIPAddress;
    private final IPAddress.IPVersion ipVersion;

    @JsonCreator
    public IPBoundedRange(
            @JsonProperty("lower") @Nullable String lower,
            @JsonProperty("upper") @Nullable String upper,
            @JsonProperty("lowerOpen") boolean lowerOpen,
            @JsonProperty("upperOpen") boolean upperOpen) {
        this(
                lower != null ? new IPAddressString(lower).getAddress() : null,
                upper != null ? new IPAddressString(upper).getAddress() : null,
                lowerOpen,
                upperOpen);
    }

    public IPBoundedRange(
            @Nullable IPAddress lowerIPAddress,
            @Nullable IPAddress upperIPAddress,
            boolean lowerOpen,
            boolean upperOpen) {

        if (lowerIPAddress == null && upperIPAddress == null) {
            throw new IAE("At least one of the valid lower or upper bounds must be provided");
        }
        this.lowerIPAddress = lowerIPAddress;
        this.upperIPAddress = upperIPAddress;

        if (this.lowerIPAddress != null
                && this.upperIPAddress != null
                && this.lowerIPAddress.getIPVersion() != this.upperIPAddress.getIPVersion()) {
            throw new IAE("Both lower and upper bounds must be of the same IP type (IPv4 or IPv6)");
        }

        this.lowerOpen = lowerOpen;
        this.upperOpen = upperOpen;
        this.ipVersion =
                this.lowerIPAddress != null ? this.lowerIPAddress.getIPVersion() : this.upperIPAddress.getIPVersion();
    }

    public IPBoundedRange(IPAddressSeqRange range, boolean lowerOpen, boolean upperOpen) {
        this(range.getLower(), range.getUpper(), lowerOpen, upperOpen);
    }

    @JsonProperty("lower")
    public String getLower() {
        return lowerIPAddress.toCanonicalString();
    }

    @JsonProperty("upper")
    public String getUpper() {
        return upperIPAddress.toCanonicalString();
    }

    @JsonProperty
    public boolean isLowerOpen() {
        return lowerOpen;
    }

    @JsonProperty
    public boolean isUpperOpen() {
        return upperOpen;
    }

    public IPAddress getLowerIPAddress() {
        return lowerIPAddress;
    }

    public IPAddress getUpperIPAddress() {
        return upperIPAddress;
    }

    private boolean matchUpperBound(IPAddress ipValue) {
        return upperIPAddress == null || upperOpen
                ? ipValue.compareTo(upperIPAddress) < 0
                : ipValue.compareTo(upperIPAddress) <= 0;
    }

    private boolean matchLowerBound(IPAddress ipValue) {
        return lowerIPAddress == null || lowerOpen
                ? ipValue.compareTo(lowerIPAddress) > 0
                : ipValue.compareTo(lowerIPAddress) >= 0;
    }

    @VisibleForTesting
    public boolean contains(IPAddress ipAddress, boolean ignoreVersionMismatch) {
        return ipAddress == null || ipAddress.getIPVersion() != this.ipVersion
                ? ignoreVersionMismatch
                : matchLowerBound(ipAddress) && matchUpperBound(ipAddress);
    }

    @VisibleForTesting
    public boolean containsAnyIP(List<IPAddress> ipAddress, boolean ignoreVersionMismatch) {
        return ipAddress.stream().anyMatch(address -> contains(address, ignoreVersionMismatch));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IPBoundedRange)) {
            return false;
        }
        final IPBoundedRange range = (IPBoundedRange) o;
        return lowerOpen == range.lowerOpen
                && upperOpen == range.upperOpen
                && Objects.equals(lowerIPAddress, range.lowerIPAddress)
                && Objects.equals(upperIPAddress, range.upperIPAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowerIPAddress, upperIPAddress, lowerOpen, upperOpen);
    }
}
