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

/**
 * Keeps track of the number of nodes, users and groups that were affected in a BulkUpdate operation.
 */
public final class BulkUpdateStatistics {

    // the number of users that had their nodes updated/deleted due to the bulk update
    private int affectedUsers = 0;

    // the number of groups that had their nodes updated/deleted
    private int affectedGroups = 0;

    // the total number of affected nodes
    private int affectedNodes = 0;

    public BulkUpdateStatistics() {

    }

    public int getAffectedNodes() {
        return affectedNodes;
    }

    public int getAffectedUsers() {
        return affectedUsers;
    }

    public int getAffectedGroups() {
        return affectedGroups;
    }

    public void incrementAffectedNodes() {
        ++affectedNodes;
    }

    public void incrementAffectedUsers() {
        ++affectedUsers;
    }

    public void incrementAffectedGroups() {
        ++affectedGroups;
    }

    public void incrementAffectedNodesBy(int delta) {
        affectedNodes += delta;
    }

    public void incrementAffectedUsersBy(int delta) {
        affectedUsers += delta;
    }

    public void incrementAffectedGroupsBy(int delta) {
        affectedGroups += delta;
    }
}
