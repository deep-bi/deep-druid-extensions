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
package bi.deep.aggregation.match.sql;

import bi.deep.aggregation.match.aggregator.ValueMatchAggregatorFactory;
import com.google.common.collect.Iterables;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.sql.SqlAggFunction;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.type.SqlTypeFamily;
import org.apache.druid.segment.column.ColumnType;
import org.apache.druid.sql.calcite.aggregation.Aggregation;
import org.apache.druid.sql.calcite.aggregation.Aggregations;
import org.apache.druid.sql.calcite.aggregation.SqlAggregator;
import org.apache.druid.sql.calcite.expression.DruidExpression;
import org.apache.druid.sql.calcite.expression.OperatorConversions;
import org.apache.druid.sql.calcite.planner.Calcites;
import org.apache.druid.sql.calcite.planner.PlannerContext;
import org.apache.druid.sql.calcite.rel.InputAccessor;
import org.apache.druid.sql.calcite.rel.VirtualColumnRegistry;

public class ValueMatchSqlAggregator implements SqlAggregator {
    private static final String NAME = "SAME_OR_NULL";
    private static final SqlAggFunction FUNCTION_INSTANCE = OperatorConversions.aggregatorBuilder(NAME)
            .operandNames("column")
            .operandTypes(SqlTypeFamily.ANY)
            .returnTypeInference(Calcites.complexReturnTypeWithNullability(ColumnType.UNKNOWN_COMPLEX, true))
            .requiredOperandCount(1)
            .functionCategory(SqlFunctionCategory.USER_DEFINED_FUNCTION)
            .build();

    @Override
    public SqlAggFunction calciteFunction() {
        return FUNCTION_INSTANCE;
    }

    @Nullable
    @Override
    public Aggregation toDruidAggregation(
            final PlannerContext plannerContext,
            final VirtualColumnRegistry virtualColumnRegistry,
            final String name,
            final AggregateCall aggregateCall,
            final InputAccessor inputAccessor,
            final List<Aggregation> existingAggregations,
            final boolean finalizeAggregations) {
        final List<DruidExpression> arguments =
                Aggregations.getArgumentsForSimpleAggregator(plannerContext, aggregateCall, inputAccessor);

        if (arguments == null) {
            return null;
        }

        final DruidExpression arg = Iterables.getOnlyElement(arguments);
        final String fieldName = arg.isDirectColumnAccess()
                ? arg.getDirectColumn()
                : virtualColumnRegistry.getOrCreateVirtualColumnForExpression(arg, aggregateCall.getType());
        final ColumnType valueType = Calcites.getColumnTypeForRelDataType(aggregateCall.getType());

        return Aggregation.create(new ValueMatchAggregatorFactory(name, fieldName, null, valueType));
    }
}
