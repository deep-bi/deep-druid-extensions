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

import bi.deep.entity.IPSetContents;
import bi.deep.util.IPRangeUtil;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.apache.druid.query.filter.DruidObjectPredicate;
import org.apache.druid.query.filter.DruidPredicateMatch;

public class IPAddressRangeListPredicate implements DruidObjectPredicate<String> {

    private final Predicate<IPSetContents> predicate;

    public IPAddressRangeListPredicate(Predicate<IPSetContents> predicate) {
        this.predicate = predicate;
    }

    public static IPAddressRangeListPredicate of(Predicate<IPSetContents> predicate) {
        return new IPAddressRangeListPredicate(predicate);
    }

    @Override
    public DruidPredicateMatch apply(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return DruidPredicateMatch.UNKNOWN;
        }
        return DruidPredicateMatch.of(predicate.test(IPRangeUtil.extractIPSetContents(value)));
    }
}
