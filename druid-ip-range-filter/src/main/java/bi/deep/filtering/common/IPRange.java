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
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.format.IPAddressRange;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.druid.error.InvalidInput;
import org.apache.druid.java.util.common.IAE;

public class IPRange {
    private static final String SEPARATOR = "/";
    private final IPAddressRange addressRange;
    private final IPAddress.IPVersion ipVersion;

    @JsonCreator
    public IPRange(String range) {
        if (StringUtils.isBlank(range)) {
            throw InvalidInput.exception("Range cannot be null or empty");
        }

        final String[] bounds = range.split(SEPARATOR);

        if (bounds.length != 2) {
            throw InvalidInput.exception(
                    String.format("Range should include lower and upper bounds in format `lower%supper`", SEPARATOR));
        }

        final IPAddress lower = new IPAddressString(bounds[0]).getAddress();
        final IPAddress upper = new IPAddressString(bounds[1]).getAddress();

        if (lower == null) {
            throw InvalidInput.exception("Invalid lower IP address");
        }

        if (upper == null) {
            throw InvalidInput.exception("Invalid upper IP address");
        }

        if (!lower.getIPVersion().equals(upper.getIPVersion())) {
            throw new IAE("Invalid IP type, it must be of the same IP type (IPv4 or IPv6)");
        }

        this.addressRange = lower.spanWithRange(upper);
        this.ipVersion = lower.getIPVersion();
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
