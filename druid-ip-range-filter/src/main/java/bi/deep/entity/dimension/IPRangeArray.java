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

import static inet.ipaddr.Address.ADDRESS_LOW_VALUE_COMPARATOR;

import bi.deep.entity.SerializationUtil;
import bi.deep.util.IPRangeUtil;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import inet.ipaddr.IPAddress;
import inet.ipaddr.format.IPAddressRange;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.druid.java.util.common.IAE;

@JsonSerialize(using = IPRangeArraySerializer.class)
public class IPRangeArray implements Serializable, IPRangeHandler, Comparable<IPRangeArray> {
    public static final IPRangeArray EMPTY = new IPRangeArray(Collections.emptyList());
    public static final Comparator<IPRangeArray> COMPARATOR = Comparator.nullsFirst(IPRangeArray::compareTo);
    private final SortedSet<IPAddressRange> addressRanges;

    public IPRangeArray(List<IPAddressRange> addressRanges) {
        this.addressRanges = new TreeSet<>(ADDRESS_LOW_VALUE_COMPARATOR);
        this.addressRanges.addAll(addressRanges);
    }

    public static IPRangeArray fromArray(List<Object> values) {
        if (CollectionUtils.isEmpty(values)) {
            return EMPTY;
        }

        return new IPRangeArray(values.stream()
                .map(Objects::toString)
                .map(IPRangeUtil::fromString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    public Set<IPAddressRange> getAddressRanges() {
        return addressRanges;
    }

    public boolean contains(IPAddress address) {
        for (IPAddressRange range : addressRanges) {
            if (range.getLower().compareTo(address) > 0) {
                return false;
            }

            if (range.contains(address)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean contains(final SortedSet<IPAddress> addresses) {
        final Iterator<IPAddress> addressIterator = addresses.iterator();
        final Iterator<IPAddressRange> rangeIterator = addressRanges.iterator();

        if (!addressIterator.hasNext() || !rangeIterator.hasNext()) {
            return false; // One set is empty, might wanna return true if addresses are empty?
        }

        IPAddress currentAddress = addressIterator.next();
        IPAddressRange currentRange = rangeIterator.next();

        while (currentAddress != null && currentRange != null) {
            int compareToLower = currentAddress.compareTo(currentRange.getLower());
            if (compareToLower < 0) {
                if (addressIterator.hasNext()) {
                    currentAddress = addressIterator.next();
                } else {
                    currentAddress = null;
                }
                continue;
            }

            int compareToUpper = currentAddress.compareTo(currentRange.getUpper());
            if (compareToUpper > 0) {
                if (rangeIterator.hasNext()) {
                    currentRange = rangeIterator.next();
                } else {
                    currentRange = null;
                }
                continue;
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof IPRangeArray)) {
            return false;
        }

        final IPRangeArray that = (IPRangeArray) obj;
        return Objects.equals(addressRanges, that.addressRanges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addressRanges);
    }

    @Override
    public String toString() {
        return IPRangeUtil.toString(addressRanges);
    }

    @Override
    public int compareTo(IPRangeArray other) {
        Iterator<IPAddressRange> iter = addressRanges.iterator();
        Iterator<IPAddressRange> otherIter = other.addressRanges.iterator();

        while (iter.hasNext() && otherIter.hasNext()) {
            int cmp = ADDRESS_LOW_VALUE_COMPARATOR.compare(iter.next(), otherIter.next());

            if (cmp != 0) {
                return cmp;
            }
        }

        if (iter.hasNext()) return 1;
        if (otherIter.hasNext()) return -1;
        return 0;
    }

    @Override
    public byte[] toBytes() {
        try {
            return SerializationUtil.serialize(this);
        } catch (IOException e) {
            throw new IAE("Unable to convert to bytes", e);
        }
    }

    public static IPRangeArray from(Object input) {
        if (input == null) {
            return EMPTY;
        }
        if (input instanceof IPRangeArray) {
            return (IPRangeArray) input;
        }

        if (input instanceof List) {
            return IPRangeArray.fromArray((List<Object>) input);
        }

        try {
            return SerializationUtil.deserializeToIPRangeArray((byte[]) input);
        } catch (Exception e) {
            throw new IAE("Unable to read input", e);
        }
    }

    public boolean isEmpty() {
        return addressRanges == null || addressRanges.isEmpty();
    }

    @Override
    public int getLengthOfEncodedKeyComponent() {
        if (isEmpty()) {
            return 0;
        }

        return addressRanges.stream().mapToInt(IPRangeUtil::getSize).sum();
    }
}
