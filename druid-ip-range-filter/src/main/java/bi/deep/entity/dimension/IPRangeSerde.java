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
package bi.deep.entity.dimension;

import bi.deep.guice.IPRangeDimensionModule;
import it.unimi.dsi.fastutil.Hash;
import java.util.Objects;
import org.apache.druid.segment.column.ColumnType;
import org.apache.druid.segment.column.ObjectStrategyComplexTypeStrategy;
import org.apache.druid.segment.column.TypeStrategy;
import org.apache.druid.segment.data.ObjectStrategy;
import org.apache.druid.segment.serde.ComplexColumnSerializer;
import org.apache.druid.segment.serde.ComplexMetricExtractor;
import org.apache.druid.segment.serde.ComplexMetricSerde;
import org.apache.druid.segment.writeout.SegmentWriteOutMedium;

public class IPRangeSerde extends ComplexMetricSerde {
    @Override
    public String getTypeName() {
        return IPRangeDimensionModule.TYPE_NAME;
    }

    @Override
    public ComplexMetricExtractor getExtractor() {
        return new IPRangeComplexMetricExtractor();
    }

    @Override
    public ObjectStrategy<IPRange> getObjectStrategy() {
        return IPRangeObjectStrategy.INSTANCE;
    }

    @Override
    public ComplexColumnSerializer getSerializer(SegmentWriteOutMedium segmentWriteOutMedium, String column) {
        return ComplexColumnSerializer.create(segmentWriteOutMedium, column, getObjectStrategy());
    }

    @Override
    public TypeStrategy<IPRange> getTypeStrategy() {
        return new ObjectStrategyComplexTypeStrategy<>(
                getObjectStrategy(), ColumnType.ofComplex(getTypeName()), new Hash.Strategy<>() {
                    @Override
                    public int hashCode(IPRange ipRange) {
                        return Objects.hashCode(ipRange);
                    }

                    @Override
                    public boolean equals(IPRange a, IPRange b) {
                        return Objects.equals(a, b);
                    }
                });
    }
}
