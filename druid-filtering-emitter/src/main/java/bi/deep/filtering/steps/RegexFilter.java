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

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RegexFilter implements FilterStep {
    private final List<Pattern> patterns;

    public RegexFilter(Set<String> regexes) {
        this.patterns = regexes.stream().map(Pattern::compile).collect(Collectors.toList());
    }

    @Override
    public TrinaryBool filter(String value) {
        if (patterns.stream().anyMatch(pattern -> pattern.matcher(value).matches())) return TRUE;
        else return MAYBE_FALSE;
    }

    @Override
    public boolean isEnabled() {
        return !patterns.isEmpty();
    }
}
