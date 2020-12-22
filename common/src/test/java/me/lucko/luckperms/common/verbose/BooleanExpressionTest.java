package me.lucko.luckperms.common.verbose;

import me.lucko.luckperms.common.verbose.expression.BooleanExpressionCompiler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BooleanExpressionTest {

    private static void test(String expression, boolean expected) {
        assertEquals(
                expected,
                BooleanExpressionCompiler.compile(expression).eval(var -> var.equals("true")),
                expression + " is not " + expected
        );
    }

    @Test
    void testBrackets() {
        test("false & false | true", true);
        test("false & (false | true)", false);
        test("true | false & false", true);
        test("(true | false) & false", false);
        test("(true & ((true | false) & !(true & false)))", true);
    }

}
