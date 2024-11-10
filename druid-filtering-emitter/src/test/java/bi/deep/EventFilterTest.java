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
package bi.deep;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bi.deep.filtering.EventFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.druid.java.util.emitter.core.Event;
import org.apache.druid.java.util.emitter.service.ServiceMetricEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EventFilterTest {

    private static class Builder {
        private String key = "metric";
        private Set<String> allowList = Collections.emptySet();
        private Set<String> excludeList = Collections.emptySet();
        private Set<String> startsWithList = Collections.emptySet();
        private Set<String> regexList = Collections.emptySet();

        public Builder withKey(String key) {
            this.key = key;
            return this;
        }

        public Builder withAllowed(String... allowed) {
            allowList = Arrays.stream(allowed).collect(Collectors.toSet());
            return this;
        }

        public Builder withExcluded(String... excluded) {
            excludeList = Arrays.stream(excluded).collect(Collectors.toSet());
            return this;
        }

        public Builder withStartsWith(String... startsWith) {
            startsWithList = Arrays.stream(startsWith).collect(Collectors.toSet());
            return this;
        }

        public Builder withRegex(String... regex) {
            regexList = Arrays.stream(regex).collect(Collectors.toSet());
            return this;
        }

        public EventFilter build() {
            return new EventFilter(key, allowList, excludeList, startsWithList, regexList);
        }
    }

    private static Builder builder() {
        return new Builder();
    }

    private static Event makeEvent(String name, Number value) {
        return ServiceMetricEvent.builder().setMetric(name, value).build("test", "localhost");
    }

    @Test
    void testEmptyFilter() {
        Event event = makeEvent("abc", 1);
        EventFilter filter = builder().build();

        assertTrue(filter.test(event)); // true - no rules to follow
    }

    @Test
    void test1() {
        Event e1 = makeEvent("a/1", 1);
        Event e2 = makeEvent("a/2", 1);
        Event e3 = makeEvent("b/1", 1);
        Event e4 = makeEvent("b/2", 1);

        EventFilter filter = builder()
                .withAllowed("a/1")
                .withExcluded("b/1")
                .withStartsWith("b")
                .build();

        assertTrue(filter.test(e1));
        assertFalse(filter.test(e2));
        assertFalse(filter.test(e3));
        assertTrue(filter.test(e4));
    }

    @Test
    void test2() {
        Event e1 = makeEvent("a/1", 1);
        Event e2 = makeEvent("a/2", 1);
        Event e3 = makeEvent("b/1", 1);
        Event e4 = makeEvent("b/2", 1);

        EventFilter filter = builder().withExcluded("b/1").build();

        assertTrue(filter.test(e1));
        assertTrue(filter.test(e2));
        assertFalse(filter.test(e3));
        assertTrue(filter.test(e4));
    }

    @Test
    void test3() {
        Event e1 = makeEvent("a/1", 1);
        Event e2 = makeEvent("a/2", 1);
        Event e3 = makeEvent("b/1", 1);
        Event e4 = makeEvent("b/2", 1);

        EventFilter filter = builder().withExcluded("b/1").build();

        assertTrue(filter.test(e1));
        assertTrue(filter.test(e2));
        assertFalse(filter.test(e3));
        assertTrue(filter.test(e4));
    }

    @Test
    void testAllowList() {
        Event e1 = makeEvent("abc", 1);
        Event e2 = makeEvent("def", 2);
        Event e3 = makeEvent("ghi", 3);

        EventFilter filter = builder().withAllowed("abc", "def").build();

        assertTrue(filter.test(e1)); // because abc on list
        assertTrue(filter.test(e2)); // because def on list
        assertFalse(filter.test(e3)); // because ghi is not on allowed
    }

    @Test
    void testAllowListWithDifferentKey() {
        Event e1 = makeEvent("abc", 1);
        Event e2 = makeEvent("def", 1);
        Event e3 = makeEvent("ghi", 1);

        EventFilter filter =
                builder().withAllowed("abc", "def").withKey("_metric_").build();

        assertTrue(filter.test(e1)); // because _metric_ is not in the event
        assertTrue(filter.test(e2)); // because _metric_ is not in the event
        assertTrue(filter.test(e3)); // because _metric_ is not in the event
    }

    @Test
    void testExcludedList() {
        Event e1 = makeEvent("e1", 1);
        Event e2 = makeEvent("e2", 1);
        Event e3 = makeEvent("e3", 1);

        EventFilter filter = builder().withExcluded("e1", "e2").build();

        assertFalse(filter.test(e1)); // because 'e1' on exclude list
        assertFalse(filter.test(e2)); // because 'e2' on exclude list
        assertTrue(filter.test(e3)); // because 'e3' not on exclude list and allow list is empty (everything passes)
    }

    @Test
    void testAllowedExcludedList() {
        Assertions.assertThrows(
                RuntimeException.class,
                () -> builder().withAllowed("e1").withExcluded("e2").build());
    }

    @Test
    void testStartsWithList() {
        Event e1 = makeEvent("a1", 1);
        Event e2 = makeEvent("a2", 1);
        Event e3 = makeEvent("b3", 1);

        EventFilter filter = builder().withStartsWith("a").build();

        assertTrue(filter.test(e1)); // because 'a1' starts with 'a'
        assertTrue(filter.test(e2)); // because 'a2' starts with 'a'
        assertFalse(filter.test(e3)); // because 'b3' starts with 'b'
    }

    @Test
    void testExcludeStartsWithList() {
        Event e1 = makeEvent("a1", 1);
        Event e2 = makeEvent("a2", 1);
        Event e3 = makeEvent("a3", 1);

        EventFilter filter =
                builder().withExcluded("a1", "a2").withStartsWith("a").build();

        assertFalse(filter.test(e1)); // because 'a1' is excluded
        assertFalse(filter.test(e2)); // because 'a2' is excluded
        assertTrue(filter.test(e3)); // because 'a3' starts with 'a'
    }

    @Test
    void testRegexList() {
        Event e1 = makeEvent("a1", 1);
        Event e2 = makeEvent("a2", 1);
        Event e3 = makeEvent("a123", 1);
        Event e4 = makeEvent("b12", 1);
        Event e5 = makeEvent("123", 1);

        EventFilter filter = builder().withRegex("a\\d+").build();

        assertTrue(filter.test(e1)); // because 'a1' matches regex
        assertTrue(filter.test(e2)); // because 'a2' matches regex
        assertTrue(filter.test(e3)); // because 'a123' matches regex
        assertFalse(filter.test(e4)); // because 'b12' doesn't match regex
        assertFalse(filter.test(e5)); // because '123' doesn't match regex
    }

    @Test
    void testExcludeRegexList() {
        Event e1 = makeEvent("a1", 1);
        Event e2 = makeEvent("a2", 1);
        Event e3 = makeEvent("a3", 1);
        Event e4 = makeEvent("b3", 1);

        EventFilter filter =
                builder().withExcluded("a1", "a2").withRegex("a\\d+").build();

        assertFalse(filter.test(e1)); // because 'a1' is excluded
        assertFalse(filter.test(e2)); // because 'a2' is excluded
        assertTrue(filter.test(e3)); // because 'a3' matches regex
        assertFalse(filter.test(e4)); // because 'b3' doesn't match regex
    }
}
