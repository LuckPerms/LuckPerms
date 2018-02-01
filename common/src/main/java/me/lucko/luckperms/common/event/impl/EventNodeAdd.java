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
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.PermissionHolder;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.event.node.NodeAddEvent;
import me.lucko.luckperms.common.event.AbstractEvent;

import java.util.Set;

import javax.annotation.Nonnull;

public class EventNodeAdd extends AbstractEvent implements NodeAddEvent {

    private final Node node;
    private final PermissionHolder target;
    private final Set<Node> dataBefore;
    private final Set<Node> dataAfter;

    public EventNodeAdd(Node node, PermissionHolder target, Set<Node> dataBefore, Set<Node> dataAfter) {
        this.node = node;
        this.target = target;
        this.dataBefore = dataBefore;
        this.dataAfter = dataAfter;
    }

    @Override
    public boolean isUser() {
        return this.target instanceof User;
    }

    @Override
    public boolean isGroup() {
        return this.target instanceof Group;
    }

    @Nonnull
    @Override
    public Node getNode() {
        return this.node;
    }

    @Nonnull
    @Override
    public PermissionHolder getTarget() {
        return this.target;
    }

    @Nonnull
    @Override
    public Set<Node> getDataBefore() {
        return this.dataBefore;
    }

    @Nonnull
    @Override
    public Set<Node> getDataAfter() {
        return this.dataAfter;
    }

    @Override
    public String toString() {
        return "NodeAddEvent(" +
                "node=" + this.getNode() + ", " +
                "target=" + this.getTarget() + ", " +
                "dataBefore=" + this.getDataBefore() + ", " +
                "dataAfter=" + this.getDataAfter() + ")";
    }
}
