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
package bi.deep;

import java.io.IOException;
import java.util.function.Predicate;
import org.apache.druid.java.util.common.lifecycle.LifecycleStart;
import org.apache.druid.java.util.common.lifecycle.LifecycleStop;
import org.apache.druid.java.util.emitter.core.Emitter;
import org.apache.druid.java.util.emitter.core.Event;

public class FilteringEmitter implements Emitter {

    private final Emitter inner;
    private final Predicate<Event> filter;

    public FilteringEmitter(Emitter inner, Predicate<Event> filter) {
        this.inner = inner;
        this.filter = filter;
    }

    @Override
    @LifecycleStart
    public void start() {
        inner.start();
    }

    @Override
    public void emit(Event event) {
        if (filter.test(event)) {
            inner.emit(event);
        }
    }

    @Override
    public void flush() throws IOException {
        inner.flush();
    }

    @Override
    @LifecycleStop
    public void close() throws IOException {
        inner.close();
    }

    @Override
    public String toString() {
        return "FilteringEmitter{" + "emitter=" + inner + "}";
    }
}
