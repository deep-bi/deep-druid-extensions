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
package bi.deep.util;

import bi.deep.entity.IPSetContents;
import bi.deep.range.IPBoundedRange;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressSeqRange;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.format.IPAddressRange;
import inet.ipaddr.ipv4.IPv4Address;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.druid.error.InvalidInput;
import org.apache.druid.java.util.common.IAE;

public final class IPRangeUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern DASH_REGEX = Pattern.compile("^([0-9A-Fa-f:.]+)[–-]([0-9A-Fa-f:.]+)$");
    private static final Pattern SLASH_REGEX = Pattern.compile("^([0-9A-Fa-f:.]+)/([0-9A-Fa-f:.]+)$");
    private static final Pattern CIDR_REGEX = Pattern.compile("^[0-9A-Fa-f:.]+/\\d+$");
    private static final Pattern IP_REGEX = Pattern.compile("^[0-9A-Fa-f:.]+$");
    private static final int PARALLEL_LIMIT = 200;

    private IPRangeUtil() {
        throw new AssertionError("No bi.deep.util.IPRangeUtil instances for you!");
    }

    public static IPAddressRange fromString(String input) {
        if (StringUtils.isBlank(input)) {
            throw InvalidInput.exception("Range cannot be null or empty");
        }

        final String range = input.trim();
        final SeparatorMatch match = SeparatorMatch.detect(range);

        if (match.separator == Separator.NONE) { // Single IP
            return parseIPAddress(range, "Malformed input '%s'. Expected IP, CIDR, or range.");
        }

        final String left = range.substring(0, match.position).trim();
        final String right =
                range.substring(match.position + match.separator.length).trim();

        if (left.isEmpty() || right.isEmpty()) {
            throw InvalidInput.exception("Malformed '%s'. Empty side around separator.", range);
        }

        // CIDR: ip/prefix
        if (match.separator == Separator.SLASH && isDigits(right)) {
            return parseIPAddress(range, "Malformed CIDR '%s'. Expected ip/prefix.")
                    .toPrefixBlock()
                    .toSequentialRange();
        }

        // IP range
        IPAddress lower = parseIPAddress(left, "Invalid lower IP '%s'.");
        IPAddress upper = parseIPAddress(right, "Invalid upper IP '%s'.");

        if (!lower.getIPVersion().equals(upper.getIPVersion())) {
            throw new IAE("IPv4/IPv6 mismatch: '%s' vs '%s'.", lower, upper);
        }

        return lower.spanWithRange(upper);
    }

    private static IPAddress parseIPAddress(String value, String errorMessage) {
        IPAddress ip = new IPAddressString(value).getAddress();

        if (ip == null) {
            throw InvalidInput.exception(errorMessage, value);
        }
        return ip;
    }

    public static int getSize(IPAddressRange range) {
        if (range == null) return 0;

        return range.getByteCount() == IPv4Address.BYTE_COUNT ? 20 : 44;
    }

    private static boolean isDigits(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static Object parseToken(String data, Map<String, Object> cache) {
        return cache.computeIfAbsent(data, token -> {
            Matcher dashMatcher = DASH_REGEX.matcher(token);

            if (dashMatcher.matches()) {
                String lo = dashMatcher.group(1);
                String hi = dashMatcher.group(2);
                IPAddress la = new IPAddressString(lo).getAddress();
                IPAddress ha = new IPAddressString(hi).getAddress();
                if (la != null && ha != null && la.getIPVersion() == ha.getIPVersion()) {
                    return new IPBoundedRange(lo, hi, false, false);
                }

                return null;
            }

            Matcher slashMatcher = SLASH_REGEX.matcher(token);
            Matcher cidrMatcher = CIDR_REGEX.matcher(token);
            boolean isCidrSpecificFormat = cidrMatcher.matches();

            if (slashMatcher.matches() && !isCidrSpecificFormat) {
                String lo = slashMatcher.group(1);
                String hi = slashMatcher.group(2);

                return new IPBoundedRange(lo, hi, false, false);
            }

            if (isCidrSpecificFormat) {
                IPAddressSeqRange seq = new IPAddressString(token).getAddress().toSequentialRange();
                if (seq != null) {
                    return new IPBoundedRange(seq, false, false);
                }
                return null;
            }

            Matcher ipMatcher = IP_REGEX.matcher(token);

            if (ipMatcher.matches()) {
                return new IPAddressString(token).getAddress();
            }

            return null;
        });
    }

    public static IPSetContents extractIPSetContents(String input) {
        if (StringUtils.isBlank(input)) {
            return new IPSetContents(Collections.emptyList(), Collections.emptyList());
        }

        final Map<String, Object> cache = new ConcurrentHashMap<>();
        final String[] tokens = input.split("\\s*,\\s*");
        Stream<String> tokenStream = Arrays.stream(tokens).filter(StringUtils::isNotBlank);

        if (tokens.length > PARALLEL_LIMIT) {
            tokenStream = tokenStream.parallel();
        }

        final List<Object> parsedResults = tokenStream
                .map(token -> parseToken(token, cache))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        final List<IPAddress> ips = new ArrayList<>();
        final List<IPBoundedRange> ranges = new ArrayList<>();

        parsedResults.forEach(result -> {
            if (result instanceof IPAddress) {
                ips.add((IPAddress) result);
            } else if (result instanceof IPBoundedRange) {
                ranges.add((IPBoundedRange) result);
            }
        });

        return new IPSetContents(ips, ranges);
    }

    public static String getMatchingIPs(String input, List<IPAddress> ips) {
        if (StringUtils.isBlank(input) || CollectionUtils.isEmpty(ips)) {
            return null;
        }

        IPSetContents ranges = extractIPSetContents(input);

        if (ranges.isEmpty()) {
            return null;
        }

        // Filter matching IPs
        List<String> matchingIps = ips.stream()
                .filter(ip -> ranges.contains(ip, false))
                .map(IPAddress::toString)
                .collect(Collectors.toList());

        if (matchingIps.isEmpty()) {
            return null;
        }
        if (matchingIps.size() == 1) {
            return matchingIps.get(0);
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(matchingIps);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting to JSON", e);
        }
    }

    public static List<IPAddress> mapStringsToIps(final Set<String> ips) {
        return ips.stream()
                .map(ip -> new IPAddressString(ip).getAddress())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    enum Separator {
        ARROW("->", 2),
        SLASH("/", 1),
        HYPHEN("-", 1),
        EN_DASH("–", 1),
        NONE(null, 0);

        private final String token;
        private final int length;

        Separator(String token, int length) {
            this.token = token;
            this.length = length;
        }
    }

    private static final class SeparatorMatch {
        private final Separator separator;
        private final int position;

        private SeparatorMatch(Separator separator, int position) {
            this.separator = separator;
            this.position = position;
        }

        public static SeparatorMatch detect(String value) {
            // Highest priority: ->
            int arrowPos = value.indexOf(Separator.ARROW.token);

            if (arrowPos >= 0) {
                ensureSingle(value, Separator.ARROW, arrowPos);
                return new SeparatorMatch(Separator.ARROW, arrowPos);
            }

            Separator found = null;
            int foundPos = -1;

            for (Separator sep : new Separator[] {Separator.SLASH, Separator.HYPHEN, Separator.EN_DASH}) {
                int pos = value.indexOf(sep.token);

                if (pos >= 0) {
                    if (found != null) {
                        throw InvalidInput.exception(
                                "Expected exactly one separator: '/', '-', '–', or '->': '%s'", value);
                    }
                    ensureSingle(value, sep, pos);
                    found = sep;
                    foundPos = pos;
                }
            }

            return new SeparatorMatch(found == null ? Separator.NONE : found, foundPos);
        }

        private static void ensureSingle(String value, Separator sep, int firstPos) {
            if (value.indexOf(sep.token, firstPos + sep.length) != -1) {
                throw InvalidInput.exception("Multiple '%s' in '%s'", sep.token, value);
            }
        }
    }

    public static String toString(IPAddressRange obj) {
        if (obj instanceof IPAddress) {
            return obj.toString();
        }

        Function<IPAddress, String> stringer = IPAddress::toCanonicalString;
        return (obj == null) ? "null" : stringer.apply(obj.getLower()) + "-" + stringer.apply(obj.getUpper());
    }

    public static String toString(Collection<IPAddressRange> addressRanges) {
        return CollectionUtils.isEmpty(addressRanges)
                ? "[]"
                : addressRanges.stream().map(IPRangeUtil::toString).collect(Collectors.joining(",", "[", "]"));
    }
}
