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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import org.apache.druid.segment.transform.RowFunction;
import org.apache.druid.segment.transform.Transform;

public class CurrentTimestampTransform implements Transform {
    public static final String TYPE_NAME = "currentTimestamp";

    private final String name;
    private final RowFunction rowFunction;

    @JsonCreator
    public CurrentTimestampTransform(@JsonProperty("name") final String name) {
        this.name = Preconditions.checkNotNull(name, "name");
        this.rowFunction = row -> System.currentTimeMillis();
    }

    @JsonProperty
    @Override
    public String getName() {
        return name;
    }

    @Override
    public RowFunction getRowFunction() {
        return rowFunction;
    }

    @Override
    public Set<String> getRequiredColumns() {
        return Collections.emptySet();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CurrentTimestampTransform that = (CurrentTimestampTransform) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "CurrentTimestampTransform{" + "name='" + name + '\'' + '}';
    }
}
