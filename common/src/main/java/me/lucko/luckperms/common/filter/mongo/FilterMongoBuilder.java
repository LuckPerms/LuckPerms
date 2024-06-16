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

import com.mongodb.client.model.Filters;
import me.lucko.luckperms.common.filter.Filter;
import me.lucko.luckperms.common.filter.FilterField;
import me.lucko.luckperms.common.filter.FilterList;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.stream.Collectors;

public abstract class FilterMongoBuilder<T> extends ConstraintMongoBuilder {

    public abstract String mapFieldName(FilterField<T, ?> field);

    public Bson make(Filter<T, ?> filter) {
        return make(filter.constraint(), mapFieldName(filter.field()));
    }

    public Bson make(FilterList.LogicalOperator combineOperator, List<? extends Filter<T, ?>> filters) {
        if (filters.isEmpty()) {
            return Filters.empty();
        }

        List<Bson> bsonFilters = filters.stream().map(this::make).collect(Collectors.toList());
        switch (combineOperator) {
            case AND:
                return Filters.and(bsonFilters);
            case OR:
                return Filters.or(bsonFilters);
            default:
                throw new AssertionError(combineOperator);
        }
    }

    public Bson make(FilterList<T> filters) {
        return make(filters.operator(), filters);
    }

}
