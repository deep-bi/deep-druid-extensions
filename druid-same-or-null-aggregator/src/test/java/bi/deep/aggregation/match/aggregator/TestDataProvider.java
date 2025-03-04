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
package bi.deep.aggregation.match.aggregator;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public class TestDataProvider {

    public static Stream<Object> provideMixedTypeValues() {
        return Stream.of(
                "String", // String type
                123, // Integer type
                45.67f, // Float type
                89.12, // Double type
                true, // Boolean type
                new Data("data") // Object with no comparator
                );
    }

    static Stream<Arguments> provideMixedTypeDifferentValues() {
        return Stream.of(
                Arguments.of("String", "String1"), // String type
                Arguments.of(123, 124), // Integer type
                Arguments.of(45.67f, 45.68f), // Float type
                Arguments.of(89.12, 89.13), // Double type
                Arguments.of(true, false), // Boolean type
                Arguments.of(new Data("data"), new Data("data1")) // Object with no comparator
                );
    }

    public static class Data implements Serializable {
        private final String value;

        Data(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Data)) {
                return false;
            }

            Data data = (Data) object;
            return Objects.equals(value, data.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }
    }
}
