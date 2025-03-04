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

import bi.deep.filtering.EventFilter;
import com.fasterxml.jackson.databind.Module;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.Collections;
import java.util.List;
import org.apache.druid.guice.JsonConfigProvider;
import org.apache.druid.guice.ManageLifecycle;
import org.apache.druid.initialization.DruidModule;
import org.apache.druid.java.util.common.logger.Logger;
import org.apache.druid.java.util.emitter.core.Emitter;

public class FilteringEmitterModule implements DruidModule {
    private static final Logger log = new Logger(FilteringEmitterModule.class);

    @Override
    public List<? extends Module> getJacksonModules() {
        return Collections.emptyList();
    }

    @Override
    public void configure(Binder binder) {
        JsonConfigProvider.bind(binder, "druid.emitter.filtering", FilteringEmitterConfig.class);
    }

    @Provides
    @ManageLifecycle
    @Named("filtering")
    public Emitter getEmitter(FilteringEmitterConfig config, final Injector injector) {
        config.validate();
        log.info("Creating Filtering Emitter with %s", config.getEmitter());
        Emitter inner = injector.getInstance(Key.get(Emitter.class, Names.named(config.getEmitter())));

        return new FilteringEmitter(inner, EventFilter.of(config));
    }
}
