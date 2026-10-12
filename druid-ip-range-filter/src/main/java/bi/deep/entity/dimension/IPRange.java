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

import bi.deep.entity.SerializationUtil;
import bi.deep.util.IPRangeUtil;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import inet.ipaddr.IPAddress;
import inet.ipaddr.format.IPAddressRange;
import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.druid.error.InvalidInput;
import org.apache.druid.java.util.common.IAE;

@JsonSerialize(using = IPRangeSerializer.class)
public class IPRange implements Serializable, IPRangeHandler, Comparable<IPRange> {
    public static final IPRange EMPTY = new IPRange(null);
    public static final Comparator<IPRange> COMPARATOR = Comparator.nullsFirst(
            Comparator.comparing(IPRange::getAddressRange, Comparator.nullsFirst(Comparator.naturalOrder())));
    private final IPAddressRange addressRange;

    public IPRange(@Nullable IPAddressRange addressRange) {
        this.addressRange = addressRange;
    }

    public static IPRange fromString(String val) {
        if (StringUtils.isBlank(val)) {
            throw InvalidInput.exception("Range cannot be null or empty");
        }

        IPAddressRange range = IPRangeUtil.fromString(val);

        if (range == null) {
            throw InvalidInput.exception("Invalid input");
        }

        return new IPRange(range);
    }

    public IPAddressRange getAddressRange() {
        return addressRange;
    }

    public boolean contains(IPAddress value) {
        return value != null && addressRange != null && addressRange.contains(value);
    }

    @Override
    public boolean contains(SortedSet<IPAddress> addresses) {
        if (addressRange == null || CollectionUtils.isEmpty(addresses)) {
            return false;
        }

        Iterator<IPAddress> addressIter = addresses.iterator();
        IPAddress lower = addressRange.getLower();

        while (addressIter.hasNext()) {
            IPAddress address = addressIter.next();

            if (address.compareTo(lower) >= 0) {
                return addressRange.contains(address);
            }
        }

        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof IPRange)) {
            return false;
        }

        final IPRange that = (IPRange) obj;
        return Objects.equals(addressRange, that.addressRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addressRange);
    }

    @Override
    public String toString() {
        return IPRangeUtil.toString(addressRange);
    }

    @Override
    public int compareTo(IPRange other) {
        return COMPARATOR.compare(this, other);
    }

    @Override
    public byte[] toBytes() {
        try {
            return SerializationUtil.serialize(this);
        } catch (IOException e) {
            throw new IAE("Unable to convert to bytes", e);
        }
    }

    public static IPRange from(Object input) {
        if (input == null) {
            return EMPTY;
        }
        if (input instanceof IPRange) {
            return (IPRange) input;
        }

        if (input instanceof String) {
            return IPRange.fromString((String) input);
        }

        try {
            return SerializationUtil.deserializeToIPRange((byte[]) input);
        } catch (Exception e) {
            throw new IAE("Unable to read input", e);
        }
    }

    @Override
    public int getLengthOfEncodedKeyComponent() {
        return IPRangeUtil.getSize(addressRange);
    }
}
