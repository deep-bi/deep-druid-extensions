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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.druid.segment.DimensionSelector;

public class HashcodeRegistry {
    private static final String NULL = "NULL";
    private final Set<Integer> hashcodeSet;

    public HashcodeRegistry(Set<Integer> hashcodeSet) {
        this.hashcodeSet = hashcodeSet;
    }

    public void add(List<DimensionSelector> selectors) {
        hashcodeSet.add(getHashcode(selectors));
    }

    public int size() {
        return hashcodeSet.size();
    }

    public boolean contains(List<DimensionSelector> selectors) {
        return hashcodeSet.contains(getHashcode(selectors));
    }

    private int getHashcode(List<DimensionSelector> selectors) {
        List<Object> objectList = new ArrayList<>();

        selectors.forEach(selector -> objectList.add(findObject(selector)));
        return objectList.hashCode();
    }

    private Object findObject(DimensionSelector selector) {
        return selector.getObject() == null ? NULL : selector.getObject();
    }

    public HashSet<Integer> getRegistry() {
        return new HashSet<>(hashcodeSet);
    }
}
