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

package me.lucko.luckperms.common.filter.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import me.lucko.luckperms.common.filter.Comparison;
import me.lucko.luckperms.common.filter.Constraint;
import me.lucko.luckperms.common.filter.PageParameters;
import org.bson.conversions.Bson;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.regex.Pattern;

public class ConstraintMongoBuilder {
    public static final ConstraintMongoBuilder INSTANCE = new ConstraintMongoBuilder();

    protected ConstraintMongoBuilder() {

    }

    public Object mapConstraintValue(Object value) {
        return value;
    }

    public Bson make(Constraint<?> constraint, String fieldName) {
        Comparison comparison = constraint.comparison();
        Object value = mapConstraintValue(constraint.value());

        switch (comparison) {
            case EQUAL:
                return Filters.eq(fieldName, value);
            case NOT_EQUAL:
                return Filters.ne(fieldName, value);
            case SIMILAR: {
                if (!(value instanceof String)) {
                    throw new IllegalArgumentException("Unable to create SIMILAR comparison for non-string type: " + value.getClass().getName());
                }
                Pattern pattern = Comparison.compilePatternForLikeSyntax((String) value);
                return Filters.regex(fieldName, pattern);
            }
            case NOT_SIMILAR: {
                if (!(value instanceof String)) {
                    throw new IllegalArgumentException("Unable to create NOT_SIMILAR comparison for non-string type: " + value.getClass().getName());
                }
                Pattern pattern = Comparison.compilePatternForLikeSyntax((String) value);
                return Filters.not(Filters.regex(fieldName, pattern));
            }
            default:
                throw new AssertionError(comparison);
        }
    }

    public static <R> FindIterable<R> page(@Nullable PageParameters params, FindIterable<R> iterable) {
        if (params == null) {
            return iterable;
        }

        int pageSize = params.pageSize();
        int pageNumber = params.pageNumber();
        return iterable.limit(pageSize).skip((pageNumber - 1) * pageSize);
    }

}
