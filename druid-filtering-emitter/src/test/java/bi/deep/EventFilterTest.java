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

import bi.deep.filtering.EventFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.druid.java.util.emitter.core.Event;
import org.apache.druid.java.util.emitter.service.ServiceMetricEvent;
import org.junit.Assert;
import org.junit.Test;

public class EventFilterTest {

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
        return ServiceMetricEvent.builder().build(name, value).build("test", "localhost");
    }

    @Test
    public void testEmptyFilter() {
        Event event = makeEvent("abc", 1);
        EventFilter filter = builder().build();

        Assert.assertTrue(filter.test(event)); // true - no rules to follow
    }

    @Test
    public void test1() {
        Event e1 = makeEvent("a/1", 1);
        Event e2 = makeEvent("a/2", 1);
        Event e3 = makeEvent("b/1", 1);
        Event e4 = makeEvent("b/2", 1);

        EventFilter filter = builder()
                .withAllowed("a/1")
                .withExcluded("b/1")
                .withStartsWith("b")
                .build();

        Assert.assertTrue(filter.test(e1));
        Assert.assertFalse(filter.test(e2));
        Assert.assertFalse(filter.test(e3));
        Assert.assertTrue(filter.test(e4));
    }

    @Test
    public void test2() {
        Event e1 = makeEvent("a/1", 1);
        Event e2 = makeEvent("a/2", 1);
        Event e3 = makeEvent("b/1", 1);
        Event e4 = makeEvent("b/2", 1);

        EventFilter filter = builder().withExcluded("b/1").build();

        Assert.assertTrue(filter.test(e1));
        Assert.assertTrue(filter.test(e2));
        Assert.assertFalse(filter.test(e3));
        Assert.assertTrue(filter.test(e4));
    }

    @Test
    public void test3() {
        Event e1 = makeEvent("a/1", 1);
        Event e2 = makeEvent("a/2", 1);
        Event e3 = makeEvent("b/1", 1);
        Event e4 = makeEvent("b/2", 1);

        EventFilter filter = builder().withExcluded("b/1").build();

        Assert.assertTrue(filter.test(e1));
        Assert.assertTrue(filter.test(e2));
        Assert.assertFalse(filter.test(e3));
        Assert.assertTrue(filter.test(e4));
    }

    @Test
    public void testAllowList() {
        Event e1 = makeEvent("abc", 1);
        Event e2 = makeEvent("def", 2);
        Event e3 = makeEvent("ghi", 3);

        EventFilter filter = builder().withAllowed("abc", "def").build();

        Assert.assertTrue(filter.test(e1)); // because abc on list
        Assert.assertTrue(filter.test(e2)); // because def on list
        Assert.assertFalse(filter.test(e3)); // because ghi is not on allowed
    }

    @Test
    public void testAllowListWithDifferentKey() {
        Event e1 = makeEvent("abc", 1);
        Event e2 = makeEvent("def", 1);
        Event e3 = makeEvent("ghi", 1);

        EventFilter filter =
                builder().withAllowed("abc", "def").withKey("_metric_").build();

        Assert.assertTrue(filter.test(e1)); // because _metric_ is not in the event
        Assert.assertTrue(filter.test(e2)); // because _metric_ is not in the event
        Assert.assertTrue(filter.test(e3)); // because _metric_ is not in the event
    }

    @Test
    public void testExcludedList() {
        Event e1 = makeEvent("e1", 1);
        Event e2 = makeEvent("e2", 1);
        Event e3 = makeEvent("e3", 1);

        EventFilter filter = builder().withExcluded("e1", "e2").build();

        Assert.assertFalse(filter.test(e1)); // because 'e1' on exclude list
        Assert.assertFalse(filter.test(e2)); // because 'e2' on exclude list
        Assert.assertTrue(
                filter.test(e3)); // because 'e3' not on exclude list and allow list is empty (everything passes)
    }

    @Test
    public void testAllowedExcludedList() {
        Assert.assertThrows(
                RuntimeException.class,
                () -> builder().withAllowed("e1").withExcluded("e2").build());
    }

    @Test
    public void testStartsWithList() {
        Event e1 = makeEvent("a1", 1);
        Event e2 = makeEvent("a2", 1);
        Event e3 = makeEvent("b3", 1);

        EventFilter filter = builder().withStartsWith("a").build();

        Assert.assertTrue(filter.test(e1)); // because 'a1' starts with 'a'
        Assert.assertTrue(filter.test(e2)); // because 'a2' starts with 'a'
        Assert.assertFalse(filter.test(e3)); // because 'b3' starts with 'b'
    }

    @Test
    public void testExcludeStartsWithList() {
        Event e1 = makeEvent("a1", 1);
        Event e2 = makeEvent("a2", 1);
        Event e3 = makeEvent("a3", 1);

        EventFilter filter =
                builder().withExcluded("a1", "a2").withStartsWith("a").build();

        Assert.assertFalse(filter.test(e1)); // because 'a1' is excluded
        Assert.assertFalse(filter.test(e2)); // because 'a2' is excluded
        Assert.assertTrue(filter.test(e3)); // because 'a3' starts with 'a'
    }

    @Test
    public void testRegexList() {
        Event e1 = makeEvent("a1", 1);
        Event e2 = makeEvent("a2", 1);
        Event e3 = makeEvent("a123", 1);
        Event e4 = makeEvent("b12", 1);
        Event e5 = makeEvent("123", 1);

        EventFilter filter = builder().withRegex("a\\d+").build();

        Assert.assertTrue(filter.test(e1)); // because 'a1' matches regex
        Assert.assertTrue(filter.test(e2)); // because 'a2' matches regex
        Assert.assertTrue(filter.test(e3)); // because 'a123' matches regex
        Assert.assertFalse(filter.test(e4)); // because 'b12' doesn't match regex
        Assert.assertFalse(filter.test(e5)); // because '123' doesn't match regex
    }

    @Test
    public void testExcludeRegexList() {
        Event e1 = makeEvent("a1", 1);
        Event e2 = makeEvent("a2", 1);
        Event e3 = makeEvent("a3", 1);
        Event e4 = makeEvent("b3", 1);

        EventFilter filter =
                builder().withExcluded("a1", "a2").withRegex("a\\d+").build();

        Assert.assertFalse(filter.test(e1)); // because 'a1' is excluded
        Assert.assertFalse(filter.test(e2)); // because 'a2' is excluded
        Assert.assertTrue(filter.test(e3)); // because 'a3' matches regex
        Assert.assertFalse(filter.test(e4)); // because 'b3' doesn't match regex
    }
}
