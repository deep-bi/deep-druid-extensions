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

import static bi.deep.filtering.steps.TrinaryBool.*;

import java.util.Set;

public class AllowedFilter implements FilterStep {
    private final Set<String> allowed;

    public AllowedFilter(Set<String> allowed) {
        this.allowed = allowed;
    }

    @Override
    public TrinaryBool filter(String value) {
        if (allowed.contains(value)) return TRUE;
        else return MAYBE_FALSE;
    }

    @Override
    public boolean isEnabled() {
        return !allowed.isEmpty();
    }
}
