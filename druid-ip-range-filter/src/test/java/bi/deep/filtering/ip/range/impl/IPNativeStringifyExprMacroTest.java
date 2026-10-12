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
package bi.deep.filtering.ip.range.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bi.deep.entity.dimension.IPRange;
import bi.deep.entity.dimension.IPRangeArray;
import com.google.common.collect.ImmutableList;
import org.apache.druid.math.expr.Expr;
import org.apache.druid.math.expr.ExprEval;
import org.apache.druid.math.expr.ExprMacroTable;
import org.apache.druid.math.expr.ExpressionType;
import org.apache.druid.math.expr.InputBindings;
import org.apache.druid.math.expr.Parser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IPNativeStringifyExprMacroTest {
    private static final ExprMacroTable MACRO_TABLE =
            new ExprMacroTable(ImmutableList.of(new IPNativeStringifyExprMacro()));

    @Test
    void testWithNativeIPRange() {
        Expr.ObjectBinding inputBindings = InputBindings.forInputSupplier(
                "ipAddress", ExpressionType.UNKNOWN_COMPLEX, () -> IPRange.from("255.255.255.255"));
        Expr expr = Parser.parse("ip_native_stringify(ipAddress)", MACRO_TABLE);
        ExprEval<?> eval = expr.eval(inputBindings);

        assertEquals(ExpressionType.STRING_ARRAY, eval.type());

        Object[] value = eval.asArray();
        assertEquals(1, value.length);
        assertEquals("255.255.255.255", value[0]);
    }

    @Test
    void testNativeIPRangeWithUpperAndLower() {
        Expr.ObjectBinding inputBindings = InputBindings.forInputSupplier(
                "ipAddress", ExpressionType.UNKNOWN_COMPLEX, () -> IPRange.from("0.0.0.0 -> 255.255.255.255"));
        Expr expr = Parser.parse("ip_native_stringify(ipAddress)", MACRO_TABLE);
        ExprEval<?> eval = expr.eval(inputBindings);

        assertEquals(ExpressionType.STRING_ARRAY, eval.type());

        Object[] value = eval.asArray();
        assertEquals(1, value.length);
        assertEquals("0.0.0.0-255.255.255.255", value[0]);
    }

    @Test
    void testWithNativeIPRangeArray() {
        Expr.ObjectBinding inputBindings = InputBindings.forInputSupplier(
                "ipAddress",
                ExpressionType.UNKNOWN_COMPLEX,
                () -> IPRangeArray.fromArray(ImmutableList.of("255.255.255.255")));
        Expr expr = Parser.parse("ip_native_stringify(ipAddress)", MACRO_TABLE);
        ExprEval<?> eval = expr.eval(inputBindings);

        assertEquals(ExpressionType.STRING_ARRAY, eval.type());

        Object[] value = eval.asArray();
        assertEquals(1, value.length);
        assertEquals("255.255.255.255", value[0]);
    }

    @Test
    void testNativeIPRangeArrayWithUpperAndLower() {
        Expr.ObjectBinding inputBindings = InputBindings.forInputSupplier(
                "ipAddress",
                ExpressionType.UNKNOWN_COMPLEX,
                () -> IPRangeArray.fromArray(ImmutableList.of("0.0.0.0 -> 255.255.255.255", "10.0.0.0/24")));
        Expr expr = Parser.parse("ip_native_stringify(ipAddress)", MACRO_TABLE);
        ExprEval<?> eval = expr.eval(inputBindings);

        assertEquals(ExpressionType.STRING_ARRAY, eval.type());

        Object[] value = eval.asArray();
        assertEquals(2, value.length);
        assertEquals("0.0.0.0-255.255.255.255", value[0]);
        assertEquals("10.0.0.0-10.0.0.255", value[1]);
    }

    @Test
    void testWithNull() {
        Expr.ObjectBinding inputBindings =
                InputBindings.forInputSupplier("ipAddress", ExpressionType.UNKNOWN_COMPLEX, () -> null);
        Expr expr = Parser.parse("ip_native_stringify(ipAddress)", MACRO_TABLE);
        ExprEval<?> eval = expr.eval(inputBindings);

        assertEquals(ExpressionType.STRING_ARRAY, eval.type());

        Object[] value = eval.asArray();
        Assertions.assertNull(value);
    }
}
