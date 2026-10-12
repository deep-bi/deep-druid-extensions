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
package bi.deep.filtering.common;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.apache.druid.query.filter.DruidObjectPredicate;
import org.apache.druid.query.filter.DruidPredicateMatch;

public class IPAddressPredicate implements DruidObjectPredicate<String> {
    private final Predicate<IPAddress> predicate;

    public IPAddressPredicate(Predicate<IPAddress> predicate) {
        this.predicate = predicate;
    }

    public static IPAddressPredicate of(Predicate<IPAddress> predicate) {
        return new IPAddressPredicate(predicate);
    }

    @Override
    public DruidPredicateMatch apply(@Nullable String value) {
        return mapToIPAddress(value)
                .map(predicate::test)
                .map(DruidPredicateMatch::of)
                .orElse(DruidPredicateMatch.UNKNOWN);
    }

    private Optional<IPAddress> mapToIPAddress(@Nullable String value) {
        return Optional.ofNullable(new IPAddressString(value).getAddress());
    }
}
