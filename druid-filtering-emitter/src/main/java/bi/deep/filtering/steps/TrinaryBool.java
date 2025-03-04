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
package bi.deep.filtering.steps;

import java.util.Objects;

public class TrinaryBool {
    private final boolean value;
    private final boolean known;

    private TrinaryBool(boolean value, boolean isKnown) {
        this.value = value;
        this.known = isKnown;
    }

    public boolean isKnown() {
        return known;
    }

    public boolean getValue() {
        return value;
    }

    public static TrinaryBool TRUE = new TrinaryBool(true, true);
    public static TrinaryBool FALSE = new TrinaryBool(false, true);

    public static TrinaryBool MAYBE_TRUE = new TrinaryBool(true, false);

    public static TrinaryBool MAYBE_FALSE = new TrinaryBool(false, false);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrinaryBool that = (TrinaryBool) o;
        return value == that.value && known == that.known;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, known);
    }

    @Override
    public String toString() {
        return "TrinaryBool{" + "value=" + value + ", known=" + known + '}';
    }
}

// public enum TrinaryBool {
//    TRUE(true, true),
//    FALSE(false, true),
//    MAYBE(false, false);
//
//    private final boolean value;
//    private final boolean shortCircuit;
//
//    public boolean getValue() {
//        return value;
//    }
//
//
//    public boolean canShortCircuit() {
//        return shortCircuit;
//    }
//
//    TrinaryBool(boolean value, boolean shortCircuit) {
//        this.value = value;
//        this.shortCircuit = shortCircuit;
//    }
// }
