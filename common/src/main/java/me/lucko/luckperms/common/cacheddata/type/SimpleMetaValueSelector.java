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

package me.lucko.luckperms.common.cacheddata.type;

import net.luckperms.api.cacheddata.Result;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.query.meta.MetaValueSelector;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SimpleMetaValueSelector implements MetaValueSelector {
    private final Map<String, Strategy> strategies;
    private final Strategy defaultStrategy;

    public SimpleMetaValueSelector(Map<String, Strategy> strategies, Strategy defaultStrategy) {
        this.strategies = strategies;
        this.defaultStrategy = defaultStrategy;
    }

    @Override
    public @NonNull Result<String, MetaNode> selectValue(@NonNull String key, @NonNull List<? extends Result<String, MetaNode>> values) {
        switch (values.size()) {
            case 0:
                throw new IllegalArgumentException("values is empty");
            case 1:
                return values.get(0);
            default:
                return this.strategies.getOrDefault(key, this.defaultStrategy).select(values);
        }
    }

    public enum Strategy {
        INHERITANCE {
            @Override
            public Result<String, MetaNode> select(List<? extends Result<String, MetaNode>> values) {
                return values.get(0);
            }
        },
        HIGHEST_NUMBER {
            private final DoubleSelectionPredicate selection = (value, current) -> value > current;

            @Override
            public Result<String, MetaNode> select(List<? extends Result<String, MetaNode>> values) {
                return selectNumber(values, this.selection);
            }
        },
        LOWEST_NUMBER {
            private final DoubleSelectionPredicate selection = (value, current) -> value < current;

            @Override
            public Result<String, MetaNode> select(List<? extends Result<String, MetaNode>> values) {
                return selectNumber(values, this.selection);
            }
        };

        public abstract Result<String, MetaNode> select(List<? extends Result<String, MetaNode>> values);

        public static Strategy parse(String s) {
            try {
                return Strategy.valueOf(s.replace('-', '_').toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    @FunctionalInterface
    private interface DoubleSelectionPredicate {
        boolean shouldSelect(double value, double current);
    }

    private static Result<String, MetaNode> selectNumber(List<? extends Result<String, MetaNode>> values, DoubleSelectionPredicate selection) {
        double current = 0;
        Result<String, MetaNode> selected = null;

        for (Result<String, MetaNode> result : values) {
            try {
                double parse = Double.parseDouble(result.result());
                if (selected == null || selection.shouldSelect(parse, current)) {
                    selected = result;
                    current = parse;
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        return selected != null ? selected : Strategy.INHERITANCE.select(values);
    }
}
