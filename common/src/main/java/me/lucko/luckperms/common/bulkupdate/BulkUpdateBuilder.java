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

package me.lucko.luckperms.common.bulkupdate;

import com.google.common.collect.ImmutableList;
import me.lucko.luckperms.common.bulkupdate.action.Action;
import me.lucko.luckperms.common.bulkupdate.query.Query;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Responsible for building a {@link BulkUpdate}
 */
public class BulkUpdateBuilder {

    public static BulkUpdateBuilder create() {
        return new BulkUpdateBuilder();
    }

    // the data type this query should affect
    private DataType dataType = DataType.ALL;

    // the action to apply to the data which matches the constraints
    private Action action = null;

    // should the operation count the number of affected nodes, users and groups
    private boolean trackStatistics = false;

    // a set of constraints which data must match to be acted upon
    private final Set<Query> queries = new LinkedHashSet<>();

    private BulkUpdateBuilder() {
    }

    public BulkUpdateBuilder action(Action action) {
        this.action = action;
        return this;
    }

    public BulkUpdateBuilder dataType(DataType dataType) {
        this.dataType = dataType;
        return this;
    }

    public BulkUpdateBuilder trackStatistics(boolean trackStatistics) {
        this.trackStatistics = trackStatistics;
        return this;
    }

    public BulkUpdateBuilder query(Query query) {
        this.queries.add(query);
        return this;
    }

    public BulkUpdate build() {
        if (this.action == null) {
            throw new IllegalStateException("no action specified");
        }

        return new BulkUpdate(this.dataType, this.action, ImmutableList.copyOf(this.queries), this.trackStatistics);
    }

    @Override
    public String toString() {
        return "BulkUpdateBuilder(" +
                "dataType=" + this.dataType + ", " +
                "action=" + this.action + ", " +
                "constraints=" + this.queries + ", " +
                "trackStatistics=" + this.trackStatistics + ")";
    }
}
