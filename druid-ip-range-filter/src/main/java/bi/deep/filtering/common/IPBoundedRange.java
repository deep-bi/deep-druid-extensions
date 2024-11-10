/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package bi.deep.filtering.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.util.Objects;
import javax.annotation.Nullable;
import org.apache.druid.error.InvalidInput;
import org.apache.druid.java.util.common.IAE;

public class IPBoundedRange {
    private final String lower;
    private final String upper;
    private final boolean lowerOpen;
    private final boolean upperOpen;

    private final IPAddress lowerIPAddress;
    private final IPAddress upperIPAddress;
    private final IPAddress.IPVersion ipVersion;

    @JsonCreator
    public IPBoundedRange(
            @JsonProperty("lower") @Nullable String lower,
            @JsonProperty("upper") @Nullable String upper,
            @JsonProperty("lowerOpen") @Nullable Boolean lowerOpen,
            @JsonProperty("upperOpen") @Nullable Boolean upperOpen) {

        if (lower == null && upper == null) {
            throw InvalidInput.exception("Invalid input, lower and upper cannot be null at the same time");
        }

        this.lower = lower;
        this.upper = upper;
        this.lowerIPAddress = lower != null ? new IPAddressString(lower).getAddress() : null;
        this.upperIPAddress = upper != null ? new IPAddressString(upper).getAddress() : null;
        this.lowerOpen = lowerOpen != null && lowerOpen;
        this.upperOpen = upperOpen != null && upperOpen;

        if (this.lowerIPAddress != null
                && this.upperIPAddress != null
                && this.lowerIPAddress.getIPVersion() != this.upperIPAddress.getIPVersion()) {
            throw new IAE("Both lower and upper bounds must be of the same IP type (IPv4 or IPv6)");
        }

        this.ipVersion =
                this.lowerIPAddress != null ? this.lowerIPAddress.getIPVersion() : this.upperIPAddress.getIPVersion();
    }

    @JsonProperty
    public String getLower() {
        return lower;
    }

    @JsonProperty
    public String getUpper() {
        return upper;
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
                && Objects.equals(lower, range.lower)
                && Objects.equals(upper, range.upper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lower, upper, lowerOpen, upperOpen);
    }
}
