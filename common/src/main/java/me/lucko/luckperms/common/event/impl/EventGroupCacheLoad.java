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

package me.lucko.luckperms.common.event.impl;

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.caching.GroupData;
import me.lucko.luckperms.api.event.group.GroupCacheLoadEvent;
import me.lucko.luckperms.common.event.AbstractEvent;

import javax.annotation.Nonnull;

public class EventGroupCacheLoad extends AbstractEvent implements GroupCacheLoadEvent {

    private final Group group;
    private final GroupData loadedData;

    public EventGroupCacheLoad(Group group, GroupData loadedData) {
        this.group = group;
        this.loadedData = loadedData;
    }

    @Nonnull
    @Override
    public Group getGroup() {
        return this.group;
    }

    @Nonnull
    @Override
    public GroupData getLoadedData() {
        return this.loadedData;
    }

    @Override
    public String toString() {
        return "EventGroupCacheLoad(group=" + this.getGroup() + ", loadedData=" + this.getLoadedData() + ")";
    }
}
