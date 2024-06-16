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

import me.lucko.luckperms.common.bulkupdate.action.BulkUpdateAction;
import me.lucko.luckperms.common.filter.FilterList;
import me.lucko.luckperms.common.model.HolderType;
import net.luckperms.api.node.Node;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a query to be applied to a set of data.
 * Queries can either be applied to im-memory sets of data, or converted to SQL syntax to be executed remotely.
 */
public final class BulkUpdate {

    // the data types which this query should apply to
    private final DataType dataType;

    // the action to apply to the data which matches the constraints
    private final BulkUpdateAction action;

    // a set of filters which data must match to be acted upon
    private final FilterList<Node> filters;

    // update statistics of the operation (number of nodes, users and groups affected)
    private final BulkUpdateStatistics statistics = new BulkUpdateStatistics();
    private final boolean trackStatistics;

    public BulkUpdate(DataType dataType, BulkUpdateAction action, FilterList<Node> filters, boolean trackStatistics) {
        this.dataType = dataType;
        this.action = action;
        this.filters = filters;
        this.trackStatistics = trackStatistics;
    }

    /**
     * Check to see if a Node instance satisfies the constraints of this query
     *
     * @param node the node to check
     * @return true if satisfied
     */
    public boolean satisfiesFilters(Node node) {
        return this.filters.evaluate(node);
    }

    /**
     * Applies this query to the given node, and returns the result.
     *
     * @param node the node to apply changes to
     * @return the transformed node, or null if the node should be deleted
     */
    private Node apply(Node node) {
        if (!satisfiesFilters(node)) {
            return node; // make no change
        }

        Node result = this.action.apply(node);

        if (this.trackStatistics && result != node) {
            this.statistics.incrementAffectedNodes();
        }

        return result;
    }

    /**
     * Applies this query to the given set of nodes, and returns the result.
     *
     * @param nodes the input nodes
     * @param holderType the holder type the nodes are from
     * @return the transformed nodes, or null if no change was made
     */
    public @Nullable Set<Node> apply(Set<Node> nodes, HolderType holderType) {
        Set<Node> results = new HashSet<>();
        boolean change = false;

        for (Node node : nodes) {
            Node result = apply(node);
            if (result != node) {
                change = true;
            }
            if (result != null) {
                results.add(result);
            }
        }

        if (!change) {
            return null;
        }

        if (this.trackStatistics) {
            this.statistics.incrementAffected(holderType);
        }

        return results;
    }

    public DataType getDataType() {
        return this.dataType;
    }

    public BulkUpdateAction getAction() {
        return this.action;
    }

    public FilterList<Node> getFilters() {
        return this.filters;
    }

    public boolean isTrackingStatistics() {
        return this.trackStatistics;
    }

    public BulkUpdateStatistics getStatistics() {
        return this.statistics;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof BulkUpdate)) return false;
        final BulkUpdate that = (BulkUpdate) o;

        return this.getDataType() == that.getDataType() &&
                Objects.equals(this.getAction(), that.getAction()) &&
                Objects.equals(this.getFilters(), that.getFilters());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataType(), getAction(), getFilters(), isTrackingStatistics());
    }

    @Override
    public String toString() {
        return "BulkUpdate(" +
                "dataType=" + this.getDataType() + ", " +
                "action=" + this.getAction() + ", " +
                "constraints=" + this.getFilters() + ", " +
                "trackStatistics=" + this.isTrackingStatistics() + ")";
    }
}
