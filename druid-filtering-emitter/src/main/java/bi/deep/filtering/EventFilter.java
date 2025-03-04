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
package bi.deep.filtering;

import bi.deep.FilteringEmitterConfig;
import bi.deep.filtering.steps.*;
import bi.deep.filtering.steps.TrinaryBool;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.druid.java.util.emitter.core.Event;

public class EventFilter implements Predicate<Event> {
    private final String key;
    private final List<FilterStep> steps;

    public EventFilter(
            String key,
            Set<String> allowList,
            Set<String> blockList,
            Set<String> startsWithList,
            Set<String> regexList) {
        if (!allowList.isEmpty() && !blockList.isEmpty() && startsWithList.isEmpty() && regexList.isEmpty()) {
            throw new RuntimeException(
                    "Defining both 'allowList' and 'blockList' without other filters is not supported.");
        }

        this.key = key;
        this.steps = Stream.of(
                        new AllowedFilter(allowList),
                        new BlockFilter(blockList),
                        new StartsWithFilter(startsWithList),
                        new RegexFilter(regexList))
                .filter(FilterStep::isEnabled)
                .collect(Collectors.toList());
    }

    public static EventFilter of(FilteringEmitterConfig config) {
        return new EventFilter(
                config.getKey(),
                config.getAllowList(),
                config.getBlockList(),
                config.getStartsWithList(),
                config.getRegexMatchList());
    }

    @Override
    public boolean test(Event event) {
        if (steps.isEmpty()) return true;

        Map<String, Object> kv = event.toMap();
        if (!kv.containsKey(key)) return true;

        String value = kv.get(key).toString();

        TrinaryBool result = TrinaryBool.MAYBE_FALSE;
        for (FilterStep step : steps) {
            result = step.filter(value);
            if (result.isKnown()) return result.getValue();
        }

        return result.getValue();
    }
}
