/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.common.verbose.expression;

import com.google.common.collect.AbstractIterator;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;

/**
 * Compiler for boolean expressions with variables.
 */
public class BooleanExpressionCompiler {

    /**
     * Compiles an {@link AST} for a given boolean expression.
     *
     * @param expression the expression string
     * @return the compiled AST
     * @throws LexerException if an error occurs when lexing the expression
     * @throws ParserException if an error occurs when parsing the expression
     */
    public static AST compile(String expression) throws LexerException, ParserException {
        return new Parser(new Lexer(expression)).parse();
    }

    /**
     * Evaluates the value of variables within an expression.
     */
    @FunctionalInterface
    public interface VariableEvaluator {

        /**
         * Evaluates the value of a variable.
         *
         * @param variable the variable
         * @return the result
         */
        boolean eval(String variable);
    }

    /**
     * AST for a boolean expression.
     */
    public interface AST {
        AST ALWAYS_TRUE = e -> true;

        /**
         * Evaluates the AST.
         *
         * @param variableEvaluator the variable evaluator
         * @return the result
         */
        boolean eval(VariableEvaluator variableEvaluator);
    }

    /**
     * Represents a lexing error.
     */
    public static final class LexerException extends RuntimeException {
        LexerException(String message) {
            super(message);
        }

        LexerException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Represents a parsing error.
     */
    public static final class ParserException extends RuntimeException {
        ParserException(String message) {
            super(message);
        }
    }

    /**
     * Parses a list of {@link Token}s into an {@link AST}.
     */
    private static final class Parser {
        /*
        CFG accepted by this parser:

        exp → term {OR term}
        term → factor {AND factor}
        factor → VARIABLE
        factor → NOT factor
        factor → OPEN_BRACKET exp CLOSE_BRACKET
         */

        /** The lexer generating the tokens. */
        private final Lexer lexer;
        /** The current token being parsed */
        private Token currentToken;
        /** The current root of the AST */
        private AST root;

        Parser(Lexer lexer) {
            this.lexer = lexer;
        }

        AST parse() {
            exp();
            return this.root;
        }

        private void exp() {
            term();
            while (this.currentToken == ConstantToken.OR) {
                Or or = new Or();
                or.left = this.root;
                term();
                or.right = this.root;
                this.root = or;
            }
        }

        private void term() {
            factor();
            while (this.currentToken == ConstantToken.AND) {
                And and = new And();
                and.left = this.root;
                factor();
                and.right = this.root;
                this.root = and;
            }
        }

        private void factor() {
            this.currentToken = this.lexer.next();

            if (this.currentToken instanceof VariableToken) {
                Variable variable = new Variable();
                variable.variable = ((VariableToken) this.currentToken).string;
                this.root = variable;
                this.currentToken = this.lexer.next();
            } else if (this.currentToken == ConstantToken.NOT) {
                Not not = new Not();
                factor();
                not.child = this.root;
                this.root = not;
            } else if (this.currentToken == ConstantToken.OPEN_BRACKET) {
                exp();
                if (this.currentToken != ConstantToken.CLOSE_BRACKET) {
                    throw new ParserException("Brackets are not matched");
                }
                this.currentToken = this.lexer.next();
            } else {
                throw new ParserException("Malformed expression");
            }
        }
    }

    /* AST implementations */

    private static final class And implements AST {
        AST left;
        AST right;

        @Override
        public boolean eval(VariableEvaluator variableEvaluator) {
            return this.left.eval(variableEvaluator) && this.right.eval(variableEvaluator);
        }
    }

    private static final class Or implements AST {
        AST left;
        AST right;

        @Override
        public boolean eval(VariableEvaluator variableEvaluator) {
            return this.left.eval(variableEvaluator) || this.right.eval(variableEvaluator);
        }
    }

    private static final class Not implements AST {
        AST child;

        @Override
        public boolean eval(VariableEvaluator variableEvaluator) {
            return !this.child.eval(variableEvaluator);
        }
    }

    private static final class Variable implements AST {
        String variable;

        @Override
        public boolean eval(VariableEvaluator variableEvaluator) {
            return variableEvaluator.eval(this.variable);
        }
    }

    /**
     * Lexes a {@link String} into a list of {@link Token}s.
     */
    private static final class Lexer extends AbstractIterator<Token> {
        private final StreamTokenizer tokenizer;
        private boolean end = false;

        Lexer(String expression) {
            this.tokenizer = new StreamTokenizer(new StringReader(expression));
            this.tokenizer.resetSyntax();
            this.tokenizer.whitespaceChars('\u0000', '\u0020');
            this.tokenizer.wordChars('\u0021', '\u007E');
            "()&|!".chars().forEach(this.tokenizer::ordinaryChar);
        }

        @Override
        protected Token computeNext() {
            if (this.end) {
                return endOfData();
            }
            try {
                int token = this.tokenizer.nextToken();
                switch (token) {
                    case StreamTokenizer.TT_EOF:
                        this.end = true;
                        return ConstantToken.EOF;
                    case StreamTokenizer.TT_WORD:
                        return new VariableToken(this.tokenizer.sval);
                    case '(':
                        return ConstantToken.OPEN_BRACKET;
                    case ')':
                        return ConstantToken.CLOSE_BRACKET;
                    case '&':
                        return ConstantToken.AND;
                    case '|':
                        return ConstantToken.OR;
                    case '!':
                        return ConstantToken.NOT;
                    default:
                        throw new LexerException("Unknown token: " + ((char) token) + "(" + token + ")");
                }
            } catch (IOException e) {
                throw new LexerException(e);
            }
        }
    }

    private interface Token { }

    private enum ConstantToken implements Token {
        OPEN_BRACKET, CLOSE_BRACKET, AND, OR, NOT, EOF
    }

    private static final class VariableToken implements Token {
        final String string;

        VariableToken(String string) {
            this.string = string;
        }
    }

}
