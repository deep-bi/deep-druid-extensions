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

import bi.deep.entity.dimension.IPRange;
import bi.deep.entity.dimension.IPRangeArray;
import inet.ipaddr.IPAddress;
import inet.ipaddr.format.IPAddressRange;
import inet.ipaddr.ipv4.IPv4Address;
import inet.ipaddr.ipv6.IPv6Address;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SerializationUtil {

    private SerializationUtil() {
        throw new AssertionError("No bi.deep.entity.SerializationUtil instances for you!");
    }

    public static byte[] serialize(IPRange range) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bos)) {
            serialize(range.getAddressRange(), out);
            return bos.toByteArray();
        }
    }

    public static byte[] serialize(IPRangeArray rangeArray) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bos)) {
            for (IPAddressRange range : rangeArray.getAddressRanges()) {
                serialize(range, out);
            }
            return bos.toByteArray();
        }
    }

    public static IPRangeArray deserializeToIPRangeArray(byte[] data) throws IOException {
        List<IPAddressRange> array = new ArrayList<>();

        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                DataInputStream in = new DataInputStream(bis)) {
            while (in.available() > 0) {
                array.add(deserialize(in));
            }
        }

        return new IPRangeArray(array);
    }

    public static IPRange deserializeToIPRange(byte[] data) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                DataInputStream in = new DataInputStream(bis)) {
            return new IPRange(deserialize(in));
        }
    }

    private static IPAddressRange deserialize(DataInputStream in) throws IOException {
        int version = in.readInt();
        byte[] lowerBytes = new byte[in.readInt()];
        in.readFully(lowerBytes);

        byte[] upperBytes = new byte[in.readInt()];
        in.readFully(upperBytes);

        return parseAddress(version, lowerBytes).spanWithRange(parseAddress(version, upperBytes));
    }

    private static IPAddress parseAddress(int version, byte[] bytes) {
        switch (version) {
            case IPv4Address.BYTE_COUNT:
                return new IPv4Address(bytes);
            case IPv6Address.BYTE_COUNT:
                return new IPv6Address(bytes);
            default:
                throw new IllegalArgumentException("Unknown IP version");
        }
    }

    private static void serialize(IPAddressRange range, DataOutputStream out) throws IOException {
        IPAddress lowerAddress = range.getLower();
        byte[] lowerBytes = lowerAddress.getBytes();
        out.writeInt(range.getByteCount());
        out.writeInt(lowerBytes.length);
        out.write(lowerBytes);

        byte[] upperBytes = range.getUpper().getBytes();
        out.writeInt(upperBytes.length);
        out.write(upperBytes);
    }
}
