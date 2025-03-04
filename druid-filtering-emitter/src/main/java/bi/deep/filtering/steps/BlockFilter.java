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

import static bi.deep.filtering.steps.TrinaryBool.FALSE;
import static bi.deep.filtering.steps.TrinaryBool.MAYBE_TRUE;

import java.util.Set;

public class BlockFilter implements FilterStep {
    private final Set<String> excluded;

    public BlockFilter(Set<String> excluded) {
        this.excluded = excluded;
    }

    @Override
    public TrinaryBool filter(String value) {
        if (excluded.contains(value)) return FALSE;
        else return MAYBE_TRUE;
    }

    @Override
    public boolean isEnabled() {
        return !excluded.isEmpty();
    }
}
